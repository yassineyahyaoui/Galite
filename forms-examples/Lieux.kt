// ----------------------------------------------------------------------
// Copyright (c) 2013-2024 kopiLeft Services SARL, Tunisie
// Copyright (c) 2018-2024 ProGmag SAS, France
// ----------------------------------------------------------------------
// All rights reserved - tous droits réservés.
// ----------------------------------------------------------------------

package com.progmag.inventaire

import java.time.LocalDateTime

import org.jetbrains.exposed.sql.update

import org.kopi.galite.visual.database.transaction
import org.kopi.galite.visual.domain.DATETIME
import org.kopi.galite.visual.domain.Fixed
import org.kopi.galite.visual.domain.INT
import org.kopi.galite.visual.domain.STRING
import org.kopi.galite.visual.dsl.common.Mode
import org.kopi.galite.visual.dsl.form.Access
import org.kopi.galite.visual.dsl.form.Block
import org.kopi.galite.visual.dsl.form.FieldOption
import org.kopi.galite.visual.form.VBlock
import org.kopi.galite.visual.form.VConstants

import com.progmag.inventaire.base.DefaultDictionaryForm
import com.progmag.inventaire.dbschema.locations

class Lieux : DefaultDictionaryForm(title = "Lieux") {

  val lieu = page("Lieu").insertBlock(Lieu())

  inner class Lieu : Block("Lieu", 1, 1000) {
    init {
      blockVisibility(Access.VISIT, Mode.QUERY)
      // Défintion des commandes du block
      breakCommand
      command(item = serialQuery, Mode.QUERY) { serialQuery() }
      command(item = searchOperator, Mode.QUERY) { searchOperator() }
      command(item = menuQuery, Mode.QUERY) { recursiveQuery() }
      command(item = delete, Mode.UPDATE) { supprimer(block) }
      command(item = insertMode, Mode.QUERY) { insertMode() }
      command(item = save, Mode.INSERT, Mode.UPDATE) { saveBlock() }
      command(item = dynamicReport) { createDynamicReport() }

      // Initialisation des triggers
      trigger(PREINS) {
        crééLe.value = LocalDateTime.now()
        modifiéLe.value = LocalDateTime.now()
        idUtilisateur.value = getUserID()
      }
      trigger(PREUPD) {
        modifiéLe.value = LocalDateTime.now()
        idUtilisateur.value = getUserID()
      }
      trigger(PREQRY) {
        // Forcer la condition : deleted_at IS NULL
        suppriméLe.vField.setSearchOperator(VConstants.SOP_LE)
      }
      trigger(POSTQRY, POSTINS, POSTUPD) {
        idUtilisateur.value?.let { utilisateur.value = Utils.chargerUtilisateur(it) }
      }
    }

    // Définition des alias des tables du block
    val l = table(locations)

    // Champs du block
    val id = hidden(INT(11)) {
      columns(l.id)
    }
    val nom = mustFill(STRING(100), at(1, 1..2)) {
      label = "Nom"
      help = "Le nom du lieu"
      columns(l.name) {
        priority = 8
      }
      options(FieldOption.QUERY_UPPER)
    }
    val devise = visit(STRING(10), at(2, 1)) {
      label = "Devise"
      help = " Devise de l'emplacement."
      columns(l.currency) {
        priority = 5
      }
    }
    val adresse = visit(STRING(38, 5, 1, Fixed.OFF), at(3, 1)) {
      label = "Adresse"
      help = "Adresse."
      columns(l.address)
    }
    val adresse2 = visit(STRING(38, 5, 1, Fixed.OFF), at(3, 2)) {
      label = "Complément adresse"
      help = "Complément d'adresse."
      columns(l.address2)
    }
    val ville = visit(STRING(100), at(4, 1..2)) {
      label = "Ville"
      columns(l.city) {
        priority = 2
      }
      options(FieldOption.QUERY_UPPER)
    }
    val état = visit(STRING(100), at(5, 1..2)) {
      label = "État"
      columns(l.state) {
        priority = 3
      }
      options(FieldOption.QUERY_UPPER)
    }
    val pays = visit(STRING(100), at(6, 1..2)) {
      label = "Pays"
      columns(l.country) {
        priority = 4
      }
      options(FieldOption.QUERY_UPPER)
    }
    val codePostal = visit(STRING(10), at(7, 1)) {
      label = "Code postal"
      columns(l.zip) {
        priority = 1
      }
    }
    val idUtilisateur = hidden(INT(11)) {
      columns(l.user_id)
      trigger(PREINS, PREUPD) {
        value = getUserID()
      }
    }
    val utilisateur = skipped(STRING(50), at(9, 1..2)) {
      label = "Utilisateur"
      options(FieldOption.TRANSIENT)
    }
    val crééLe = skipped(DATETIME, at(10, 1)) {
      label = "Créé le"
      help = "la date de création du lieu"
      columns(l.created_at)
    }
    val modifiéLe = skipped(DATETIME, at(11, 1)) {
      label = "Modifié le"
      help = "La date de modification du lieu"
      columns(l.updated_at)
    }
    val suppriméLe = skipped(DATETIME, at(12, 1)) {
      label = "Supprimé le"
      help = "La date de suppression du lieu"
      columns(l.deleted_at)
    }

    /**
     * Supprimer un enregistrement : Mettre à jour le champ [locations.deleted_at]
     */
    private fun supprimer(b: VBlock) {
      b.validate()
      transaction {
        locations.update({ locations.id eq this@Lieu.id.value!! }) {
          it[user_id] = getUserID()
          it[updated_at] = LocalDateTime.now()
          it[deleted_at] = LocalDateTime.now()
        }
      }
      b.form.reset()
    }
  }
}
