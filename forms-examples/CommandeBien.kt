// ----------------------------------------------------------------------
// Copyright (c) 2013-2024 kopiLeft Services SARL, Tunisie
// Copyright (c) 2018-2024 ProGmag SAS, France
// ----------------------------------------------------------------------
// All rights reserved - tous droits réservés.
// ----------------------------------------------------------------------

package com.progmag.inventaire

import java.time.LocalDateTime

import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.update

import org.kopi.galite.visual.VWindow
import org.kopi.galite.visual.database.transaction
import org.kopi.galite.visual.domain.DATE
import org.kopi.galite.visual.domain.DATETIME
import org.kopi.galite.visual.domain.Fixed
import org.kopi.galite.visual.domain.INT
import org.kopi.galite.visual.domain.STRING
import org.kopi.galite.visual.dsl.common.Mode
import org.kopi.galite.visual.dsl.form.Access
import org.kopi.galite.visual.dsl.form.Block
import org.kopi.galite.visual.form.VBlock

import com.progmag.inventaire.base.DefaultDictionaryForm
import com.progmag.inventaire.base.Actifs
import com.progmag.inventaire.base.Lieux
import com.progmag.inventaire.base.Utilisateurs
import com.progmag.inventaire.base.TypeAssociation
import com.progmag.inventaire.dbschema.assets
import com.progmag.inventaire.dbschema.models
import org.jetbrains.exposed.sql.select

class CommandeBien() : DefaultDictionaryForm(title = "Commande des biens", allowInterrupt = false) {
  constructor(id: Int) : this() {
    commande.id.value = id
    transaction {
      commande.load()
    }
    commande.setMode(Mode.INSERT)
  }

  val commande = insertBlock(Commande())

  inner class Commande : Block("Commande des biens", 1, 100) {
    init {
      command(item = valider, Mode.INSERT) { valider(block) }
    }

    val a = table(assets)
    val m = table(models)

    // Champs du block
    val id = hidden(INT(11)) {
      columns(a.id)
    }
    val étiquette = skipped(STRING(63, 3, 1, Fixed.OFF), at(1, 1..3)) {
      label = "Étiquette"
      help = "L'étiquette de l'actifs. C'est un identifiant unique."
      columns(a.asset_tag)
    }
    val idModèle = hidden(INT(11)) {
      columns(m.id, nullable(a.model_id))
    }
    val modèle = skipped(STRING(63, 3, 1, Fixed.OFF), at(2, 1..3)) {
      label = "Modèle"
      help = "Le modèle de l'actif."
      columns(m.name)
    }
    val typeAffectation = mustFill(TypeAssociation(), at(3, 1)) {
      label = "Affecté au"
      columns(a.assigned_type)
      trigger(POSTCHG) {
        réinitialiserValeurs(value)
        réinitialiserAccès(value)
      }
      trigger(AUTOLEAVE) { true }
    }
    val utilisateur = mustFill(Utilisateurs(), at(4, 1)) {
      label = "Utilisateur"
      trigger(POSTFLD) {
        réinitialiserAccès(typeAffectation.value)
      }
    }
    val lieu = mustFill(Lieux(), at(4, 2)) {
      label = "Lieu"
      trigger(POSTFLD) {
        réinitialiserAccès(typeAffectation.value)
      }
    }
    val actif = mustFill(Actifs(), at(4, 3)) {
      label = "Actif"
      trigger(POSTFLD) {
        réinitialiserAccès(typeAffectation.value)
      }
    }
    val dateAttribution = visit(DATETIME, at(5, 1)) {
      label = "Date d'attribution"
      trigger(POSTFLD) {
        réinitialiserAccès(typeAffectation.value)
      }
    }
    val dateDissociationPrévue = visit(DATE, at(6, 1)) {
      label = "Date de dissociation prévue"
      trigger(POSTFLD) {
        réinitialiserAccès(typeAffectation.value)
      }
    }

    /**
     * Associer l'actif
     */
    private fun valider(b: VBlock) {
      b.validate()
      transaction {
        assets.update({ assets.id eq this@Commande.id.value!! }) {
          it[assigned_to] = when (typeAffectation.value) {
            "App\\Models\\User" -> utilisateur.value
            "App\\Models\\Location" -> lieu.value
            "App\\Models\\Asset" -> actif.value
            else -> null
          }
          it[last_checkout] = dateAttribution.value ?: LocalDateTime.now()
          it[expected_checkin] = dateDissociationPrévue.value
          it[assigned_type] = typeAffectation.value
          it[location_id] = when (typeAffectation.value) {
            "App\\Models\\User" -> null
            "App\\Models\\Location" -> lieu.value
            "App\\Models\\Asset" -> assets.slice(location_id).select { id eq actif.value!! }.map { bien -> bien[location_id] }.firstOrNull()
            else -> null
          }
          it[updated_at] = LocalDateTime.now()
        }
        assets.update({ (assets.id eq this@Commande.id.value!!) and assets.deleted_at.isNull() }) {
          with(SqlExpressionBuilder) { it[checkout_counter] = checkout_counter + 1 }
        }
      }

      b.form.close(VWindow.CDE_QUIT)
    }

    /**
     * Réinitialiser les valeurs affectées suite au changement du type d'affectation
     */
    private fun réinitialiserValeurs(typeAffectation: String? = null) {
      when (typeAffectation) {
        "App\\Models\\User" -> { lieu.value = null ; actif.value = null }
        "App\\Models\\Location" -> { utilisateur.value = null ; actif.value = null }
        "App\\Models\\Asset" -> { utilisateur.value = null ; lieu.value = null }
      }
    }

    /**
     * Mettre à jour l'accès aux champs d'affectation
     */
    fun réinitialiserAccès(typeAffectation: String? = null) {
      when (typeAffectation) {
        "App\\Models\\User" -> {
          utilisateur.vField.setAccess(Access.MUSTFILL.value)
          lieu.vField.setAccess(Access.SKIPPED.value)
          actif.vField.setAccess(Access.SKIPPED.value)
        }
        "App\\Models\\Location" -> {
          lieu.vField.setAccess(Access.MUSTFILL.value)
          utilisateur.vField.setAccess(Access.SKIPPED.value)
          actif.vField.setAccess(Access.SKIPPED.value)
        }
        "App\\Models\\Asset" -> {
          actif.vField.setAccess(Access.MUSTFILL.value)
          utilisateur.vField.setAccess(Access.SKIPPED.value)
          lieu.vField.setAccess(Access.SKIPPED.value)
        }
        else -> {
          utilisateur.vField.setAccess(Access.SKIPPED.value)
          lieu.vField.setAccess(Access.SKIPPED.value)
          actif.vField.setAccess(Access.SKIPPED.value)
        }
      }
    }
  }
}
