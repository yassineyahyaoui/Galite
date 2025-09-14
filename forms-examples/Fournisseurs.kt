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
import com.progmag.inventaire.dbschema.suppliers

class Fournisseurs : DefaultDictionaryForm(title = "Fournisseurs")  {

  val fournisseur = insertBlock(Fournisseur())

  inner class Fournisseur : Block("Fournisseur", 1, 1000) {
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
      trigger(POSTINS, POSTUPD, POSTQRY) {
        idUtilisateur.value?.let { utilisateur.value = Utils.chargerUtilisateur(it) }
      }
    }

    // Définition des alias des tables du block
    val s = table(suppliers)

    // Champs du block
    val id = hidden(INT(11)) {
      columns(s.id)
    }
    val nom = mustFill(STRING(100), at(1, 1..2)) {
      label = "Nom"
      help = "Le nom du fournisseur"
      columns(s.name) {
        priority = 6
      }
    }
    val adresse = visit(STRING(50), at(2, 1)) {
      label = "Adresse"
      help = "Adresse du fournisseur"
      columns(s.address) {
        priority = 5
      }
    }
    val adresse2 = visit(STRING(50), at(3, 1)) {
      label = "Complément d'adresse"
      columns(s.address2)
    }
    val ville = visit(STRING(100), at(4, 1..2)) {
      label = "Ville"
      help = "Ville du fournisseur"
      columns(s.city) {
        priority = 2
      }
    }
    val état = visit(STRING(32), at(5, 1)) {
      label = "État"
      help = "État du fournisseur"
      columns(s.state) {
        priority = 3
      }
    }
    val pays = visit(STRING(2), at(6, 1)) {
      label = "Pays"
      help = "Pays du fournisseur"
      columns(s.country) {
        priority = 4
      }
    }
    val codePostale = visit(STRING(10), at(7, 1)) {
      label = "Code postal"
      help = "Code postal du fournisseur"
      columns(s.zip) {
        priority = 1
      }
    }
    val contact = visit(STRING(100), at(8, 1..2)) {
      label = "Nom du Contact"
      help = "Contact du fournisseur"
      columns(s.contact)
    }
    val email = visit(STRING(100), at(9, 1..2)) {
      label = "E-mail"
      help = "E-mail du fournisseur"
      columns(s.email)
    }
    val url = visit(STRING(50, 5, 1, Fixed.OFF), at(10, 1)) {
      label = "URL"
      help = "URL du fournisseur"
      columns(s.url)
    }
    val téléphone = visit(STRING(35), at(11, 1)) {
      label = "Téléphone"
      help = "Téléphone du fournisseur"
      columns(s.phone)
    }
    val fax = visit(STRING(35), at(12, 1)) {
      label = "Fax"
      help = "Fax du fournisseur"
      columns(s.fax)
    }
    val remarques = visit(STRING(95, 2, 1, Fixed.OFF), at(13, 1..2)) {
      label = "Remarques"
      help = "Remarques concernant le fournisseur"
      columns(s.notes)
    }
    val idUtilisateur = hidden(INT(11)) {
      columns(s.user_id)
    }
    val utilisateur = skipped(STRING(50), at(15, 1)) {
      label = "Utilisateur"
      options(FieldOption.TRANSIENT)
    }
    val crééLe = skipped(DATETIME, at(16, 1)) {
      label = "Créé le"
      help = "La date de création du fournisseur"
      columns(s.created_at)
    }
    val modifiéLe = skipped(DATETIME, at(17, 1)) {
      label = "Modifié le"
      help = "La date de modification du fournisseur"
      columns(s.updated_at)
    }
    val suppriméLe = skipped(DATETIME, at(18, 1)) {
      label = "Supprimé le"
      help = "La date de suppression du fournisseur"
      columns(s.deleted_at)
    }

    /**
     * Mise à jour colonne SupprimeLe
     */
    private fun supprimer(b: VBlock) {
      b.validate()
      transaction {
        suppliers.update({ suppliers.id eq this@Fournisseur.id.value!! }) {
          it[user_id] = getUserID()
          it[updated_at] = LocalDateTime.now()
          it[deleted_at] = LocalDateTime.now()
        }
      }
      b.form.reset()
    }
  }
}
