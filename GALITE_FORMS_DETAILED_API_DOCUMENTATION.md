# Galite Forms - Detailed API Documentation

## Table of Contents

1. [Form Architecture](#form-architecture)
2. [Block System](#block-system)
3. [FormField API](#formfield-api)
4. [Triggers System](#triggers-system)
5. [Commands System](#commands-system)
6. [Database Integration & Joins](#database-integration--joins)
7. [Advanced Patterns](#advanced-patterns)
8. [Complete Examples](#complete-examples)

---

## Form Architecture

### Form Class Structure

Forms in Galite inherit from `DefaultDictionaryForm` or `Form` and provide the main container for business functionality.

#### Basic Form Declaration

```kotlin
class Actifs : DefaultDictionaryForm(title = "Actif") {
    init {
        // Form-level triggers
        trigger(RESET) {
            setTitle("Actif")
            return@trigger false
        }
    }

    // Page organization
    val actif = page("Détails").insertBlock(Actif())
    val licences = page("Licences").insertBlock(Licence())
}
```

#### Form Constructor Options

```kotlin
// Basic form
class SimpleForm : Form(title = "Simple Form", locale = Locale.UK)

// Dictionary form with additional features
class DictionaryForm : DefaultDictionaryForm(
    title = "Dictionary Form",
    allowInterrupt = false  // Prevents form interruption
)

// Modal form for dialogs
class ModalForm(val entityId: Int) : DefaultDictionaryForm(
    title = "Modal Form",
    allowInterrupt = false
) {
    init {
        // Initialize with specific data
        block.id.value = entityId
        transaction { block.load() }
        block.setMode(Mode.INSERT)
    }
}
```

#### Form-Level Functions

```kotlin
init {
    // Form initialization
    trigger(INIT) {
        setupForm()
    }
    
    // Form reset handling
    trigger(RESET) {
        setTitle("New Title")
        return@trigger false  // Prevent default reset behavior
    }
    
    // Form close handling
    trigger(PREFORM) {
        validateUserAccess()
    }
    
    trigger(POSTFORM) {
        cleanupResources()
    }
}

// Page management
val mainPage = page("Main Information")
val detailPage = page("Additional Details")

// Block insertion
val mainBlock = mainPage.insertBlock(MainBlock())
val detailBlock = detailPage.insertBlock(DetailBlock())
```

---

## Block System

### Block Class Structure

Blocks represent data containers that map to database tables and contain fields.

#### Block Declaration

```kotlin
inner class Actif : Block("Actif", buffer = 1, visible = 1000) {
    // buffer = 1: Single record editing
    // visible = 1000: Display up to 1000 records in lists
}

inner class Licence : Block("Licence", buffer = 100, visible = 20) {
    // buffer = 100: Multi-record buffer for performance
    // visible = 20: Show 20 records at a time
}
```

#### Block Configuration Functions

```kotlin
init {
    // Block visibility by mode
    blockVisibility(Access.VISIT, Mode.QUERY)
    
    // Block options
    options(BlockOption.NODETAIL)  // Hide detail view
    options(BlockOption.NOINSERT)  // Disable insert
    options(BlockOption.NOUPDATE)  // Disable update
    options(BlockOption.NODELETE)  // Disable delete
    
    // Block border style
    border = Border.LINE   // Options: NONE, LINE, RAISED, LOWERED
    border = Border.NONE
    
    // Block help text
    help = "This block manages asset information"
}
```

#### Standard Block Commands

```kotlin
init {
    // Standard navigation and operation commands
    breakCommand                                    // Reset/cancel changes
    command(item = serialQuery, Mode.QUERY) { serialQuery() }      // Serial search
    command(item = searchOperator, Mode.QUERY) { searchOperator() } // Search operators
    command(item = menuQuery, Mode.QUERY) { recursiveQuery() }      // Recursive query
    command(item = insertMode, Mode.QUERY) { insertMode() }         // Switch to insert mode
    command(item = save, Mode.INSERT, Mode.UPDATE) { saveBlock() }  // Save record
    command(item = delete, Mode.UPDATE) { deleteRecord() }          // Delete record
    command(item = dynamicReport) { createDynamicReport() }         // Generate report
}
```

#### Custom Block Commands

```kotlin
init {
    // Custom commands with validation
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
    
    command(item = copier, Mode.UPDATE) { 
        copyRecord() 
    }
}
```

#### Block Navigation Functions

```kotlin
// Record navigation
fun gotoFirstField()              // Move to first field
fun gotoNextField()               // Move to next field
fun gotoFirstRecord()             // Move to first record
fun gotoLastRecord()              // Move to last record

// Record state management
fun clear()                       // Clear all records
fun load()                        // Load records from database
fun setMode(mode: Mode)           // Set block mode (QUERY, INSERT, UPDATE)
fun getMode(): Int                // Get current mode
fun isRecordFilled(record: Int): Boolean  // Check if record has data

// Record operations
fun validate()                    // Validate current record
fun saveBlock()                   // Save current record
fun deleteBlock()                 // Delete current record
fun insertMode()                  // Switch to insert mode
fun setRecordFetched(record: Int, fetched: Boolean)  // Mark record as fetched
```

---

## FormField API

### Field Access Levels

#### MUSTFILL - Required Fields

```kotlin
val étiquette = mustFill(STRING(100, Convert.UPPER), at(1, 1..3)) {
    label = "Étiquette"
    help = "L'étiquette de l'actif"
    columns(a.asset_tag) {
        index = étiquetteUnique
        priority = 9
    }
}
```

#### VISIT - Editable Optional Fields

```kotlin
val série = visit(STRING(100, Convert.UPPER), at(2, 1..3)) {
    label = "Série"
    help = "Le numéro de série de l'actif"
    columns(a.serial) {
        priority = 8
    }
}
```

#### SKIPPED - Read-Only Fields

```kotlin
val utilisateur = skipped(STRING(50), at(21, 1..2)) {
    label = "Utilisateur"
    options(FieldOption.TRANSIENT)
}

val crééLe = skipped(DATETIME, at(22, 1)) {
    label = "Créé le"
    help = "la date de création du lieu"
    columns(a.created_at)
}
```

#### HIDDEN - Non-Visible Fields

```kotlin
val id = hidden(INT(11)) {
    columns(a.id)
}

val idAffecté = hidden(INT(11)) {
    columns(a.assigned_to)
}
```

### Field Positioning System

#### Basic Positioning

```kotlin
// Single position
val field1 = visit(STRING(50), at(1, 1)) { }

// Column range
val field2 = visit(STRING(100), at(1, 1..3)) { }

// Line range
val field3 = visit(STRING(50, 5, 3, Fixed.OFF), at(3..7, 1)) { }

// Both line and column ranges
val field4 = visit(STRING(100, 10, 5, Fixed.OFF), at(19, 1..3)) { }

// Following another field
val affectéÀ = skipped(STRING(100), follow(typeAffectation)) { }
```

#### Multi-Line and Multi-Column Fields

```kotlin
// Text area spanning multiple lines and columns
val remarques = visit(STRING(100, 10, 5, Fixed.OFF), at(19, 1..3)) {
    label = "Remarques"
    help = "Le nombre de mois de garantie"
    columns(a.notes)
}

// Image field spanning large area
val image = visit(IMAGE(300, 300), at(1..15, 5)) {
    label = "Image"
    help = "La photo du bien"
    columns(a.image_source)
}
```

### Field Options

#### Field Behavior Options

```kotlin
val field = visit(STRING(50), at(1, 1)) {
    // Field options
    options(FieldOption.TRANSIENT)     // Not saved to database
    options(FieldOption.NOEDIT)        // Cannot be edited
    options(FieldOption.SORTABLE)      // Can be sorted
    options(FieldOption.QUERY_UPPER)   // Convert to uppercase in query
    options(FieldOption.NO_DETAIL)     // Hide in detail view
    
    // Mode-specific access
    onInsertSkipped()                  // Skip in insert mode
    onUpdateSkipped()                  // Skip in update mode
    onInsertHidden()                   // Hide in insert mode
    onUpdateHidden()                   // Hide in update mode
    onQueryVisit()                     // Editable in query mode
}
```

#### Dynamic Field Access

```kotlin
val statusField = visit(STRING(20), at(4, 1)) {
    label = "Status"
    columns(table.status)
    
    // Dynamic access control
    access {
        when (getCurrentUserRole()) {
            "ADMIN" -> Access.VISIT
            "USER" -> Access.SKIPPED
            else -> Access.HIDDEN
        }
    }
}
```

### Field Value Management

#### Getting and Setting Values

```kotlin
// Get field value
val currentValue = field.value

// Set field value
field.value = "New Value"

// Clear field value
field.clear(recordNumber)

// Check if field has value
if (field.value != null) {
    // Process value
}

// Array access for multi-record blocks
val valueAtRecord = field[recordIndex]
field[recordIndex] = newValue
```

#### Field State Management

```kotlin
// Set field colors
field.setColor(foreground = VColor.BLACK, background = VColor.LIGHT_BLUE)

// Access underlying VField
field.vField.setAccess(Access.MUSTFILL.value)
field.vField.setSearchOperator(VConstants.SOP_LE)
```

---

## Triggers System

### Form-Level Triggers

```kotlin
init {
    trigger(INIT) {
        // Form initialization - called once when form is created
        setupDefaultValues()
    }
    
    trigger(PREFORM) {
        // Before form display - called before form is shown
        validateUserPermissions()
    }
    
    trigger(POSTFORM) {
        // After form close - called when form is closed
        cleanupResources()
    }
    
    trigger(RESET) {
        // Form reset - called when form is reset
        setTitle("Reset Title")
        return@trigger false  // Return false to prevent default reset
    }
}
```

### Block-Level Triggers

#### Database Operation Triggers

```kotlin
init {
    // Pre-database operation triggers
    trigger(PREINS) {
        // Before insert - set audit fields
        crééLe.value = LocalDateTime.now()
        modifiéLe.value = LocalDateTime.now()
        idUtilisateur.value = getUserID()
    }
    
    trigger(PREUPD) {
        // Before update - update modification fields
        modifiéLe.value = LocalDateTime.now()
        idUtilisateur.value = getUserID()
    }
    
    trigger(PREQRY) {
        // Before query - set search conditions
        suppriméLe.vField.setSearchOperator(VConstants.SOP_LE)
    }
    
    // Post-database operation triggers
    trigger(POSTINS, POSTUPD) {
        // After insert/update - load related data
        idUtilisateur.value?.let { 
            utilisateur.value = Utils.chargerUtilisateur(it) 
        }
    }
    
    trigger(POSTQRY) {
        // After query - process loaded data
        idUtilisateur.value?.let { 
            utilisateur.value = Utils.chargerUtilisateur(it) 
        }
        affectéÀ.value = chargerAffectationActif(idAffecté.value, typeAffectation.value)
        
        // Set field colors based on data
        affectéÀ.value?.let {
            typeAffectation.setColor(foreground = null, background = VColor(230, 255, 230))
            affectéÀ.setColor(foreground = null, background = VColor(230, 255, 230))
        } ?: apply {
            typeAffectation.setColor(foreground = null, background = VColor(224, 224, 235))
            affectéÀ.setColor(foreground = null, background = VColor(224, 224, 235))
        }

        // Load related blocks
        licences.block.clear()
        licences.load()

        // Update form title
        setTitle("Actif : ${étiquette.value}")
    }
}
```

#### Block Navigation Triggers

```kotlin
init {
    trigger(PREBLK) {
        // Before entering block
        println("Entering block: $title")
    }
    
    trigger(POSTBLK) {
        // After leaving block
        println("Leaving block: $title")
    }
    
    trigger(PREREC) {
        // Before entering record
        println("Entering record ${currentRecord}")
    }
    
    trigger(POSTREC) {
        // After leaving record
        println("Leaving record ${currentRecord}")
    }
}
```

#### Block Access Control Triggers

```kotlin
init {
    trigger(ACCESS) {
        // Control block access based on conditions
        actif.getMode() != Mode.QUERY.value
    }
}
```

### Field-Level Triggers

#### Field Value Triggers

```kotlin
val étiquette = mustFill(STRING(100, Convert.UPPER), at(1, 1..3)) {
    label = "Étiquette"
    
    trigger(DEFAULT) {
        // Set default value
        value = generateDefaultTag()
    }
    
    trigger(POSTCHG) {
        // After field value changes
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
    
    trigger(VALIDATE) {
        // Field validation
        if (value.isNullOrBlank()) {
            throw VExecFailedException("Field cannot be empty")
        }
    }
}
```

#### Field Navigation Triggers

```kotlin
val modèle = mustFill(Modèles(), at(5, 1..3)) {
    label = "Modèle"
    
    trigger(POSTCHG) {
        // Fetch lookup data after change
        block.fetchLookupFirst(vField)
    }
    
    trigger(AUTOLEAVE) { 
        // Automatically leave field when condition met
        true 
    }
    
    trigger(PREFLD) {
        // Before entering field
        setupFieldContext()
    }
    
    trigger(POSTFLD) {
        // After leaving field
        validateFieldData()
    }
}
```

#### Field Database Operation Triggers

```kotlin
val idUtilisateur = hidden(INT(11)) {
    columns(a.user_id)
    
    trigger(PREINS, PREUPD) {
        // Before insert/update
        value = getUserID()
    }
    
    trigger(POSTINS) {
        // After insert
        logFieldChange("User assigned: $value")
    }
}
```

---

## Commands System

### Standard Commands

#### Navigation Commands

```kotlin
init {
    // Basic navigation
    breakCommand                                    // Reset/cancel
    command(item = serialQuery, Mode.QUERY) { 
        serialQuery() 
    }
    command(item = searchOperator, Mode.QUERY) { 
        searchOperator() 
    }
    command(item = menuQuery, Mode.QUERY) { 
        recursiveQuery() 
    }
    
    // Mode switching
    command(item = insertMode, Mode.QUERY) { 
        insertMode() 
    }
    
    // Data operations
    command(item = save, Mode.INSERT, Mode.UPDATE) { 
        saveBlock() 
    }
    command(item = delete, Mode.UPDATE) { 
        deleteRecord() 
    }
    
    // Reporting
    command(item = dynamicReport) { 
        createDynamicReport() 
    }
}
```

### Custom Commands

#### Complex Business Logic Commands

```kotlin
command(item = associer, Mode.UPDATE) {
    // Validate current state
    block.validate()
    
    // Business rule validation
    if (idAffecté.value != null) {
        throw VExecFailedException(MessageCode.getMessage("INV-00005"))
    } else {
        val idBien = id.value!!
        
        // Open modal dialog
        WindowController.windowController.doModal(CommandeBien(actif.id.value!!))
        
        // Refresh after dialog
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
```

#### Record Management Commands

```kotlin
command(item = copier, Mode.UPDATE) { 
    copier() 
}

private fun copier() {
    // Clear specific fields for duplication
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
    
    // Reset record state
    setRecordFetched(0, false)
    gotoFirstField()
    setMode(Mode.INSERT)
}
```

#### Custom Delete Commands

```kotlin
command(item = delete, Mode.UPDATE) { 
    supprimer(block) 
}

private fun supprimer(b: VBlock) {
    b.validate()
    
    // Soft delete implementation
    transaction {
        assets.update({ assets.id eq this@Actif.id.value!! }) {
            it[user_id] = getUserID()
            it[updated_at] = LocalDateTime.now()
            it[deleted_at] = LocalDateTime.now()
        }
    }
    
    b.form.reset()
}
```

#### Conditional Commands

```kotlin
command(item = dissocier, Mode.QUERY) {
    if (isRecordFilled(currentRecord)) {
        libérerPoste(currentRecord)
    } else {
        throw VExecFailedException(MessageCode.getMessage("INV-00002"))
    }
}
```

---

## Database Integration & Joins

### Table Mapping

#### Basic Table Mapping

```kotlin
// Single table mapping
val a = table(assets)

// Table with primary key and sequence
val c = table(Clients, idColumn = Clients.id, sequence = Sequence("CLIENTS_ID_SEQ"))
```

#### Multiple Table Mapping for Joins

```kotlin
// Multiple tables for complex joins
val a = table(assets)           // Main table
val m = table(models)           // Lookup table 1
val s = table(status_labels)    // Lookup table 2
val l = table(licenses)         // Lookup table 3
val p = table(license_seats)    // Junction table
```

### Simple Column Mapping

```kotlin
// Direct column mapping
val id = hidden(INT(11)) {
    columns(a.id)
}

val nom = mustFill(STRING(100), at(1, 1)) {
    columns(m.name) {
        priority = 8
    }
}
```

### Complex Joins

#### Inner Joins

```kotlin
// Inner join - both columns non-nullable
val modèle = mustFill(Modèles(), at(5, 1..3)) {
    label = "Modèle"
    help = "Le modèle du bien"
    columns(m.id, nullable(a.model_id)) {  // nullable() creates LEFT JOIN
        priority = 6
    }
    trigger(POSTCHG) {
        block.fetchLookupFirst(vField)  // Fetch related data
    }
}
```

#### Left Outer Joins

```kotlin
// Left outer join using nullable()
val statut = visit(Statuts(), at(6, 1..3)) {
    label = "Statut"
    columns(s.id, nullable(a.status_id)) {
        priority = 4
    }
    trigger(POSTCHG) {
        block.fetchLookupFirst(vField)
    }
}

// Display related data from joined table
val déployable = skipped(Deployable, follow(statut)) {
    columns(s.deployable)  // From joined table
}
```

#### Multi-Table Joins

```kotlin
// Complex join across multiple tables
val licence = skipped(Licences(), at(1)) {
    label = "Nom"
    columns(l.id, nullable(p.license_id))  // license_seats -> licenses
}

val clé = skipped(STRING(38, 5, 1, Fixed.OFF), at(1)) {
    label = "Clé du produit"
    columns(l.serial)  // From joined licenses table
}
```

#### Master-Detail Relationships

```kotlin
// Master block
inner class Licence : Block("Licence", 1, 1000) {
    val id = hidden(INT(11)) {
        columns(l.id)
    }
}

// Detail block with foreign key link
inner class Poste : Block("Poste", 100, 20) {
    val idLicense = hidden(INT(11)) {
        alias = licence.id  // Link to master record
        columns(p.license_id)
    }
}
```

#### Complex Multi-Table Join Example

```kotlin
// Join across 4 tables: license_seats -> licenses -> assets -> locations -> users
val utilisateur = skipped(STRING(50), at(1)) {
    label = "Utilisateur"
    columns(k.name)  // From Users table
}

val actif = skipped(Actifs(), at(1)) {
    label = "Actif"
    columns(a.id, nullable(p.asset_id))  // license_seats -> assets
}

val lieu = skipped(STRING(50), at(1)) {
    label = "Lieu"
    columns(o.name)  // From locations table
}

// Hidden fields to establish join relationships
val idUtilisateur = hidden(INT(11)) {
    columns(k.id, nullable(p.assigned_to))  // license_seats -> users
}

val idLieu = hidden(INT(11)) {
    columns(nullable(a.location_id), nullable(o.id))  // assets -> locations
}
```

### Column Configuration

#### Column Properties

```kotlin
val field = mustFill(STRING(100), at(1, 1)) {
    columns(table.column) {
        priority = 10          // Search priority (higher = more important)
        index = uniqueIndex    // Unique constraint
    }
}
```

#### Index Definitions

```kotlin
// Define unique index
val étiquetteUnique = index(MessageCode.getMessage("INV-00012"))

// Use index in field
val étiquette = mustFill(STRING(100, Convert.UPPER), at(1, 1..3)) {
    columns(a.asset_tag) {
        index = étiquetteUnique
        priority = 9
    }
}
```

---

## Advanced Patterns

### Dynamic Field Access Control

```kotlin
val typeAffectation = mustFill(TypeAssociation(false), at(4, 1)) {
    label = "Affecté au"
    trigger(POSTCHG) {
        réinitialiserValeurs(value)
        réinitialiserAccès(value)
    }
}

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
    }
}
```

### Conditional Field Display and Colors

```kotlin
trigger(POSTQRY) {
    // Load related data
    affectéÀ.value = chargerAffectationActif(idAffecté.value, typeAffectation.value)
    
    // Set colors based on data state
    affectéÀ.value?.let {
        typeAffectation.setColor(foreground = null, background = VColor(230, 255, 230))
        affectéÀ.setColor(foreground = null, background = VColor(230, 255, 230))
    } ?: apply {
        typeAffectation.setColor(foreground = null, background = VColor(224, 224, 235))
        affectéÀ.setColor(foreground = null, background = VColor(224, 224, 235))
    }
}

// In detail block
trigger(POSTQRY) {
    (0 until block.bufferSize).filter { i -> isRecordFilled(i) }.forEach { i ->
        état[i] = "Attribuée"
        état.setColor(null, VColor(255, 204, 204))
    }
}
```

### Complex Business Logic in Commands

```kotlin
fun sauver(b: VBlock) {
    b.validate()
    
    transaction("Sauvegarde de la licence.") {
        b.save()

        val nombrePostesAvant = Utils.chargerPostes(this@Licence.id.value!!)
        val nouveauxPostes = nombrePostes.value!! - nombrePostesAvant

        if (nouveauxPostes > 0) {
            // Increase license seats
            ajouterPostes(nouveauxPostes)
        } else if (nouveauxPostes < 0 && 
                   Utils.chargerPostesDisponibles(this@Licence.id.value!!) < abs(nouveauxPostes)) {
            throw VExecFailedException(
                MessageCode.getMessage("INV-00009", 
                    arrayOf(nombrePostesAvant, nombrePostes.value!!, abs(nouveauxPostes)))
            )
        } else if (nouveauxPostes < 0) {
           supprimerPostesLibres(nouveauxPostes)
        }
    }
    
    b.form.reset()
}
```

### Modal Dialog Integration

```kotlin
command(item = associer, Mode.UPDATE) {
    block.validate()
    
    if (idAffecté.value != null) {
        throw VExecFailedException(MessageCode.getMessage("INV-00005"))
    } else {
        val idBien = id.value!!
        
        // Open modal dialog with parameter
        WindowController.windowController.doModal(CommandeBien(actif.id.value!!))
        
        // Refresh current form after modal closes
        block.clear()
        id.value = idBien
        transaction { block.load() }
    }
}
```

### Search Operator Configuration

```kotlin
trigger(PREQRY) {
    // Force specific search conditions
    suppriméLe.vField.setSearchOperator(VConstants.SOP_LE)  // Less than or equal
    // Other operators:
    // VConstants.SOP_EQ   // Equal
    // VConstants.SOP_NE   // Not equal  
    // VConstants.SOP_LT   // Less than
    // VConstants.SOP_GT   // Greater than
    // VConstants.SOP_GE   // Greater than or equal
    // VConstants.SOP_LIKE // Like pattern
}
```

---

## Complete Examples

### Master-Detail Form with Complex Joins

```kotlin
class Licences : DefaultDictionaryForm(title = "Licences") {
    val licence = page("Détails").insertBlock(Licence())
    val postes = page("Postes").insertBlock(Poste())

    inner class Licence : Block("Licence", 1, 1000) {
        init {
            blockVisibility(Access.VISIT, Mode.QUERY)
            
            // Standard commands
            breakCommand
            command(item = serialQuery, Mode.QUERY) { serialQuery() }
            command(item = save, Mode.INSERT, Mode.UPDATE) { sauver(block) }
            command(item = delete, Mode.UPDATE) { supprimer(block) }
            command(item = copier, Mode.UPDATE) { copier() }
            command(item = associer, Mode.UPDATE) { 
                block.validate() 
                attribuerPoste() 
            }
            
            // Block triggers
            trigger(PREINS) {
                crééLe.value = LocalDateTime.now()
                modifiéLe.value = LocalDateTime.now()
                idUtilisateur.value = getUserID()
            }
            
            trigger(POSTQRY) {
                idUtilisateur.value?.let { 
                    utilisateur.value = Utils.chargerUtilisateur(it) 
                }
                postes.block.clear()
                postes.load()
                setTitle("Licence : ${nom.value} - ${clé.value}")
            }
        }

        // Table mappings for joins
        val l = table(licenses)
        val a = table(license_available_seats)

        // Fields with various access levels and joins
        val id = hidden(INT(11)) {
            columns(l.id)
        }
        
        val nom = mustFill(STRING(120), at(1, 1..3)) {
            label = "Nom du logiciel"
            columns(l.name) {
                priority = 6
            }
        }
        
        val catégorie = mustFill(domain = Categories("license"), at(5, 1..2)) {
            label = "Catégorie"
            columns(l.category_id) {
                priority = 3
            }
        }
        
        // Join to license_available_seats table
        val idPostesDisponibles = visit(INT(11), at(7, 1)) {
            columns(a.license_id, l.id)  // Join condition
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
    }

    inner class Poste : Block("Poste", 100, 20) {
        init {
            options(BlockOption.NODETAIL)
            border = Border.LINE

            // Conditional command
            command(item = dissocier, Mode.QUERY) { 
                libérerPoste() 
            }

            trigger(POSTQRY) {
                // Process multiple records with colors
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

        // Complex multi-table joins
        val p = table(license_seats)
        val a = table(assets)
        val o = table(locations)
        val k = table(Users)

        val idLicense = hidden(INT(11)) {
            alias = licence.id  // Master-detail link
            columns(p.license_id)
        }
        
        // Multi-table join: license_seats -> users
        val idUtilisateur = hidden(INT(11)) {
            columns(k.id, nullable(p.assigned_to))
        }
        
        val utilisateur = skipped(STRING(50), at(1)) {
            label = "Utilisateur"
            columns(k.name)
            options(FieldOption.TRANSIENT)
        }
        
        // Multi-table join: license_seats -> assets
        val actif = skipped(Actifs(), at(1)) {
            label = "Actif"
            columns(a.id, nullable(p.asset_id))
        }
        
        // Multi-table join: assets -> locations
        val idLieu = hidden(INT(11)) {
            columns(nullable(a.location_id), nullable(o.id))
        }
        
        val lieu = skipped(STRING(50), at(1)) {
            label = "Lieu"
            columns(o.name)
        }
    }
}
```

### Dynamic Field Access Form

```kotlin
class CommandeBien() : DefaultDictionaryForm(title = "Commande des biens", allowInterrupt = false) {
    constructor(id: Int) : this() {
        commande.id.value = id
        transaction { commande.load() }
        commande.setMode(Mode.INSERT)
    }

    val commande = insertBlock(Commande())

    inner class Commande : Block("Commande des biens", 1, 100) {
        init {
            command(item = valider, Mode.INSERT) { valider(block) }
        }

        val a = table(assets)
        val m = table(models)

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
                        "App\\Models\\Asset" -> assets.slice(location_id)
                            .select { id eq actif.value!! }
                            .map { bien -> bien[location_id] }
                            .firstOrNull()
                        else -> null
                    }
                    it[updated_at] = LocalDateTime.now()
                }
            }

            b.form.close(VWindow.CDE_QUIT)
        }

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
            }
        }
    }
}
```

This comprehensive documentation covers all the major functions, patterns, and techniques used in the Galite Forms system based on the detailed analysis of the forms-examples directory. Each section includes practical examples showing how to implement complex business logic, database joins, dynamic field behavior, and sophisticated user interactions.