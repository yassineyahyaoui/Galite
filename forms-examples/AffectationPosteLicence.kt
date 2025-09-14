// ----------------------------------------------------------------------
// Copyright (c) 2013-2024 kopiLeft Services SARL, Tunisie
// Copyright (c) 2018-2024 ProGmag SAS, France
// ----------------------------------------------------------------------
// All rights reserved - tous droits réservés.
// ----------------------------------------------------------------------

package com.progmag.inventaire

import java.time.LocalDateTime

import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update

import org.kopi.galite.visual.VWindow
import org.kopi.galite.visual.database.transaction
import org.kopi.galite.visual.domain.Fixed
import org.kopi.galite.visual.domain.STRING
import org.kopi.galite.visual.dsl.common.Mode
import org.kopi.galite.visual.dsl.form.Access
import org.kopi.galite.visual.dsl.form.Block
import org.kopi.galite.visual.form.VBlock

import com.progmag.inventaire.base.DefaultDictionaryForm
import com.progmag.inventaire.base.Actifs
import com.progmag.inventaire.base.PostesDisponibles
import com.progmag.inventaire.base.Utilisateurs
import com.progmag.inventaire.base.Licences
import com.progmag.inventaire.base.TypeAssociation
import com.progmag.inventaire.dbschema.assets
import com.progmag.inventaire.dbschema.license_seats
import com.progmag.inventaire.dbschema.licenses

class AffectationPosteLicence(val licence: Int, idPoste: Int? = null) : DefaultDictionaryForm(title = "Associer la licence multiposte", allowInterrupt = false) {
  val poste = insertBlock(Poste())

  init {
    poste.nom.value = licence
    transaction {
      poste.load()
    }
    poste.id.value = idPoste
    poste.setMode(Mode.INSERT)
  }

  inner class Poste : Block("Associer la licence multiposte", 1, 100) {
    init {
      command(item = valider, Mode.INSERT) { valider(block) }
    }

    val l = table(licenses)

    // Champs du block
    val id = mustFill(PostesDisponibles(licence), at(1, 1)) {
      label = "Poste"
      trigger(POSTFLD) {
        réinitialiserAccès(typeAffectation.value)
      }
    }
    val nom = skipped(Licences(), at(2, 1..3)) {
      label = "Nom"
      help = "Le nom de la licence."
      columns(l.id)
    }
    val série = skipped(STRING(38, 5, 1, Fixed.OFF), at(3, 1..3)) {
      label = "Série"
      help = "Le numéro de série de la licence."
      columns(l.serial)
    }
    val typeAffectation = mustFill(TypeAssociation(false), at(4, 1)) {
      label = "Affecté au"
      trigger(POSTCHG) {
        réinitialiserValeurs(value)
        réinitialiserAccès(value)
      }
      trigger(AUTOLEAVE) { true }
    }
    val utilisateur = mustFill(Utilisateurs(), at(5, 1)) {
      label = "Utilisateur"
      trigger(POSTCHG) {
        réinitialiserAccès(typeAffectation.value)
      }
    }
    val actif = mustFill(Actifs(), at(5, 2)) {
      label = "Actif"
      trigger(POSTCHG) {
        réinitialiserAccès(typeAffectation.value)
      }
    }

    /**
     * Associer l'actif
     */
    private fun valider(b: VBlock) {
      b.validate()
      transaction {
        license_seats.update({ license_seats.id eq this@Poste.id.value!! }) {
          it[assigned_to] =  when (typeAffectation.value) {
            "App\\Models\\User" -> utilisateur.value
            "App\\Models\\Asset" -> assets.slice(assets.assigned_to).select {
              (assets.id eq actif.value!!) and (assets.assigned_type eq "App\\Models\\User")
            }.map { bien ->
              bien[assets.assigned_to]
            }.firstOrNull()
            else -> null
          }
          it[asset_id] = if (typeAffectation.value == "App\\Models\\Asset") actif.value else null
          it[user_id] = getUserID()
          it[updated_at] = LocalDateTime.now()
        }
      }

      b.form.close(VWindow.CDE_QUIT)
    }

    /**
     * Réinitialiser les valeurs affectées suite au changement du type d'affectation
     */
    private fun réinitialiserValeurs(typeAffectation: String? = null) {
      when (typeAffectation) {
        "App\\Models\\User" -> { actif.value = null }
        "App\\Models\\Asset" -> { utilisateur.value = null }
      }
    }

    /**
     * Mettre à jour l'accès aux champs d'affectation
     */
    fun réinitialiserAccès(typeAffectation: String? = null) {
      when (typeAffectation) {
        "App\\Models\\User" -> {
          utilisateur.vField.setAccess(Access.MUSTFILL.value)
          actif.vField.setAccess(Access.SKIPPED.value)
        }
        "App\\Models\\Asset" -> {
          actif.vField.setAccess(Access.MUSTFILL.value)
          utilisateur.vField.setAccess(Access.SKIPPED.value)
        }
        else -> {
          utilisateur.vField.setAccess(Access.SKIPPED.value)
          actif.vField.setAccess(Access.SKIPPED.value)
        }
      }
    }
  }
}
