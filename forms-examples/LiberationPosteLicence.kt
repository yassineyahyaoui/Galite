// ----------------------------------------------------------------------
// Copyright (c) 2013-2024 kopiLeft Services SARL, Tunisie
// Copyright (c) 2018-2024 ProGmag SAS, France
// ----------------------------------------------------------------------
// All rights reserved - tous droits réservés.
// ----------------------------------------------------------------------

package com.progmag.inventaire

import org.kopi.galite.visual.VWindow
import org.kopi.galite.visual.database.transaction
import org.kopi.galite.visual.dsl.common.Mode
import org.kopi.galite.visual.dsl.form.Block
import org.kopi.galite.visual.form.VBlock

import com.progmag.inventaire.base.DefaultDictionaryForm
import com.progmag.inventaire.base.PostesAffectés
import com.progmag.inventaire.base.Licences
import com.progmag.inventaire.dbschema.licenses
import org.kopi.galite.visual.domain.Fixed
import org.kopi.galite.visual.domain.STRING
import org.kopi.galite.visual.dsl.form.FieldOption
import org.kopi.galite.visual.dsl.form.FormField

class LiberationPosteLicence(val licence: Int, idPoste: Int? = null, val posteModifiable: Boolean = true) :
  DefaultDictionaryForm(title = "Libération poste", allowInterrupt = false)
{
  val poste = insertBlock(Poste())

  init {
    poste.nom.value = licence
    transaction {
      poste.load()
    }
    poste.id.value = idPoste
    poste.setMode(Mode.INSERT)
  }

  inner class Poste : Block("Libération poste", 1, 100) {
    init {
      command(item = valider, Mode.INSERT) { valider(block) }
    }

    val l = table(licenses)

    // Champs du block
    val id: FormField<Int> = if (posteModifiable) {
      mustFill(PostesAffectés(licence), at(1, 1)) {
        label = "Poste"
      }
    } else {
      skipped(PostesAffectés(licence), at(1, 1)) {
        label = "Poste"
      }
    }
    val nom = skipped(Licences(), at(2, 1..3)) {
      label = "Nom"
      help = "Le nom de la licence."
      columns(l.id)
    }

    val série = if (posteModifiable) {
      skipped(STRING(38, 5, 1, Fixed.OFF), at(3, 1..3)) {
        label = "Série"
        help = "Le numéro de série de la licence."
        columns(l.serial)
      }
    } else {
      visit(STRING(38, 5, 1, Fixed.OFF), at(3, 1..3)) {
        label = "Série"
        help = "Le numéro de série de la licence."
        columns(l.serial)
        FieldOption.NOEDIT
      }
    }

    /**
     * Libérer le poste de licence
     */
    private fun valider(b: VBlock) {
      b.validate()
      transaction {
        Utils.dissocierLicence(this@Poste.id.value!!, getUserID())
      }
      b.form.close(VWindow.CDE_QUIT)
    }
  }
}
