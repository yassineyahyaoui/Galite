// ----------------------------------------------------------------------
// Copyright (c) 2013-2025 kopiLeft Services SARL, Tunisie
// Copyright (c) 2018-2025 ProGmag SAS, France
// ----------------------------------------------------------------------
// All rights reserved - tous droits réservés.
// ----------------------------------------------------------------------

package com.progmag.inventaire

import java.util.Locale

import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.notExists
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsert

import org.kopi.galite.visual.database.transaction
import org.kopi.galite.visual.domain.BOOL
import org.kopi.galite.visual.domain.Fixed
import org.kopi.galite.visual.domain.INT
import org.kopi.galite.visual.domain.STRING
import org.kopi.galite.visual.dsl.common.Icon
import org.kopi.galite.visual.dsl.form.Block
import org.kopi.galite.visual.dsl.form.BlockOption
import org.kopi.galite.visual.dsl.form.Border
import org.kopi.galite.visual.dsl.form.Form
import org.kopi.galite.visual.dsl.form.Key
import org.kopi.galite.visual.form.VConstants

import com.progmag.inventaire.base.Articles
import com.progmag.inventaire.dbschema.models
import com.progmag.inventaire.dbschema.mappingModelesSnipePDV
import com.progmag.pdv.dbschema.pdv.art_main

class CorrespondancesModeles : Form(title = "Correspondances modèles", locale = Locale.FRANCE)  {
    init {
        insertMenus()
        insertCommands()
    }

    // Définition d'un nouvel acteur : Valider les modèles saisis pour association des données.
    val valider by lazy {
        actor(menu = actionMenu, label = "Valider", help = "Valider l'association des modèles") {
            key = Key.F11
            icon = Icon.VALIDATE
        }
    }

    // Définition d'un nouvel acteur : Annuler les associations PDV / SNIPE-IT
    val annuler by lazy {
        actor(menu = actionMenu, label = "Annuler", help = "Annuler les associations des lignes sélectionnées") {
            key = Key.SHIFT_F11
            icon = Icon.UNDO
        }
    }

    val associationModeles = insertBlock(AssociationModeles())
    val affichageModelesAssociés = insertBlock(AffichageModelesAssociés())

    // ----------------------------------------------------------------------
    // 1er block : Permet l'association des modèles PDV à leurs équivalences
    // dans SNIPE-IT
    // ----------------------------------------------------------------------

    inner class AssociationModeles : Block("Association modèles PDV / SNIPE-IT ", 10000, 15) {
        init {
            options(BlockOption.NODETAIL)
            border = Border.LINE

            // Définition des commandes du block
            breakCommand
            command(item = menuQuery) { clear() ; load() }
            command(item = valider) {
                associerModeles()
                associationModeles.clear()
                associationModeles.load()
                affichageModelesAssociés.clear()
                affichageModelesAssociés.load()
            }

            trigger(INIT) {
                associationModeles.load()
                affichageModelesAssociés.load()
            }
        }

        // Définition des tables du block
        val m = table(models)
        val a = table(art_main)

        // Définition des champs du block
        val id = hidden(INT(11)) {
            columns(m.id)
        }
        val modeleSnipe = skipped(STRING(95, 2, 1, Fixed.OFF), at(1)) {
            label = "Modèle (SNIPE-IT)"
            help = "Le nom du modèle comme renseigné dans le module SNIPE-IT"
            columns(m.name) {
                priority = 9 // Pour ordonner l'affichage par ce champ
            }
        }
        val articlePDV = visit(Articles, at(1)) {
            label = "Article (PDV)"
            help = "Le code de l'article dans PDV"
            columns(a.article)
        }

        /**
         * Chargement des enregistrements de la table [models] de SNIPE-IT, non liés aux articles de PDV.
         */
        override fun load() {
            transaction {
                models.slice(models.id, models.name).select {
                    notExists(
                        // Exclure les enregistrements de la table [mapping_modeles_snipe_pdv],
                        // dont l'affectation à l'article PDV est réalisée : mapping_modeles_snipe_pdv.pdv IS NOT NULL
                        mappingModelesSnipePDV.slice(mappingModelesSnipePDV.snipe).select {
                            (mappingModelesSnipePDV.snipe eq models.name) and (mappingModelesSnipePDV.pdv.isNotNull())
                        }
                    )
                }.distinct().orderBy(models.name).forEachIndexed { index, it ->
                    // Remplir les lignes du block
                    this@AssociationModeles.id[index] = it[models.id]
                    modeleSnipe[index] = it[models.name]
                }
            }
        }

        /**
         * Associer les articles PDV renseignés à leurs équivalents de SNIPE-IT.
         */
        fun associerModeles() {
            transaction {
                (0 until block.bufferSize).filter { i ->
                    isRecordFilled(i) && !articlePDV[i].isNullOrBlank()
                }.forEach { i ->
                    // Pour chaque ligne où l'article PDV est renseigné, insérer ou mettre à jour un enregistrement
                    // à la table mapping_modeles_snipe_pdv
                    mappingModelesSnipePDV.upsert(mappingModelesSnipePDV.snipe) {
                        it[mappingModelesSnipePDV.snipe] = modeleSnipe[i]!!
                        it[mappingModelesSnipePDV.pdv] = articlePDV[i]
                    }
                }
            }
        }
    }

    // ----------------------------------------------------------------------
    // 2ème block : Permet l'affichage des modèles PDV associés à leurs
    // équivalents dans SNIPE-IT, et l'annulation de ces associations
    // en cas d'erreur détectée.
    // ----------------------------------------------------------------------

    inner class AffichageModelesAssociés : Block("Modèles associés", 10000, 10) {
        init {
            options(BlockOption.NODETAIL)
            border = Border.LINE

            // Définition des commandes du block
            command(item = annuler) {
                annulerAssociationModeles()
                associationModeles.clear()
                associationModeles.load()
                affichageModelesAssociés.clear()
                affichageModelesAssociés.load()
            }
            trigger(PREQRY) {
                // Forcer la condition : m.pdv IS NOT NULL
                articlePDV.vField.setSearchOperator(VConstants.SOP_NE)
            }
        }

        // Définition des tables du block
        val mo = table(models)
        val m = table(mappingModelesSnipePDV)

        val id = hidden(INT(11)) {
            columns(mo.id)
        }

        // Définition des champs du block
        val modeleSnipe = skipped(STRING(95, 2, 1, Fixed.OFF), at(1)) {
            label = "Modèle (SNIPE-IT)"
            help = "Le nom du modèle comme renseigné dans le module SNIPE-IT"
            columns(m.snipe, mo.name) {
                priority = 9
            }
            trigger(POSTCHG) {
                block.fetchLookupFirst(vField)
            }
        }
        val articlePDV = skipped(STRING(25), at(1)) {
            label = "Article (PDV)"
            help = "Le code de l'article associé dans PDV"
            columns(m.pdv)
        }

        val annulation = visit(BOOL, at(1)) {
            label = "Annulation"
            help = "Annuler l'association de ce modèle ?"
        }

        /**
         * Charger le contenu de la table mapping_modeles_snipe_pdv dans une transaction
         */
        override fun load() {
            transaction {
                super.load()
            }
        }

        /**
         * Annuler l'association des données modèles entre SNIPE-IT et PDV
         * des lignes du block où le champ booléen [annulation] est coché.
         */
        fun annulerAssociationModeles() {
            transaction {
                (0 until block.bufferSize).filter { i ->
                    isRecordFilled(i) && annulation[i] != null && annulation[i]!!
                }.forEach { i ->
                    // Vider le champ mapping_modeles_snipe_pdv.pdv
                    mappingModelesSnipePDV.update({ mappingModelesSnipePDV.snipe eq modeleSnipe[i]!! }) {
                        it[pdv] = null
                    }
                }
            }
        }
    }
}