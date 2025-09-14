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
import org.kopi.galite.visual.domain.IMAGE
import org.kopi.galite.visual.domain.INT
import org.kopi.galite.visual.domain.STRING
import org.kopi.galite.visual.dsl.common.Mode
import org.kopi.galite.visual.dsl.form.Access
import org.kopi.galite.visual.dsl.form.Block
import org.kopi.galite.visual.dsl.form.FieldOption
import org.kopi.galite.visual.form.VBlock
import org.kopi.galite.visual.form.VConstants

import com.progmag.inventaire.base.Fabricants
import com.progmag.inventaire.base.Categories
import com.progmag.inventaire.base.ChampsSpecifiques
import com.progmag.inventaire.base.DefaultDictionaryForm
import com.progmag.inventaire.dbschema.models

class Modeles : DefaultDictionaryForm(title = "Modèles") {

  val modèle = insertBlock(Modele())

  inner class Modele : Block("Modèle", 1, 1000) {
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
      command(item = copier, Mode.UPDATE) { copier() }

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
    val m = table(models)

    // Champs du block
    val id = hidden(INT(11)) {
      columns(m.id)
    }
    val nom = mustFill(STRING(100), at(1, 1)) {
      label = "Nom"
      help = "Le nom du modèle d'actif"
      columns(m.name) {
        priority = 8
      }
    }
    val fabricant = mustFill(Fabricants(), at(2, 1)) {
      label = "Fabricant"
      columns(m.manufacturer_id) {
        priority = 7
      }
    }
    val catégorie = mustFill(Categories("asset"), at(3, 1)) {
      label = "Catégorie"
      columns(m.category_id) {
        priority = 6
      }
    }
    val numéro = visit(STRING(100), at(4, 1)) {
      label = "N° modèle"
      columns(m.model_number){
        priority = 5
      }
    }
    val finVie = visit(INT(11), at(5, 1)) {
      label = "Fin de vie (mois)"
      help = "La fin de vie du modèle (en mois)."
      columns(m.eol) {
        priority = 4
      }
    }
    val spécifique = visit(ChampsSpecifiques(), at(6, 1)) {
      label = "Champs Spécifiques"
      help = "L'ensemble des champs spécifiques associés au modèle."
      columns(m.fieldset_id) {
        priority = 3
      }
    }
    val image = visit(IMAGE(300, 300), at(1..15, 2)) {
      label = "Image"
      help = "L'image du modèle."
      columns(m.image_source)
    }
    val remarques = visit(STRING(100, 10, 5, Fixed.OFF), at(8..12, 1)) {
      label = "Remarques"
      help = "Remarques."
      columns(m.notes)
    }
    val idUtilisateur = hidden(INT(11)) {
      columns(m.user_id)
      trigger(PREINS, PREUPD) {
        value = getUserID()
      }
    }
    val utilisateur = skipped(STRING(50), at(14, 1..2)) {
      label = "Utilisateur"
      options(FieldOption.TRANSIENT)
    }
    val crééLe = skipped(DATETIME, at(15, 1)) {
      label = "Créé le"
      help = "la date de création du modèle"
      columns(m.created_at)
    }
    val modifiéLe = skipped(DATETIME, at(16, 1)) {
      label = "Modifié le"
      help = "La date de modification du modèle"
      columns(m.updated_at)
    }
    val suppriméLe = skipped(DATETIME, at(17, 1)) {
      label = "Supprimé le"
      help = "La date de suppression du modèle"
      columns(m.deleted_at)
    }

    /**
     * Supprimer un enregistrement : Mettre à jour le champ [models.deleted_at]
     */
    private fun supprimer(b: VBlock) {
      b.validate()
      transaction {
        models.update({ models.id eq this@Modele.id.value!! }) {
          it[user_id] = getUserID()
          it[updated_at] = LocalDateTime.now()
          it[deleted_at] = LocalDateTime.now()
        }
      }
      b.form.reset()
    }

    /**
     * Dupliquer un modèle
     */
    private fun copier() {
      id.clear(0)
      idUtilisateur.clear(0)
      utilisateur.clear(0)
      crééLe.clear(0)
      modifiéLe.clear(0)
      suppriméLe.clear(0)
      this.block.setRecordFetched(0, false)
      this.setMode(Mode.INSERT)
    }
  }
}
