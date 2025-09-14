// ----------------------------------------------------------------------
// Copyright (c) 2013-2024 kopiLeft Services SARL, Tunisie
// Copyright (c) 2018-2024 ProGmag SAS, France
// ----------------------------------------------------------------------
// All rights reserved - tous droits réservés.
// ----------------------------------------------------------------------

package com.progmag.inventaire

import java.time.LocalDateTime

import org.jetbrains.exposed.sql.insert

import org.kopi.galite.visual.VWindow
import org.kopi.galite.visual.database.transaction
import org.kopi.galite.visual.domain.Fixed
import org.kopi.galite.visual.domain.INT
import org.kopi.galite.visual.domain.STRING
import org.kopi.galite.visual.dsl.common.Mode
import org.kopi.galite.visual.dsl.form.Block
import org.kopi.galite.visual.form.VBlock

import com.progmag.inventaire.base.DefaultDictionaryForm
import com.progmag.inventaire.base.Utilisateurs
import com.progmag.inventaire.dbschema.accessories
import com.progmag.inventaire.dbschema.accessories_users
import com.progmag.inventaire.dbschema.categories

class AffectationAccessoireAuxUtilisateurs() : DefaultDictionaryForm(title = "Attribuer accessoire", allowInterrupt = false) {
  constructor(id: Int) : this() {
    utilisateurAccessoire.id.value = id
    transaction {
      utilisateurAccessoire.load()
    }
    utilisateurAccessoire.setMode(Mode.INSERT)
  }

  val utilisateurAccessoire = insertBlock(UtilisateurAccessoire())

  inner class UtilisateurAccessoire : Block("Attribuer accessoire", 1, 100) {
    init {
      command(item = valider, Mode.INSERT) { valider(block) }
    }

    val a = table(accessories)
    val c = table(categories)

    // Champs du block
    val id = hidden(INT(11)) {
      columns(a.id)
    }
    val nom = skipped(STRING(63, 3, 1, Fixed.OFF), at(1, 1..3)) {
      label = "Nom"
      help = "Le nom de l'accessoire."
      columns(a.name)
    }
    val idCatégorie = hidden(INT(11)) {
      columns(c.id, nullable(a.category_id))
    }
    val catégorie = skipped(STRING(63, 3, 1, Fixed.OFF), at(2, 1..3)) {
      label = "Catégorie"
      help = "Le nom de la catégorie de l'accessoire."
      columns(c.name)
    }
    val utilisateur = mustFill(Utilisateurs(), at(3, 1)) {
      label = "Affecté au"
    }
    val remarques = visit(STRING(63, 3, 1, Fixed.OFF), at(4, 1)) {
      label = "Remarques"
    }

    /**
     * Associer l'actif
     */
    private fun valider(b: VBlock) {
      b.validate()
      transaction {
        accessories_users.insert {
          it[accessory_id] = this@UtilisateurAccessoire.id.value
          it[assigned_to] = utilisateur.value
          it[note] = remarques.value
          it[user_id] = getUserID()
          it[created_at] = LocalDateTime.now()
          it[updated_at] = LocalDateTime.now()
        }
      }

      b.form.close(VWindow.CDE_QUIT)
    }
  }
}
