// ----------------------------------------------------------------------
// Copyright (c) 2013-2024 kopiLeft Services SARL, Tunisie
// Copyright (c) 2018-2024 ProGmag SAS, France
// ----------------------------------------------------------------------
// All rights reserved - tous droits réservés.
// ----------------------------------------------------------------------

package com.progmag.inventaire

import java.time.LocalDateTime

import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

import org.kopi.galite.visual.domain.BOOL
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
import com.progmag.inventaire.base.TypeCategorie
import com.progmag.inventaire.dbschema.categories

class Categories : DefaultDictionaryForm(title = "Categories") {

  val catégorie = insertBlock(Categorie())

  inner class Categorie : Block("Catégorie", 1, 1000) {
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
        crééeLe.value = LocalDateTime.now()
        modifiéeLe.value = LocalDateTime.now()
        idUtilisateur.value = getUserID()
      }
      trigger(PREUPD) {
        modifiéeLe.value = LocalDateTime.now()
        idUtilisateur.value = getUserID()
      }
      trigger(PREQRY) {
        // Forcer la condition : deleted_at IS NULL
        suppriméeLe.vField.setSearchOperator(VConstants.SOP_LE)
      }
      trigger(POSTQRY, POSTINS, POSTUPD) {
        idUtilisateur.value?.let { utilisateur.value = Utils.chargerUtilisateur(it) }
      }
    }

    // Définition des alias des tables du block
    val c = table(categories)

    // Champs du block
    val id = hidden(INT(11)) {
      columns(c.id)
    }
    val nom = mustFill(STRING(38, 5, 1, Fixed.OFF), at(1, 1)) {
      label = "Nom"
      help = "Le nom de la catégorie"
      columns(c.name) {
        priority = 2
      }
    }
    val type = visit(TypeCategorie, at(2, 1)) {
      label = "Type"
      help = "Le type de la catégorie"
      columns(c.category_type) {
        priority = 1
      }
    }
    val idUtilisateur = hidden(INT(11)) {
      columns(c.user_id)
    }
    val acceptationRequise = mustFill(BOOL, at(3, 1)) {
      label = "Acceptation des actifs requise"
      help = "L'utilisateur doit confirmer qu'il accepte les actifs de cette catégorie. "
      columns(c.require_acceptance)
      trigger(DEFAULT) { value = false }
    }
    val envoiMail = mustFill(BOOL, at(4, 1)) {
      label = "Information par mail"
      help = "Envoyer un courriel à l'utilisateur lors de l'association/dissociation."
      columns(c.checkin_email)
      trigger(DEFAULT) { value = false }
    }
    val utilisateur = skipped(STRING(50), at(6, 1..2)) {
      label = "Utilisateur"
      options(FieldOption.TRANSIENT)
    }
    val crééeLe = skipped(DATETIME, at(7, 1)) {
      label = "Créée le"
      help = "la date de création de la catégorie"
      columns(c.created_at)
    }
    val modifiéeLe = skipped(DATETIME, at(8, 1)) {
      label = "Modifiée le"
      help = "La date de modification de la catégorie"
      columns(c.updated_at)
    }
    val suppriméeLe = skipped(DATETIME, at(9, 1)) {
      label = "Supprimée le"
      help = "La date de suppression de la catégorie"
      columns(c.deleted_at)
    }

    /**
     * Supprimer un enregistrement : Mettre à jour le champ [categories.deleted_at]
     */
    private fun supprimer(b: VBlock) {
      b.validate()
      transaction {
        categories.update({ categories.id eq this@Categorie.id.value!! }) {
          it[user_id] = getUserID()
          it[updated_at] = LocalDateTime.now()
          it[deleted_at] = LocalDateTime.now()
        }
      }
      b.form.reset()
    }
  }
}
