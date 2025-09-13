# Galite Forms - Complete Developer Guide

## Table of Contents

1. [Introduction](#introduction)
2. [Form Architecture](#form-architecture)
3. [Basic Form Structure](#basic-form-structure)
4. [Blocks and Fields](#blocks-and-fields)
5. [Field Types and Access Levels](#field-types-and-access-levels)
6. [Field Positioning](#field-positioning)
7. [Database Integration](#database-integration)
8. [Triggers and Events](#triggers-and-events)
9. [Commands and Actions](#commands-and-actions)
10. [Form Pages and Navigation](#form-pages-and-navigation)
11. [Advanced Features](#advanced-features)
12. [Complete Examples](#complete-examples)
13. [Best Practices](#best-practices)
14. [Troubleshooting](#troubleshooting)

## Introduction

Forms are the primary user interface component in Galite for data entry, editing, and viewing. They provide a structured way to interact with database records through blocks and fields, with built-in validation, triggers, and navigation capabilities.

### Key Concepts

- **Form**: The main container that holds blocks and manages overall behavior
- **Block**: A logical grouping of fields that typically corresponds to a database table
- **Field**: Individual data input/display elements with specific types and behaviors
- **Page**: Tab-like containers for organizing blocks within a form
- **Trigger**: Event handlers that execute custom logic at specific points
- **Command**: User actions that can be invoked through menus or keyboard shortcuts

## Form Architecture

### Class Hierarchy

```kotlin
abstract class Form(title: String, locale: Locale? = null) : Window(title, locale) {
    val blocks: MutableList<Block>
    val pages: MutableList<FormPage>
    
    // Core methods
    fun page(title: String): FormPage
    fun <T : Block> insertBlock(block: T, init: (T.() -> Unit)?): T
    fun <T> trigger(vararg events: FormTriggerEvent<T>, method: () -> T): Trigger
}
```

### Form Lifecycle

1. **Construction**: Form class instantiated
2. **Initialization**: `init` block executed, blocks and fields created
3. **Model Creation**: Internal Vaadin models built
4. **Display**: Form rendered in UI
5. **User Interaction**: Triggers fire, commands execute
6. **Cleanup**: Form closed, resources released

## Basic Form Structure

### Minimal Form Example

```kotlin
class SimpleForm : Form(title = "Simple Example", locale = Locale.UK) {
    
    // Create a page (optional - forms can have blocks without pages)
    val mainPage = page("Main Information")
    
    // Create and insert a block
    val userBlock = mainPage.insertBlock(UserBlock())
    
    // Define the block
    inner class UserBlock : Block("Users", buffer = 1, visible = 1) {
        // Link to database table
        val users = table(Users, Users.id)
        
        // Define fields
        val name = visit(domain = STRING(50), position = at(1, 1)) {
            label = "Name"
            help = "User's full name"
            columns(users.name)
        }
        
        val email = visit(domain = STRING(100), position = at(2, 1)) {
            label = "Email"
            help = "Email address"
            columns(users.email)
        }
    }
}
```

### Complete Form Template

```kotlin
class ComprehensiveForm : Form(title = "Comprehensive Example", locale = Locale.UK) {
    
    // Initialize form components
    init {
        insertMenus()    // Add standard menus
        insertCommands() // Add standard commands
        
        // Form-level initialization trigger
        trigger(INIT) {
            // Set initial form state
            userBlock.insertMode()
        }
        
        // Form validation trigger
        trigger(CHANGED) {
            // Return false to bypass change detection
            true
        }
    }
    
    // Define custom menus
    val customMenu = menu("Custom")
    
    // Define custom actors (menu items)
    val customAction = actor(menu = customMenu, label = "Custom Action", help = "Perform custom operation") {
        key = Key.F10
        icon = Icon.CUSTOM
    }
    
    // Create pages for organization
    val mainPage = page("Main Information") 
    val detailPage = page("Additional Details")
    
    // Insert blocks into pages
    val userBlock = mainPage.insertBlock(UserBlock())
    val addressBlock = detailPage.insertBlock(AddressBlock())
    
    // Define blocks (see detailed block examples below)
    inner class UserBlock : Block("Users", buffer = 1, visible = 10) {
        // Block implementation...
    }
    
    inner class AddressBlock : Block("Addresses", buffer = 5, visible = 5) {
        // Block implementation...
    }
    
    // Form-level commands
    val customCommand = command(item = customAction) {
        // Custom action implementation
        performCustomOperation()
    }
    
    private fun performCustomOperation() {
        // Custom logic here
        model.notice("Custom operation completed")
    }
}
```

## Blocks and Fields

### Block Structure and Properties

```kotlin
inner class DetailedBlock : Block("Example Block", buffer = 10, visible = 5) {
    
    // Database table linking
    val mainTable = table(Users, Users.id)
    val lookupTable = table(Departments) // Lookup table for joins
    
    // Block properties
    init {
        // Set block options
        options(BlockOption.NODETAIL, BlockOption.NOCHART)
        
        // Set block border
        border = Border.LINE
        
        // Set block help
        help = "This block manages user information"
        
        // Block visibility by mode
        blockVisibility(Access.VISIT, Mode.QUERY, Mode.UPDATE)
        blockVisibility(Access.HIDDEN, Mode.INSERT)
    }
    
    // Block triggers
    init {
        trigger(PREBLK) {
            // Executed when entering the block
            println("Entering block: ${title}")
        }
        
        trigger(POSTBLK) {
            // Executed when leaving the block
            println("Leaving block: ${title}")
        }
        
        trigger(PREQRY) {
            // Executed before database query
            // Set up query conditions
            if (searchCriteria.isNotEmpty()) {
                // Apply search filters
            }
        }
        
        trigger(POSTQRY) {
            // Executed after database query
            // Update related blocks or fields
            relatedBlock.load()
        }
    }
    
    // Field definitions (see field examples below)
}
```

### Block Options

```kotlin
enum class BlockOption(val value: Int) {
    NODETAIL    // Hide block in detail view
    NOCHART     // Exclude block from chart view
    NOINSERT    // Disable insert operations
    NOUPDATE    // Disable update operations
    NODELETE    // Disable delete operations
}

// Usage
init {
    options(BlockOption.NODETAIL, BlockOption.NOCHART)
}
```

### Block Alignment

```kotlin
// Align blocks relative to each other
init {
    align(targetBlock, 
          fieldInThisBlock to fieldInTargetBlock,
          anotherField to anotherTargetField)
}
```

## Field Types and Access Levels

### Field Access Levels

```kotlin
// MUSTFILL - Required fields that must be filled
val requiredField = mustFill(domain = STRING(50), position = at(1, 1)) {
    label = "Required Field"
    help = "This field must be filled"
    columns(table.requiredColumn)
}

// VISIT - Editable optional fields
val editableField = visit(domain = STRING(100), position = at(1, 2)) {
    label = "Editable Field"
    help = "This field can be edited"
    columns(table.editableColumn)
}

// SKIPPED - Read-only fields
val readOnlyField = skipped(domain = STRING(50), position = at(2, 1)) {
    label = "Read Only"
    help = "This field cannot be edited"
    columns(table.readOnlyColumn)
}

// HIDDEN - Fields not visible in UI but available for processing
val hiddenField = hidden(domain = LONG(10)) {
    label = "Hidden ID"
    help = "Internal identifier"
    columns(table.hiddenId)
}
```

### Dynamic Access Control

```kotlin
val conditionalField = visit(domain = STRING(50), position = at(3, 1)) {
    label = "Conditional Field"
    help = "Access changes based on conditions"
    columns(table.conditionalColumn)
    
    // Dynamic access control
    access {
        if (userRole == "ADMIN") {
            Access.VISIT
        } else {
            Access.SKIPPED
        }
    }
    
    // Mode-specific access control
    onQueryVisit()      // Editable in query mode
    onInsertMustFill()  // Required in insert mode
    onUpdateSkipped()   // Read-only in update mode
}
```

### Field Options

```kotlin
val advancedField = visit(domain = STRING(100), position = at(1, 1)) {
    label = "Advanced Field"
    help = "Field with various options"
    columns(table.advancedColumn)
    
    // Field options
    options(
        FieldOption.SORTABLE,        // Add sort arrows
        FieldOption.QUERY_UPPER,     // Convert to uppercase in query
        FieldOption.NO_DETAIL,       // Hide in detail view
        FieldOption.TRANSIENT        // Don't track changes
    )
    
    // Alignment
    align = FieldAlignment.CENTER
}

// Password field example
val passwordField = visit(domain = STRING(50), position = at(2, 1)) {
    label = "Password"
    help = "User password"
    columns(table.password)
    
    options(FieldOption.NOECHO)  // Hide characters with asterisks
}

// Non-editable field
val calculatedField = visit(domain = DECIMAL(10, 2), position = at(3, 1)) {
    label = "Total"
    help = "Calculated total"
    columns(table.total)
    
    options(FieldOption.NOEDIT)  // Prevent editing
}
```

## Field Positioning

### Basic Positioning

```kotlin
// Simple coordinate positioning
val field1 = visit(domain = STRING(50), position = at(line = 1, column = 1)) {
    label = "Field 1"
}

// Spanning multiple columns
val wideField = visit(domain = STRING(200), position = at(line = 1, columnRange = 2..4)) {
    label = "Wide Field"
}

// Spanning multiple lines
val tallField = visit(domain = TEXT(50, 5), position = at(lineRange = 2..4, column = 1)) {
    label = "Tall Field"
}

// Spanning both lines and columns
val largeField = visit(domain = TEXT(100, 3), position = at(lineRange = 2..4, columnRange = 2..4)) {
    label = "Large Field"
}
```

### Relative Positioning

```kotlin
val baseField = visit(domain = STRING(50), position = at(1, 1)) {
    label = "Base Field"
}

// Position field to follow another field
val followingField = visit(domain = STRING(50), position = follow(baseField)) {
    label = "Following Field"
}

// Multi-field positioning (for blocks with multiple records)
val multiField = visit(domain = STRING(30), position = at(lineRange = 1..3)) {
    label = "Multi Field"
}
```

### Complex Layout Example

```kotlin
inner class LayoutBlock : Block("Layout Example", buffer = 1, visible = 1) {
    val table = table(Users, Users.id)
    
    // Row 1: ID and Status (short fields)
    val id = skipped(domain = LONG(10), position = at(1, 1)) {
        label = "ID"
        columns(table.id)
    }
    
    val status = visit(domain = StatusDomain, position = at(1, 2)) {
        label = "Status"
        columns(table.status)
    }
    
    val active = visit(domain = BOOL, position = at(1, 3)) {
        label = "Active"
        columns(table.active)
    }
    
    // Row 2: Name spanning multiple columns
    val fullName = mustFill(domain = STRING(100), position = at(2, 1..3)) {
        label = "Full Name"
        columns(table.fullName)
    }
    
    // Row 3-4: Address (multi-line field)
    val address = visit(domain = TEXT(100, 3), position = at(lineRange = 3..4, columnRange = 1..2)) {
        label = "Address"
        columns(table.address)
    }
    
    val country = visit(domain = STRING(50), position = at(3, 3)) {
        label = "Country"
        columns(table.country)
    }
    
    val zipCode = visit(domain = STRING(10), position = at(4, 3)) {
        label = "Zip Code"
        columns(table.zipCode)
    }
    
    // Row 5: Email spanning remaining columns
    val email = visit(domain = STRING(150), position = at(5, 1..3)) {
        label = "Email Address"
        columns(table.email)
    }
}
```

## Database Integration

### Table Linking

```kotlin
inner class DatabaseBlock : Block("Database Integration", buffer = 10, visible = 10) {
    
    // Main table (first table is always the main table)
    val users = table(Users, idColumn = Users.id, sequence = Sequence("USERS_SEQ"))
    
    // Lookup tables (for joins and foreign key relationships)
    val departments = table(Departments)
    val roles = table(Roles)
    
    // Fields with database columns
    val userId = visit(domain = LONG(10), position = at(1, 1)) {
        label = "User ID"
        help = "Unique user identifier"
        columns(users.id) {
            priority = 1           // Query priority
            onInsertSkipped()      // Auto-generated, skip in insert
            onUpdateSkipped()      // Cannot be updated
        }
    }
    
    val userName = mustFill(domain = STRING(50), position = at(1, 2)) {
        label = "User Name"
        help = "Login name"
        columns(users.username) {
            priority = 2
        }
    }
    
    // Foreign key field with lookup
    val departmentId = visit(domain = DepartmentList, position = at(2, 1)) {
        label = "Department"
        help = "User's department"
        columns(departments.id, users.departmentId) {
            priority = 3
        }
        
        // Trigger to update related fields when department changes
        trigger(POSTCHG) {
            // Fetch department details
            block.fetchLookupFirst(vField)
            
            // Update department name field
            departmentName.value = departments.name.value
        }
    }
    
    val departmentName = skipped(domain = STRING(100), position = at(2, 2)) {
        label = "Department Name"
        help = "Name of the department"
        columns(departments.name)
    }
}
```

### List Domains for Lookups

```kotlin
// Define list domain for dropdown/lookup functionality
object DepartmentList : ListDomain<Int>(width = 30) {
    override val table = Departments
    
    init {
        // Define columns to display in lookup
        "ID"           keyOf Departments.id          hasWidth 10
        "Name"         keyOf Departments.name        hasWidth 50
        "Manager"      keyOf Departments.manager     hasWidth 30
        "Budget"       keyOf Departments.budget      hasWidth 15
    }
}

// Code domain for fixed value lists
object StatusDomain : CodeDomain<String>() {
    init {
        "Active"    keyOf "A"
        "Inactive"  keyOf "I"  
        "Pending"   keyOf "P"
        "Suspended" keyOf "S"
    }
}
```

### Custom Query Conditions

```kotlin
inner class QueryBlock : Block("Query Example", buffer = 50, visible = 20) {
    val users = table(Users)
    
    // Search fields
    val searchName = visit(domain = STRING(50), position = at(1, 1)) {
        label = "Search Name"
        help = "Search by name pattern"
        // Not linked to database column - used for search only
    }
    
    val searchActive = visit(domain = BOOL, position = at(1, 2)) {
        label = "Active Only"
        help = "Show only active users"
    }
    
    // Data fields
    val name = visit(domain = STRING(50), position = at(2, 1)) {
        label = "Name"
        columns(users.name)
    }
    
    val email = visit(domain = STRING(100), position = at(2, 2)) {
        label = "Email"
        columns(users.email)
    }
    
    init {
        // Custom query logic
        trigger(PREQRY) {
            val conditions = mutableListOf<Op<Boolean>>()
            
            // Add name search condition
            if (!searchName.value.isNullOrEmpty()) {
                conditions.add(Users.name like "%${searchName.value}%")
            }
            
            // Add active filter
            if (searchActive.value == true) {
                conditions.add(Users.active eq true)
            }
            
            // Apply combined conditions
            if (conditions.isNotEmpty()) {
                val combinedCondition = conditions.reduce { acc, condition -> 
                    acc and condition 
                }
                // Set query condition (framework-specific method)
                setQueryCondition(combinedCondition)
            }
        }
    }
}
```

## Triggers and Events

### Form-Level Triggers

```kotlin
class TriggerExampleForm : Form(title = "Trigger Examples", locale = Locale.UK) {
    
    init {
        // Form initialization - executed once when form is created
        trigger(INIT) {
            println("Form initialized")
            // Set default values, initialize components
            mainBlock.insertMode()
        }
        
        // Pre-form display - executed before form is shown
        trigger(PREFORM) {
            println("Form about to be displayed")
            // Load initial data, set up UI state
            loadInitialData()
        }
        
        // Post-form close - executed when form is closed
        trigger(POSTFORM) {
            println("Form is closing")
            // Cleanup, save preferences
            cleanup()
        }
        
        // Form reset - executed when form is reset
        trigger(RESET) {
            println("Form reset")
            // Return true to allow reset, false to prevent
            true
        }
        
        // Change detection - executed to check if form has changes
        trigger(CHANGED) {
            // Custom change detection logic
            hasUnsavedChanges()
        }
        
        // Quit validation - executed when user tries to quit
        trigger(QUITFORM) {
            if (hasUnsavedChanges()) {
                // Show confirmation dialog
                showConfirmationDialog("Unsaved changes will be lost. Continue?")
            } else {
                true // Allow quit
            }
        }
    }
    
    val mainBlock = insertBlock(MainBlock())
    
    private fun loadInitialData() {
        // Load initial form data
    }
    
    private fun cleanup() {
        // Cleanup resources
    }
    
    private fun hasUnsavedChanges(): Boolean {
        return mainBlock.isChanged
    }
}
```

### Block-Level Triggers

```kotlin
inner class TriggerBlock : Block("Trigger Block", buffer = 10, visible = 5) {
    val users = table(Users, Users.id)
    
    init {
        // Block entry/exit triggers
        trigger(PREBLK) {
            println("Entering block")
            // Block initialization, security checks
        }
        
        trigger(POSTBLK) {
            println("Leaving block")
            // Block cleanup, validation
        }
        
        // Record navigation triggers
        trigger(PREREC) {
            println("Entering record ${activeRecord}")
            // Record-level initialization
        }
        
        trigger(POSTREC) {
            println("Leaving record ${activeRecord}")
            // Record-level validation, cleanup
        }
        
        // Database operation triggers
        trigger(PREQRY) {
            println("Before query")
            // Set up query conditions, prepare data
        }
        
        trigger(POSTQRY) {
            println("After query - loaded ${recordCount} records")
            // Process loaded data, update related blocks
            updateRelatedBlocks()
        }
        
        trigger(PRESAVE) {
            println("Before save")
            // Final validation before database save
            validateBusinessRules()
        }
        
        trigger(PREINS) {
            println("Before insert")
            // Set default values, validate insert
            setInsertDefaults()
        }
        
        trigger(POSTINS) {
            println("After insert - new ID: ${id.value}")
            // Post-insert processing, update related data
        }
        
        trigger(PREUPD) {
            println("Before update")
            // Validate update operation
        }
        
        trigger(POSTUPD) {
            println("After update")
            // Post-update processing
        }
        
        trigger(PREDEL) {
            println("Before delete")
            // Validate delete operation, check dependencies
            if (hasRelatedRecords()) {
                throw VExecFailedException("Cannot delete: related records exist")
            }
        }
        
        trigger(POSTDEL) {
            println("After delete")
            // Cleanup related data
        }
        
        // Block validation triggers
        trigger(VALBLK) {
            println("Validating block")
            // Block-level validation
            validateBlockData()
        }
        
        trigger(VALREC) {
            println("Validating record")
            // Record-level validation
            validateRecordData()
        }
        
        // Special triggers
        trigger(DEFAULT) {
            println("Setting defaults")
            // Set default values in insert mode
            setDefaultValues()
        }
        
        trigger(ACCESS) {
            // Dynamic block access control
            when (currentMode) {
                Mode.QUERY -> true
                Mode.INSERT -> userHasInsertPermission()
                Mode.UPDATE -> userHasUpdatePermission()
                else -> false
            }
        }
        
        trigger(CHANGED) {
            // Custom change detection for block
            hasBlockChanged()
        }
    }
    
    // Field definitions...
    val id = visit(domain = LONG(10), position = at(1, 1)) {
        label = "ID"
        columns(users.id)
    }
    
    private fun updateRelatedBlocks() {
        // Update related blocks after query
    }
    
    private fun validateBusinessRules() {
        // Business rule validation
    }
    
    private fun setInsertDefaults() {
        // Set default values for new records
        createdDate.value = LocalDate.now()
        createdBy.value = getCurrentUser()
    }
    
    private fun hasRelatedRecords(): Boolean {
        // Check for related records
        return false
    }
    
    private fun validateBlockData() {
        // Block validation logic
    }
    
    private fun validateRecordData() {
        // Record validation logic
    }
    
    private fun setDefaultValues() {
        // Set field defaults
    }
    
    private fun hasBlockChanged(): Boolean {
        // Custom change detection
        return isChanged
    }
}
```

### Field-Level Triggers

```kotlin
val advancedField = visit(domain = STRING(50), position = at(1, 1)) {
    label = "Advanced Field"
    help = "Field with comprehensive triggers"
    columns(table.advancedColumn)
    
    // Field entry/exit triggers
    trigger(PREFLD) {
        println("Entering field: $label")
        // Field initialization, setup
        setupFieldContext()
    }
    
    trigger(POSTFLD) {
        println("Leaving field: $label")
        // Field validation, cleanup
        validateFieldData()
    }
    
    // Value change triggers
    trigger(POSTCHG) {
        println("Field value changed to: $value")
        // React to value changes
        updateRelatedFields()
        calculateDerivedValues()
    }
    
    // Validation triggers
    trigger(PREVAL) {
        println("Before validation")
        // Pre-validation setup
    }
    
    trigger(VALFLD) {
        println("Validating field value: $value")
        // Custom field validation
        if (value != null && value.length < 3) {
            throw VFieldException("Value must be at least 3 characters")
        }
    }
    
    // Auto-leave trigger
    trigger(AUTOLEAVE) {
        // Return true to automatically move to next field
        value?.length == maxLength
    }
    
    // Default value trigger
    trigger(DEFAULT) {
        println("Setting default value")
        // Set default value when in insert mode
        value = getDefaultValue()
    }
    
    // Format trigger
    trigger(FORMAT) {
        // Custom formatting logic
        formatFieldDisplay()
    }
    
    // Action trigger (makes field clickable)
    trigger(ACTION) {
        println("Field clicked")
        // Handle field click
        showFieldDialog()
    }
    
    // Value computation trigger
    trigger(VALUE) {
        // Compute field value based on other fields
        computeFieldValue()
    }
    
    // Database operation triggers
    trigger(PREINS) {
        println("Field before insert")
        // Pre-insert field processing
    }
    
    trigger(POSTINS) {
        println("Field after insert")
        // Post-insert field processing
    }
    
    trigger(PREUPD) {
        println("Field before update")
        // Pre-update field processing
    }
    
    trigger(POSTUPD) {
        println("Field after update")
        // Post-update field processing
    }
    
    trigger(PREDEL) {
        println("Field before delete")
        // Pre-delete field processing
    }
    
    // Drag and drop triggers
    trigger(PREDROP) {
        println("Before drop on field")
        // Prepare for drop operation
    }
    
    trigger(POSTDROP) {
        println("After drop on field")
        // Process dropped data
        processDroppedData()
    }
    
    private fun setupFieldContext() {
        // Field setup logic
    }
    
    private fun validateFieldData() {
        // Field validation logic
    }
    
    private fun updateRelatedFields() {
        // Update related fields based on this field's value
    }
    
    private fun calculateDerivedValues() {
        // Calculate values in other fields
    }
    
    private fun getDefaultValue(): String {
        return "Default Value"
    }
    
    private fun formatFieldDisplay() {
        // Custom formatting
    }
    
    private fun showFieldDialog() {
        // Show custom dialog
    }
    
    private fun computeFieldValue(): String {
        // Compute and return field value
        return "Computed Value"
    }
    
    private fun processDroppedData() {
        // Process drag and drop data
    }
}
```

## Commands and Actions

### Standard Commands

```kotlin
class CommandForm : Form(title = "Command Examples", locale = Locale.UK) {
    
    init {
        // Insert standard menus and commands
        insertMenus()     // Adds File, Edit, Action, Help menus
        insertCommands()  // Adds standard commands
        insertActors()    // Adds standard actors
    }
    
    // Custom menu
    val customMenu = menu("Custom")
    
    // Custom actors
    val exportData = actor(menu = customMenu, label = "Export", help = "Export data to file") {
        key = Key.F11
        icon = Icon.EXPORT
    }
    
    val importData = actor(menu = customMenu, label = "Import", help = "Import data from file") {
        key = Key.F12
        icon = Icon.IMPORT
    }
    
    val validateAll = actor(menu = customMenu, label = "Validate All", help = "Validate all records") {
        key = Key.SHIFT_F11
        icon = Icon.VALIDATE
    }
    
    // Form-level commands
    val exportCommand = command(item = exportData) {
        exportToFile()
    }
    
    val importCommand = command(item = importData) {
        importFromFile()
    }
    
    val validateCommand = command(item = validateAll) {
        validateAllRecords()
    }
    
    val mainBlock = insertBlock(MainBlock())
    
    inner class MainBlock : Block("Main Block", buffer = 10, visible = 10) {
        val users = table(Users, Users.id)
        
        // Block-level commands
        init {
            // Standard block commands with custom implementations
            command(item = save, Mode.INSERT, Mode.UPDATE) {
                customSave()
            }
            
            command(item = delete, Mode.UPDATE) {
                customDelete()
            }
            
            command(item = insertMode, Mode.QUERY, Mode.UPDATE) {
                insertMode()
            }
            
            // Custom block commands
            command(item = exportData, Mode.QUERY) {
                exportBlockData()
            }
        }
        
        // Fields
        val id = visit(domain = LONG(10), position = at(1, 1)) {
            label = "ID"
            columns(users.id)
            
            // Field-level commands
            command(item = validateAll) {
                validateField()
            }
        }
        
        val name = mustFill(domain = STRING(50), position = at(1, 2)) {
            label = "Name"
            columns(users.name)
            
            // Mode-specific field command
            command(item = exportData, Mode.QUERY, Mode.UPDATE) {
                exportFieldData()
            }
        }
        
        private fun customSave() {
            try {
                // Custom validation
                validateBusinessRules()
                
                // Save the block
                saveBlock()
                
                // Post-save processing
                model.notice("Record saved successfully")
                
            } catch (e: VException) {
                model.error("Save failed: ${e.message}")
            }
        }
        
        private fun customDelete() {
            try {
                // Check dependencies
                if (hasRelatedRecords()) {
                    throw VExecFailedException("Cannot delete: related records exist")
                }
                
                // Confirm deletion
                if (model.ask("Are you sure you want to delete this record?")) {
                    deleteBlock()
                    model.notice("Record deleted successfully")
                }
                
            } catch (e: VException) {
                model.error("Delete failed: ${e.message}")
            }
        }
        
        private fun exportBlockData() {
            // Export current block data
            println("Exporting block data...")
        }
        
        private fun validateField() {
            // Validate specific field
            println("Validating field: ${id.value}")
        }
        
        private fun exportFieldData() {
            // Export field data
            println("Exporting field data: ${name.value}")
        }
        
        private fun validateBusinessRules() {
            // Business rule validation
        }
        
        private fun hasRelatedRecords(): Boolean {
            // Check for related records
            return false
        }
    }
    
    private fun exportToFile() {
        // Export form data to file
        println("Exporting to file...")
    }
    
    private fun importFromFile() {
        // Import data from file
        println("Importing from file...")
    }
    
    private fun validateAllRecords() {
        // Validate all records in form
        println("Validating all records...")
    }
}
```

### Command Modes

Commands can be restricted to specific modes:

```kotlin
// Command available in all modes
command(item = helpActor) {
    showHelp()
}

// Command available only in specific modes
command(item = saveActor, Mode.INSERT, Mode.UPDATE) {
    saveRecord()
}

// Command available in query and update modes
command(item = deleteActor, Mode.QUERY, Mode.UPDATE) {
    deleteRecord()
}

// Command available only in query mode
command(item = reportActor, Mode.QUERY) {
    generateReport()
}
```

## Form Pages and Navigation

### Multi-Page Forms

```kotlin
class MultiPageForm : Form(title = "Multi-Page Example", locale = Locale.UK) {
    
    // Define pages
    val personalPage = page("Personal Information") {
        // Page-specific initialization
        help = "Enter personal details"
    }
    
    val contactPage = page("Contact Information") {
        help = "Enter contact details"
    }
    
    val preferencesPage = page("Preferences") {
        help = "Configure user preferences"
    }
    
    // Blocks assigned to pages
    val personalBlock = personalPage.insertBlock(PersonalBlock())
    val contactBlock = contactPage.insertBlock(ContactBlock())
    val preferencesBlock = preferencesPage.insertBlock(PreferencesBlock())
    
    // Cross-page navigation and validation
    init {
        trigger(PREFORM) {
            // Initialize all pages
            personalBlock.load()
        }
        
        // Custom page navigation logic
        personalBlock.trigger(POSTBLK) {
            // When leaving personal block, update contact block
            contactBlock.userId.value = personalBlock.id.value
        }
    }
    
    inner class PersonalBlock : Block("Personal", buffer = 1, visible = 1) {
        val users = table(Users, Users.id)
        
        val id = visit(domain = LONG(10), position = at(1, 1)) {
            label = "ID"
            columns(users.id)
        }
        
        val firstName = mustFill(domain = STRING(50), position = at(1, 2)) {
            label = "First Name"
            columns(users.firstName)
        }
        
        val lastName = mustFill(domain = STRING(50), position = at(2, 1)) {
            label = "Last Name"
            columns(users.lastName)
        }
        
        val birthDate = visit(domain = DATE, position = at(2, 2)) {
            label = "Birth Date"
            columns(users.birthDate)
        }
    }
    
    inner class ContactBlock : Block("Contact", buffer = 1, visible = 1) {
        val contacts = table(Contacts, Contacts.id)
        
        val userId = hidden(domain = LONG(10)) {
            label = "User ID"
            columns(contacts.userId)
        }
        
        val email = visit(domain = STRING(100), position = at(1, 1..2)) {
            label = "Email"
            columns(contacts.email)
        }
        
        val phone = visit(domain = STRING(20), position = at(2, 1)) {
            label = "Phone"
            columns(contacts.phone)
        }
        
        val address = visit(domain = TEXT(100, 3), position = at(3..4, 1..2)) {
            label = "Address"
            columns(contacts.address)
        }
    }
    
    inner class PreferencesBlock : Block("Preferences", buffer = 1, visible = 1) {
        val prefs = table(UserPreferences, UserPreferences.id)
        
        val userId = hidden(domain = LONG(10)) {
            columns(prefs.userId)
        }
        
        val theme = visit(domain = ThemeDomain, position = at(1, 1)) {
            label = "Theme"
            columns(prefs.theme)
        }
        
        val language = visit(domain = LanguageDomain, position = at(1, 2)) {
            label = "Language"
            columns(prefs.language)
        }
        
        val notifications = visit(domain = BOOL, position = at(2, 1)) {
            label = "Enable Notifications"
            columns(prefs.notifications)
        }
    }
}
```

### Page Navigation Commands

```kotlin
// Custom page navigation
val nextPageActor = actor(menu = actionMenu, label = "Next Page", help = "Go to next page") {
    key = Key.CTRL_N
    icon = Icon.NEXT
}

val prevPageActor = actor(menu = actionMenu, label = "Previous Page", help = "Go to previous page") {
    key = Key.CTRL_P
    icon = Icon.PREV
}

val nextPageCommand = command(item = nextPageActor) {
    gotoNextPage()
}

val prevPageCommand = command(item = prevPageActor) {
    gotoPrevPage()
}

private fun gotoNextPage() {
    // Custom page navigation logic
    when (model.getCurrentPage()) {
        0 -> model.gotoPage(1) // Personal to Contact
        1 -> model.gotoPage(2) // Contact to Preferences
        else -> model.notice("Already on last page")
    }
}

private fun gotoPrevPage() {
    when (model.getCurrentPage()) {
        1 -> model.gotoPage(0) // Contact to Personal
        2 -> model.gotoPage(1) // Preferences to Contact
        else -> model.notice("Already on first page")
    }
}
```

## Advanced Features

### Field Calculations and Dependencies

```kotlin
inner class CalculationBlock : Block("Calculations", buffer = 10, visible = 5) {
    val orders = table(Orders, Orders.id)
    val items = table(OrderItems)
    
    // Input fields
    val quantity = visit(domain = INT(5), position = at(1, 1)) {
        label = "Quantity"
        columns(items.quantity)
        
        trigger(POSTCHG) {
            calculateTotal()
        }
    }
    
    val unitPrice = visit(domain = DECIMAL(10, 2), position = at(1, 2)) {
        label = "Unit Price"
        columns(items.unitPrice)
        
        trigger(POSTCHG) {
            calculateTotal()
        }
    }
    
    val discountPercent = visit(domain = DECIMAL(5, 2), position = at(1, 3)) {
        label = "Discount %"
        columns(items.discountPercent)
        
        trigger(POSTCHG) {
            calculateTotal()
        }
        
        trigger(VALFLD) {
            if (value != null && (value < BigDecimal.ZERO || value > BigDecimal("100"))) {
                throw VFieldException("Discount must be between 0 and 100")
            }
        }
    }
    
    // Calculated fields
    val subtotal = skipped(domain = DECIMAL(12, 2), position = at(2, 1)) {
        label = "Subtotal"
        columns(items.subtotal)
        options(FieldOption.NOEDIT)
    }
    
    val discountAmount = skipped(domain = DECIMAL(12, 2), position = at(2, 2)) {
        label = "Discount Amount"
        columns(items.discountAmount)
        options(FieldOption.NOEDIT)
    }
    
    val total = skipped(domain = DECIMAL(12, 2), position = at(2, 3)) {
        label = "Total"
        columns(items.total)
        options(FieldOption.NOEDIT)
    }
    
    // Tax calculation
    val taxRate = visit(domain = DECIMAL(5, 4), position = at(3, 1)) {
        label = "Tax Rate"
        columns(items.taxRate)
        
        trigger(POSTCHG) {
            calculateTax()
        }
        
        trigger(DEFAULT) {
            // Set default tax rate based on location
            value = getDefaultTaxRate()
        }
    }
    
    val taxAmount = skipped(domain = DECIMAL(12, 2), position = at(3, 2)) {
        label = "Tax Amount"
        columns(items.taxAmount)
        options(FieldOption.NOEDIT)
    }
    
    val finalTotal = skipped(domain = DECIMAL(12, 2), position = at(3, 3)) {
        label = "Final Total"
        columns(items.finalTotal)
        options(FieldOption.NOEDIT)
    }
    
    private fun calculateTotal() {
        val qty = quantity.value ?: 0
        val price = unitPrice.value ?: BigDecimal.ZERO
        val discount = discountPercent.value ?: BigDecimal.ZERO
        
        // Calculate subtotal
        val sub = price.multiply(BigDecimal(qty))
        subtotal.value = sub
        
        // Calculate discount amount
        val discAmt = sub.multiply(discount).divide(BigDecimal("100"))
        discountAmount.value = discAmt
        
        // Calculate total after discount
        val tot = sub.subtract(discAmt)
        total.value = tot
        
        // Recalculate tax
        calculateTax()
    }
    
    private fun calculateTax() {
        val tot = total.value ?: BigDecimal.ZERO
        val rate = taxRate.value ?: BigDecimal.ZERO
        
        // Calculate tax amount
        val tax = tot.multiply(rate)
        taxAmount.value = tax
        
        // Calculate final total
        finalTotal.value = tot.add(tax)
    }
    
    private fun getDefaultTaxRate(): BigDecimal {
        // Get default tax rate based on business logic
        return BigDecimal("0.0875") // 8.75%
    }
}
```

### Conditional Field Display

```kotlin
inner class ConditionalBlock : Block("Conditional Fields", buffer = 1, visible = 1) {
    val users = table(Users, Users.id)
    
    val userType = visit(domain = UserTypeDomain, position = at(1, 1)) {
        label = "User Type"
        columns(users.userType)
        
        trigger(POSTCHG) {
            updateFieldVisibility()
        }
    }
    
    val employeeId = visit(domain = STRING(20), position = at(2, 1)) {
        label = "Employee ID"
        columns(users.employeeId)
        
        // Dynamic access control
        access {
            if (userType.value == "EMPLOYEE") {
                Access.MUSTFILL
            } else {
                Access.HIDDEN
            }
        }
    }
    
    val department = visit(domain = DepartmentDomain, position = at(2, 2)) {
        label = "Department"
        columns(users.department)
        
        access {
            if (userType.value == "EMPLOYEE") {
                Access.VISIT
            } else {
                Access.HIDDEN
            }
        }
    }
    
    val customerCode = visit(domain = STRING(30), position = at(3, 1)) {
        label = "Customer Code"
        columns(users.customerCode)
        
        access {
            if (userType.value == "CUSTOMER") {
                Access.MUSTFILL
            } else {
                Access.HIDDEN
            }
        }
    }
    
    val creditLimit = visit(domain = DECIMAL(12, 2), position = at(3, 2)) {
        label = "Credit Limit"
        columns(users.creditLimit)
        
        access {
            if (userType.value == "CUSTOMER") {
                Access.VISIT
            } else {
                Access.HIDDEN
            }
        }
    }
    
    private fun updateFieldVisibility() {
        // Force re-evaluation of field access
        updateAccess()
        
        // Additional custom logic
        when (userType.value) {
            "EMPLOYEE" -> {
                employeeId.setColor(null, VColor.LIGHT_BLUE)
                department.setColor(null, VColor.LIGHT_BLUE)
            }
            "CUSTOMER" -> {
                customerCode.setColor(null, VColor.LIGHT_GREEN)
                creditLimit.setColor(null, VColor.LIGHT_GREEN)
            }
        }
    }
}

object UserTypeDomain : CodeDomain<String>() {
    init {
        "Employee"  keyOf "EMPLOYEE"
        "Customer"  keyOf "CUSTOMER"
        "Partner"   keyOf "PARTNER"
        "Admin"     keyOf "ADMIN"
    }
}
```

### Master-Detail Relationships

```kotlin
class MasterDetailForm : Form(title = "Master-Detail Example", locale = Locale.UK) {
    
    val masterPage = page("Orders")
    val detailPage = page("Order Items")
    
    val orderBlock = masterPage.insertBlock(OrderBlock())
    val itemBlock = detailPage.insertBlock(OrderItemBlock())
    
    inner class OrderBlock : Block("Orders", buffer = 1, visible = 10) {
        val orders = table(Orders, Orders.id)
        
        val orderId = visit(domain = LONG(10), position = at(1, 1)) {
            label = "Order ID"
            columns(orders.id)
            
            trigger(POSTCHG) {
                // Load order items when order changes
                loadOrderItems()
            }
        }
        
        val orderDate = visit(domain = DATE, position = at(1, 2)) {
            label = "Order Date"
            columns(orders.orderDate)
        }
        
        val customerId = visit(domain = CustomerDomain, position = at(2, 1)) {
            label = "Customer"
            columns(orders.customerId)
        }
        
        val status = visit(domain = OrderStatusDomain, position = at(2, 2)) {
            label = "Status"
            columns(orders.status)
        }
        
        val total = skipped(domain = DECIMAL(12, 2), position = at(3, 1)) {
            label = "Total"
            columns(orders.total)
        }
        
        init {
            trigger(POSTQRY) {
                // Load items for current order
                loadOrderItems()
            }
        }
        
        private fun loadOrderItems() {
            if (orderId.value != null) {
                // Set filter for item block
                itemBlock.orderId.value = orderId.value
                itemBlock.load()
                
                // Calculate order total
                calculateOrderTotal()
            }
        }
        
        private fun calculateOrderTotal() {
            // Calculate total from item block
            var orderTotal = BigDecimal.ZERO
            for (i in 0 until itemBlock.recordCount) {
                val itemTotal = itemBlock.total[i]
                if (itemTotal != null) {
                    orderTotal = orderTotal.add(itemTotal)
                }
            }
            total.value = orderTotal
        }
    }
    
    inner class OrderItemBlock : Block("Order Items", buffer = 20, visible = 10) {
        val items = table(OrderItems, OrderItems.id)
        val products = table(Products)
        
        val itemId = visit(domain = LONG(10), position = at(1, 1)) {
            label = "Item ID"
            columns(items.id)
        }
        
        val orderId = hidden(domain = LONG(10)) {
            label = "Order ID"
            columns(items.orderId)
        }
        
        val productId = visit(domain = ProductDomain, position = at(1, 2)) {
            label = "Product"
            columns(products.id, items.productId)
            
            trigger(POSTCHG) {
                // Update product details when product changes
                block.fetchLookupFirst(vField)
                updateProductDetails()
            }
        }
        
        val productName = skipped(domain = STRING(100), position = at(2, 1..2)) {
            label = "Product Name"
            columns(products.name)
        }
        
        val quantity = visit(domain = INT(5), position = at(3, 1)) {
            label = "Quantity"
            columns(items.quantity)
            
            trigger(POSTCHG) {
                calculateItemTotal()
            }
        }
        
        val unitPrice = visit(domain = DECIMAL(10, 2), position = at(3, 2)) {
            label = "Unit Price"
            columns(items.unitPrice)
            
            trigger(POSTCHG) {
                calculateItemTotal()
            }
        }
        
        val total = skipped(domain = DECIMAL(12, 2), position = at(3, 3)) {
            label = "Total"
            columns(items.total)
        }
        
        init {
            trigger(POSTINS, POSTUPD, POSTDEL) {
                // Recalculate order total when items change
                orderBlock.calculateOrderTotal()
            }
        }
        
        private fun updateProductDetails() {
            // Product details are automatically updated via lookup
            // Set default unit price from product
            if (unitPrice.value == null) {
                unitPrice.value = products.price.value
            }
            calculateItemTotal()
        }
        
        private fun calculateItemTotal() {
            val qty = quantity.value ?: 0
            val price = unitPrice.value ?: BigDecimal.ZERO
            total.value = price.multiply(BigDecimal(qty))
        }
    }
}
```

## Complete Examples

### Customer Management Form

```kotlin
class CustomerForm : DictionaryForm(title = "Customer Management", locale = Locale.UK) {
    
    init {
        insertMenus()
        insertCommands()
        
        // Form initialization
        trigger(INIT) {
            customerBlock.insertMode()
        }
        
        // Custom validation before closing
        trigger(QUITFORM) {
            if (hasUnsavedChanges()) {
                model.ask("You have unsaved changes. Do you want to continue?")
            } else {
                true
            }
        }
    }
    
    // Custom menus and actors
    val reportsMenu = menu("Reports")
    val toolsMenu = menu("Tools")
    
    val customerReport = actor(menu = reportsMenu, label = "Customer Report", help = "Generate customer report") {
        key = Key.F9
        icon = Icon.REPORT
    }
    
    val exportData = actor(menu = toolsMenu, label = "Export", help = "Export customer data") {
        key = Key.F11
        icon = Icon.EXPORT
    }
    
    val importData = actor(menu = toolsMenu, label = "Import", help = "Import customer data") {
        key = Key.F12
        icon = Icon.IMPORT
    }
    
    val validateData = actor(menu = toolsMenu, label = "Validate", help = "Validate all data") {
        key = Key.SHIFT_F11
        icon = Icon.VALIDATE
    }
    
    // Form pages
    val mainPage = page("Customer Information")
    val contactPage = page("Contact Details") 
    val ordersPage = page("Order History")
    
    // Blocks
    val customerBlock = mainPage.insertBlock(CustomerBlock())
    val contactBlock = contactPage.insertBlock(ContactBlock())
    val orderBlock = ordersPage.insertBlock(OrderHistoryBlock())
    
    // Form-level commands
    val reportCommand = command(item = customerReport) {
        generateCustomerReport()
    }
    
    val exportCommand = command(item = exportData) {
        exportCustomerData()
    }
    
    val importCommand = command(item = importData) {
        importCustomerData()
    }
    
    val validateCommand = command(item = validateData) {
        validateAllData()
    }
    
    inner class CustomerBlock : Block("Customers", buffer = 1, visible = 50) {
        val customers = table(Customers, Customers.id)
        
        init {
            // Block options
            options(BlockOption.NODETAIL)
            border = Border.LINE
            help = "Customer master data"
            
            // Block triggers
            trigger(POSTQRY) {
                // Load related data after query
                loadContactInfo()
                loadOrderHistory()
            }
            
            trigger(PRESAVE) {
                // Validate before save
                validateCustomerData()
            }
            
            trigger(DEFAULT) {
                // Set defaults for new customers
                setCustomerDefaults()
            }
        }
        
        // Customer ID
        val customerId = visit(domain = LONG(10), position = at(1, 1)) {
            label = "Customer ID"
            help = "Unique customer identifier"
            columns(customers.id) {
                priority = 1
                onInsertSkipped()
                onUpdateSkipped()
            }
        }
        
        // Customer Code
        val customerCode = mustFill(domain = STRING(20), position = at(1, 2)) {
            label = "Customer Code"
            help = "Unique customer code"
            columns(customers.code) {
                priority = 2
            }
            
            trigger(VALFLD) {
                // Validate customer code format
                if (value != null && !value.matches(Regex("[A-Z]{2}\\d{4}"))) {
                    throw VFieldException("Customer code must be in format XX9999")
                }
            }
            
            trigger(POSTCHG) {
                // Check for duplicate codes
                checkDuplicateCode()
            }
            
            options(FieldOption.QUERY_UPPER)
        }
        
        // Company Name
        val companyName = mustFill(domain = STRING(100), position = at(2, 1..2)) {
            label = "Company Name"
            help = "Customer company name"
            columns(customers.companyName) {
                priority = 3
            }
            
            trigger(POSTCHG) {
                // Auto-generate customer code if empty
                if (customerCode.value.isNullOrEmpty()) {
                    generateCustomerCode()
                }
            }
        }
        
        // Contact Person
        val contactPerson = visit(domain = STRING(50), position = at(3, 1)) {
            label = "Contact Person"
            help = "Primary contact person"
            columns(customers.contactPerson)
        }
        
        // Industry
        val industry = visit(domain = IndustryDomain, position = at(3, 2)) {
            label = "Industry"
            help = "Customer industry"
            columns(customers.industry)
        }
        
        // Customer Type
        val customerType = visit(domain = CustomerTypeDomain, position = at(4, 1)) {
            label = "Customer Type"
            help = "Type of customer"
            columns(customers.customerType)
            
            trigger(POSTCHG) {
                updateFieldsBasedOnType()
            }
        }
        
        // Credit Limit
        val creditLimit = visit(domain = DECIMAL(12, 2), position = at(4, 2)) {
            label = "Credit Limit"
            help = "Customer credit limit"
            columns(customers.creditLimit)
            
            access {
                if (customerType.value == "CASH") {
                    Access.HIDDEN
                } else {
                    Access.VISIT
                }
            }
            
            trigger(VALFLD) {
                if (value != null && value < BigDecimal.ZERO) {
                    throw VFieldException("Credit limit cannot be negative")
                }
            }
        }
        
        // Payment Terms
        val paymentTerms = visit(domain = PaymentTermsDomain, position = at(5, 1)) {
            label = "Payment Terms"
            help = "Default payment terms"
            columns(customers.paymentTerms)
        }
        
        // Tax ID
        val taxId = visit(domain = STRING(20), position = at(5, 2)) {
            label = "Tax ID"
            help = "Tax identification number"
            columns(customers.taxId)
            
            trigger(VALFLD) {
                validateTaxId()
            }
        }
        
        // Status
        val status = visit(domain = CustomerStatusDomain, position = at(6, 1)) {
            label = "Status"
            help = "Customer status"
            columns(customers.status)
            
            trigger(DEFAULT) {
                value = "ACTIVE"
            }
        }
        
        // Created Date
        val createdDate = skipped(domain = DATE, position = at(6, 2)) {
            label = "Created Date"
            help = "Date customer was created"
            columns(customers.createdDate)
            
            trigger(DEFAULT) {
                value = LocalDate.now()
            }
        }
        
        // Notes
        val notes = visit(domain = TEXT(200, 3), position = at(7..8, 1..2)) {
            label = "Notes"
            help = "Additional customer notes"
            columns(customers.notes)
        }
        
        // Block commands
        init {
            command(item = save, Mode.INSERT, Mode.UPDATE) {
                customSave()
            }
            
            command(item = delete, Mode.UPDATE) {
                customDelete()
            }
            
            command(item = customerReport, Mode.QUERY) {
                generateCurrentCustomerReport()
            }
            
            command(item = validateData) {
                validateCurrentCustomer()
            }
        }
        
        private fun loadContactInfo() {
            if (customerId.value != null) {
                contactBlock.customerId.value = customerId.value
                contactBlock.load()
            }
        }
        
        private fun loadOrderHistory() {
            if (customerId.value != null) {
                orderBlock.customerId.value = customerId.value
                orderBlock.load()
            }
        }
        
        private fun validateCustomerData() {
            // Comprehensive customer validation
            if (companyName.value.isNullOrBlank()) {
                throw VExecFailedException("Company name is required")
            }
            
            if (customerCode.value.isNullOrBlank()) {
                throw VExecFailedException("Customer code is required")
            }
            
            // Check for duplicate customer code
            checkDuplicateCode()
        }
        
        private fun setCustomerDefaults() {
            status.value = "ACTIVE"
            createdDate.value = LocalDate.now()
            paymentTerms.value = "NET30"
            customerType.value = "REGULAR"
        }
        
        private fun checkDuplicateCode() {
            val code = customerCode.value
            if (!code.isNullOrBlank()) {
                transaction {
                    val existing = Customers.select { 
                        (Customers.code eq code) and (Customers.id neq (customerId.value ?: 0))
                    }.count()
                    
                    if (existing > 0) {
                        throw VFieldException("Customer code '$code' already exists")
                    }
                }
            }
        }
        
        private fun generateCustomerCode() {
            val company = companyName.value
            if (!company.isNullOrBlank()) {
                // Generate code from company name
                val prefix = company.take(2).uppercase()
                val number = getNextCustomerNumber()
                customerCode.value = "$prefix${number.toString().padStart(4, '0')}"
            }
        }
        
        private fun updateFieldsBasedOnType() {
            // Update field visibility and requirements based on customer type
            updateAccess()
            
            when (customerType.value) {
                "CASH" -> {
                    paymentTerms.value = "CASH"
                    creditLimit.value = BigDecimal.ZERO
                }
                "CREDIT" -> {
                    if (paymentTerms.value == "CASH") {
                        paymentTerms.value = "NET30"
                    }
                }
            }
        }
        
        private fun validateTaxId() {
            val taxId = taxId.value
            if (!taxId.isNullOrBlank()) {
                // Validate tax ID format (simplified)
                if (!taxId.matches(Regex("\\d{2}-\\d{7}"))) {
                    throw VFieldException("Tax ID must be in format XX-XXXXXXX")
                }
            }
        }
        
        private fun customSave() {
            try {
                validateCustomerData()
                saveBlock()
                model.notice("Customer saved successfully")
                
                // Refresh related blocks
                loadContactInfo()
                
            } catch (e: VException) {
                model.error("Save failed: ${e.message}")
            }
        }
        
        private fun customDelete() {
            try {
                // Check for related records
                if (hasOrders()) {
                    throw VExecFailedException("Cannot delete customer with existing orders")
                }
                
                if (model.ask("Are you sure you want to delete this customer?")) {
                    deleteBlock()
                    model.notice("Customer deleted successfully")
                }
                
            } catch (e: VException) {
                model.error("Delete failed: ${e.message}")
            }
        }
        
        private fun generateCurrentCustomerReport() {
            if (customerId.value != null) {
                // Generate report for current customer
                generateCustomerReport(customerId.value!!)
            }
        }
        
        private fun validateCurrentCustomer() {
            try {
                validateCustomerData()
                model.notice("Customer data is valid")
            } catch (e: VException) {
                model.error("Validation failed: ${e.message}")
            }
        }
        
        private fun getNextCustomerNumber(): Int {
            // Get next sequential customer number
            return transaction {
                (Customers.select { Customers.code.isNotNull() }
                    .count().toInt() + 1)
            }
        }
        
        private fun hasOrders(): Boolean {
            return transaction {
                Orders.select { Orders.customerId eq customerId.value }.count() > 0
            }
        }
    }
    
    inner class ContactBlock : Block("Contact Information", buffer = 5, visible = 5) {
        val contacts = table(CustomerContacts, CustomerContacts.id)
        
        val customerId = hidden(domain = LONG(10)) {
            columns(contacts.customerId)
        }
        
        val contactType = visit(domain = ContactTypeDomain, position = at(1, 1)) {
            label = "Type"
            columns(contacts.contactType)
        }
        
        val contactValue = visit(domain = STRING(100), position = at(1, 2..3)) {
            label = "Contact"
            columns(contacts.contactValue)
        }
        
        val isPrimary = visit(domain = BOOL, position = at(1, 4)) {
            label = "Primary"
            columns(contacts.isPrimary)
        }
        
        val notes = visit(domain = STRING(200), position = at(2, 1..4)) {
            label = "Notes"
            columns(contacts.notes)
        }
    }
    
    inner class OrderHistoryBlock : Block("Order History", buffer = 50, visible = 10) {
        val orders = table(Orders)
        
        val customerId = hidden(domain = LONG(10)) {
            columns(orders.customerId)
        }
        
        val orderId = skipped(domain = LONG(10), position = at(1, 1)) {
            label = "Order ID"
            columns(orders.id)
        }
        
        val orderDate = skipped(domain = DATE, position = at(1, 2)) {
            label = "Date"
            columns(orders.orderDate)
        }
        
        val status = skipped(domain = OrderStatusDomain, position = at(1, 3)) {
            label = "Status"
            columns(orders.status)
        }
        
        val total = skipped(domain = DECIMAL(12, 2), position = at(1, 4)) {
            label = "Total"
            columns(orders.total)
        }
        
        init {
            options(BlockOption.NOINSERT, BlockOption.NOUPDATE, BlockOption.NODELETE)
        }
    }
    
    // Domain definitions
    object IndustryDomain : CodeDomain<String>() {
        init {
            "Manufacturing" keyOf "MFG"
            "Retail" keyOf "RTL"
            "Technology" keyOf "TECH"
            "Healthcare" keyOf "HLTH"
            "Finance" keyOf "FIN"
            "Other" keyOf "OTHER"
        }
    }
    
    object CustomerTypeDomain : CodeDomain<String>() {
        init {
            "Regular" keyOf "REGULAR"
            "Premium" keyOf "PREMIUM"
            "Cash Only" keyOf "CASH"
            "Credit" keyOf "CREDIT"
        }
    }
    
    object PaymentTermsDomain : CodeDomain<String>() {
        init {
            "Cash" keyOf "CASH"
            "Net 15" keyOf "NET15"
            "Net 30" keyOf "NET30"
            "Net 60" keyOf "NET60"
            "Net 90" keyOf "NET90"
        }
    }
    
    object CustomerStatusDomain : CodeDomain<String>() {
        init {
            "Active" keyOf "ACTIVE"
            "Inactive" keyOf "INACTIVE"
            "Suspended" keyOf "SUSPENDED"
            "Prospect" keyOf "PROSPECT"
        }
    }
    
    object ContactTypeDomain : CodeDomain<String>() {
        init {
            "Phone" keyOf "PHONE"
            "Email" keyOf "EMAIL"
            "Fax" keyOf "FAX"
            "Website" keyOf "WEBSITE"
        }
    }
    
    object OrderStatusDomain : CodeDomain<String>() {
        init {
            "Draft" keyOf "DRAFT"
            "Confirmed" keyOf "CONFIRMED"
            "Shipped" keyOf "SHIPPED"
            "Delivered" keyOf "DELIVERED"
            "Cancelled" keyOf "CANCELLED"
        }
    }
    
    // Form-level methods
    private fun hasUnsavedChanges(): Boolean {
        return customerBlock.isChanged || contactBlock.isChanged
    }
    
    private fun generateCustomerReport() {
        // Generate comprehensive customer report
        model.notice("Generating customer report...")
    }
    
    private fun generateCustomerReport(customerId: Long) {
        // Generate report for specific customer
        model.notice("Generating report for customer $customerId")
    }
    
    private fun exportCustomerData() {
        // Export customer data to file
        model.notice("Exporting customer data...")
    }
    
    private fun importCustomerData() {
        // Import customer data from file
        model.notice("Importing customer data...")
    }
    
    private fun validateAllData() {
        try {
            customerBlock.validateCurrentCustomer()
            model.notice("All data validated successfully")
        } catch (e: VException) {
            model.error("Validation failed: ${e.message}")
        }
    }
}

// Usage example
fun main() {
    runForm(form = CustomerForm::class)
}
```

## Best Practices

### 1. Form Organization

```kotlin
// Good: Well-organized form structure
class WellOrganizedForm : Form(title = "Well Organized", locale = Locale.UK) {
    
    // Group related properties together
    init {
        insertMenus()
        insertCommands()
        setupFormTriggers()
    }
    
    // Define custom menus and actors in logical groups
    private val customMenus = createCustomMenus()
    private val customActors = createCustomActors()
    private val customCommands = createCustomCommands()
    
    // Organize pages logically
    private val pages = createPages()
    
    // Create blocks with clear separation
    private val blocks = createBlocks()
    
    private fun setupFormTriggers() {
        trigger(INIT) { initializeForm() }
        trigger(PREFORM) { prepareFormDisplay() }
        trigger(POSTFORM) { cleanupForm() }
    }
    
    private fun createCustomMenus() = mapOf(
        "reports" to menu("Reports"),
        "tools" to menu("Tools")
    )
    
    // ... other initialization methods
}
```

### 2. Field Definition Patterns

```kotlin
// Good: Consistent field definition pattern
private fun createUserFields() {
    val userId = visit(domain = LONG(10), position = at(1, 1)) {
        label = "User ID"
        help = "Unique user identifier"
        columns(users.id) {
            priority = 1
            onInsertSkipped()
            onUpdateSkipped()
        }
        setupUserIdTriggers()
    }
    
    val userName = mustFill(domain = STRING(50), position = at(1, 2)) {
        label = "User Name"
        help = "Login name (required)"
        columns(users.name) {
            priority = 2
        }
        setupUserNameTriggers()
        options(FieldOption.QUERY_UPPER)
    }
}

private fun FormField<Long>.setupUserIdTriggers() {
    trigger(POSTCHG) {
        loadUserDetails()
    }
}

private fun FormField<String>.setupUserNameTriggers() {
    trigger(VALFLD) {
        validateUserName()
    }
    
    trigger(POSTCHG) {
        checkUserNameAvailability()
    }
}
```

### 3. Error Handling

```kotlin
// Good: Comprehensive error handling
private fun saveCustomerData() {
    try {
        // Pre-save validation
        validateBusinessRules()
        
        // Save operation
        transaction {
            customerBlock.saveBlock()
            contactBlock.saveBlock()
        }
        
        // Post-save operations
        updateRelatedData()
        model.notice("Customer saved successfully")
        
    } catch (e: VFieldException) {
        // Field-specific validation error
        model.error("Field validation failed: ${e.message}")
        
    } catch (e: VExecFailedException) {
        // Business rule violation
        model.error("Business rule violation: ${e.message}")
        
    } catch (e: SQLException) {
        // Database error
        model.error("Database error: ${e.message}")
        logger.error("Database error in saveCustomerData", e)
        
    } catch (e: Exception) {
        // Unexpected error
        model.error("Unexpected error occurred")
        logger.error("Unexpected error in saveCustomerData", e)
    }
}
```

### 4. Performance Optimization

```kotlin
// Good: Optimized block configuration
inner class OptimizedBlock : Block("Optimized", buffer = 20, visible = 10) {
    
    init {
        // Use appropriate buffer size for data volume
        // buffer = 20 for moderate datasets
        // visible = 10 for good UI performance
        
        // Optimize block options
        options(BlockOption.NODETAIL) // If detail view not needed
        
        // Use efficient triggers
        trigger(POSTQRY) {
            // Batch operations instead of individual calls
            batchUpdateRelatedData()
        }
    }
    
    // Optimize field definitions
    val efficientField = visit(domain = STRING(50), position = at(1, 1)) {
        label = "Efficient Field"
        columns(table.column) {
            priority = 1 // Set appropriate query priority
        }
        
        // Use efficient triggers
        trigger(POSTCHG) {
            // Debounce expensive operations
            debounceExpensiveOperation()
        }
    }
    
    private fun batchUpdateRelatedData() {
        // Batch multiple operations together
        transaction {
            // Multiple operations in single transaction
        }
    }
    
    private fun debounceExpensiveOperation() {
        // Implement debouncing for expensive operations
    }
}
```

### 5. Testing Strategies

```kotlin
// Good: Comprehensive testing approach
class CustomerFormTest {
    
    @Test
    fun testFormInitialization() {
        val form = CustomerForm()
        
        // Test form structure
        assertEquals("Customer Management", form.title)
        assertEquals(3, form.pages.size)
        assertEquals(3, form.blocks.size)
        
        // Test initial state
        assertTrue(form.customerBlock.getMode() == Mode.INSERT.value)
    }
    
    @Test
    fun testFieldValidation() {
        val form = CustomerForm()
        val block = form.customerBlock
        
        // Test required field validation
        assertThrows<VFieldException> {
            block.customerCode.value = "" // Invalid empty code
            block.customerCode.validate()
        }
        
        // Test format validation
        assertThrows<VFieldException> {
            block.customerCode.value = "INVALID" // Invalid format
            block.customerCode.validate()
        }
        
        // Test valid value
        block.customerCode.value = "AB1234"
        block.customerCode.validate() // Should not throw
    }
    
    @Test
    fun testBusinessLogic() {
        val form = CustomerForm()
        val block = form.customerBlock
        
        // Test customer type change logic
        block.customerType.value = "CASH"
        block.updateFieldsBasedOnType()
        
        assertEquals("CASH", block.paymentTerms.value)
        assertEquals(BigDecimal.ZERO, block.creditLimit.value)
        assertEquals(Access.HIDDEN, block.creditLimit.getAccess())
    }
    
    @Test
    fun testMasterDetailRelationship() {
        val form = CustomerForm()
        
        // Set customer ID
        form.customerBlock.customerId.value = 123L
        form.customerBlock.loadContactInfo()
        
        // Verify contact block is updated
        assertEquals(123L, form.contactBlock.customerId.value)
    }
}
```

## Troubleshooting

### Common Issues and Solutions

#### 1. Field Not Displaying

**Problem**: Field defined but not visible in form

**Solutions**:
```kotlin
// Check field access levels
val field = visit(domain = STRING(50), position = at(1, 1)) {
    // Make sure field is not hidden
    onQueryVisit()    // Visible in query mode
    onInsertVisit()   // Visible in insert mode  
    onUpdateVisit()   // Visible in update mode
}

// Check block buffer and visible settings
inner class MyBlock : Block("MyBlock", buffer = 1, visible = 1) {
    // buffer > 0 and visible > 0
}

// Check position conflicts
val field1 = visit(domain = STRING(50), position = at(1, 1)) { }
val field2 = visit(domain = STRING(50), position = at(1, 2)) { } // Different position
```

#### 2. Database Connection Issues

**Problem**: Fields not saving to database

**Solutions**:
```kotlin
// Ensure proper table linking
inner class MyBlock : Block("MyBlock", buffer = 1, visible = 1) {
    val table = table(MyTable, MyTable.id) // Link to database table
    
    val field = visit(domain = STRING(50), position = at(1, 1)) {
        columns(table.column) // Link field to table column
    }
}

// Check transaction handling
private fun saveData() {
    transaction {
        block.saveBlock() // Save within transaction
    }
}
```

#### 3. Trigger Not Firing

**Problem**: Trigger code not executing

**Solutions**:
```kotlin
// Check trigger syntax
trigger(POSTCHG) {  // Correct trigger event
    // Trigger code here
}

// Ensure trigger is added in correct scope
val field = visit(domain = STRING(50), position = at(1, 1)) {
    // Field-level trigger - inside field definition
    trigger(POSTCHG) {
        handleFieldChange()
    }
}

// Block-level trigger - inside block init
init {
    trigger(POSTQRY) {
        handleBlockQuery()
    }
}
```

#### 4. Validation Errors

**Problem**: Custom validation not working

**Solutions**:
```kotlin
// Use correct validation trigger
val field = visit(domain = STRING(50), position = at(1, 1)) {
    trigger(VALFLD) {  // Use VALFLD for validation
        if (value.isNullOrBlank()) {
            throw VFieldException("Field cannot be empty")
        }
    }
}

// Check exception types
trigger(PRESAVE) {
    if (businessRuleViolated()) {
        throw VExecFailedException("Business rule violation")
    }
}
```

#### 5. Performance Issues

**Problem**: Form is slow to load or respond

**Solutions**:
```kotlin
// Optimize buffer sizes
inner class OptimizedBlock : Block("Block", buffer = 50, visible = 20) {
    // Reasonable buffer size for your data volume
}

// Use efficient queries
trigger(PREQRY) {
    // Add appropriate WHERE conditions
    setQueryConditions(efficientConditions)
}

// Avoid expensive operations in frequently-fired triggers
trigger(POSTCHG) {
    // Use debouncing for expensive operations
    if (shouldPerformExpensiveOperation()) {
        performExpensiveOperation()
    }
}
```

### Debugging Tips

#### 1. Enable Debug Logging

```kotlin
init {
    if (ApplicationConfiguration.isDebugModeEnabled) {
        trigger(INIT) {
            println("Form ${this::class.simpleName} initialized")
        }
        
        trigger(PREBLK) {
            println("Entering block: $title")
        }
    }
}
```

#### 2. Validate Form Structure

```kotlin
private fun validateFormStructure() {
    // Check all blocks have proper table links
    blocks.forEach { block ->
        require(block.tables.isNotEmpty()) { "Block ${block.title} has no table links" }
    }
    
    // Check all fields have proper positioning
    blocks.forEach { block ->
        block.fields.forEach { field ->
            require(field.position != null) { "Field ${field.label} has no position" }
        }
    }
}
```

#### 3. Monitor Field Values

```kotlin
val debugField = visit(domain = STRING(50), position = at(1, 1)) {
    label = "Debug Field"
    
    trigger(POSTCHG) {
        println("Field ${label} changed from ${oldValue} to ${value}")
    }
    
    trigger(VALFLD) {
        println("Validating field ${label} with value: ${value}")
    }
}
```

This comprehensive documentation covers all aspects of Galite Forms development, from basic structure to advanced features, with extensive examples and best practices. Use it as a complete reference for building robust, maintainable forms in your Galite applications.