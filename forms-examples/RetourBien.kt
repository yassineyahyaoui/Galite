// ----------------------------------------------------------------------
// Copyright (c) 2013-2024 kopiLeft Services SARL, Tunisie
// Copyright (c) 2018-2024 ProGmag SAS, France
// ----------------------------------------------------------------------
// All rights reserved - tous droits réservés.
// ----------------------------------------------------------------------

package com.progmag.inventaire

import java.time.LocalDateTime
import java.time.LocalDate

import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.update

import org.kopi.galite.visual.VWindow
import org.kopi.galite.visual.database.transaction
import org.kopi.galite.visual.domain.DATE
import org.kopi.galite.visual.domain.Fixed
import org.kopi.galite.visual.domain.INT
import org.kopi.galite.visual.domain.STRING
import org.kopi.galite.visual.dsl.common.Mode
import org.kopi.galite.visual.dsl.form.Access
import org.kopi.galite.visual.dsl.form.Block
import org.kopi.galite.visual.form.VBlock

import com.progmag.inventaire.base.DefaultDictionaryForm
import com.progmag.inventaire.base.Lieux
import com.progmag.inventaire.base.Statuts
import com.progmag.inventaire.dbschema.assets
import com.progmag.inventaire.dbschema.locations
import com.progmag.inventaire.dbschema.models

class RetourBien() : DefaultDictionaryForm(title = "Retour des biens", allowInterrupt = false) {
  constructor(id: Int) : this() {
    retour.id.value = id
    transaction {
      retour.load()
    }
    retour.setMode(Mode.INSERT)
  }

  val retour = insertBlock(Retour())
  inner class Retour : Block("Retour des biens", 1, 100) {
    init {
      blockVisibility(Access.VISIT, Mode.QUERY)
      command(item = valider, Mode.INSERT) { valider(block) }

      trigger(INIT) {
        dateDissociation.value = LocalDate.now()
      }
    }

    val a = table(assets)
    val m = table(models)
    val l = table(locations)

    // Champs du block
    val id = hidden(INT(11)) {
      columns(a.id)
    }
    val étiquette = skipped(STRING(63, 3, 1, Fixed.OFF), at(1, 1)) {
      label = "Étiquette"
      help = "L'étiquette de l'actif."
      columns(a.asset_tag)
    }
    val idModèle = hidden(INT(11)) {
      columns(m.id, nullable(a.model_id))
    }
    val modèle = skipped(STRING(63, 3, 1, Fixed.OFF), at(2, 1)) {
      label = "Modèle"
      help = "Le modèle de l'actif."
      columns(m.name)
    }
    val nom = skipped(STRING(63, 3, 1, Fixed.OFF), at(3, 1)) {
      label = "Nom"
      help = "Le nom de l'actif."
      columns(a.name)
    }
    val statut = mustFill(Statuts(), at(4, 1)) {
      label = "Statut"
      help = "Choisir un nouveau statut de l'actif."
      columns(a.status_id)
      trigger(POSTCHG) {
        block.fetchLookupFirst(vField)
      }
    }
    val emplacement = visit(Lieux(), at(5, 1)) {
      label = "Lieu"
      help = "Affecter l'actif à un nouveau lieu"
      columns(a.location_id)
    }
    val dateDissociation = mustFill(DATE, at(6, 1)) {
      label = "Date de dissociation"
      help = "La date prévue de dissociation (par défaut date du jour)"
      trigger(DEFAULT) {
        value = LocalDate.now()
      }
    }

    private fun valider(b: VBlock) {
      b.validate()
      transaction {
        assets.update({ assets.id eq this@Retour.id.value!! }) {
          it[assigned_to] = null
          it[last_checkout] = null
          it[expected_checkin] = null
          it[assigned_type] = null
          it[status_id] = statut.value
          emplacement.value?.let { lieu ->
            it[location_id] = lieu
          }
          it[updated_at] = LocalDateTime.now()
        }
        assets.update({ (assets.id eq this@Retour.id.value!!) and (assets.deleted_at.isNull()) }) {
          with(SqlExpressionBuilder) { it[checkin_counter] = checkin_counter + 1 }
        }
      }
      b.form.close(VWindow.CDE_QUIT)
    }
  }
}
