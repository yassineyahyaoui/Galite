// ----------------------------------------------------------------------
// Copyright (c) 2013-2024 kopiLeft Services SARL, Tunisie
// Copyright (c) 2018-2024 ProGmag SAS, France
// ----------------------------------------------------------------------
// All rights reserved - tous droits réservés.
// ----------------------------------------------------------------------

package com.progmag.inventaire

import java.time.LocalDateTime

import org.jetbrains.exposed.sql.update

import org.kopi.galite.visual.MessageCode
import org.kopi.galite.visual.VColor
import org.kopi.galite.visual.VExecFailedException
import org.kopi.galite.visual.VException
import org.kopi.galite.visual.WindowController
import org.kopi.galite.visual.database.transaction
import org.kopi.galite.visual.domain.BOOL
import org.kopi.galite.visual.domain.Convert
import org.kopi.galite.visual.domain.DATETIME
import org.kopi.galite.visual.domain.DATE
import org.kopi.galite.visual.domain.DECIMAL
import org.kopi.galite.visual.domain.Fixed
import org.kopi.galite.visual.domain.IMAGE
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

import com.progmag.inventaire.base.Deployable
import com.progmag.inventaire.base.DefaultDictionaryForm
import com.progmag.inventaire.base.Entreprises
import com.progmag.inventaire.base.Fournisseurs
import com.progmag.inventaire.base.Lieux
import com.progmag.inventaire.base.Modèles
import com.progmag.inventaire.base.Licences
import com.progmag.inventaire.base.Statuts
import com.progmag.inventaire.base.TypeAssociation
import com.progmag.inventaire.dbschema.assets
import com.progmag.inventaire.dbschema.license_seats
import com.progmag.inventaire.dbschema.licenses
import com.progmag.inventaire.dbschema.locations
import com.progmag.inventaire.dbschema.models
import com.progmag.inventaire.dbschema.status_labels

class Actifs : DefaultDictionaryForm(title = "Actif") {
  init {
    trigger(RESET) {
      setTitle("Actif")
      return@trigger false
    }
  }

  val actif = page("Détails").insertBlock(Actif())

  inner class Actif : Block("Actif", 1, 1000) {

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
      command(item = associer, Mode.UPDATE) {
        block.validate()
        if (idAffecté.value != null) {
          throw VExecFailedException(MessageCode.getMessage("INV-00005"))
        } else {
          val idBien = id.value!!

          WindowController.windowController.doModal(CommandeBien(actif.id.value!!))
          block.clear()
          id.value = idBien
          transaction { block.load() }
        }
      }
      command(item = dissocier, Mode.UPDATE) {
        block.validate()
        if (idAffecté.value == null) {
          throw VExecFailedException(MessageCode.getMessage("INV-00006"))
        } else {
          val idBien = id.value!!

          WindowController.windowController.doModal(RetourBien(actif.id.value!!))
          block.clear()
          id.value = idBien
          transaction { block.load() }
        }
      }
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
        affectéÀ.value = chargerAffectationActif(idAffecté.value, typeAffectation.value)
        // Colorier les champs typeAffectation et affectéÀ pour plus de lisibilité
        affectéÀ.value?.let {
          typeAffectation.setColor(foreground = null, background = VColor(230, 255, 230))
          affectéÀ.setColor(foreground = null, background = VColor(230, 255, 230))
        } ?: apply {
          typeAffectation.setColor(foreground = null, background = VColor(224, 224, 235))
          affectéÀ.setColor(foreground = null, background = VColor(224, 224, 235))
        }

        licences.block.clear()
        licences.load()

        setTitle("Actif : ${étiquette.value}")
      }
    }

    // Définition des alias des tables du block
    val a = table(assets)
    val m = table(models)
    val s = table(status_labels)

    // Définition des indexes à vérifier avant insertion
    val étiquetteUnique = index(MessageCode.getMessage("INV-00012"))

    // Champs du block
    val id = hidden(INT(11)) {
      columns(a.id)
    }
    val étiquette = mustFill(STRING(100, Convert.UPPER), at(1, 1..3)) {
      label = "Étiquette"
      help = "L'étiquette de l'actif"
      columns(a.asset_tag) {
        index = étiquetteUnique
        priority = 9
      }
      trigger(POSTCHG) {
        if (block.getMode() == Mode.QUERY.value) {
          val nouvelleÉtiquette = value
          try {
            transaction { block.load() }
          } catch (e: VException) {
            value = nouvelleÉtiquette
            setMode(Mode.INSERT)
            gotoNextField()
          }
        }
      }
    }
    val série = visit(STRING(100, Convert.UPPER), at(2, 1..3)) {
      label = "Série"
      help = "Le numéro de série de l'actif"
      columns(a.serial) {
        priority = 8
      }
    }
    val nom = visit(STRING(100), at(3, 1..3)) {
      label = "Nom"
      help = "Le nom de l'actif"
      columns(a.name) {
        priority = 7
      }
    }
    val typeAffectation = visit(TypeAssociation(), at(4, 1..3)) {
      label = "Affecté au"
      columns(a.assigned_type)
      onUpdateSkipped()
      onInsertSkipped()
    }
    val idAffecté = hidden(INT(11)) {
      columns(a.assigned_to)
    }
    val affectéÀ = skipped(STRING(100), follow(typeAffectation)) {
      help = "L'actif est attribué à cet élément"
    }
    val modèle = mustFill(Modèles(), at(5, 1..3)) {
      label = "Modèle"
      help = "Le modèle du bien"
      columns(m.id, nullable(a.model_id)) {
        priority = 6
      }
      trigger(POSTCHG) {
        block.fetchLookupFirst(vField)
      }
      trigger(AUTOLEAVE) { true }
    }
    val statut = visit(Statuts(), at(6, 1..3)) {
      label = "Statut"
      columns(s.id, nullable(a.status_id)) {
        priority = 4
      }
      trigger(POSTCHG) {
        block.fetchLookupFirst(vField)
      }
    }
    val déployable = skipped(Deployable, follow(statut)) {
      columns(s.deployable)
    }
    val entreprise = visit(Entreprises(), at(7, 1..3)) {
      label = "Entreprise"
      help = "L'entreprise du bien"
      columns(a.company_id) {
        priority = 5
      }
    }
    val emplacement = visit(Lieux(), at(8, 1..3)) {
      label = "Emplacement par défaut"
      help = "L'emplacement par défaut du bien."
      columns(a.rtd_location_id)
      trigger(POSTCHG) {
        lieu.value = value
      }
      trigger(AUTOLEAVE) { true }
    }
    val lieu = visit(Lieux(), at(9, 1..3)) {
      label = "Lieu"
      help = "Le lieu du bien"
      columns(a.location_id)
    }
    val fournisseur = visit(Fournisseurs(), at(10, 1..2)) {
      label = "fournisseur"
      help = "Le fournisseur de l'article"
      columns(a.supplier_id) {
        priority = 3
      }
    }
    val commande = visit(STRING(20, convert = Convert.UPPER), at(11, 1)) {
      label = "Numéro de commande"
      help = "Le numéro de la commande"
      columns(a.order_number)
    }
    val dateAchat = visit(DATE, at(12, 1)) {
      label = "Date d'achat"
      help = "La date d'achat du bien"
      columns(a.purchase_date) {
        priority = -10
      }
    }
    val prixAchat = visit(DECIMAL(21, 2), at(13, 1..2)) {
      label = "Prix d'achat (EUR)"
      help = "Le prix d'achat du bien"
      columns(a.purchase_cost)
    }
    val garantie = visit(INT(11), at(14, 1)) {
      label = "Garantie (mois)"
      help = "Le nombre de mois de garantie"
      columns(a.warranty_months)
    }
    val serveur = visit(STRING(50), at(15, 1..2)) {
      label = "Nom serveur"
      columns(a._snipeit_hostname_9)
    }
    val numéroBL = visit(STRING(50, convert = Convert.UPPER), at(16, 1..2)) {
      label = "Numéro BL"
      columns(a._snipeit_numero_bl_10)
    }
    val ethernetMAC = visit(STRING(20), at(17, 1)) {
      label = "MAC Ethernet"
      columns(a._snipeit_mac_ethernet_1)
    }
    val wifiMAC = visit(STRING(20), at(17, 2)) {
      label = "MAC Wifi"
      columns(a._snipeit_mac_wifi_2)
    }
    val drbdMAC = visit(STRING(20), at(17, 3)) {
      label = "MAC DRBD"
      columns(a._snipeit_mac_drbd_3)
    }
    val macManager = visit(STRING(20), at(17, 4)) {
      label = "MAC Manager"
      columns(a._snipeit_mac_manager_4)
    }
    val ipEthernet = visit(STRING(20), at(18, 1)) {
      label = "IP Ethernet "
      columns(a._snipeit_ip_adress_ethernet_5)
    }
    val ipWifi = visit(STRING(20), at(18, 2)) {
      label = "IP Wifi"
      columns(a._snipeit_ip_adress_wifi_6)
    }
    val ipDRBD = visit(STRING(20), at(18, 3)) {
      label = "IP DRBD"
      columns(a._snipeit_ip_adress_wifi_6)
    }
    val ipManager = visit(STRING(20), at(18, 4)) {
      label = "IP Manager"
      columns(a._snipeit_ip_adress_wifi_6)
    }
    val remarques = visit(STRING(100, 10, 5, Fixed.OFF), at(19, 1..3)) {
      label = "Remarques"
      help = "Le nombre de mois de garantie"
      columns(a.notes)
    }
    val idUtilisateur = hidden(INT(11)) {
      columns(a.user_id)
      trigger(PREINS, PREUPD) {
        value = getUserID()
      }
    }
    val utilisateur = skipped(STRING(50), at(21, 1..2)) {
      label = "Utilisateur"
      options(FieldOption.TRANSIENT)
    }
    val crééLe = skipped(DATETIME, at(22, 1)) {
      label = "Créé le"
      help = "la date de création du lieu"
      columns(a.created_at)
    }
    val modifiéLe = skipped(DATETIME, at(23, 1)) {
      label = "Modifié le"
      help = "La date de modification du lieu"
      columns(a.updated_at)
    }
    val suppriméLe = hidden(DATETIME) {
      label = "Supprimé le"
      help = "La date de suppression du lieu"
      columns(a.deleted_at)
    }
    val imageModèle = skipped(IMAGE(300, 300), at(1..15, 4)) {
      label = "Image Modèle"
      help = "La photo du modèle"
      columns(m.image_source)
      options(FieldOption.TRANSIENT)
    }
    val image = visit(IMAGE(300, 300), at(1..15, 5)) {
      label = "Image"
      help = "La photo du bien"
      columns(a.image_source)
    }

    /**
     * Supprimer un enregistrement : Mettre à jour le champ [locations.deleted_at]
     */
    private fun supprimer(b: VBlock) {
      b.validate()
      transaction {
        assets.update({ assets.id eq this@Actif.id.value!! }) {
          it[user_id] = getUserID()
          it[updated_at] = LocalDateTime.now()
          it[deleted_at] = LocalDateTime.now()
        }
      }
      b.form.reset()
    }

    /**
     * Dupliquer un actif
     */
    private fun copier() {
      id.clear(0)
      étiquette.clear(0)
      série.clear(0)
      idAffecté.clear(0)
      typeAffectation.clear(0)
      affectéÀ.clear(0)
      idUtilisateur.clear(0)
      utilisateur.clear(0)
      crééLe.clear(0)
      modifiéLe.clear(0)
      suppriméLe.clear(0)
      licences.clear()
      setRecordFetched(0, false)
      gotoFirstField()
      setMode(Mode.INSERT)
    }

    /**
     * Charger l'élément auquel l'actif est associé
     */
    private fun chargerAffectationActif(id: Int?, typeAffectation: String?): String? {
      return id?.let {
        when (typeAffectation) {
          "App\\Models\\User" -> Utils.chargerUtilisateur(it)
          "App\\Models\\Location" -> Utils.chargerNomEmplacement(it)
          "App\\Models\\Asset" -> Utils.chargerDescriptionActif(it)
          else -> null
        }
      }
    }
  }

  val licences = page("Licences").insertBlock(Licence())

  inner class Licence : Block("Licence", 100, 20) {
    init {
      options(BlockOption.NODETAIL)
      border = Border.LINE

      command(item = dissocier, Mode.QUERY) {
        if (isRecordFilled(currentRecord)) {
          libérerPoste(currentRecord)
        } else {
          throw VExecFailedException(MessageCode.getMessage("INV-00002"))
        }
      }

      trigger(PREQRY) {
        // Forcer la condition : deleted_at IS NULL
        suppriméLe.vField.setSearchOperator(VConstants.SOP_LE)
      }
      trigger(POSTQRY) {
        (0 until block.bufferSize).filter { i -> isRecordFilled(i) }.forEach { i ->
          état[i] = "Attribuée"
          état.setColor(null, VColor(255, 204, 204))
        }
      }
      trigger(ACCESS) {
        actif.getMode() != Mode.QUERY.value
      }
    }

    val p = table(license_seats)
    val l = table(licenses)

    val id = hidden(INT(11)){
      columns(p.id)
    }
    val idActif = hidden(INT(11)) {
      alias = actif.id
      columns(p.asset_id)
    }
    val licence = skipped(Licences(), at(1)) {
      label = "Nom"
      columns(l.id, nullable(p.license_id))
      FieldOption.NOEDIT
    }
    val clé = skipped(STRING(38, 5, 1, Fixed.OFF), at(1)) {
      label = "Clé du produit"
      columns(l.serial)
      FieldOption.NOEDIT
    }
    val état = visit(STRING(10), at(1)) {
      label = "État"
      options(FieldOption.TRANSIENT)
    }
    val réAttribuable = hidden(BOOL) {
      columns(l.reassignable)
    }
    val suppriméLe = hidden(DATETIME) {
      columns(p.deleted_at)
    }

    /**
     * Libérer le poste de licence
     */
    fun libérerPoste(rec: Int) {
      val idActif = actif.id.value

      if (réAttribuable[rec] != true) {
        throw VExecFailedException(MessageCode.getMessage("INV-00011"))
      }
      WindowController.windowController.doModal(LiberationPosteLicence(licence[rec]!!, id[rec], false))

      actif.clear()
      actif.id.value = idActif
      transaction { actif.load() }
    }
  }
}
