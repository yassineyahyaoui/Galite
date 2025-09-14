# Galite Forms - Complete Beginner's Guide

## Table of Contents

1. [Getting Started](#getting-started)
2. [Understanding Forms Architecture](#understanding-forms-architecture)
3. [Your First Form](#your-first-form)
4. [Working with Fields](#working-with-fields)
5. [Database Integration](#database-integration)
6. [Form Navigation and User Interaction](#form-navigation-and-user-interaction)
7. [Advanced Form Features](#advanced-form-features)
8. [Real-World Examples](#real-world-examples)
9. [Best Practices](#best-practices)
10. [Common Patterns](#common-patterns)
11. [Troubleshooting](#troubleshooting)

---

## Getting Started

### What are Galite Forms?

Galite Forms are powerful, database-backed user interface components that provide a complete solution for data entry, editing, and viewing. They automatically handle:

- **Database operations** (Create, Read, Update, Delete)
- **Form validation** and business rules
- **User navigation** between fields and records
- **Multi-page layouts** with tabs
- **Master-detail relationships**
- **Lookup fields** with dropdown lists

### Prerequisites

Before working with Galite Forms, you should have:
- Basic Kotlin knowledge
- Understanding of database concepts (tables, columns, relationships)
- Familiarity with the Exposed ORM library (used by Galite)

### Setting Up Your Project

Add Galite dependencies to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("org.kopi:galite-core:1.3.0")
    implementation("org.kopi:galite-data:1.3.0")
    implementation("org.kopi:galite-util:1.3.0")
}
```

---

## Understanding Forms Architecture

### Core Components

A Galite Form consists of these main components:

```
Form
├── Pages (tabs)
│   ├── Block 1 (data container)
│   │   ├── Field 1
│   │   ├── Field 2
│   │   └── Field 3
│   └── Block 2
│       ├── Field 1
│       └── Field 2
├── Menus & Commands
└── Triggers (event handlers)
```

### Key Concepts

- **Form**: The main container that holds everything
- **Page**: Tab-like sections for organizing content
- **Block**: A logical group of fields, usually linked to a database table
- **Field**: Individual data input/display elements
- **Trigger**: Event handlers that run at specific moments
- **Command**: User actions (save, delete, etc.)

---

## Your First Form

Let's create a simple form to manage users. We'll build it step by step.

### Step 1: Define Your Database Table

First, define your database table using Exposed:

```kotlin
// Tables.kt
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime

object Users : IntIdTable("users") {
    val name = varchar("name", 100)
    val email = varchar("email", 150)
    val active = bool("active").default(true)
    val createdAt = datetime("created_at")
}
```

### Step 2: Create Your First Form

```kotlin
// UserForm.kt
import org.kopi.galite.visual.dsl.form.*
import org.kopi.galite.visual.domain.*
import java.util.*

class UserForm : Form(title = "User Management", locale = Locale.UK) {
    
    // Create a page (tab)
    val mainPage = page("User Information")
    
    // Create a block and add it to the page
    val userBlock = mainPage.insertBlock(UserBlock())
    
    // Define the block
    inner class UserBlock : Block("Users", buffer = 1, visible = 10) {
        
        // Link to database table
        val users = table(Users, Users.id)
        
        // Define fields
        val id = visit(domain = LONG(10), position = at(1, 1)) {
            label = "ID"
            help = "User identifier"
            columns(users.id) {
                onInsertSkipped()  // Auto-generated, skip in insert mode
                onUpdateSkipped()  // Cannot be updated
            }
        }
        
        val name = mustFill(domain = STRING(100), position = at(1, 2)) {
            label = "Name"
            help = "User's full name"
            columns(users.name)
        }
        
        val email = visit(domain = STRING(150), position = at(2, 1..2)) {
            label = "Email"
            help = "Email address"
            columns(users.email)
        }
        
        val active = visit(domain = BOOL, position = at(3, 1)) {
            label = "Active"
            help = "Is user active?"
            columns(users.active)
        }
        
        // Add standard commands
        init {
            command(item = save) { saveBlock() }
            command(item = insertMode) { insertMode() }
            command(item = delete) { deleteRecord() }
        }
    }
}
```

### Step 3: Understanding What We Built

Let's break down what each part does:

1. **Form Declaration**: `class UserForm : Form(...)`
   - Creates the main form container
   - Sets title and locale

2. **Page Creation**: `val mainPage = page("User Information")`
   - Creates a tab called "User Information"

3. **Block Definition**: `inner class UserBlock : Block(...)`
   - `buffer = 1`: Single record editing mode
   - `visible = 10`: Show up to 10 records in list view

4. **Table Linking**: `val users = table(Users, Users.id)`
   - Links the block to the Users database table
   - Specifies the primary key column

5. **Field Definitions**: Each field has:
   - **Domain**: Data type (LONG, STRING, BOOL)
   - **Position**: Where it appears on screen `at(row, column)`
   - **Label**: Display name
   - **Help**: Tooltip text
   - **Columns**: Database column mapping

---

## Working with Fields

### Field Access Levels

Galite provides four access levels for fields:

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

### Common Domain Types

```kotlin
// Text fields
STRING(maxLength)              // Single-line text
TEXT(maxLength, height)        // Multi-line text

// Numbers
INT(width)                     // Integers
LONG(width)                    // Long integers
DECIMAL(width, scale)          // Decimal numbers

// Dates and times
DATE                           // Date only
DATETIME                       // Date and time
TIME                           // Time only

// Other types
BOOL                          // Boolean (checkbox)
IMAGE(width, height)          // Image field
```

### Field Positioning

Fields are positioned using coordinates:

```kotlin
// Basic positioning
at(1, 1)        // Row 1, Column 1
at(1, 2)        // Row 1, Column 2
at(2, 1)        // Row 2, Column 1

// Spanning multiple columns
at(1, 1..3)     // Row 1, Columns 1-3

// Spanning multiple rows
at(1..2, 1)     // Rows 1-2, Column 1

// Spanning both
at(1..2, 1..3)  // Rows 1-2, Columns 1-3

// Position relative to another field
follow(otherField)  // Position after another field
```

### Field Properties and Options

```kotlin
val advancedField = visit(domain = STRING(100), position = at(1, 1)) {
    label = "Advanced Field"
    help = "Field with various options"
    columns(table.column)
    
    // Field options
    options(
        FieldOption.SORTABLE,        // Add sort arrows
        FieldOption.QUERY_UPPER,     // Convert to uppercase in query
        FieldOption.NO_DETAIL,       // Hide in detail view
        FieldOption.TRANSIENT        // Don't track changes
    )
    
    // Mode-specific access
    onQueryVisit()      // Editable in query mode
    onInsertMustFill()  // Required in insert mode
    onUpdateSkipped()   // Read-only in update mode
}
```

---

## Database Integration

### Simple Table Mapping

```kotlin
inner class SimpleBlock : Block("Simple", buffer = 1, visible = 10) {
    // Link to single table
    val users = table(Users, Users.id)
    
    val name = visit(domain = STRING(100), position = at(1, 1)) {
        label = "Name"
        columns(users.name)  // Direct column mapping
    }
}
```

### Working with Relationships

#### One-to-Many Relationships (Lookups)

```kotlin
// Define lookup domain
object DepartmentList : ListDomain<Int>(width = 30) {
    override val table = Departments
    
    init {
        "ID"   keyOf Departments.id    hasWidth 10
        "Name" keyOf Departments.name  hasWidth 50
    }
}

// Use in form
val department = visit(domain = DepartmentList, position = at(1, 1)) {
    label = "Department"
    help = "Select user's department"
    columns(departments.id, users.departmentId)
    
    trigger(POSTCHG) {
        // Automatically fetch department details
        block.fetchLookupFirst(vField)
    }
}

// Display related data
val departmentName = skipped(domain = STRING(100), position = at(1, 2)) {
    label = "Department Name"
    columns(departments.name)  // From joined table
}
```

#### Master-Detail Relationships

```kotlin
class OrderForm : Form(title = "Orders", locale = Locale.UK) {
    
    val orderPage = page("Order")
    val itemsPage = page("Items")
    
    val orderBlock = orderPage.insertBlock(OrderBlock())
    val itemsBlock = itemsPage.insertBlock(OrderItemsBlock())
    
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
        
        private fun loadOrderItems() {
            if (orderId.value != null) {
                itemsBlock.orderId.value = orderId.value
                itemsBlock.load()
            }
        }
    }
    
    inner class OrderItemsBlock : Block("Order Items", buffer = 50, visible = 15) {
        val items = table(OrderItems, OrderItems.id)
        val products = table(Products)
        
        // Hidden link to master record
        val orderId = hidden(domain = LONG(10)) {
            columns(items.orderId)
        }
        
        val productId = visit(domain = ProductList, position = at(1, 1)) {
            label = "Product"
            columns(products.id, items.productId)
        }
        
        val quantity = visit(domain = INT(5), position = at(1, 2)) {
            label = "Quantity"
            columns(items.quantity)
        }
    }
}
```

---

## Form Navigation and User Interaction

### Form Modes

Galite forms operate in different modes:

- **QUERY**: Search and browse records
- **INSERT**: Add new records
- **UPDATE**: Edit existing records

### Adding Commands

```kotlin
inner class UserBlock : Block("Users", buffer = 1, visible = 10) {
    // ... field definitions ...
    
    init {
        // Standard commands
        command(item = save, Mode.INSERT, Mode.UPDATE) {
            customSave()
        }
        
        command(item = delete, Mode.UPDATE) {
            customDelete()
        }
        
        command(item = insertMode, Mode.QUERY) {
            insertMode()
        }
        
        // Custom commands
        command(item = validateAll) {
            validateAllData()
        }
    }
    
    private fun customSave() {
        try {
            validateBusinessRules()
            saveBlock()
            model.notice("Record saved successfully")
        } catch (e: VException) {
            model.error("Save failed: ${e.message}")
        }
    }
    
    private fun customDelete() {
        if (model.ask("Are you sure you want to delete this record?")) {
            deleteBlock()
            model.notice("Record deleted successfully")
        }
    }
    
    private fun validateAllData() {
        // Custom validation logic
        model.notice("All data is valid")
    }
}
```

### Form-Level Triggers

```kotlin
class UserForm : Form(title = "User Management", locale = Locale.UK) {
    
    init {
        // Form initialization
        trigger(INIT) {
            userBlock.insertMode()
        }
        
        // Before form display
        trigger(PREFORM) {
            setupFormDefaults()
        }
        
        // Before form close
        trigger(QUITFORM) {
            if (hasUnsavedChanges()) {
                model.ask("You have unsaved changes. Continue?")
            } else {
                true
            }
        }
    }
    
    private fun setupFormDefaults() {
        // Set default values
    }
    
    private fun hasUnsavedChanges(): Boolean {
        return userBlock.isChanged
    }
}
```

### Block-Level Triggers

```kotlin
inner class UserBlock : Block("Users", buffer = 1, visible = 10) {
    
    init {
        // Database operation triggers
        trigger(PREINS) {
            // Before insert - set audit fields
            createdAt.value = LocalDateTime.now()
            createdBy.value = getCurrentUser()
        }
        
        trigger(PREUPD) {
            // Before update
            updatedAt.value = LocalDateTime.now()
            updatedBy.value = getCurrentUser()
        }
        
        trigger(POSTQRY) {
            // After query - process loaded data
            setTitle("User: ${name.value}")
            loadRelatedData()
        }
        
        // Block navigation triggers
        trigger(PREBLK) {
            println("Entering user block")
        }
        
        trigger(POSTBLK) {
            println("Leaving user block")
        }
    }
}
```

### Field-Level Triggers

```kotlin
val email = visit(domain = STRING(150), position = at(2, 1)) {
    label = "Email"
    columns(users.email)
    
    // Field validation
    trigger(VALFLD) {
        if (value != null && !value.contains("@")) {
            throw VFieldException("Invalid email format")
        }
    }
    
    // After field change
    trigger(POSTCHG) {
        updateRelatedFields()
    }
    
    // Before entering field
    trigger(PREFLD) {
        setupFieldContext()
    }
    
    // Auto-leave when complete
    trigger(AUTOLEAVE) {
        value?.length == maxLength
    }
}
```

---

## Advanced Form Features

### Dynamic Field Behavior

```kotlin
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

private fun updateFieldVisibility() {
    // Force re-evaluation of field access
    updateAccess()
    
    // Additional custom logic
    when (userType.value) {
        "EMPLOYEE" -> {
            employeeId.setColor(null, VColor.LIGHT_BLUE)
        }
        "CUSTOMER" -> {
            customerCode.setColor(null, VColor.LIGHT_GREEN)
        }
    }
}
```

### Field Calculations

```kotlin
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

val total = skipped(domain = DECIMAL(12, 2), position = at(1, 3)) {
    label = "Total"
    columns(items.total)
    options(FieldOption.NOEDIT)
}

private fun calculateTotal() {
    val qty = quantity.value ?: 0
    val price = unitPrice.value ?: BigDecimal.ZERO
    total.value = price.multiply(BigDecimal(qty))
}
```

### Multi-Page Forms

```kotlin
class ComprehensiveForm : Form(title = "Comprehensive Example", locale = Locale.UK) {
    
    // Define multiple pages
    val personalPage = page("Personal Information")
    val contactPage = page("Contact Information") 
    val preferencesPage = page("Preferences")
    
    // Blocks assigned to pages
    val personalBlock = personalPage.insertBlock(PersonalBlock())
    val contactBlock = contactPage.insertBlock(ContactBlock())
    val preferencesBlock = preferencesPage.insertBlock(PreferencesBlock())
    
    // Cross-page navigation and validation
    init {
        personalBlock.trigger(POSTBLK) {
            // When leaving personal block, update contact block
            contactBlock.userId.value = personalBlock.id.value
        }
    }
    
    inner class PersonalBlock : Block("Personal", buffer = 1, visible = 1) {
        // Personal information fields
    }
    
    inner class ContactBlock : Block("Contact", buffer = 1, visible = 1) {
        // Contact information fields
    }
    
    inner class PreferencesBlock : Block("Preferences", buffer = 1, visible = 1) {
        // User preference fields
    }
}
```

---

## Real-World Examples

### Example 1: Customer Management Form

```kotlin
class CustomerForm : Form(title = "Customer Management", locale = Locale.UK) {
    
    val mainPage = page("Customer Information")
    val customerBlock = mainPage.insertBlock(CustomerBlock())
    
    inner class CustomerBlock : Block("Customers", buffer = 1, visible = 50) {
        val customers = table(Customers, Customers.id)
        
        // Customer ID (auto-generated)
        val customerId = visit(domain = LONG(10), position = at(1, 1)) {
            label = "Customer ID"
            help = "Unique customer identifier"
            columns(customers.id) {
                onInsertSkipped()
                onUpdateSkipped()
            }
        }
        
        // Customer Code (required, unique)
        val customerCode = mustFill(domain = STRING(20), position = at(1, 2)) {
            label = "Customer Code"
            help = "Unique customer code"
            columns(customers.code)
            
            trigger(VALFLD) {
                // Validate format
                if (value != null && !value.matches(Regex("[A-Z]{2}\\d{4}"))) {
                    throw VFieldException("Customer code must be in format XX9999")
                }
            }
            
            trigger(POSTCHG) {
                checkDuplicateCode()
            }
            
            options(FieldOption.QUERY_UPPER)
        }
        
        // Company Name (required)
        val companyName = mustFill(domain = STRING(100), position = at(2, 1..2)) {
            label = "Company Name"
            help = "Customer company name"
            columns(customers.companyName)
            
            trigger(POSTCHG) {
                // Auto-generate customer code if empty
                if (customerCode.value.isNullOrEmpty()) {
                    generateCustomerCode()
                }
            }
        }
        
        // Industry (dropdown)
        val industry = visit(domain = IndustryDomain, position = at(3, 1)) {
            label = "Industry"
            help = "Customer industry"
            columns(customers.industry)
        }
        
        // Credit Limit (conditional)
        val creditLimit = visit(domain = DECIMAL(12, 2), position = at(4, 1)) {
            label = "Credit Limit"
            help = "Customer credit limit"
            columns(customers.creditLimit)
            
            trigger(VALFLD) {
                if (value != null && value < BigDecimal.ZERO) {
                    throw VFieldException("Credit limit cannot be negative")
                }
            }
        }
        
        // Status
        val status = visit(domain = CustomerStatusDomain, position = at(5, 1)) {
            label = "Status"
            help = "Customer status"
            columns(customers.status)
            
            trigger(DEFAULT) {
                value = "ACTIVE"
            }
        }
        
        // Notes (multi-line)
        val notes = visit(domain = TEXT(200, 3), position = at(6..7, 1..2)) {
            label = "Notes"
            help = "Additional customer notes"
            columns(customers.notes)
        }
        
        init {
            // Custom save command
            command(item = save, Mode.INSERT, Mode.UPDATE) {
                customSave()
            }
            
            // Custom validation command
            command(item = validateData) {
                validateCustomer()
            }
        }
        
        private fun checkDuplicateCode() {
            val code = customerCode.value
            if (!code.isNullOrBlank()) {
                transaction {
                    val existing = Customers.select { 
                        (Customers.code eq code) and 
                        (Customers.id neq (customerId.value ?: 0))
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
                val prefix = company.take(2).uppercase()
                val number = getNextCustomerNumber()
                customerCode.value = "$prefix${number.toString().padStart(4, '0')}"
            }
        }
        
        private fun customSave() {
            try {
                validateCustomer()
                saveBlock()
                model.notice("Customer saved successfully")
            } catch (e: VException) {
                model.error("Save failed: ${e.message}")
            }
        }
        
        private fun validateCustomer() {
            if (companyName.value.isNullOrBlank()) {
                throw VExecFailedException("Company name is required")
            }
            
            if (customerCode.value.isNullOrBlank()) {
                throw VExecFailedException("Customer code is required")
            }
        }
        
        private fun getNextCustomerNumber(): Int {
            return transaction {
                (Customers.select { Customers.code.isNotNull() }
                    .count().toInt() + 1)
            }
        }
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
```

### Example 2: Order Entry Form with Master-Detail

```kotlin
class OrderForm : Form(title = "Order Entry", locale = Locale.UK) {
    
    val orderPage = page("Order Header")
    val itemsPage = page("Order Items")
    
    val orderBlock = orderPage.insertBlock(OrderBlock())
    val itemsBlock = itemsPage.insertBlock(OrderItemsBlock())
    
    inner class OrderBlock : Block("Orders", buffer = 1, visible = 10) {
        val orders = table(Orders, Orders.id)
        val customers = table(Customers)
        
        val orderId = visit(domain = LONG(10), position = at(1, 1)) {
            label = "Order ID"
            columns(orders.id)
            
            trigger(POSTCHG) {
                loadOrderItems()
            }
        }
        
        val orderDate = visit(domain = DATE, position = at(1, 2)) {
            label = "Order Date"
            columns(orders.orderDate)
            
            trigger(DEFAULT) {
                value = LocalDate.now()
            }
        }
        
        val customerId = visit(domain = CustomerDomain, position = at(2, 1)) {
            label = "Customer"
            columns(customers.id, orders.customerId)
            
            trigger(POSTCHG) {
                block.fetchLookupFirst(vField)
                updateCustomerInfo()
            }
        }
        
        val customerName = skipped(domain = STRING(100), position = at(2, 2)) {
            label = "Customer Name"
            columns(customers.name)
        }
        
        val total = skipped(domain = DECIMAL(12, 2), position = at(3, 1)) {
            label = "Order Total"
            columns(orders.total)
            options(FieldOption.NOEDIT)
        }
        
        init {
            trigger(POSTQRY) {
                loadOrderItems()
                calculateOrderTotal()
            }
        }
        
        private fun loadOrderItems() {
            if (orderId.value != null) {
                itemsBlock.orderId.value = orderId.value
                itemsBlock.load()
                calculateOrderTotal()
            }
        }
        
        private fun updateCustomerInfo() {
            // Customer information is automatically loaded via lookup
        }
        
        private fun calculateOrderTotal() {
            var orderTotal = BigDecimal.ZERO
            for (i in 0 until itemsBlock.recordCount) {
                val itemTotal = itemsBlock.total[i]
                if (itemTotal != null) {
                    orderTotal = orderTotal.add(itemTotal)
                }
            }
            total.value = orderTotal
        }
    }
    
    inner class OrderItemsBlock : Block("Order Items", buffer = 20, visible = 10) {
        val items = table(OrderItems, OrderItems.id)
        val products = table(Products)
        
        // Hidden link to master record
        val orderId = hidden(domain = LONG(10)) {
            columns(items.orderId)
        }
        
        val productId = visit(domain = ProductDomain, position = at(1, 1)) {
            label = "Product"
            columns(products.id, items.productId)
            
            trigger(POSTCHG) {
                block.fetchLookupFirst(vField)
                updateProductInfo()
            }
        }
        
        val productName = skipped(domain = STRING(100), position = at(1, 2)) {
            label = "Product Name"
            columns(products.name)
        }
        
        val quantity = visit(domain = INT(5), position = at(2, 1)) {
            label = "Quantity"
            columns(items.quantity)
            
            trigger(POSTCHG) {
                calculateItemTotal()
            }
        }
        
        val unitPrice = visit(domain = DECIMAL(10, 2), position = at(2, 2)) {
            label = "Unit Price"
            columns(items.unitPrice)
            
            trigger(POSTCHG) {
                calculateItemTotal()
            }
        }
        
        val total = skipped(domain = DECIMAL(12, 2), position = at(2, 3)) {
            label = "Line Total"
            columns(items.total)
            options(FieldOption.NOEDIT)
        }
        
        init {
            trigger(POSTINS, POSTUPD, POSTDEL) {
                // Recalculate order total when items change
                orderBlock.calculateOrderTotal()
            }
        }
        
        private fun updateProductInfo() {
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

---

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
    
    // Define pages logically
    private val pages = createPages()
    
    // Create blocks with clear separation
    private val blocks = createBlocks()
    
    private fun setupFormTriggers() {
        trigger(INIT) { initializeForm() }
        trigger(PREFORM) { prepareFormDisplay() }
        trigger(POSTFORM) { cleanupForm() }
    }
    
    private fun createPages() = mapOf(
        "main" to page("Main Information"),
        "details" to page("Additional Details")
    )
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
            onInsertSkipped()
            onUpdateSkipped()
        }
        setupUserIdTriggers()
    }
}

private fun FormField<Long>.setupUserIdTriggers() {
    trigger(POSTCHG) {
        loadUserDetails()
    }
}
```

### 3. Error Handling

```kotlin
// Good: Comprehensive error handling
private fun saveCustomerData() {
    try {
        validateBusinessRules()
        
        transaction {
            customerBlock.saveBlock()
            contactBlock.saveBlock()
        }
        
        updateRelatedData()
        model.notice("Customer saved successfully")
        
    } catch (e: VFieldException) {
        model.error("Field validation failed: ${e.message}")
    } catch (e: VExecFailedException) {
        model.error("Business rule violation: ${e.message}")
    } catch (e: SQLException) {
        model.error("Database error: ${e.message}")
        logger.error("Database error in saveCustomerData", e)
    }
}
```

### 4. Performance Optimization

```kotlin
// Good: Optimized block configuration
inner class OptimizedBlock : Block("Optimized", buffer = 20, visible = 10) {
    
    init {
        // Use appropriate buffer size for data volume
        // Optimize block options
        options(BlockOption.NODETAIL) // If detail view not needed
        
        // Use efficient triggers
        trigger(POSTQRY) {
            batchUpdateRelatedData()
        }
    }
    
    private fun batchUpdateRelatedData() {
        transaction {
            // Batch multiple operations together
        }
    }
}
```

---

## Common Patterns

### 1. Lookup Fields with Auto-Complete

```kotlin
val customer = visit(domain = CustomerDomain, position = at(1, 1)) {
    label = "Customer"
    columns(customers.id, orders.customerId)
    
    trigger(POSTCHG) {
        // Fetch customer details
        block.fetchLookupFirst(vField)
        
        // Update related fields
        customerName.value = customers.name.value
        customerEmail.value = customers.email.value
    }
}
```

### 2. Conditional Field Visibility

```kotlin
val fieldType = visit(domain = FieldTypeDomain, position = at(1, 1)) {
    label = "Field Type"
    
    trigger(POSTCHG) {
        updateFieldVisibility()
    }
}

val textField = visit(domain = STRING(100), position = at(2, 1)) {
    label = "Text Value"
    
    access {
        if (fieldType.value == "TEXT") {
            Access.VISIT
        } else {
            Access.HIDDEN
        }
    }
}

val numberField = visit(domain = INT(10), position = at(2, 1)) {
    label = "Number Value"
    
    access {
        if (fieldType.value == "NUMBER") {
            Access.VISIT
        } else {
            Access.HIDDEN
        }
    }
}
```

### 3. Audit Fields

```kotlin
// Common audit fields pattern
val createdBy = hidden(domain = LONG(10)) {
    columns(table.createdBy)
    
    trigger(PREINS) {
        value = getCurrentUserId()
    }
}

val createdAt = skipped(domain = DATETIME, position = at(10, 1)) {
    label = "Created At"
    columns(table.createdAt)
    
    trigger(PREINS) {
        value = LocalDateTime.now()
    }
}

val updatedBy = hidden(domain = LONG(10)) {
    columns(table.updatedBy)
    
    trigger(PREINS, PREUPD) {
        value = getCurrentUserId()
    }
}

val updatedAt = skipped(domain = DATETIME, position = at(10, 2)) {
    label = "Updated At"
    columns(table.updatedAt)
    
    trigger(PREINS, PREUPD) {
        value = LocalDateTime.now()
    }
}
```

### 4. Search Form Pattern

```kotlin
inner class SearchBlock : Block("Search", buffer = 50, visible = 20) {
    val users = table(Users)
    
    // Search criteria fields
    val searchName = visit(domain = STRING(50), position = at(1, 1)) {
        label = "Name Contains"
        help = "Search by name pattern"
        // Not linked to database column - used for search only
    }
    
    val searchActive = visit(domain = BOOL, position = at(1, 2)) {
        label = "Active Only"
        help = "Show only active users"
    }
    
    // Result fields
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
                setQueryCondition(combinedCondition)
            }
        }
    }
}
```

---

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

#### 2. Monitor Field Values

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

---

## Next Steps

Now that you understand the basics of Galite Forms, you can:

1. **Practice with Simple Forms**: Start with basic CRUD operations
2. **Explore Advanced Features**: Try master-detail relationships, lookups, and calculations
3. **Study Real Examples**: Look at the forms in the `forms-examples` directory
4. **Read the API Documentation**: Dive deeper into specific classes and methods
5. **Build Your Own Application**: Apply what you've learned to your own project

### Additional Resources

- **API Documentation**: `GALITE_FORMS_DETAILED_API_DOCUMENTATION.md`
- **Framework Documentation**: `GALITE_FRAMEWORK_DOCUMENTATION.md`
- **Example Forms**: Check the `forms-examples/` directory for real-world implementations
- **Official Repository**: https://github.com/kopiLeft/Galite

Remember: The best way to learn Galite Forms is by building them. Start simple, and gradually add complexity as you become more comfortable with the framework!