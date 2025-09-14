// ----------------------------------------------------------------------
// Copyright (c) 2013-2024 kopiLeft Services SARL, Tunisie
// Copyright (c) 2018-2024 ProGmag SAS, France
// ----------------------------------------------------------------------
// All rights reserved - tous droits réservés.
// ----------------------------------------------------------------------

package com.progmag.inventaire

import java.time.LocalDateTime
import kotlin.math.abs

import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update

import org.kopi.galite.database.Users
import org.kopi.galite.visual.MessageCode
import org.kopi.galite.visual.VColor
import org.kopi.galite.visual.VExecFailedException
import org.kopi.galite.visual.WindowController
import org.kopi.galite.visual.database.transaction
import org.kopi.galite.visual.domain.BOOL
import org.kopi.galite.visual.domain.DATETIME
import org.kopi.galite.visual.domain.DATE
import org.kopi.galite.visual.domain.DECIMAL
import org.kopi.galite.visual.domain.Fixed
import org.kopi.galite.visual.domain.INT
import org.kopi.galite.visual.domain.STRING
import org.kopi.galite.visual.domain.TEXT
import org.kopi.galite.visual.dsl.common.Mode
import org.kopi.galite.visual.dsl.form.Access
import org.kopi.galite.visual.dsl.form.BlockOption
import org.kopi.galite.visual.dsl.form.Block
import org.kopi.galite.visual.dsl.form.Border
import org.kopi.galite.visual.dsl.form.FieldOption
import org.kopi.galite.visual.form.VBlock
import org.kopi.galite.visual.form.VConstants

import com.progmag.inventaire.base.Actifs
import com.progmag.inventaire.base.Categories
import com.progmag.inventaire.base.DefaultDictionaryForm
import com.progmag.inventaire.base.Entreprises
import com.progmag.inventaire.base.Fabricants
import com.progmag.inventaire.base.Fournisseurs
import com.progmag.inventaire.dbschema.assets
import com.progmag.inventaire.dbschema.license_available_seats
import com.progmag.inventaire.dbschema.license_seats
import com.progmag.inventaire.dbschema.licenses
import com.progmag.inventaire.dbschema.locations

class Licences : DefaultDictionaryForm(title = "Licences") {
  init {
    trigger(RESET) {
      setTitle("Licence")
      return@trigger false
    }
  }
  val licence = page("Détails").insertBlock(Licence())

  inner class Licence : Block("Licence", 1, 1000) {
    init {
      blockVisibility(Access.VISIT, Mode.QUERY)
      // Défintion des commandes du block
      breakCommand
      command(item = serialQuery, Mode.QUERY) { serialQuery() }
      command(item = searchOperator, Mode.QUERY) { searchOperator() }
      command(item = menuQuery, Mode.QUERY) { recursiveQuery() }
      command(item = delete, Mode.UPDATE) { supprimer(block) }
      command(item = insertMode, Mode.QUERY) { insertMode() }
      command(item = save, Mode.INSERT, Mode.UPDATE) { sauver(block) }
      command(item = dynamicReport) { createDynamicReport() }
      command(item = copier, Mode.UPDATE) { copier() }
      command(item = associer, Mode.UPDATE) { block.validate() ; attribuerPoste() }
      command(item = dissocier, Mode.UPDATE) { block.validate() ; libérerPoste() }

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
      }
      trigger(PREQRY) {
        // Forcer la condition : deleted_at IS NULL
        suppriméLe.vField.setSearchOperator(VConstants.SOP_LE)
      }
      trigger(POSTQRY) {
        idUtilisateur.value?.let { utilisateur.value = Utils.chargerUtilisateur(it) }
        postes.block.clear()
        postes.load()
        setTitle("Licence : ${nom.value} - ${clé.value}")
      }
    }

    // Définition des alias des tables du block
    val l = table(licenses)
    val a = table(license_available_seats)

    // Champs du block
    val id = hidden(INT(11)) {
      columns(l.id)
    }
    val idUtilisateur = hidden(INT(11)) {
      columns(l.user_id)
    }
    val nom = mustFill(STRING(120), at(1, 1..3)) {
      label = "Nom du logiciel"
      columns(l.name) {
        priority = 6
      }
    }
    val clé = visit(STRING(120), at(2, 1..3)) {
      label = "Clé du produit"
      columns(l.serial) {
        priority = 5
      }
    }
    val nomLicence = visit(STRING(120), at(3, 1..3)) {
      label = "Nom de la licence"
      columns(l.license_name) {
        priority = 4
      }
    }
    val email = visit(STRING(50), at(4, 1..2)) {
      label = "E-mail de la licence"
      columns(l.license_email)
    }
    val catégorie = mustFill(domain = Categories("license"), at(5, 1..2)) {
      label = "Catégorie"
      columns(l.category_id) {
        priority = 3
      }
    }
    val nombrePostes = mustFill(INT(11), at(6, 1)) {
      label = "Postes"
      columns(l.seats) {
        priority = -8
      }
    }
    val idPostesDisponibles = visit(INT(11), at(7, 1)) {
      columns(a.license_id, l.id)
      onUpdateHidden()
      onInsertHidden()
    }
    val postesDisponibles = visit(INT(11), at(7, 1)) {
      label = "Dispo."
      columns(a.available_seats) {
        priority = -9
      }
      onUpdateSkipped()
      onInsertSkipped()
    }
    val réattribuable = mustFill(BOOL, at(8, 1)) {
      label = "Réattribuables"
      columns(l.reassignable)
    }
    val entreprise = visit(domain = Entreprises(), at(9, 1..3)) {
      label = "Entreprise"
      columns(l.company_id)
    }
    val fabricant = mustFill(domain = Fabricants(), at(10, 1..2)) {
      label = "Fabricant"
      columns(l.manufacturer_id) {
        priority = 1
      }
    }
    val fournisseur = visit(Fournisseurs(), at(11, 1..2)) {
      label = "Fournisseur"
      columns(l.supplier_id)
    }
    val numéroCommande = visit(STRING(50), at(12, 1..2)) {
      label = "Numéro de commande"
      columns(l.order_number)
    }
    val prixAchat = visit(DECIMAL(20, 2), at(13, 1..2)) {
      label = "Prix d'achat"
      columns(l.purchase_cost)
    }
    val dateAchat = visit(DATE, at(14, 1..2)) {
      label = "Date d'achat"
      columns(l.purchase_date)
    }
    val dateExpiration = visit(DATE, at(15, 1..2)) {
      label = "Date d'expiration"
      columns(l.expiration_date)
    }
    val remarque = visit(TEXT(100, 10, 1), at(16, 1..3)) {
      label = "Remarques"
      help = "Remarques concernant fournisseur"
      columns(l.notes)
    }
    val utilisateur = visit(STRING(50), at(18, 1..2)) {
      label = "Utilisateur"
      options(FieldOption.TRANSIENT)
    }
    val crééLe = skipped(DATETIME, at(19, 1..2)) {
      label = "Créé le"
      help = "la date de création du Licence"
      columns(l.created_at)
    }
    val modifiéLe = skipped(DATETIME, at(20, 1..2)) {
      label = "Modifié le"
      help = "La date de modification du Licence"
      columns(l.updated_at)
    }
    val suppriméLe = skipped(DATETIME, at(21, 1..2)) {
      label = "supprimé le"
      help = "La date de suppression du Licence"
      columns(l.deleted_at)
    }

    /**
     * Supprimer un enregistrement : Mettre à jour le champ [licenses.deleted_at]
     */
    private fun supprimer(b: VBlock) {
      b.validate()
      if (Utils.chargerPostesAffectés(this@Licence.id.value!!) > 0) {
        throw VExecFailedException(MessageCode.getMessage("INV-00007"))
      } else {
        transaction {
          license_seats.update({ license_seats.license_id eq this@Licence.id.value!! })  {
            it[user_id] = getUserID()
            it[updated_at] = LocalDateTime.now()
            it[deleted_at] = LocalDateTime.now()
          }
          licenses.update({ licenses.id eq this@Licence.id.value!! }) {
            it[user_id] = getUserID()
            it[updated_at] = LocalDateTime.now()
            it[deleted_at] = LocalDateTime.now()
          }
        }
        b.form.reset()
      }
    }

    /**
     * Dupliquer une licence
     */
    private fun copier() {
      id.clear(0)
      clé.clear(0)
      idPostesDisponibles.clear(0)
      postesDisponibles.clear(0)
      idUtilisateur.clear(0)
      utilisateur.clear(0)
      crééLe.clear(0)
      modifiéLe.clear(0)
      suppriméLe.clear(0)
      postes.clear()
      this.block.setRecordFetched(0, false)
      this.setMode(Mode.INSERT)
    }

    /**
     * Sauvegarde de la licence
     */
    fun sauver(b: VBlock) {
      b.validate()
      transaction("Sauvegarde de la licence.") {
        b.save()

        val nombrePostesAvant = Utils.chargerPostes(this@Licence.id.value!!)
        val nouveauxPostes = nombrePostes.value!! - nombrePostesAvant

        if (nouveauxPostes > 0) {
          // Augmenter le nombre de postes associés à la licence
          ajouterPostes(nouveauxPostes)
        } else if (nouveauxPostes < 0 && Utils.chargerPostesDisponibles(this@Licence.id.value!!) < abs(nouveauxPostes)) {
          // Pour diminuer le nombre de postes affectés à la licence,
          // il faut vérifier qu'il y existe un nombre suffisant de postes libres qu'on peut supprimer.
          throw VExecFailedException(MessageCode.getMessage("INV-00009", arrayOf(nombrePostesAvant, nombrePostes.value!!, abs(nouveauxPostes))))
        } else if (nouveauxPostes < 0) {
           supprimerPostesLibres(nouveauxPostes)
        }
      }
      b.form.reset()
    }

    /**
     * Ajouter un nouveau poste de licence
     */
    private fun ajouterPostes(nombre: Int) {
      (1..nombre).forEach { _ ->
        license_seats.insert {
          it[license_id] = this@Licence.id.value!!
          it[user_id] = getUserID()
          it[created_at] = LocalDateTime.now()
          it[updated_at] = LocalDateTime.now()
        }
      }
    }

    /**
     * Supprimer un postes libre
     */
    private fun supprimerPostesLibres(nombre: Int) {
      (1..nombre).forEach { _ ->
        license_seats.update({
          license_seats.deleted_at.isNull() and
          license_seats.asset_id.isNull() and
          license_seats.assigned_to.isNull()
        }, limit = 1) {
          it[user_id] = getUserID()
          it[updated_at] = LocalDateTime.now()
          it[deleted_at] = LocalDateTime.now()
        }
      }
    }
  }

  val postes = page("Postes").insertBlock(Poste())

  inner class Poste : Block("Poste", 100, 20) {
    init {
      options(BlockOption.NODETAIL)
      border = Border.LINE

      command(item = associer, Mode.QUERY) { attribuerPoste() }
      command(item = dissocier, Mode.QUERY) { libérerPoste() }

      trigger(PREQRY) {
        // Forcer la condition : deleted_at IS NULL
        suppriméLe.vField.setSearchOperator(VConstants.SOP_LE)
      }
      trigger(POSTQRY) {
        for (i in 0 until postes.block.bufferSize) {
          id[i]?.let {
            poste[i] = "Poste ${i + 1}"
            if (utilisateur.value.isNullOrBlank() && actif.value == null) {
              état.value = "Disponible"
              état.setColor(null, VColor(204, 255, 204))
            } else {
              état.value = "Attribué"
              état.setColor(null, VColor(255, 204, 204))
            }
          }
        }
      }
      trigger(ACCESS) {
        licence.getMode() != Mode.QUERY.value
      }
    }

    // Définition des alias des tables du block
    val p = table(license_seats)
    val a = table(assets)
    val o = table(locations)
    val k = table(Users)

    val id = hidden(INT(11)) {
      columns(p.id) {
        priority = 1
      }
    }
    val idLicense = hidden(INT(11)) {
      alias = licence.id
      columns(p.license_id)
    }
    val poste = skipped(STRING(10), at(1)) {
      label = "Poste"
      FieldOption.NOEDIT
    }
    val idUtilisateur = hidden(INT(11)) {
      columns(k.id, nullable(p.assigned_to))
    }
    val utilisateur = skipped(STRING(50), at(1)) {
      label = "Utilisateur"
      columns(k.name)
      options(FieldOption.TRANSIENT)
    }
    val actif = skipped(Actifs(), at(1)) {
      label = "Actif"
      columns(a.id, nullable(p.asset_id))
      FieldOption.NOEDIT
    }
    val idLieu = hidden(INT(11)) {
      columns(nullable(a.location_id), nullable(o.id))
    }
    val lieu = skipped(STRING(50), at(1)) {
      label = "Lieu"
      columns(o.name)
      FieldOption.NOEDIT
    }
    val état = visit(STRING(10), at(1)) {
      label = "État"
      FieldOption.TRANSIENT
    }
    val suppriméLe = hidden(DATETIME) {
      columns(p.deleted_at)
    }
  }

  /**
   * Attribuer le poste à un actif ou à un utilisateur
   */
  fun attribuerPoste() {
    val idLicence = licence.id.value!!

    if (Utils.chargerPostesDisponibles(idLicence) == 0) {
      throw VExecFailedException(MessageCode.getMessage("INV-00001"))
    } else {
      val associer: Boolean = licence.block.form.getActiveBlock() == postes.block && postes.état[postes.currentRecord] == "Disponible"
      val idPoste: Int? = if (licence.block.form.getActiveBlock() == postes.block) postes.id[postes.currentRecord] else null

      WindowController.windowController.doModal(AffectationPosteLicence(idLicence, if (associer) idPoste else null))

      licence.clear()
      licence.id.value = idLicence
      transaction { licence.load() }
    }
  }

  /**
   * Libérer le poste de licence
   */
  fun libérerPoste() {
    val idLicence = licence.id.value!!

    if (Utils.chargerPostesAffectés(idLicence) == 0) {
      throw VExecFailedException(MessageCode.getMessage("INV-00002"))
    } else if (licence.réattribuable.value != true) {
      throw VExecFailedException(MessageCode.getMessage("INV-00011"))
    } else {
      val dissocier: Boolean = licence.block.form.getActiveBlock() == postes.block && postes.état[postes.currentRecord] == "Attribué"
      val idPoste: Int? = if (licence.block.form.getActiveBlock() == postes.block) postes.id[postes.currentRecord] else null

      WindowController.windowController.doModal(LiberationPosteLicence(idLicence, if (dissocier) idPoste else null))

      licence.clear()
      licence.id.value = idLicence
      transaction { licence.load() }
    }
  }
}
