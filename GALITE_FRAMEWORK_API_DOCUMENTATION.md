# Galite Framework - Comprehensive API Documentation

![Galite Logo](docs/logo_galite.png)

## Table of Contents

1. [Framework Overview](#framework-overview)
2. [Setup & Integration](#setup--integration)
3. [Core Architecture](#core-architecture)
4. [Forms API Reference](#forms-api-reference)
5. [Domain System](#domain-system)
6. [Reports API Reference](#reports-api-reference)
7. [Charts API Reference](#charts-api-reference)
8. [Pivot Tables API Reference](#pivot-tables-api-reference)
9. [Database Integration](#database-integration)
10. [Advanced Patterns & Examples](#advanced-patterns--examples)
11. [Migration Guide](#migration-guide)
12. [Troubleshooting](#troubleshooting)

---

## Framework Overview

**Galite** is a powerful Kotlin DSL-based enterprise framework designed for building business applications with forms, reports, charts, and pivot tables. It provides a declarative, type-safe approach to application development with strong database integration capabilities.

### Key Features

- **Kotlin DSL**: Type-safe builders for application components
- **Database Integration**: Built on Exposed framework with multi-dialect support
- **Strongly Typed**: Compile-time type checking for all field definitions
- **Vaadin UI**: Modern web interface with responsive design
- **Comprehensive Components**: Forms, Reports, Charts, and Pivot Tables
- **Enterprise Ready**: Localization, security, and configuration management

### Architecture Principles

1. **Declarative Programming**: Define what you want, not how to build it
2. **Type Safety**: Leverage Kotlin's type system for robust applications
3. **Database First**: Strong integration with relational databases
4. **Component Based**: Modular architecture with reusable components

---

## Setup & Integration

### Gradle Dependency Configuration

#### Basic Setup (Kotlin DSL)

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("org.kopi:galite-core:1.3.0")
    implementation("org.kopi:galite-data:1.3.0")
    implementation("org.kopi:galite-util:1.3.0")
    
    // Optional: Testing support
    testImplementation("org.kopi:galite-testing:1.3.0")
}
```

#### Advanced Configuration

```kotlin
dependencies {
    // Core framework
    implementation("org.kopi:galite-core:1.3.0")
    implementation("org.kopi:galite-data:1.3.0")
    implementation("org.kopi:galite-util:1.3.0")
    
    // Database drivers (choose your database)
    runtimeOnly("org.postgresql:postgresql:42.5.0")
    runtimeOnly("mysql:mysql-connector-java:8.0.33")
    runtimeOnly("com.h2database:h2:2.1.214")
    
    // Vaadin UI components (included automatically)
    implementation("com.vaadin:vaadin-core")
    
    // Additional Vaadin addons (optional)
    implementation("org.vaadin.stefan:fullcalendar2")
    implementation("org.vaadin.addons.componentfactory:pivottable-flow")
}
```

### Application Bootstrap

#### Basic Application Setup

```kotlin
@Route("")
class MyApplication : VApplication(Registry(domain = "MY_APP", parent = null)) {
    
    override val sologanImage get() = "my-slogan.png"
    override val logoImage get() = "my-logo.png"
    override val logoHref get() = "https://mycompany.com"
    override val alternateLocale get() = Locale.UK
    override val supportedLocales get() = arrayOf(
        Locale.UK, 
        Locale.FRANCE, 
        Locale.GERMANY
    )

    companion object {
        init {
            ApplicationConfiguration.setConfiguration(AppConfig)
        }
    }

    object AppConfig : ApplicationConfiguration() {
        override val version: String = "1.0.0"
        override val applicationName: String = "My Business App"
        override val informationText: String = "Business Management System"
        override val logFile: String = "application.log"
        override val debugMailRecipient: String = "admin@mycompany.com"
        override fun getSMTPServer(): String = "smtp.mycompany.com"
        override fun mailErrors(): Boolean = true
        override fun logErrors(): Boolean = true
        override fun debugMessageInTransaction(): Boolean = false
    }
}
```

### Database Configuration

```kotlin
// Database connection setup using Exposed
Database.connect(
    url = "jdbc:postgresql://localhost:5432/myapp",
    driver = "org.postgresql.Driver",
    user = "username",
    password = "password"
)
```

---

## Core Architecture

### Component Hierarchy

```
VApplication
├── Form (Business Forms)
│   ├── Block (Data Containers)
│   │   ├── FormField (Input Fields)
│   │   ├── Triggers (Business Logic)
│   │   └── Commands (User Actions)
│   └── Pages (UI Organization)
├── Report (Data Reports)
│   ├── ReportField (Data Columns)
│   └── ReportRow (Data Records)
├── Chart (Data Visualization)
│   ├── ChartDimension (X-axis)
│   └── ChartMeasure (Y-axis)
└── PivotTable (Data Analysis)
    ├── Dimension (Row/Column Headers)
    └── Measure (Aggregated Values)
```

### Module Structure

- **galite-core**: Main framework with DSL components
- **galite-data**: Database integration and data access
- **galite-util**: Utility functions and helpers
- **galite-testing**: Testing framework and utilities

---

## Forms API Reference

Forms are the primary interface for data entry and manipulation in Galite applications.

### Form Class

#### Basic Form Structure

```kotlin
class ClientForm : Form(title = "Client Management", locale = Locale.UK) {
    
    // Menu definitions
    val action = menu("Action")
    val edit = menu("Edit")
    
    // Actor definitions (buttons/actions)
    val save = actor(menu = action, label = "Save", help = "Save changes") {
        key = Key.F7
        icon = Icon.SAVE
    }
    
    // Command definitions (business logic)
    val saveCommand = command(item = save) {
        saveBlock()
    }
    
    // Page organization
    val clientPage = page("Client Details")
    
    // Block definitions
    val clientBlock = clientPage.insertBlock(ClientBlock())
}
```

### Block Class

Blocks represent data containers that map to database tables and contain fields.

#### Block Definition

```kotlin
inner class ClientBlock : Block("Client", buffer = 1, visible = 1000) {
    
    init {
        // Block visibility settings
        blockVisibility(Access.VISIT, Mode.QUERY)
        
        // Standard commands
        breakCommand
        command(item = serialQuery, Mode.QUERY) { serialQuery() }
        command(item = save, Mode.INSERT, Mode.UPDATE) { saveBlock() }
        command(item = delete, Mode.UPDATE) { deleteBlock() }
        
        // Block triggers
        trigger(PREINS) {
            createdAt.value = LocalDateTime.now()
            userId.value = getUserID()
        }
        
        trigger(POSTQRY) {
            // Post-query processing
            setTitle("Client: ${name.value}")
        }
    }
    
    // Table mapping
    val c = table(Clients, idColumn = Clients.id, sequence = Sequence("CLIENTS_ID_SEQ"))
    
    // Field definitions
    val id = hidden(INT(11)) {
        columns(c.id)
    }
    
    val name = mustFill(STRING(100, Convert.UPPER), at(1, 1..3)) {
        label = "Client Name"
        help = "The full name of the client"
        columns(c.name) {
            priority = 10
        }
    }
    
    val email = visit(STRING(100), at(2, 1..2)) {
        label = "Email Address"
        columns(c.email) {
            priority = 8
        }
        
        // Field validation trigger
        trigger(VALIDATE) {
            if (!value.isNullOrBlank() && !isValidEmail(value!!)) {
                throw VExecFailedException("Invalid email format")
            }
        }
    }
}
```

### Field Types and Access Levels

#### Field Access Types

```kotlin
// MUSTFILL - Required fields that users must complete
val name = mustFill(STRING(50), at(1, 1)) {
    label = "Name"
    columns(table.name)
}

// VISIT - Optional fields that can be modified
val description = visit(STRING(200), at(2, 1..3)) {
    label = "Description"
    columns(table.description)
}

// SKIPPED - Read-only fields
val createdAt = skipped(DATETIME, at(3, 1)) {
    label = "Created"
    columns(table.created_at)
}

// HIDDEN - Fields not displayed in UI
val id = hidden(INT(11)) {
    columns(table.id)
}
```

#### Dynamic Field Access

```kotlin
val statusField = visit(STRING(20), at(4, 1)) {
    label = "Status"
    columns(table.status)
    
    // Dynamic access based on user role
    access {
        when (getCurrentUserRole()) {
            "ADMIN" -> Access.VISIT
            "USER" -> Access.SKIPPED
            else -> Access.HIDDEN
        }
    }
}
```

### Field Positioning System

#### Coordinate-Based Positioning

```kotlin
// Basic positioning: at(line, column)
val field1 = visit(STRING(50), at(1, 1)) { }

// Range positioning: spans multiple columns
val field2 = visit(STRING(100), at(2, 1..3)) { }

// Multi-line fields
val description = visit(STRING(50, 5, 3, Fixed.OFF), at(3..7, 1..3)) {
    label = "Description"
}

// Following another field
val suffix = visit(STRING(10), follow(prefix)) {
    label = "Suffix"
}
```

### Domain System Integration

#### Built-in Domain Types

```kotlin
// Numeric domains
val quantity = mustFill(INT(10), at(1, 1)) { }
val price = visit(DECIMAL(10, 2), at(1, 2)) { }
val total = skipped(DECIMAL(15, 2), at(1, 3)) { }

// Text domains
val shortText = visit(STRING(50), at(2, 1)) { }
val longText = visit(STRING(100, 5, 3, Fixed.OFF), at(3, 1..3)) { }
val styledText = visit(TEXT(200, 10, styled = true), at(4, 1..3)) { }

// Date/Time domains
val birthDate = visit(DATE, at(5, 1)) { }
val appointmentTime = visit(DATETIME, at(5, 2)) { }
val duration = visit(TIME, at(5, 3)) { }

// Boolean and special types
val isActive = visit(BOOL, at(6, 1)) { }
val profilePicture = visit(IMAGE(200, 200), at(7, 1)) { }
val favoriteColor = visit(COLOR, at(6, 2)) { }
```

#### Custom Domain Types

```kotlin
// Code domains for dropdown lists
object ClientType : CodeDomain<String>() {
    init {
        "INDIVIDUAL" keyOf "Individual Client"
        "CORPORATE" keyOf "Corporate Client"
        "GOVERNMENT" keyOf "Government Entity"
        "NON_PROFIT" keyOf "Non-Profit Organization"
    }
}

// Usage in forms
val clientType = mustFill(ClientType, at(1, 1)) {
    label = "Client Type"
    columns(table.client_type)
}

// List domains for complex lookups
class SupplierDomain : ListDomain<Int>(20) {
    init {
        "name" keyOf Suppliers.name
        "city" keyOf Suppliers.city
        "country" keyOf Suppliers.country
    }
}

val supplier = visit(SupplierDomain(), at(2, 1..2)) {
    label = "Supplier"
    columns(table.supplier_id)
}
```

### Triggers and Business Logic

#### Form-Level Triggers

```kotlin
class MyForm : Form("My Form") {
    init {
        // Form initialization
        trigger(INIT) {
            // Initialize form data
            setupDefaultValues()
        }
        
        // Pre-form display
        trigger(PREFORM) {
            // Validate user permissions
            checkUserAccess()
        }
        
        // Form reset handling
        trigger(RESET) {
            // Return true to allow reset, false to prevent
            confirmReset()
        }
        
        // Form close handling
        trigger(POSTFORM) {
            // Cleanup resources
            closeConnections()
        }
    }
}
```

#### Block-Level Triggers

```kotlin
inner class DataBlock : Block("Data", 1, 100) {
    init {
        // Database operation triggers
        trigger(PREQRY) {
            // Before querying database
            setupQueryConditions()
        }
        
        trigger(POSTQRY) {
            // After querying database
            calculateDerivedFields()
        }
        
        trigger(PREINS) {
            // Before inserting record
            auditFields.createdAt.value = LocalDateTime.now()
            auditFields.createdBy.value = getCurrentUser()
        }
        
        trigger(POSTINS) {
            // After inserting record
            logActivity("Record created: ${id.value}")
        }
        
        trigger(PREUPD) {
            // Before updating record
            auditFields.modifiedAt.value = LocalDateTime.now()
            auditFields.modifiedBy.value = getCurrentUser()
        }
        
        trigger(POSTUPD) {
            // After updating record
            logActivity("Record updated: ${id.value}")
        }
        
        // Record navigation triggers
        trigger(PREREC) {
            // Before entering record
        }
        
        trigger(POSTREC) {
            // After leaving record
        }
    }
}
```

#### Field-Level Triggers

```kotlin
val emailField = visit(STRING(100), at(1, 1)) {
    label = "Email"
    
    // Default value trigger
    trigger(DEFAULT) {
        value = generateDefaultEmail()
    }
    
    // Field validation trigger
    trigger(VALIDATE) {
        if (!value.isNullOrBlank() && !isValidEmail(value!!)) {
            throw VExecFailedException("Invalid email format")
        }
    }
    
    // Post-change trigger
    trigger(POSTCHG) {
        // Triggered when field value changes
        updateRelatedFields()
    }
    
    // Auto-leave trigger
    trigger(AUTOLEAVE) {
        // Return true to automatically move to next field
        value?.length == maxLength
    }
    
    // Field action trigger (for clickable fields)
    trigger(ACTION) {
        // Handle field click
        openEmailClient()
    }
}
```

### Database Column Mapping

#### Basic Column Mapping

```kotlin
val name = mustFill(STRING(100), at(1, 1)) {
    label = "Name"
    columns(Clients.name) {
        priority = 10  // Search priority
        index = nameIndex  // Unique constraint
    }
}
```

#### Multi-Table Joins

```kotlin
inner class OrderBlock : Block("Order", 1, 100) {
    // Define table aliases
    val o = table(Orders)  // Main table
    val c = table(Clients) // Lookup table
    val p = table(Products) // Lookup table
    
    val orderId = hidden(INT(11)) {
        columns(o.id)
    }
    
    // Join to client table
    val clientName = visit(STRING(100), at(1, 1)) {
        label = "Client"
        columns(c.name, nullable(o.client_id))
    }
    
    // Join to product table
    val productName = visit(STRING(100), at(2, 1)) {
        label = "Product"  
        columns(p.name, nullable(o.product_id))
    }
}
```

#### Nullable and Key Columns

```kotlin
val optionalField = visit(STRING(50), at(1, 1)) {
    columns(nullable(table.optional_field))
}

val foreignKey = visit(INT(11), at(2, 1)) {
    columns(key(table.foreign_key_id))
}
```

### Commands and User Actions

#### Standard Commands

```kotlin
inner class MyBlock : Block("Data", 1, 100) {
    init {
        // Standard form commands
        breakCommand  // Reset/cancel changes
        command(item = serialQuery, Mode.QUERY) { serialQuery() }
        command(item = menuQuery, Mode.QUERY) { recursiveQuery() }
        command(item = insertMode, Mode.QUERY) { insertMode() }
        command(item = save, Mode.INSERT, Mode.UPDATE) { saveBlock() }
        command(item = delete, Mode.UPDATE) { deleteBlock() }
        
        // Custom commands
        command(item = customAction, Mode.UPDATE) {
            performCustomOperation()
        }
    }
}
```

#### Custom Commands with Validation

```kotlin
val processOrder = actor(menu = action, label = "Process Order") {
    key = Key.F10
    icon = Icon.PROCESS
}

val processCommand = command(item = processOrder, Mode.UPDATE) {
    // Validate before processing
    block.validate()
    
    if (status.value != "PENDING") {
        throw VExecFailedException("Order must be in PENDING status")
    }
    
    // Perform processing
    transaction {
        updateOrderStatus("PROCESSING")
        createShipmentRecord()
        sendNotification()
    }
    
    // Refresh form
    block.clear()
    block.load()
}
```

### Advanced Form Patterns

#### Master-Detail Forms

```kotlin
class OrderForm : Form("Order Management") {
    val orderPage = page("Order Details")
    val itemsPage = page("Order Items")
    
    val orderBlock = orderPage.insertBlock(OrderBlock())
    val itemsBlock = itemsPage.insertBlock(OrderItemsBlock())
    
    inner class OrderBlock : Block("Order", 1, 1) {
        // Master record fields
        val id = hidden(INT(11)) { columns(Orders.id) }
        val orderNumber = mustFill(STRING(20), at(1, 1)) {
            columns(Orders.order_number)
        }
        
        trigger(POSTQRY) {
            // Load related items when order is loaded
            itemsBlock.clear()
            itemsBlock.load()
        }
    }
    
    inner class OrderItemsBlock : Block("Items", 100, 10) {
        // Detail records
        val id = hidden(INT(11)) { columns(OrderItems.id) }
        val orderId = hidden(INT(11)) {
            alias = orderBlock.id  // Link to master record
            columns(OrderItems.order_id)
        }
        val productName = visit(STRING(100), at(1)) {
            columns(OrderItems.product_name)
        }
        val quantity = mustFill(INT(10), at(1)) {
            columns(OrderItems.quantity)
        }
    }
}
```

#### Multi-Page Forms

```kotlin
class EmployeeForm : Form("Employee Management") {
    val personalPage = page("Personal Information")
    val contactPage = page("Contact Details")
    val employmentPage = page("Employment Information")
    
    val personalBlock = personalPage.insertBlock(PersonalBlock())
    val contactBlock = contactPage.insertBlock(ContactBlock())
    val employmentBlock = employmentPage.insertBlock(EmploymentBlock())
    
    // Blocks can share the same table
    inner class PersonalBlock : Block("Personal", 1, 1) {
        val e = table(Employees)
        val firstName = mustFill(STRING(50), at(1, 1)) {
            columns(e.first_name)
        }
        val lastName = mustFill(STRING(50), at(1, 2)) {
            columns(e.last_name)
        }
    }
    
    inner class ContactBlock : Block("Contact", 1, 1) {
        val e = table(Employees)
        val email = visit(STRING(100), at(1, 1)) {
            columns(e.email)
        }
        val phone = visit(STRING(20), at(2, 1)) {
            columns(e.phone)
        }
    }
}
```

---

## Domain System

The domain system provides type-safe field definitions with built-in validation and formatting.

### Built-in Domains

#### Numeric Domains

```kotlin
// Integer domains
val count = visit(INT(10), at(1, 1)) {
    label = "Count"
    help = "Number of items"
}

val longValue = visit(LONG(15), at(1, 2)) {
    label = "Long Value"
}

// Decimal domains
val price = visit(DECIMAL(width = 10, scale = 2), at(2, 1)) {
    label = "Price"
    help = "Price in EUR"
}

val percentage = visit(FRACTION(5), at(2, 2)) {
    label = "Percentage"
}
```

#### Text Domains

```kotlin
// Simple string
val name = mustFill(STRING(50, Convert.UPPER), at(1, 1)) {
    label = "Name"
}

// Multi-line text with fixed formatting
val address = visit(STRING(100, 3, 2, Fixed.ON), at(2, 1..3)) {
    label = "Address"
}

// Styled text (rich text editor)
val description = visit(TEXT(500, 10, styled = true), at(3, 1..4)) {
    label = "Description"
}
```

#### Date and Time Domains

```kotlin
val birthDate = visit(DATE, at(1, 1)) {
    label = "Birth Date"
}

val appointmentTime = visit(DATETIME, at(1, 2)) {
    label = "Appointment"
}

val duration = visit(TIME, at(1, 3)) {
    label = "Duration"
}

val timestamp = visit(TIMESTAMP, at(2, 1)) {
    label = "Last Updated"
}

val month = visit(MONTH, at(2, 2)) {
    label = "Report Month"
}

val week = visit(WEEK, at(2, 3)) {
    label = "Week Number"
}
```

#### Special Domains

```kotlin
val isActive = visit(BOOL, at(1, 1)) {
    label = "Active"
}

val profileImage = visit(IMAGE(200, 300), at(1, 2)) {
    label = "Profile Picture"
}

val favoriteColor = visit(COLOR, at(1, 3)) {
    label = "Favorite Color"
}
```

### Custom Domain Types

#### Code Domains

Code domains provide dropdown lists with predefined values.

```kotlin
object Priority : CodeDomain<Int>() {
    init {
        "Low" keyOf 1
        "Medium" keyOf 2
        "High" keyOf 3
        "Critical" keyOf 4
    }
}

object Status : CodeDomain<String>() {
    init {
        "Draft" keyOf "DRAFT"
        "Published" keyOf "PUBLISHED"
        "Archived" keyOf "ARCHIVED"
    }
}

// Usage in forms
val taskPriority = mustFill(Priority, at(1, 1)) {
    label = "Priority"
    columns(Tasks.priority)
}

val documentStatus = visit(Status, at(1, 2)) {
    label = "Status"
    columns(Documents.status)
}
```

#### List Domains

List domains provide lookup functionality with multiple display columns.

```kotlin
class ClientLookup : ListDomain<Int>(25) {
    init {
        "name" keyOf Clients.name
        "city" keyOf Clients.city
        "email" keyOf Clients.email
        "phone" keyOf Clients.phone
    }
}

class ProductLookup : ListDomain<Int>(30) {
    init {
        "code" keyOf Products.code
        "name" keyOf Products.name
        "category" keyOf Products.category
        "price" keyOf Products.price
    }
}

// Usage in forms
val client = mustFill(ClientLookup(), at(1, 1..2)) {
    label = "Client"
    columns(Orders.client_id)
}

val product = visit(ProductLookup(), at(2, 1..2)) {
    label = "Product"
    columns(OrderItems.product_id)
}
```

### Domain Constraints and Validation

#### Built-in Constraints

```kotlin
val email = visit(STRING(100), at(1, 1)) {
    label = "Email"
    
    // Built-in format validation
    constraint = Constraint.EMAIL
}

val phone = visit(STRING(20), at(1, 2)) {
    label = "Phone"
    
    // Regular expression constraint
    constraint = Constraint.REGEX("\\+?[0-9\\s\\-\\(\\)]+")
}

val age = visit(INT(3), at(1, 3)) {
    label = "Age"
    
    // Range constraint
    constraint = Constraint.RANGE(0, 150)
}
```

#### Custom Validation

```kotlin
val customField = visit(STRING(50), at(1, 1)) {
    label = "Custom Field"
    
    trigger(VALIDATE) {
        if (!value.isNullOrBlank()) {
            // Custom validation logic
            if (!isValidCustomFormat(value!!)) {
                throw VExecFailedException("Invalid format for custom field")
            }
            
            // Database validation
            if (isDuplicateValue(value!!)) {
                throw VExecFailedException("Value already exists")
            }
        }
    }
}
```

---

## Reports API Reference

Reports provide data presentation and analysis capabilities with export functionality.

### Basic Report Structure

```kotlin
class ProductReport : Report(title = "Product Catalog", locale = Locale.UK) {
    
    // Menu and actions
    val action = menu("Action")
    
    val exportCSV = actor(menu = action, label = "Export CSV", help = "Export to CSV") {
        key = Key.F8
        icon = Icon.EXPORT_CSV
    }
    
    val exportPDF = actor(menu = action, label = "Export PDF", help = "Export to PDF") {
        key = Key.F9
        icon = Icon.EXPORT_PDF
    }
    
    // Commands
    val csvCommand = command(item = exportCSV) {
        model.export(VReport.TYP_CSV)
    }
    
    val pdfCommand = command(item = exportPDF) {
        model.export(VReport.TYP_PDF)
    }
    
    // Report fields
    val category = field(STRING(30)) {
        label = "Category"
        help = "Product category"
        group = department  // Group by this field
    }
    
    val department = field(STRING(20)) {
        label = "Department"
        help = "Product department"
        group = name  // Sub-group by this field
    }
    
    val name = field(STRING(100)) {
        label = "Product Name"
        help = "Product name"
        
        // Custom formatting
        format { value ->
            value?.uppercase()
        }
    }
    
    val price = field(DECIMAL(10, 2)) {
        label = "Price (EUR)"
        help = "Unit price excluding VAT"
        
        // Alignment
        align = FieldAlignment.RIGHT
    }
    
    val stock = field(INT(10)) {
        label = "Stock"
        help = "Available quantity"
        
        // Conditional formatting
        format { value ->
            when {
                value == null -> "N/A"
                value < 10 -> "LOW: $value"
                value < 50 -> "MED: $value"
                else -> "OK: $value"
            }
        }
    }
    
    // Data loading
    init {
        transaction {
            Products.selectAll().forEach { row ->
                add {
                    this[category] = row[Products.category]
                    this[department] = row[Products.department]
                    this[name] = row[Products.name]
                    this[price] = row[Products.price]
                    this[stock] = row[Products.stock]
                }
            }
        }
    }
}
```

### Report Field Types

#### Basic Field Definition

```kotlin
// Simple field
val simpleField = field(STRING(50)) {
    label = "Simple Field"
    help = "Help text for the field"
}

// Nullable field
val nullableField = nullableField(STRING(50)) {
    label = "Nullable Field"
    help = "This field can contain null values"
}

// Computed field (no direct data binding)
val computedField = field(DECIMAL(10, 2)) {
    label = "Computed Total"
    
    compute { row ->
        // Access other field values in the same row
        val quantity = row[quantityField] ?: 0
        val price = row[priceField] ?: BigDecimal.ZERO
        BigDecimal(quantity) * price
    }
}
```

#### Field Formatting and Alignment

```kotlin
val formattedField = field(DECIMAL(15, 2)) {
    label = "Amount"
    align = FieldAlignment.RIGHT
    
    format { value ->
        when {
            value == null -> "-"
            value < BigDecimal.ZERO -> "($value)"
            else -> value.toString()
        }
    }
}

val dateField = field(DATE) {
    label = "Date"
    
    format { value ->
        value?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }
}
```

### Report Grouping and Aggregation

#### Grouping Configuration

```kotlin
class SalesReport : Report("Sales Report") {
    val region = field(STRING(30)) {
        label = "Region"
        group = country  // Group by country first
    }
    
    val country = field(STRING(30)) {
        label = "Country"
        group = salesperson  // Then by salesperson
    }
    
    val salesperson = field(STRING(50)) {
        label = "Salesperson"
        // No further grouping
    }
    
    val sales = field(DECIMAL(15, 2)) {
        label = "Sales Amount"
        align = FieldAlignment.RIGHT
        
        // Aggregation function
        compute { row ->
            // Sum sales for grouped records
            row.getGroupedRecords().sumOf { it[individualSalesField] ?: BigDecimal.ZERO }
        }
    }
}
```

### Data Loading Patterns

#### Database Query Loading

```kotlin
class CustomerReport : Report("Customer Report") {
    // Fields definition...
    
    init {
        transaction {
            // Simple query
            Customers.selectAll()
                .orderBy(Customers.name)
                .forEach { row ->
                    add {
                        this[name] = row[Customers.name]
                        this[city] = row[Customers.city]
                        this[email] = row[Customers.email]
                    }
                }
        }
    }
}
```

#### Complex Query Loading

```kotlin
class OrderSummaryReport : Report("Order Summary") {
    // Fields...
    
    init {
        transaction {
            // Join query with aggregation
            (Orders innerJoin Customers)
                .slice(
                    Customers.name,
                    Customers.city,
                    Orders.id.count(),
                    Orders.total.sum()
                )
                .selectAll()
                .groupBy(Customers.id)
                .having { Orders.total.sum() greater BigDecimal("1000.00") }
                .forEach { row ->
                    add {
                        this[customerName] = row[Customers.name]
                        this[customerCity] = row[Customers.city]
                        this[orderCount] = row[Orders.id.count()].toInt()
                        this[totalSales] = row[Orders.total.sum()] ?: BigDecimal.ZERO
                    }
                }
        }
    }
}
```

#### Conditional Data Loading

```kotlin
class FilteredReport : Report("Filtered Report") {
    // Fields...
    
    init {
        transaction {
            val query = Products.selectAll()
            
            // Add conditions based on parameters
            if (categoryFilter != null) {
                query.andWhere { Products.category eq categoryFilter }
            }
            
            if (priceRange != null) {
                query.andWhere { 
                    Products.price.between(priceRange.start, priceRange.end) 
                }
            }
            
            query.forEach { row ->
                add {
                    // Populate fields...
                }
            }
        }
    }
}
```

### Report Export Functionality

#### Standard Export Commands

```kotlin
class ExportableReport : Report("Exportable Report") {
    val action = menu("Action")
    
    // Export actors
    val exportCSV = actor(menu = action, label = "CSV", help = "Export as CSV") {
        key = Key.F8
        icon = Icon.EXPORT_CSV
    }
    
    val exportPDF = actor(menu = action, label = "PDF", help = "Export as PDF") {
        key = Key.F9
        icon = Icon.EXPORT_PDF
    }
    
    val exportExcel = actor(menu = action, label = "Excel", help = "Export as Excel") {
        key = Key.F10
        icon = Icon.EXPORT_EXCEL
    }
    
    // Export commands
    val csvCmd = command(item = exportCSV) {
        model.export(VReport.TYP_CSV)
    }
    
    val pdfCmd = command(item = exportPDF) {
        model.export(VReport.TYP_PDF)
    }
    
    val excelCmd = command(item = exportExcel) {
        model.export(VReport.TYP_XLSX)
    }
}
```

#### Custom Export Logic

```kotlin
val customExport = actor(menu = action, label = "Custom Export") {
    key = Key.F11
    icon = Icon.EXPORT
}

val customExportCmd = command(item = customExport) {
    // Custom export logic
    val data = model.getAllRows()
    val fileName = "custom_report_${LocalDate.now()}.csv"
    
    exportToCustomFormat(data, fileName)
    
    notice("Report exported to $fileName")
}
```

---

## Charts API Reference

Charts provide data visualization capabilities with various chart types and customization options.

### Basic Chart Structure

```kotlin
class SalesChart : Chart(
    title = "Monthly Sales Analysis",
    help = "Sales performance by month and region",
    locale = Locale.UK
) {
    
    // Chart dimension (X-axis)
    val month = dimension(STRING(20)) {
        label = "Month"
        
        // Custom formatting for dimension values
        format { value ->
            value?.let {
                try {
                    val date = LocalDate.parse("$it-01")
                    date.format(DateTimeFormatter.ofPattern("MMM yyyy"))
                } catch (e: Exception) {
                    it
                }
            }
        }
    }
    
    // Chart measures (Y-axis values)
    val revenue = measure(DECIMAL(15, 2)) {
        label = "Revenue (EUR)"
        
        // Color customization
        color {
            VColor.BLUE
        }
    }
    
    val profit = measure(DECIMAL(15, 2)) {
        label = "Profit (EUR)"
        
        color {
            VColor.GREEN
        }
    }
    
    val orderCount = measure(INT(10)) {
        label = "Order Count"
        
        color {
            VColor.RED
        }
    }
    
    // Chart type configuration
    val chartType = trigger(CHARTTYPE) {
        VChartType.COLUMN  // Options: BAR, COLUMN, LINE, AREA, PIE
    }
    
    // Data loading
    init {
        // Load data for each month
        val salesData = loadMonthlySalesData()
        
        salesData.forEach { (monthKey, data) ->
            month.add(monthKey) {
                this[revenue] = data.revenue
                this[profit] = data.profit  
                this[orderCount] = data.orderCount
            }
        }
    }
    
    private fun loadMonthlySalesData(): Map<String, SalesData> {
        return transaction {
            Orders.slice(
                Orders.orderDate.month(),
                Orders.total.sum(),
                Orders.profit.sum(),
                Orders.id.count()
            )
            .selectAll()
            .groupBy(Orders.orderDate.month())
            .associate { row ->
                val month = row[Orders.orderDate.month()].toString()
                val data = SalesData(
                    revenue = row[Orders.total.sum()] ?: BigDecimal.ZERO,
                    profit = row[Orders.profit.sum()] ?: BigDecimal.ZERO,
                    orderCount = row[Orders.id.count()].toInt()
                )
                month to data
            }
        }
    }
    
    data class SalesData(
        val revenue: BigDecimal,
        val profit: BigDecimal,
        val orderCount: Int
    )
}
```

### Chart Types and Configuration

#### Column/Bar Charts

```kotlin
class ColumnChart : Chart("Column Chart Example") {
    val category = dimension(STRING(30)) {
        label = "Category"
    }
    
    val value = measure(DECIMAL(10, 2)) {
        label = "Value"
        color { VColor.BLUE }
    }
    
    val chartType = trigger(CHARTTYPE) {
        VChartType.COLUMN  // Vertical bars
        // VChartType.BAR   // Horizontal bars
    }
}
```

#### Line/Area Charts

```kotlin
class TimeSeriesChart : Chart("Time Series Chart") {
    val date = dimension(STRING(10)) {
        label = "Date"
        
        format { value ->
            // Format date strings for display
            value?.let { LocalDate.parse(it).format(DateTimeFormatter.ofPattern("MMM dd")) }
        }
    }
    
    val temperature = measure(DECIMAL(5, 1)) {
        label = "Temperature (°C)"
        color { VColor.RED }
    }
    
    val humidity = measure(DECIMAL(5, 1)) {
        label = "Humidity (%)"
        color { VColor.BLUE }
    }
    
    val chartType = trigger(CHARTTYPE) {
        VChartType.LINE  // Line chart
        // VChartType.AREA // Area chart
    }
}
```

#### Pie Charts

```kotlin
class PieChart : Chart("Market Share Analysis") {
    val company = dimension(STRING(50)) {
        label = "Company"
    }
    
    val marketShare = measure(DECIMAL(5, 2)) {
        label = "Market Share (%)"
        
        color {
            // Dynamic color based on value
            when {
                value > 30 -> VColor.GREEN
                value > 15 -> VColor.YELLOW
                else -> VColor.RED
            }
        }
    }
    
    val chartType = trigger(CHARTTYPE) {
        VChartType.PIE
    }
}
```

### Chart Customization

#### Color Schemes

```kotlin
val coloredMeasure = measure(DECIMAL(10, 2)) {
    label = "Colored Measure"
    
    color {
        // Static color
        VColor.BLUE
    }
}

val dynamicColorMeasure = measure(DECIMAL(10, 2)) {
    label = "Dynamic Color"
    
    color {
        // Dynamic color based on value
        when {
            value > 1000 -> VColor.GREEN
            value > 500 -> VColor.YELLOW
            else -> VColor.RED
        }
    }
}

val customColorMeasure = measure(DECIMAL(10, 2)) {
    label = "Custom Color"
    
    color {
        VColor(red = 128, green = 64, blue = 192)  // Custom RGB
    }
}
```

#### Chart Triggers

```kotlin
class CustomChart : Chart("Custom Chart") {
    // Chart initialization
    val initTrigger = trigger(INIT) {
        // Initialize chart settings
        setupChartDefaults()
    }
    
    // Chart type selection
    val chartType = trigger(CHARTTYPE) {
        // Can be determined dynamically
        when (getUserPreference()) {
            "detailed" -> VChartType.LINE
            "summary" -> VChartType.COLUMN
            else -> VChartType.BAR
        }
    }
}
```

### Data Loading and Aggregation

#### Simple Data Loading

```kotlin
init {
    // Simple dimension with measures
    val categories = listOf("A", "B", "C", "D")
    val values = listOf(100, 150, 75, 200)
    
    categories.zip(values).forEach { (cat, value) ->
        dimension.add(cat) {
            this[measure] = BigDecimal(value)
        }
    }
}
```

#### Database-Driven Charts

```kotlin
init {
    transaction {
        // Group sales by region
        (Orders innerJoin Customers)
            .slice(
                Customers.region,
                Orders.total.sum(),
                Orders.id.count()
            )
            .selectAll()
            .groupBy(Customers.region)
            .forEach { row ->
                region.add(row[Customers.region]) {
                    this[totalSales] = row[Orders.total.sum()] ?: BigDecimal.ZERO
                    this[orderCount] = row[Orders.id.count()].toInt()
                }
            }
    }
}
```

#### Time-Based Data

```kotlin
init {
    transaction {
        // Daily sales for last 30 days
        val thirtyDaysAgo = LocalDate.now().minusDays(30)
        
        Orders.select { Orders.orderDate greater thirtyDaysAgo }
            .groupBy(Orders.orderDate)
            .forEach { row ->
                val date = row[Orders.orderDate].toString()
                dateTime.add(date) {
                    this[dailySales] = row[Orders.total]
                }
            }
    }
}
```

---

## Pivot Tables API Reference

Pivot tables provide interactive data analysis with drag-and-drop functionality for dimensions and measures.

### Basic Pivot Table Structure

```kotlin
class ProductPivotTable : PivotTable(title = "Product Analysis", locale = Locale.UK) {
    
    val action = menu("Action")
    
    val quit = actor(menu = action, label = "Quit", help = "Close pivot table") {
        key = Key.F1
        icon = Icon.QUIT
    }
    
    val quitCmd = command(item = quit) { 
        model.close() 
    }
    
    // Dimensions (can be dragged to rows/columns)
    val product = dimension(STRING(100), Position.NONE) {
        label = "Product"
        help = "Product name"
    }
    
    val category = dimension(STRING(50), Position.ROW) {
        label = "Category"
        help = "Product category"
    }
    
    val region = dimension(STRING(30), Position.COLUMN) {
        label = "Region"
        help = "Sales region"
    }
    
    val salesperson = dimension(STRING(50), Position.ROW) {
        label = "Salesperson"
        help = "Sales representative"
    }
    
    // Measures (aggregated values)
    val revenue = measure(DECIMAL(15, 2)) {
        label = "Revenue"
        help = "Total revenue in EUR"
    }
    
    val quantity = measure(INT(10)) {
        label = "Quantity Sold"
        help = "Total quantity sold"
    }
    
    val avgPrice = measure(DECIMAL(10, 2)) {
        label = "Average Price"
        help = "Average selling price"
    }
    
    // Pivot table configuration
    val init = trigger(INIT) {
        // Default renderer (TABLE, HEATMAP, BAR_CHART, etc.)
        defaultRenderer = Renderer.TABLE
        
        // Default aggregator (COUNT, SUM, AVERAGE, etc.)
        aggregator = Pair(Aggregator.SUM, "revenue")
        
        // Disable specific renderers
        disabledRenderers = mutableListOf(Renderer.TSV_EXPORT)
    }
    
    // Data loading
    init {
        transaction {
            // Load sales data with joins
            (Orders innerJoin OrderItems innerJoin Products innerJoin Customers)
                .selectAll()
                .forEach { row ->
                    add {
                        this[product] = row[Products.name]
                        this[category] = row[Products.category]
                        this[region] = row[Customers.region]
                        this[salesperson] = row[Orders.salesperson]
                        this[revenue] = row[OrderItems.price] * BigDecimal(row[OrderItems.quantity])
                        this[quantity] = row[OrderItems.quantity]
                        this[avgPrice] = row[OrderItems.price]
                    }
                }
        }
    }
}
```

### Dimension Positioning

#### Position Options

```kotlin
// Dimension positioning options
val rowDimension = dimension(STRING(50), Position.ROW) {
    label = "Row Dimension"
}

val columnDimension = dimension(STRING(50), Position.COLUMN) {
    label = "Column Dimension" 
}

val noneDimension = dimension(STRING(50), Position.NONE) {
    label = "Available Dimension"  // Available for dragging
}
```

#### Dynamic Positioning

```kotlin
val flexibleDimension = dimension(STRING(50), getInitialPosition()) {
    label = "Flexible Dimension"
}

private fun getInitialPosition(): Position {
    return when (userPreference) {
        "rows" -> Position.ROW
        "columns" -> Position.COLUMN
        else -> Position.NONE
    }
}
```

### Measures and Aggregation

#### Basic Measures

```kotlin
val salesAmount = measure(DECIMAL(15, 2)) {
    label = "Sales Amount"
    help = "Total sales in EUR"
}

val itemCount = measure(INT(10)) {
    label = "Item Count"
    help = "Number of items sold"
}
```

#### Calculated Measures

```kotlin
val profitMargin = measure(DECIMAL(8, 4)) {
    label = "Profit Margin"
    help = "Profit margin percentage"
    
    // This would be calculated during aggregation
    // based on the aggregator settings
}
```

### Pivot Table Configuration

#### Renderer Options

```kotlin
val init = trigger(INIT) {
    // Default renderer
    defaultRenderer = Renderer.TABLE
    
    // Available renderers:
    // Renderer.TABLE - Standard table view
    // Renderer.TABLE_BARCHART - Table with bar charts
    // Renderer.HEATMAP - Color-coded heatmap
    // Renderer.ROW_HEATMAP - Row-based heatmap
    // Renderer.COL_HEATMAP - Column-based heatmap
    // Renderer.LINE_CHART - Line chart visualization
    // Renderer.BAR_CHART - Bar chart visualization
    // Renderer.STACKED_BAR_CHART - Stacked bar chart
    // Renderer.AREA_CHART - Area chart visualization
    // Renderer.SCATTER_CHART - Scatter plot
    // Renderer.TSV_EXPORT - Tab-separated export
}
```

#### Aggregator Options

```kotlin
val init = trigger(INIT) {
    // Default aggregator
    aggregator = Pair(Aggregator.SUM, "revenue")
    
    // Available aggregators:
    // Aggregator.COUNT - Count records
    // Aggregator.COUNT_UNIQUE_VALUES - Count unique values
    // Aggregator.LIST_UNIQUE_VALUES - List unique values
    // Aggregator.SUM - Sum values
    // Aggregator.INTEGER_SUM - Sum integer values
    // Aggregator.AVERAGE - Calculate average
    // Aggregator.MEDIAN - Calculate median
    // Aggregator.SAMPLE_VARIANCE - Sample variance
    // Aggregator.SAMPLE_STANDARD_DEVIATION - Sample std dev
    // Aggregator.MINIMUM - Minimum value
    // Aggregator.MAXIMUM - Maximum value
    // Aggregator.FIRST - First value
    // Aggregator.LAST - Last value
    // Aggregator.SUM_OVER_SUM - Ratio calculation
    // Aggregator.UPPER_BOUND_80 - 80% upper bound
    // Aggregator.LOWER_BOUND_80 - 80% lower bound
}
```

#### Customization Options

```kotlin
val init = trigger(INIT) {
    defaultRenderer = Renderer.TABLE
    aggregator = Pair(Aggregator.SUM, "")
    
    // Disable specific renderers
    disabledRenderers = mutableListOf(
        Renderer.SCATTER_CHART,
        Renderer.TSV_EXPORT
    )
    
    // Custom configuration
    showUI = true  // Show/hide UI controls
    autoSortUnusedAttrs = true  // Auto-sort unused dimensions
}
```

### Advanced Pivot Table Patterns

#### Multi-Level Hierarchical Data

```kotlin
class HierarchicalPivotTable : PivotTable("Sales Hierarchy") {
    val country = dimension(STRING(50), Position.ROW) {
        label = "Country"
    }
    
    val state = dimension(STRING(50), Position.ROW) {
        label = "State/Province"
    }
    
    val city = dimension(STRING(50), Position.ROW) {
        label = "City"
    }
    
    val year = dimension(STRING(4), Position.COLUMN) {
        label = "Year"
    }
    
    val quarter = dimension(STRING(2), Position.COLUMN) {
        label = "Quarter"
    }
    
    val sales = measure(DECIMAL(15, 2)) {
        label = "Sales"
    }
    
    init {
        transaction {
            // Load hierarchical sales data
            SalesData.selectAll().forEach { row ->
                add {
                    this[country] = row[SalesData.country]
                    this[state] = row[SalesData.state]
                    this[city] = row[SalesData.city]
                    this[year] = row[SalesData.saleDate].year.toString()
                    this[quarter] = "Q${(row[SalesData.saleDate].monthValue - 1) / 3 + 1}"
                    this[sales] = row[SalesData.amount]
                }
            }
        }
    }
}
```

#### Time-Based Analysis

```kotlin
class TimePivotTable : PivotTable("Time Analysis") {
    val product = dimension(STRING(100), Position.ROW) {
        label = "Product"
    }
    
    val year = dimension(STRING(4), Position.COLUMN) {
        label = "Year"
    }
    
    val month = dimension(STRING(7), Position.COLUMN) {
        label = "Month"
    }
    
    val revenue = measure(DECIMAL(15, 2)) {
        label = "Revenue"
    }
    
    val growth = measure(DECIMAL(8, 2)) {
        label = "Growth %"
    }
    
    init {
        transaction {
            MonthlySales.selectAll().forEach { row ->
                add {
                    this[product] = row[MonthlySales.productName]
                    this[year] = row[MonthlySales.year].toString()
                    this[month] = "${row[MonthlySales.year]}-${row[MonthlySales.month].toString().padStart(2, '0')}"
                    this[revenue] = row[MonthlySales.revenue]
                    this[growth] = row[MonthlySales.growthPercent]
                }
            }
        }
    }
}
```

---

## Database Integration

Galite provides seamless integration with relational databases through the Exposed framework.

### Database Connection Setup

#### Basic Connection Configuration

```kotlin
// PostgreSQL
Database.connect(
    url = "jdbc:postgresql://localhost:5432/myapp",
    driver = "org.postgresql.Driver", 
    user = "username",
    password = "password"
)

// MySQL
Database.connect(
    url = "jdbc:mysql://localhost:3306/myapp",
    driver = "com.mysql.cj.jdbc.Driver",
    user = "username", 
    password = "password"
)

// H2 (for testing)
Database.connect(
    url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
    driver = "org.h2.Driver"
)
```

#### Connection Pool Configuration

```kotlin
val config = HikariConfig().apply {
    jdbcUrl = "jdbc:postgresql://localhost:5432/myapp"
    username = "dbuser"
    password = "dbpass"
    maximumPoolSize = 20
    minimumIdle = 5
    connectionTimeout = 30000
    idleTimeout = 600000
    maxLifetime = 1800000
}

Database.connect(HikariDataSource(config))
```

### Table Definitions

#### Basic Table Structure

```kotlin
object Users : Table("users") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100)
    val email = varchar("email", 100).uniqueIndex()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val isActive = bool("is_active").default(true)
    
    override val primaryKey = PrimaryKey(id)
}

object Orders : Table("orders") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val orderNumber = varchar("order_number", 50).uniqueIndex()
    val total = decimal("total", 15, 2)
    val orderDate = date("order_date").default(LocalDate.now())
    val status = varchar("status", 20).default("PENDING")
    
    override val primaryKey = PrimaryKey(id)
}

object OrderItems : Table("order_items") {
    val id = integer("id").autoIncrement()
    val orderId = integer("order_id").references(Orders.id)
    val productId = integer("product_id").references(Products.id)
    val quantity = integer("quantity")
    val price = decimal("price", 10, 2)
    
    override val primaryKey = PrimaryKey(id)
}
```

#### Advanced Table Features

```kotlin
object Products : Table("products") {
    val id = integer("id").autoIncrement()
    val code = varchar("code", 50).uniqueIndex()
    val name = varchar("name", 200)
    val description = text("description").nullable()
    val price = decimal("price", 10, 2)
    val categoryId = integer("category_id").references(Categories.id).nullable()
    val supplierId = integer("supplier_id").references(Suppliers.id).nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").nullable()
    val deletedAt = datetime("deleted_at").nullable()  // Soft delete
    
    // Composite indexes
    val categorySupplierIndex = index("idx_category_supplier", false, categoryId, supplierId)
    
    // Check constraints
    init {
        check("price_positive") { price greater BigDecimal.ZERO }
    }
    
    override val primaryKey = PrimaryKey(id)
}
```

### Form-Database Mapping

#### Simple Table Mapping

```kotlin
inner class ProductBlock : Block("Product", 1, 1000) {
    // Map block to database table
    val p = table(Products, idColumn = Products.id, sequence = Sequence("products_id_seq"))
    
    val id = hidden(INT(11)) {
        columns(p.id)
    }
    
    val code = mustFill(STRING(50, Convert.UPPER), at(1, 1)) {
        label = "Product Code"
        columns(p.code) {
            priority = 10
        }
    }
    
    val name = mustFill(STRING(200), at(2, 1..3)) {
        label = "Product Name"
        columns(p.name) {
            priority = 9
        }
    }
    
    val price = visit(DECIMAL(10, 2), at(3, 1)) {
        label = "Price"
        columns(p.price)
    }
}
```

#### Multi-Table Joins

```kotlin
inner class OrderBlock : Block("Order", 1, 1000) {
    // Define table aliases for joins
    val o = table(Orders)        // Main table
    val u = table(Users)         // Joined table
    val s = table(OrderStatus)   // Lookup table
    
    val id = hidden(INT(11)) {
        columns(o.id)
    }
    
    val orderNumber = mustFill(STRING(50), at(1, 1)) {
        label = "Order Number"
        columns(o.orderNumber)
    }
    
    // Join to Users table
    val customerName = visit(STRING(100), at(2, 1..2)) {
        label = "Customer"
        columns(u.name, nullable(o.userId))  // LEFT JOIN
    }
    
    val customerEmail = skipped(STRING(100), at(2, 3)) {
        label = "Email"
        columns(u.email, nullable(o.userId))
    }
    
    // Join to status lookup
    val statusName = visit(STRING(50), at(3, 1)) {
        label = "Status"
        columns(s.name, nullable(o.statusId))
    }
    
    val total = visit(DECIMAL(15, 2), at(4, 1)) {
        label = "Total"
        columns(o.total)
    }
}
```

#### Complex Column Mappings

```kotlin
val complexField = visit(STRING(100), at(1, 1)) {
    label = "Complex Field"
    
    // Multiple column mapping with different table relationships
    columns(
        mainTable.primaryColumn,
        nullable(joinTable.foreignKey),
        key(lookupTable.lookupKey)
    ) {
        priority = 5
        index = uniqueIndex
    }
}
```

### Transaction Management

#### Basic Transactions

```kotlin
// Simple transaction
transaction {
    val newUser = Users.insert {
        it[name] = "John Doe"
        it[email] = "john@example.com"
    }
    
    Orders.insert {
        it[userId] = newUser[Users.id]
        it[orderNumber] = generateOrderNumber()
        it[total] = BigDecimal("99.99")
    }
}
```

#### Named Transactions with Error Handling

```kotlin
transaction("Create Order with Items") {
    try {
        // Create order
        val orderId = Orders.insert {
            it[userId] = currentUserId
            it[orderNumber] = orderNum
            it[total] = orderTotal
        }[Orders.id]
        
        // Create order items
        orderItems.forEach { item ->
            OrderItems.insert {
                it[OrderItems.orderId] = orderId
                it[productId] = item.productId
                it[quantity] = item.quantity
                it[price] = item.price
            }
        }
        
        // Update inventory
        updateInventory(orderItems)
        
    } catch (e: Exception) {
        // Transaction will be rolled back automatically
        throw VExecFailedException("Failed to create order: ${e.message}")
    }
}
```

#### Transaction in Form Triggers

```kotlin
inner class OrderBlock : Block("Order", 1, 1000) {
    init {
        trigger(PREINS) {
            // Set audit fields before insert
            createdAt.value = LocalDateTime.now()
            createdBy.value = getCurrentUser()
            orderNumber.value = generateOrderNumber()
        }
        
        trigger(POSTINS) {
            // Post-insert processing
            transaction("Post Order Creation") {
                // Create audit log entry
                AuditLog.insert {
                    it[tableName] = "orders"
                    it[recordId] = id.value!!
                    it[action] = "INSERT"
                    it[userId] = getCurrentUser()
                    it[timestamp] = LocalDateTime.now()
                }
                
                // Send notifications
                sendOrderNotification(id.value!!)
            }
        }
        
        trigger(PREUPD) {
            // Before update validation
            if (status.value == "SHIPPED" && originalStatus != "SHIPPED") {
                if (!canShipOrder(id.value!!)) {
                    throw VExecFailedException("Order cannot be shipped - insufficient inventory")
                }
            }
            
            updatedAt.value = LocalDateTime.now()
            updatedBy.value = getCurrentUser()
        }
    }
}
```

### Query Building and Data Access

#### Simple Queries

```kotlin
// Select all active users
val activeUsers = transaction {
    Users.select { Users.isActive eq true }
        .orderBy(Users.name)
        .toList()
}

// Select with joins
val ordersWithCustomers = transaction {
    (Orders innerJoin Users)
        .select { Orders.orderDate greater LocalDate.now().minusDays(30) }
        .orderBy(Orders.orderDate.desc())
        .map { row ->
            OrderWithCustomer(
                orderId = row[Orders.id],
                orderNumber = row[Orders.orderNumber],
                customerName = row[Users.name],
                total = row[Orders.total]
            )
        }
}
```

#### Complex Aggregation Queries

```kotlin
// Sales summary by category
val categorySales = transaction {
    (Products innerJoin OrderItems innerJoin Orders)
        .slice(
            Products.categoryId,
            Categories.name,
            OrderItems.quantity.sum(),
            (OrderItems.price * OrderItems.quantity.castTo<BigDecimal>()).sum()
        )
        .select { Orders.orderDate greater startDate }
        .groupBy(Products.categoryId, Categories.name)
        .having { OrderItems.quantity.sum() greater 0 }
        .orderBy((OrderItems.price * OrderItems.quantity.castTo<BigDecimal>()).sum().desc())
        .map { row ->
            CategorySales(
                categoryId = row[Products.categoryId],
                categoryName = row[Categories.name],
                totalQuantity = row[OrderItems.quantity.sum()],
                totalRevenue = row[(OrderItems.price * OrderItems.quantity.castTo<BigDecimal>()).sum()]
            )
        }
}
```

#### Conditional Query Building

```kotlin
fun searchOrders(criteria: OrderSearchCriteria): List<OrderSummary> {
    return transaction {
        val query = (Orders innerJoin Users)
            .selectAll()
        
        // Add conditions based on criteria
        if (criteria.customerId != null) {
            query.andWhere { Orders.userId eq criteria.customerId }
        }
        
        if (criteria.status != null) {
            query.andWhere { Orders.status eq criteria.status }
        }
        
        if (criteria.dateRange != null) {
            query.andWhere { 
                Orders.orderDate.between(criteria.dateRange.start, criteria.dateRange.end) 
            }
        }
        
        if (criteria.minAmount != null) {
            query.andWhere { Orders.total greaterEq criteria.minAmount }
        }
        
        query.orderBy(Orders.orderDate.desc())
            .limit(criteria.limit ?: 100)
            .map { row ->
                OrderSummary(
                    id = row[Orders.id],
                    orderNumber = row[Orders.orderNumber],
                    customerName = row[Users.name],
                    total = row[Orders.total],
                    orderDate = row[Orders.orderDate],
                    status = row[Orders.status]
                )
            }
    }
}
```

### Database Schema Management

#### Schema Creation

```kotlin
// Create tables
transaction {
    SchemaUtils.create(Users, Orders, OrderItems, Products, Categories)
}

// Create indexes
transaction {
    SchemaUtils.createIndex(
        Index(listOf(Orders.userId, Orders.orderDate), false, "idx_orders_user_date")
    )
}
```

#### Schema Migration

```kotlin
class DatabaseMigration {
    fun migrate() {
        transaction {
            // Check current schema version
            val currentVersion = getCurrentSchemaVersion()
            
            when {
                currentVersion < 1 -> migrateToV1()
                currentVersion < 2 -> migrateToV2()
                currentVersion < 3 -> migrateToV3()
            }
        }
    }
    
    private fun migrateToV1() {
        // Add new columns
        SchemaUtils.addColumns(Users.isActive)
        SchemaUtils.addColumns(Orders.status)
        
        // Update schema version
        updateSchemaVersion(1)
    }
    
    private fun migrateToV2() {
        // Create new tables
        SchemaUtils.create(AuditLog)
        
        // Add indexes
        SchemaUtils.createIndex(
            Index(listOf(AuditLog.tableName, AuditLog.recordId), false)
        )
        
        updateSchemaVersion(2)
    }
}
```

---

## Advanced Patterns & Examples

### Custom Field Types and Validation

#### Email Field with Validation

```kotlin
class EmailField(block: Block, position: FormPosition) : 
    FormField<String>(block, STRING(100), block.fields.size, VConstants.ACS_VISIT, position) {
    
    init {
        label = "Email Address"
        
        trigger(VALIDATE) {
            value?.let { email ->
                if (!isValidEmail(email)) {
                    throw VExecFailedException("Please enter a valid email address")
                }
                
                // Check for duplicates
                if (isDuplicateEmail(email, block.id.value)) {
                    throw VExecFailedException("Email address already exists")
                }
            }
        }
        
        trigger(POSTCHG) {
            // Auto-format email
            value = value?.lowercase()?.trim()
        }
    }
    
    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        return email.matches(emailRegex.toRegex())
    }
    
    private fun isDuplicateEmail(email: String, currentId: Int?): Boolean {
        return transaction {
            Users.select { 
                (Users.email eq email) and 
                (if (currentId != null) Users.id neq currentId else Op.TRUE)
            }.count() > 0
        }
    }
}

// Usage in form
val email = EmailField(this, at(2, 1..2))
```

#### Currency Field with Formatting

```kotlin
class CurrencyField(block: Block, position: FormPosition, val currency: String = "EUR") :
    FormField<BigDecimal>(block, DECIMAL(15, 2), block.fields.size, VConstants.ACS_VISIT, position) {
    
    init {
        label = "Amount ($currency)"
        align = FieldAlignment.RIGHT
        
        trigger(VALIDATE) {
            value?.let { amount ->
                if (amount < BigDecimal.ZERO) {
                    throw VExecFailedException("Amount cannot be negative")
                }
                
                if (amount > BigDecimal("999999999.99")) {
                    throw VExecFailedException("Amount exceeds maximum limit")
                }
            }
        }
        
        // Custom formatting for display
        format { value ->
            value?.let { 
                NumberFormat.getCurrencyInstance().apply {
                    this.currency = Currency.getInstance(this@CurrencyField.currency)
                }.format(it)
            } ?: ""
        }
    }
}
```

### Complex Business Logic Patterns

#### Audit Trail Implementation

```kotlin
abstract class AuditableForm(title: String) : Form(title) {
    
    protected fun <T : Block> T.enableAuditTrail(): T {
        trigger(PREINS) {
            setAuditFields(AuditAction.INSERT)
        }
        
        trigger(PREUPD) {
            setAuditFields(AuditAction.UPDATE)
        }
        
        trigger(POSTINS, POSTUPD, PREDEL) {
            createAuditRecord()
        }
        
        return this
    }
    
    private fun Block.setAuditFields(action: AuditAction) {
        val currentTime = LocalDateTime.now()
        val currentUser = getCurrentUser()
        
        when (action) {
            AuditAction.INSERT -> {
                // Set creation fields
                fields.find { it.ident == "created_at" }?.set(0, currentTime)
                fields.find { it.ident == "created_by" }?.set(0, currentUser)
            }
            AuditAction.UPDATE -> {
                // Set modification fields
                fields.find { it.ident == "updated_at" }?.set(0, currentTime)
                fields.find { it.ident == "updated_by" }?.set(0, currentUser)
            }
        }
    }
    
    private fun Block.createAuditRecord() {
        transaction {
            AuditLog.insert {
                it[tableName] = tables.first().name
                it[recordId] = fields.find { field -> field.ident == "id" }?.get(0) as? Int ?: 0
                it[userId] = getCurrentUser()
                it[timestamp] = LocalDateTime.now()
                it[action] = when (getMode()) {
                    VConstants.MOD_INSERT -> "INSERT"
                    VConstants.MOD_UPDATE -> "UPDATE"
                    else -> "UNKNOWN"
                }
                it[oldValues] = captureOldValues()
                it[newValues] = captureNewValues()
            }
        }
    }
    
    enum class AuditAction { INSERT, UPDATE, DELETE }
}

// Usage
class CustomerForm : AuditableForm("Customer Management") {
    val customerBlock = insertBlock(CustomerBlock().enableAuditTrail())
}
```

#### Workflow State Management

```kotlin
class WorkflowForm(title: String) : Form(title) {
    
    protected fun <T : Block> T.enableWorkflow(
        statusField: FormField<String>,
        transitions: Map<String, List<String>>
    ): T {
        
        trigger(PREUPD) {
            val currentStatus = statusField.value
            val newStatus = statusField.value
            
            if (currentStatus != newStatus) {
                validateTransition(currentStatus, newStatus, transitions)
            }
        }
        
        trigger(POSTUPD) {
            val newStatus = statusField.value
            executeWorkflowActions(newStatus)
        }
        
        return this
    }
    
    private fun validateTransition(
        from: String?, 
        to: String?, 
        transitions: Map<String, List<String>>
    ) {
        if (from != null && to != null) {
            val allowedTransitions = transitions[from] ?: emptyList()
            if (to !in allowedTransitions) {
                throw VExecFailedException("Invalid status transition from $from to $to")
            }
        }
    }
    
    private fun executeWorkflowActions(status: String?) {
        when (status) {
            "APPROVED" -> {
                sendApprovalNotification()
                createFollowUpTasks()
            }
            "REJECTED" -> {
                sendRejectionNotification()
                archiveRelatedDocuments()
            }
            "COMPLETED" -> {
                finalizeProcess()
                updateReports()
            }
        }
    }
}

// Usage
class OrderForm : WorkflowForm("Order Management") {
    val orderBlock = insertBlock(OrderBlock().enableWorkflow(
        statusField = status,
        transitions = mapOf(
            "DRAFT" to listOf("PENDING", "CANCELLED"),
            "PENDING" to listOf("APPROVED", "REJECTED"),
            "APPROVED" to listOf("SHIPPED", "CANCELLED"),
            "SHIPPED" to listOf("DELIVERED"),
            "DELIVERED" to listOf("COMPLETED")
        )
    ))
}
```

### Performance Optimization Patterns

#### Lazy Loading Implementation

```kotlin
class LazyLoadingBlock : Block("LazyData", 1, 1000) {
    private var dataLoaded = false
    private val lazyFields = mutableSetOf<FormField<*>>()
    
    val id = hidden(INT(11)) {
        columns(MainTable.id)
    }
    
    val name = mustFill(STRING(100), at(1, 1)) {
        columns(MainTable.name)
    }
    
    // Lazy loaded field
    val expensiveData = visit(STRING(500), at(2, 1..3)) {
        label = "Expensive Data"
        lazyFields.add(this)
        
        trigger(PREFLD) {
            if (!dataLoaded) {
                loadLazyData()
            }
        }
    }
    
    private fun loadLazyData() {
        if (!dataLoaded && id.value != null) {
            transaction {
                val data = ExpensiveTable
                    .select { ExpensiveTable.mainId eq id.value!! }
                    .firstOrNull()
                
                data?.let {
                    expensiveData.value = it[ExpensiveTable.data]
                }
                
                dataLoaded = true
            }
        }
    }
    
    init {
        trigger(POSTQRY) {
            dataLoaded = false  // Reset lazy loading flag
        }
    }
}
```

#### Batch Processing for Large Datasets

```kotlin
class BatchProcessingReport : Report("Large Dataset Report") {
    private val batchSize = 1000
    
    val field1 = field(STRING(50)) { label = "Field 1" }
    val field2 = field(DECIMAL(10, 2)) { label = "Field 2" }
    
    init {
        loadDataInBatches()
    }
    
    private fun loadDataInBatches() {
        transaction {
            var offset = 0
            var hasMore = true
            
            while (hasMore) {
                val batch = LargeTable
                    .selectAll()
                    .limit(batchSize, offset.toLong())
                    .toList()
                
                if (batch.isEmpty()) {
                    hasMore = false
                } else {
                    batch.forEach { row ->
                        add {
                            this[field1] = row[LargeTable.field1]
                            this[field2] = row[LargeTable.field2]
                        }
                    }
                    
                    offset += batchSize
                    
                    // Progress feedback
                    updateProgress(offset)
                }
            }
        }
    }
    
    private fun updateProgress(processed: Int) {
        // Update progress indicator
        println("Processed $processed records...")
    }
}
```

### Integration Patterns

#### REST API Integration

```kotlin
class APIIntegratedForm : Form("API Integration") {
    val customerBlock = insertBlock(CustomerBlock())
    
    inner class CustomerBlock : Block("Customer", 1, 1000) {
        val id = hidden(INT(11)) { columns(Customers.id) }
        
        val externalId = visit(STRING(50), at(1, 1)) {
            label = "External ID"
            columns(Customers.externalId)
            
            trigger(POSTCHG) {
                value?.let { extId ->
                    loadFromExternalAPI(extId)
                }
            }
        }
        
        val name = visit(STRING(100), at(2, 1)) {
            label = "Name"
            columns(Customers.name)
        }
        
        val email = visit(STRING(100), at(3, 1)) {
            label = "Email"
            columns(Customers.email)
        }
        
        private fun loadFromExternalAPI(externalId: String) {
            try {
                val apiClient = createAPIClient()
                val customerData = apiClient.getCustomer(externalId)
                
                // Populate fields with API data
                name.value = customerData.name
                email.value = customerData.email
                
                notice("Customer data loaded from external system")
                
            } catch (e: Exception) {
                error("Failed to load customer data: ${e.message}")
            }
        }
        
        init {
            trigger(PRESAVE) {
                // Sync with external system before saving
                syncWithExternalSystem()
            }
        }
        
        private fun syncWithExternalSystem() {
            val apiClient = createAPIClient()
            val customerData = CustomerData(
                externalId = externalId.value,
                name = name.value,
                email = email.value
            )
            
            try {
                apiClient.updateCustomer(customerData)
            } catch (e: Exception) {
                throw VExecFailedException("Failed to sync with external system: ${e.message}")
            }
        }
    }
}
```

#### File Upload and Processing

```kotlin
class FileUploadForm : Form("File Upload") {
    val uploadBlock = insertBlock(UploadBlock())
    
    inner class UploadBlock : Block("Upload", 1, 1) {
        val id = hidden(INT(11)) { columns(FileUploads.id) }
        
        val fileName = visit(STRING(255), at(1, 1..2)) {
            label = "File Name"
            columns(FileUploads.fileName)
        }
        
        val fileData = visit(IMAGE(400, 300), at(2, 1..3)) {
            label = "File"
            
            // Configure as droppable for file uploads
            droppable("pdf", "doc", "docx", "xls", "xlsx", "jpg", "png", "gif")
            
            trigger(POSTDROP) {
                processUploadedFile()
            }
        }
        
        val status = skipped(STRING(20), at(3, 1)) {
            label = "Status"
            columns(FileUploads.status)
        }
        
        val processedAt = skipped(DATETIME, at(3, 2)) {
            label = "Processed At"
            columns(FileUploads.processedAt)
        }
        
        private fun processUploadedFile() {
            try {
                status.value = "PROCESSING"
                
                // Process file based on type
                val fileExtension = fileName.value?.substringAfterLast('.', "")
                
                when (fileExtension?.lowercase()) {
                    "pdf" -> processPDFFile()
                    "xls", "xlsx" -> processExcelFile()
                    "jpg", "png", "gif" -> processImageFile()
                    else -> throw VExecFailedException("Unsupported file type")
                }
                
                status.value = "COMPLETED"
                processedAt.value = LocalDateTime.now()
                
                notice("File processed successfully")
                
            } catch (e: Exception) {
                status.value = "ERROR"
                error("File processing failed: ${e.message}")
            }
        }
        
        private fun processPDFFile() {
            // Extract text from PDF
            // Store metadata
            // Generate thumbnails
        }
        
        private fun processExcelFile() {
            // Parse Excel data
            // Validate data format
            // Import to database
        }
        
        private fun processImageFile() {
            // Resize images
            // Generate thumbnails
            // Extract EXIF data
        }
    }
}
```

---

## Migration Guide

### Upgrading from Earlier Versions

#### Version 1.2.x to 1.3.x

##### Breaking Changes

```kotlin
// OLD (1.2.x)
class OldForm : Form(title = "Old Form") {
    val block = insertBlock(OldBlock())
}

// NEW (1.3.x)
class NewForm : Form(title = "New Form", locale = Locale.UK) {
    val page = page("Main Page")
    val block = page.insertBlock(NewBlock())
}
```

##### API Changes

```kotlin
// OLD: Field positioning
val field = visit(STRING(50)) {
    position = Position(1, 1)
}

// NEW: Field positioning
val field = visit(STRING(50), at(1, 1)) {
    label = "Field"
}

// OLD: Domain definition
val customDomain = CustomDomain(50)

// NEW: Domain definition with type safety
val customDomain = STRING(50, Convert.UPPER)
```

##### Configuration Changes

```kotlin
// OLD: Application configuration
class MyApp : VApplication() {
    override fun getApplicationConfiguration(): ApplicationConfiguration {
        return MyAppConfig()
    }
}

// NEW: Application configuration
class MyApp : VApplication(Registry(domain = "MY_APP", parent = null)) {
    companion object {
        init {
            ApplicationConfiguration.setConfiguration(MyAppConfig)
        }
    }
}
```

#### Migration Steps

1. **Update Dependencies**
   ```kotlin
   dependencies {
       implementation("org.kopi:galite-core:1.3.0") // Updated
       implementation("org.kopi:galite-data:1.3.0") // Updated
   }
   ```

2. **Update Form Declarations**
   - Add locale parameter to Form constructors
   - Use page-based block organization
   - Update field positioning syntax

3. **Update Domain Usage**
   - Replace custom domain classes with built-in domains
   - Update domain parameter syntax

4. **Update Application Bootstrap**
   - Use new VApplication constructor
   - Update configuration initialization

### Common Migration Issues

#### Field Positioning

```kotlin
// Problem: Old positioning not working
val field = visit(STRING(50)) {
    position = Position(1, 1)  // OLD - Won't compile
}

// Solution: Use new positioning syntax
val field = visit(STRING(50), at(1, 1)) {
    label = "Field"
}
```

#### Domain Type Mismatches

```kotlin
// Problem: Type inference issues
val field = visit(customDomain) {  // May cause compilation errors
    columns(table.column)
}

// Solution: Explicit domain types
val field = visit(STRING(50), at(1, 1)) {
    columns(table.column)
}
```

#### Configuration Issues

```kotlin
// Problem: Configuration not applied
class MyApp : VApplication() {
    // Configuration not properly initialized
}

// Solution: Proper configuration setup
class MyApp : VApplication(Registry(domain = "MY_APP", parent = null)) {
    companion object {
        init {
            ApplicationConfiguration.setConfiguration(ConfigManager)
        }
    }
    
    object ConfigManager : ApplicationConfiguration() {
        // Configuration implementation
    }
}
```

---

## Troubleshooting

### Common Issues and Solutions

#### Database Connection Issues

**Problem**: Database connection failures
```
SQLException: Connection refused
```

**Solutions**:
```kotlin
// 1. Verify connection parameters
Database.connect(
    url = "jdbc:postgresql://localhost:5432/mydb",
    driver = "org.postgresql.Driver",
    user = "correct_username",
    password = "correct_password"
)

// 2. Add connection validation
val config = HikariConfig().apply {
    jdbcUrl = "jdbc:postgresql://localhost:5432/mydb"
    username = "user"
    password = "pass"
    
    // Connection validation
    connectionTestQuery = "SELECT 1"
    validationTimeout = 3000
    
    // Connection pool settings
    maximumPoolSize = 10
    minimumIdle = 2
}
```

#### Field Validation Errors

**Problem**: Validation not triggering
```kotlin
val field = visit(STRING(50), at(1, 1)) {
    trigger(VALIDATE) {
        // Validation not called
        if (value.isNullOrBlank()) {
            throw VExecFailedException("Required field")
        }
    }
}
```

**Solution**: Ensure proper field access and trigger registration
```kotlin
val field = mustFill(STRING(50), at(1, 1)) {  // Use mustFill for required
    label = "Required Field"
    
    trigger(VALIDATE) {
        value?.let { 
            if (it.trim().isEmpty()) {
                throw VExecFailedException("Field cannot be empty")
            }
        }
    }
}
```

#### Form Loading Issues

**Problem**: Form not displaying data
```kotlin
class MyForm : Form("My Form") {
    val block = insertBlock(MyBlock())
    
    inner class MyBlock : Block("Block", 1, 100) {
        // Fields not loading data
    }
}
```

**Solution**: Ensure proper table mapping and triggers
```kotlin
inner class MyBlock : Block("Block", 1, 100) {
    val t = table(MyTable)  // Ensure table is mapped
    
    val id = hidden(INT(11)) {
        columns(t.id)
    }
    
    init {
        trigger(POSTQRY) {
            // Ensure post-query processing
            println("Loaded ${recordCount} records")
        }
    }
}
```

#### Performance Issues

**Problem**: Slow form loading with large datasets

**Solutions**:
```kotlin
// 1. Implement pagination
inner class OptimizedBlock : Block("Data", 100, 20) {  // Smaller buffer
    init {
        trigger(PREQRY) {
            // Add LIMIT clause to queries
            // Implement pagination logic
        }
    }
}

// 2. Use lazy loading
val expensiveField = visit(STRING(1000), at(1, 1)) {
    trigger(PREFLD) {
        if (value == null) {
            loadExpensiveData()
        }
    }
}

// 3. Optimize database queries
val optimizedQuery = transaction {
    MyTable.select { MyTable.active eq true }
        .limit(100)
        .orderBy(MyTable.id)
}
```

#### Memory Issues

**Problem**: OutOfMemoryError with large reports

**Solutions**:
```kotlin
// 1. Implement streaming for large reports
class StreamingReport : Report("Large Report") {
    init {
        // Process data in chunks
        processInBatches(batchSize = 1000)
    }
    
    private fun processInBatches(batchSize: Int) {
        var offset = 0
        do {
            val batch = loadBatch(offset, batchSize)
            batch.forEach { record ->
                add { /* populate fields */ }
            }
            offset += batchSize
        } while (batch.size == batchSize)
    }
}

// 2. Clear unused data
trigger(POSTQRY) {
    // Clear previous data
    clearUnusedFields()
}
```

### Debugging Techniques

#### Enable Debug Logging

```kotlin
// In application configuration
object DebugConfig : ApplicationConfiguration() {
    override fun logErrors(): Boolean = true
    override fun debugMessageInTransaction(): Boolean = true
    
    // Custom logging
    override val logFile: String = "application-debug.log"
}
```

#### Add Debug Triggers

```kotlin
inner class DebugBlock : Block("Debug", 1, 100) {
    init {
        trigger(PREQRY) {
            println("PREQRY: Starting query")
        }
        
        trigger(POSTQRY) {
            println("POSTQRY: Loaded ${recordCount} records")
        }
        
        trigger(PREINS) {
            println("PREINS: Inserting record")
            fields.forEach { field ->
                println("  ${field.label}: ${field.value}")
            }
        }
    }
}
```

#### Transaction Debugging

```kotlin
transaction("Debug Transaction") {
    try {
        // Your database operations
        
    } catch (e: SQLException) {
        println("SQL Error: ${e.message}")
        println("SQL State: ${e.sqlState}")
        println("Error Code: ${e.errorCode}")
        throw e
    } catch (e: Exception) {
        println("General Error: ${e.message}")
        e.printStackTrace()
        throw e
    }
}
```

### Error Messages and Solutions

#### Common Error Messages

1. **"Table not found"**
   ```
   Solution: Ensure table is created and properly mapped
   SchemaUtils.create(MyTable)
   ```

2. **"Column not found"**
   ```
   Solution: Check column mapping in field definition
   columns(table.correctColumnName)
   ```

3. **"Invalid field access"**
   ```
   Solution: Check field access levels and modes
   blockVisibility(Access.VISIT, Mode.QUERY, Mode.UPDATE)
   ```

4. **"Transaction rollback"**
   ```
   Solution: Check for validation errors and constraint violations
   Add proper error handling in triggers
   ```

### Performance Tuning

#### Database Optimization

```kotlin
// 1. Add proper indexes
object OptimizedTable : Table("optimized") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100).index()  // Add index
    val status = varchar("status", 20)
    val createdAt = datetime("created_at").index()  // Add index
    
    // Composite index
    val statusDateIndex = index("idx_status_date", false, status, createdAt)
    
    override val primaryKey = PrimaryKey(id)
}

// 2. Use efficient queries
val efficientQuery = transaction {
    OptimizedTable
        .select { (OptimizedTable.status eq "ACTIVE") and 
                 (OptimizedTable.createdAt greater startDate) }
        .limit(100)  // Always limit results
        .orderBy(OptimizedTable.createdAt.desc())
}
```

#### Memory Optimization

```kotlin
// 1. Use appropriate buffer sizes
inner class OptimizedBlock : Block("Data", 
    buffer = 50,    // Reasonable buffer size
    visible = 20    // Visible records
) { }

// 2. Clear unused data
trigger(POSTQRY) {
    // Clear previous calculations
    clearTemporaryData()
}

// 3. Use lazy initialization
val lazyField by lazy {
    createExpensiveField()
}
```

This comprehensive documentation provides developers with everything needed to effectively use the Galite framework for building enterprise applications. The examples are based on real patterns found in the codebase and provide practical, working solutions for common development scenarios.

---

## Conclusion

The Galite framework provides a powerful, type-safe approach to building enterprise applications with Kotlin. Its DSL-based architecture enables rapid development while maintaining code quality and database integrity. The comprehensive API coverage ensures that developers can build complex business applications with forms, reports, charts, and pivot tables efficiently.

For additional support and updates, visit the [Galite GitHub repository](https://github.com/kopiLeft/Galite) or check the [Maven Central releases](https://mvnrepository.com/artifact/org.kopi/galite-core).