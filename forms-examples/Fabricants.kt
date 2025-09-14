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
import org.kopi.galite.visual.domain.INT
import org.kopi.galite.visual.domain.STRING
import org.kopi.galite.visual.dsl.common.Mode
import org.kopi.galite.visual.dsl.form.Access
import org.kopi.galite.visual.dsl.form.Block
import org.kopi.galite.visual.dsl.form.FieldOption
import org.kopi.galite.visual.form.VBlock
import org.kopi.galite.visual.form.VConstants

import com.progmag.inventaire.base.DefaultDictionaryForm
import com.progmag.inventaire.dbschema.manufacturers

class Fabricants : DefaultDictionaryForm(title = "Fabricants")  {

  val fabricant = insertBlock(Fabricant())

  inner class Fabricant : Block("Fabricant", 1, 1000) {
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
    val m = table(manufacturers)

    // Champs du block
    val id = hidden(INT(11)) {
      columns(m.id)
    }
    val nom = mustFill(STRING(100), at(1, 1)) {
      label = "Nom"
      help = "Le nom du fabricant"
      columns(m.name) {
        priority = 2
      }
    }
    val url = visit(STRING(100), at(2, 1)) {
      label = "URL"
      help = "Le site du fabricant"
      columns(m.url) {
        priority = 1
      }
    }
    val urlSupport = visit(STRING(100), at(3, 1)) {
      label = "URL du support"
      help = "L'URL du site de support du fabricant"
      columns(m.support_url)
    }
    val téléphoneSupport = visit(STRING(100), at(4, 1)) {
      label = "Téléphone du support"
      help = "Le numéro de téléphone du support du fabricant"
      columns(m.support_phone)
    }
    val supportEmail = visit(STRING(100), at(5, 1)) {
      label = "E-mail du support"
      help = "L'adresse e-mail du support du fabricant"
      columns(m.support_email)
    }
    val idUtilisateur = hidden(INT(11)) {
      columns(m.user_id)
    }
    val utilisateur = skipped(STRING(50), at(7, 1..2)) {
      label = "Utilisateur"
      options(FieldOption.TRANSIENT)
    }
    val crééLe = skipped(DATETIME, at(8, 1)) {
      label = "Créé le"
      help = "la date de création du fabricant"
      columns(m.created_at)
    }
    val modifiéLe = skipped(DATETIME, at(9, 1)) {
      label = "Modifié le"
      help = "La date de modification du fabricant"
      columns(m.updated_at)
    }
    val suppriméLe  = skipped(DATETIME, at(10, 1)) {
      label = "Supprimé le"
      help = "La date de suppression du fabricant"
      columns(m.deleted_at)
    }
  }

  /**
   * Mise à jour colonne SupprimeLe
   */
  private fun supprimer(b: VBlock) {
    b.validate()
    transaction {
      manufacturers.update({ manufacturers.id eq this@Fabricants.fabricant.id.value!! }) {
        it[user_id] = getUserID()
        it[updated_at] = LocalDateTime.now()
        it[deleted_at] = LocalDateTime.now()
      }
      b.form.reset()
    }
  }
}
