// ----------------------------------------------------------------------
// Copyright (c) 2013-2024 kopiLeft Services SARL, Tunisie
// Copyright (c) 2018-2024 ProGmag SAS, France
// ----------------------------------------------------------------------
// All rights reserved - tous droits réservés.
// ----------------------------------------------------------------------

package com.progmag.inventaire

import java.time.LocalDateTime

import org.kopi.galite.visual.domain.DATETIME
import org.kopi.galite.visual.domain.INT
import org.kopi.galite.visual.domain.Fixed
import org.kopi.galite.visual.domain.STRING
import org.kopi.galite.visual.dsl.common.Mode
import org.kopi.galite.visual.dsl.form.Access
import org.kopi.galite.visual.dsl.form.Block

import com.progmag.inventaire.base.DefaultDictionaryForm
import com.progmag.inventaire.dbschema.companies

class Entreprises : DefaultDictionaryForm(title = "Entreprises")  {

  val entreprise = insertBlock(Company())

  inner class Company : Block("Entreprise", 1, 1000) {
    init {
      blockVisibility(Access.VISIT, Mode.QUERY)
      // Défintion des commandes du block
      breakCommand
      command(item = serialQuery, Mode.QUERY) { serialQuery() }
      command(item = searchOperator, Mode.QUERY) { searchOperator() }
      command(item = menuQuery, Mode.QUERY) { recursiveQuery() }
      command(item = delete, Mode.UPDATE) { deleteBlock() }
      command(item = insertMode, Mode.QUERY) { insertMode() }
      command(item = save, Mode.INSERT, Mode.UPDATE) { saveBlock() }
      command(item = dynamicReport) { createDynamicReport() }

      // Initialisation des triggers
      trigger(PREINS) {
        crééeLe.value = LocalDateTime.now()
        modifiéeLe.value = LocalDateTime.now()
      }
      trigger(PREUPD) {
        modifiéeLe.value = LocalDateTime.now()
      }
    }

    // Définition des alias des tables du block
    val c = table(companies)

    // Champs du block
    val id = hidden(INT(11)) {
      columns(c.id)
    }
    val nom = mustFill(STRING(38, 5, 1, Fixed.OFF), at(1, 1)) {
      label = "Nom"
      help = "Le nom de l'entreprise."
      columns(c.name) {
        priority = 1
      }
    }
    val crééeLe = skipped(DATETIME, at(4, 1)) {
      label = "Créée le"
      help = "Date et heure de la création de cette entreprise."
      columns(c.created_at)
    }
    val modifiéeLe = skipped(DATETIME, at(5, 1)) {
      label = "Modifiée le"
      help = "Date et heure de la création de cette entreprise."
      columns(c.updated_at)
    }
    // image non reprise
  }
}
