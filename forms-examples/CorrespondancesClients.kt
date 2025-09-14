// ----------------------------------------------------------------------
// Copyright (c) 2013-2025 kopiLeft Services SARL, Tunisie
// Copyright (c) 2018-2025 ProGmag SAS, France
// ----------------------------------------------------------------------
// All rights reserved - tous droits réservés.
// ----------------------------------------------------------------------

package com.progmag.inventaire

import java.util.Locale

import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.notExists
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsert

import org.kopi.galite.visual.database.transaction
import org.kopi.galite.visual.domain.BOOL
import org.kopi.galite.visual.domain.Fixed
import org.kopi.galite.visual.domain.INT
import org.kopi.galite.visual.domain.STRING
import org.kopi.galite.visual.dsl.common.Icon
import org.kopi.galite.visual.dsl.form.Block
import org.kopi.galite.visual.dsl.form.BlockOption
import org.kopi.galite.visual.dsl.form.Border
import org.kopi.galite.visual.dsl.form.Form
import org.kopi.galite.visual.dsl.form.Key
import org.kopi.galite.visual.form.VConstants

import com.progmag.inventaire.base.Clients
import com.progmag.inventaire.dbschema.companies
import com.progmag.inventaire.dbschema.mappingClientsSnipePDV
import com.progmag.pdv.dbschema.pdv.clients

class CorrespondancesClients : Form(title = "Correspondances clients", locale = Locale.FRANCE)  {
  init {
    insertMenus()
    insertCommands()
  }

  // Définition d'un nouvel acteur : Valider les clients saisis pour association des données.
  val valider by lazy {
    actor(menu = actionMenu, label = "Valider", help = "Valider l'association des clients") {
      key = Key.F11
      icon = Icon.VALIDATE
    }
  }

  // Définition d'un nouvel acteur : Annuler les associations PDV / SNIPE-IT
  val annuler by lazy {
    actor(menu = actionMenu, label = "Annuler", help = "Annuler les associations des lignes sélectionnées") {
      key = Key.SHIFT_F11
      icon = Icon.UNDO
    }
  }

  val associationClients = insertBlock(AssociationClients())
  val affichageClientsAssociés = insertBlock(AffichageClientsAssociés())

  // ----------------------------------------------------------------------
  // 1er block : Permet l'association des clients PDV à leurs équivalences
  // dans SNIPE-IT
  // ----------------------------------------------------------------------

  inner class AssociationClients : Block("Association clients PDV / SNIPE-IT ", 10000, 15) {
    init {
      options(BlockOption.NODETAIL)
      border = Border.LINE

      // Définition des commandes du block
      breakCommand
      command(item = menuQuery) { clear() ; load() }
      command(item = valider) {
        associerClients()
        associationClients.clear()
        associationClients.load()
        affichageClientsAssociés.clear()
        affichageClientsAssociés.load()
      }

      trigger(INIT) {
        associationClients.load()
        affichageClientsAssociés.load()
      }
    }

    // Définition des tables du block
    val s = table(companies)
    val p = table(clients)

    // Définition des champs du block
    val id = hidden(INT(11)) {
      columns(s.id)
    }
    val clientSnipe = skipped(STRING(95, 2, 1, Fixed.OFF), at(1)) {
      label = "Client (SNIPE-IT)"
      help = "Le nom du client comme renseigné dans le module SNIPE-IT"
      columns(s.name) {
        priority = 9 // Pour ordonner l'affichage par ce champ
      }
    }
    val clientPDV = visit(Clients, at(1)) {
      label = "Client (PDV)"
      help = "Le code du client dans PDV"
      columns(p.client)
    }

    /**
     * Chargement des enregistrements de la table [companies] de SNIPE-IT, non liés aux clients de PDV.
     */
    override fun load() {
      transaction {
        companies.slice(companies.id, companies.name).select {
          notExists(
            // Exclure les enregistrements de la table [mapping_clients_snipe_pdv],
            // dont l'affectation au client PDV est réalisée : mapping_clients_snipe_pdv.pdv IS NOT NULL
            mappingClientsSnipePDV.slice(mappingClientsSnipePDV.snipe).select {
              (mappingClientsSnipePDV.snipe eq companies.name) and (mappingClientsSnipePDV.pdv.isNotNull())
            }
          )
        }.orderBy(companies.name).forEachIndexed { index, it ->
          // Remplir les lignes du block
          this@AssociationClients.id[index] = it[companies.id]
          clientSnipe[index] = it[companies.name]
        }
      }
    }

    /**
     * Associer les clients PDV renseignés à leurs équivalents de SNIPE-IT.
     */
    fun associerClients() {
      transaction {
        (0 until block.bufferSize).filter { i ->
          isRecordFilled(i) && !clientPDV[i].isNullOrBlank()
        }.forEach { i ->
          // Pour chaque ligne où le client PDV est renseigné, insérer ou mettre à jour un enregistrement
          // à la table mapping_clients_snipe_pdv
          mappingClientsSnipePDV.upsert(mappingClientsSnipePDV.snipe) {
            it[mappingClientsSnipePDV.snipe] = clientSnipe[i]!!
            it[mappingClientsSnipePDV.pdv] = clientPDV[i]
          }
        }
      }
    }
  }

  // ----------------------------------------------------------------------
  // 2ème block : Permet l'affichage des clients PDV associés à leurs
  // équivalents dans SNIPE-IT, et l'annulation de ces associations
  // en cas d'erreur détectée.
  // ----------------------------------------------------------------------

  inner class AffichageClientsAssociés : Block("Clients associés", 10000, 10) {
    init {
      options(BlockOption.NODETAIL)
      border = Border.LINE

      // Définition des commandes du block
      command(item = annuler) {
        annulerAssociationClients()
        associationClients.clear()
        associationClients.load()
        affichageClientsAssociés.clear()
        affichageClientsAssociés.load()
      }
      trigger(PREQRY) {
        // Forcer la condition : m.pdv IS NOT NULL
        clientPDV.vField.setSearchOperator(VConstants.SOP_NE)
      }
    }

    // Définition des tables du block
    val c = table(companies)
    val m = table(mappingClientsSnipePDV)

    val id = hidden(INT(11)) {
      columns(c.id)
    }

    // Définition des champs du block
    val clientSnipe = skipped(STRING(95, 2, 1, Fixed.OFF), at(1)) {
      label = "Client (SNIPE-IT)"
      help = "Le nom du client comme renseigné dans le module SNIPE-IT"
      columns(m.snipe, c.name) {
        priority = 9
      }
      trigger(POSTCHG) {
        block.fetchLookupFirst(vField)
      }
    }
    val clientPDV = skipped(Clients, at(1)) {
      label = "Client (PDV)"
      help = "Le code du client associé dans PDV"
      columns(m.pdv)
    }

    val annulation = visit(BOOL, at(1)) {
      label = "Annulation"
      help = "Annuler l'association de ce client ?"
    }

    /**
     * Charger le contenu de la table mapping_clients_snipe_pdv dans une transaction
     */
    override fun load() {
      transaction {
        super.load()
      }
    }

    /**
     * Annuler l'association des données clients entre SNIPE-IT et PDV
     * des lignes du block où le champ booléen [annulation] est coché.
     */
    fun annulerAssociationClients() {
      transaction {
        (0 until block.bufferSize).filter { i ->
          isRecordFilled(i) && annulation[i] != null && annulation[i]!!
        }.forEach { i ->
          // Vider le champ mapping_clients_snipe_pdv.pdv
          mappingClientsSnipePDV.update({ mappingClientsSnipePDV.snipe eq clientSnipe[i]!! }) {
            it[pdv] = null
          }
        }
      }
    }
  }
}
