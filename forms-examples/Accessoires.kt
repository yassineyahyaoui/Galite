// ----------------------------------------------------------------------
// Copyright (c) 2013-2024 kopiLeft Services SARL, Tunisie
// Copyright (c) 2018-2024 ProGmag SAS, France
// ----------------------------------------------------------------------
// All rights reserved - tous droits réservés.
// ----------------------------------------------------------------------

package com.progmag.inventaire

import java.time.LocalDateTime

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.update

import org.kopi.galite.database.Users
import org.kopi.galite.visual.MessageCode
import org.kopi.galite.visual.VColor
import org.kopi.galite.visual.VExecFailedException
import org.kopi.galite.visual.WindowController
import org.kopi.galite.visual.database.transaction
import org.kopi.galite.visual.domain.Convert
import org.kopi.galite.visual.domain.DATETIME
import org.kopi.galite.visual.domain.DATE
import org.kopi.galite.visual.domain.DECIMAL
import org.kopi.galite.visual.domain.Fixed
import org.kopi.galite.visual.domain.INT
import org.kopi.galite.visual.domain.STRING
import org.kopi.galite.visual.dsl.common.Mode
import org.kopi.galite.visual.dsl.form.Access
import org.kopi.galite.visual.dsl.form.BlockOption
import org.kopi.galite.visual.dsl.form.Block
import org.kopi.galite.visual.dsl.form.Border
import org.kopi.galite.visual.dsl.form.FieldOption
import org.kopi.galite.visual.form.VBlock
import org.kopi.galite.visual.form.VConstants

import com.progmag.inventaire.base.Categories
import com.progmag.inventaire.base.DefaultDictionaryForm
import com.progmag.inventaire.base.Entreprises
import com.progmag.inventaire.base.Fabricants
import com.progmag.inventaire.base.Fournisseurs
import com.progmag.inventaire.base.Lieux
import com.progmag.inventaire.dbschema.accessories
import com.progmag.inventaire.dbschema.accessories_users

class Accessoires : DefaultDictionaryForm(title = "Accessoires")  {
  init {
    trigger(RESET) {
      setTitle("Accessoire")
      return@trigger false
    }
  }

  val accessoire = page("Accessoire").insertBlock(Accessoire())

  inner class Accessoire : Block("Accessoire", 1, 1000) {

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
      trigger(POSTINS, POSTUPD) {
        idUtilisateur.value?.let { utilisateur.value = Utils.chargerUtilisateur(it) }
        quantitéDisponible.value = quantité.value!! - Utils.chargerAccessoiresAffectés(id.value!!)
      }
      trigger(PREQRY) {
        // Forcer la condition : deleted_at IS NULL
        suppriméLe.vField.setSearchOperator(VConstants.SOP_LE)
      }
      trigger(POSTQRY) {
        idUtilisateur.value?.let { utilisateur.value = Utils.chargerUtilisateur(it) }
        quantitéDisponible.value = quantité.value!! - Utils.chargerAccessoiresAffectés(id.value!!)
        utilisateurs.block.clear()
        utilisateurs.load()
        setTitle("Accessoire : ${nom.value}")
      }
    }
    // Définition des alias des tables du block
    val a = table(accessories)

    // Champs du block
    val id = hidden(INT(11)) {
      columns(a.id)
    }
    val idUtilisateur = hidden(INT(11)) {
      columns(a.user_id)
    }
    val nom = mustFill(STRING(100, Convert.UPPER), at(1, 1..4)) {
      label = "Nom"
      columns(a.name) {
        priority = 8
      }
    }
    val entreprise = visit(Entreprises(), at(2, 1..4)) {
      label = "Entreprise"
      columns(a.company_id) {
        priority = 7
      }
    }
    val catégorie = mustFill(Categories("accessory"), at(3, 1..2)) {
      label = "Catégorie"
      columns(a.category_id) {
        priority = 6
      }
    }
    val fournisseur = visit(Fournisseurs(), at(4, 1..2)) {
      label = "Fournisseur"
      columns(a.supplier_id) {
        priority = 5
      }
    }
    val fabricant = visit(Fabricants(), at(5, 1..2)) {
      label = "Fabricant"
      columns(a.manufacturer_id) {
        priority = 4
      }
    }
    val lieu = visit(domain = Lieux(), at(6, 1..4)) {
      label = "Lieu"
      columns(a.location_id) {
        priority = 3
      }
    }
    val numéroModèle = visit(STRING(100), at(7, 1..4)) {
      label = "Modèle N°"
      columns(a.model_number) {
        priority = 2
      }
    }
    val numéroCommande = visit(STRING(100), at(8, 1..4)) {
      label = "Numéro de commande"
      columns(a.order_number)
    }
    val dateAchat = visit(DATE, at(9, 1..2)) {
      label = "Date d'achat"
      columns(a.purchase_date)
    }
    val prixAchat = visit(DECIMAL(20, 2), at(10, 1..2)) {
      label = "Prix d'achat"
      columns(a.purchase_cost)
    }
    val quantité = mustFill(INT(11), at(11, 1)) {
      label = "Quantité"
      columns(a.qty) {
        priority = 1
      }
    }
    val quantitéDisponible = skipped(INT(11), at(11, 2)) {
      label = "Qté Dispo."
    }
    val quantitéMin = visit(INT(11), at(12, 1)) {
      label = "Qté Min."
      help = "La quantité minimale des biens qui devraient être disponibles."
      columns(a.min_amt)
    }
    val utilisateur = visit(STRING(50), at(14, 1..3)) {
      label = "Utilisateur"
      options(FieldOption.TRANSIENT)
    }
    val crééLe = skipped(DATETIME, at(15, 1..2)) {
      label = "Créé le"
      help = "la date de création du Licence"
      columns(a.created_at)
    }
    val modifiéLe = skipped(DATETIME, at(16, 1..2)) {
      label = "Modifié le"
      help = "La date de modification du Licence"
      columns(a.updated_at)
    }
    val suppriméLe = skipped(DATETIME, at(17, 1..2)) {
      label = "Supprimé le"
      help = "La date de suppression du Licence"
      columns(a.deleted_at)
    }

    /**
     * Mise à jour colonne SupprimeLe
     */
    private fun supprimer(b: VBlock) {
      b.validate()
      if (Utils.chargerAccessoiresAffectés(this@Accessoire.id.value!!) > 0) {
        throw VExecFailedException(MessageCode.getMessage("INV-00008"))
      } else {
        transaction {
          accessories.update({ accessories.id eq this@Accessoire.id.value!! }) {
            it[user_id] = getUserID()
            it[updated_at] = LocalDateTime.now()
            it[deleted_at] = LocalDateTime.now()
          }
        }
        b.form.reset()
      }
    }
  }

  val utilisateurs = page("Utilisateurs Affectés").insertBlock(Utilisateur())

  inner class Utilisateur : Block("Utilisateur", 100, 20) {
    init {
      options(BlockOption.NODETAIL)
      border = Border.LINE

      command(item = associer, Mode.QUERY) {
        if (accessoire.quantitéDisponible.value!! <= 0) {
          throw VExecFailedException(MessageCode.getMessage("INV-00010"))
        } else {
          val id = accessoire.id.value!!

          WindowController.windowController.doModal(AffectationAccessoireAuxUtilisateurs(id))
          accessoire.clear()
          accessoire.id.value = id
          transaction { accessoire.load() }
        }
      }
      command(item = dissocier, Mode.QUERY) {
        utilisateur[currentRecord]?.let {
          val id = accessoire.id.value!!

          if (ask(MessageCode.getMessage("INV-00003", accessoire.nom.value!!, it))) {
            transaction {
              dissocierUtilisateur(this@Utilisateur.id[currentRecord]!!)
              accessoire.clear()
              accessoire.id.value = id
              accessoire.load()
            }
          }
        } ?: notice(MessageCode.getMessage("INV-00004"))
      }

      trigger(POSTQRY) {
        (0 until block.bufferSize).filter { i -> isRecordFilled(i) }.forEach { i ->
          état[i] = "Attribué"
          état.setColor(null, VColor(255, 204, 204))
        }
      }
      trigger(ACCESS) {
        accessoire.getMode() != Mode.QUERY.value
      }
    }

    // Définition des alias des tables du block
    val u = table(accessories_users)
    val a = table(accessories)
    val k = table(Users)

    val id = hidden(INT(11)) {
      columns(u.id)
    }
    val idAccessoire = hidden(INT(11)) {
      alias = accessoire.id
      columns(a.id, nullable(u.accessory_id))
    }
    val idUtilisateur = hidden(INT(11)) {
      columns(k.id, nullable(u.user_id))
    }
    val utilisateur = skipped(STRING(50), at(1)) {
      label = "Utilisateur"
      columns(k.name)
      options(FieldOption.TRANSIENT)
    }
    val remarques = skipped(STRING(38, 5, 1, Fixed.OFF), at(1)) {
      label = "Remarques"
      columns(u.note)
      FieldOption.NOEDIT
    }
    val date = skipped(DATETIME, at(1)) {
      label = "Date d'association"
      columns(u.created_at)
      FieldOption.NOEDIT
    }
    val état = visit(STRING(10), at(1)) {
      label = "État"
      FieldOption.TRANSIENT
    }
    /**
     * Dissocier la licence
     */
    fun dissocierUtilisateur(utilisateur: Int) {
      accessories_users.deleteWhere { id eq utilisateur }
    }
  }
}
