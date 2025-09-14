# Galite Framework - Comprehensive API Documentation

![Galite Logo](docs/logo_galite.png)

## Table of Contents

1. [Framework Overview](#framework-overview)
2. [Setup & Integration](#setup--integration)
3. [Core Architecture](#core-architecture)
4. [Domain System API](#domain-system-api)
5. [Forms API Reference](#forms-api-reference)
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

## Domain System API

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

---

## Advanced Patterns & Examples

### Complete Enterprise Form Example

Based on the analysis of the forms examples, here's a comprehensive enterprise form pattern:

```kotlin
class AssetForm : DefaultDictionaryForm(title = "Asset Management") {
    
    init {
        trigger(RESET) {
            setTitle("Asset Management")
            return@trigger false
        }
    }

    val assetPage = page("Asset Details").insertBlock(AssetBlock())
    val licensePage = page("Licenses").insertBlock(LicenseBlock())

    inner class AssetBlock : Block("Asset", 1, 1000) {

        init {
            blockVisibility(Access.VISIT, Mode.QUERY)
            
            // Standard commands
            breakCommand
            command(item = serialQuery, Mode.QUERY) { serialQuery() }
            command(item = searchOperator, Mode.QUERY) { searchOperator() }
            command(item = menuQuery, Mode.QUERY) { recursiveQuery() }
            command(item = delete, Mode.UPDATE) { deleteAsset(block) }
            command(item = insertMode, Mode.QUERY) { insertMode() }
            command(item = save, Mode.INSERT, Mode.UPDATE) { saveBlock() }
            command(item = dynamicReport) { createDynamicReport() }
            
            // Custom commands
            command(item = copy, Mode.UPDATE) { copyAsset() }
            command(item = assign, Mode.UPDATE) {
                block.validate()
                if (assignedId.value != null) {
                    throw VExecFailedException("Asset already assigned")
                } else {
                    val assetId = id.value!!
                    WindowController.windowController.doModal(AssignAsset(assetId))
                    block.clear()
                    id.value = assetId
                    transaction { block.load() }
                }
            }
            
            // Block triggers
            trigger(PREINS) {
                createdAt.value = LocalDateTime.now()
                modifiedAt.value = LocalDateTime.now()
                userId.value = getUserID()
            }
            
            trigger(PREUPD) {
                modifiedAt.value = LocalDateTime.now()
                userId.value = getUserID()
            }
            
            trigger(POSTINS, POSTUPD) {
                userId.value?.let { user.value = Utils.loadUser(it) }
            }
            
            trigger(PREQRY) {
                // Force condition: deleted_at IS NULL
                deletedAt.vField.setSearchOperator(VConstants.SOP_LE)
            }
            
            trigger(POSTQRY) {
                userId.value?.let { user.value = Utils.loadUser(it) }
                assignedTo.value = loadAssetAssignment(assignedId.value, assignmentType.value)
                
                // Color fields for better visibility
                assignedTo.value?.let {
                    assignmentType.setColor(foreground = null, background = VColor(230, 255, 230))
                    assignedTo.setColor(foreground = null, background = VColor(230, 255, 230))
                } ?: apply {
                    assignmentType.setColor(foreground = null, background = VColor(224, 224, 235))
                    assignedTo.setColor(foreground = null, background = VColor(224, 224, 235))
                }

                licensePage.clear()
                licensePage.load()
                setTitle("Asset: ${assetTag.value}")
            }
        }

        // Table aliases
        val a = table(assets)
        val m = table(models)
        val s = table(status_labels)

        // Index definitions
        val uniqueAssetTag = index("Unique asset tag required")

        // Field definitions
        val id = hidden(INT(11)) {
            columns(a.id)
        }
        
        val assetTag = mustFill(STRING(100, Convert.UPPER), at(1, 1..3)) {
            label = "Asset Tag"
            help = "Unique asset identifier"
            columns(a.asset_tag) {
                index = uniqueAssetTag
                priority = 9
            }
            
            trigger(POSTCHG) {
                if (block.getMode() == Mode.QUERY.value) {
                    val newTag = value
                    try {
                        transaction { block.load() }
                    } catch (e: VException) {
                        value = newTag
                        setMode(Mode.INSERT)
                        gotoNextField()
                    }
                }
            }
        }
        
        val serialNumber = visit(STRING(100, Convert.UPPER), at(2, 1..3)) {
            label = "Serial Number"
            help = "Device serial number"
            columns(a.serial) {
                priority = 8
            }
        }
        
        val name = visit(STRING(100), at(3, 1..3)) {
            label = "Name"
            help = "Asset name"
            columns(a.name) {
                priority = 7
            }
        }
        
        val assignmentType = visit(AssignmentType(), at(4, 1..3)) {
            label = "Assigned to"
            columns(a.assigned_type)
            onUpdateSkipped()
            onInsertSkipped()
        }
        
        val assignedId = hidden(INT(11)) {
            columns(a.assigned_to)
        }
        
        val assignedTo = skipped(STRING(100), follow(assignmentType)) {
            help = "Asset is assigned to this entity"
        }
        
        val model = mustFill(Models(), at(5, 1..3)) {
            label = "Model"
            help = "Asset model"
            columns(m.id, nullable(a.model_id)) {
                priority = 6
            }
            
            trigger(POSTCHG) {
                block.fetchLookupFirst(vField)
            }
            
            trigger(AUTOLEAVE) { true }
        }
        
        val status = visit(Statuses(), at(6, 1..3)) {
            label = "Status"
            columns(s.id, nullable(a.status_id)) {
                priority = 4
            }
            
            trigger(POSTCHG) {
                block.fetchLookupFirst(vField)
            }
        }
        
        val deployable = skipped(Deployable, follow(status)) {
            columns(s.deployable)
        }
        
        val company = visit(Companies(), at(7, 1..3)) {
            label = "Company"
            help = "Asset company"
            columns(a.company_id) {
                priority = 5
            }
        }
        
        val location = visit(Locations(), at(8, 1..3)) {
            label = "Default Location"
            help = "Default asset location"
            columns(a.rtd_location_id)
            
            trigger(POSTCHG) {
                currentLocation.value = value
            }
            
            trigger(AUTOLEAVE) { true }
        }
        
        val currentLocation = visit(Locations(), at(9, 1..3)) {
            label = "Current Location"
            help = "Current asset location"
            columns(a.location_id)
        }
        
        val supplier = visit(Suppliers(), at(10, 1..2)) {
            label = "Supplier"
            help = "Asset supplier"
            columns(a.supplier_id) {
                priority = 3
            }
        }
        
        val purchaseOrder = visit(STRING(20, convert = Convert.UPPER), at(11, 1)) {
            label = "Purchase Order"
            help = "Purchase order number"
            columns(a.order_number)
        }
        
        val purchaseDate = visit(DATE, at(12, 1)) {
            label = "Purchase Date"
            help = "Asset purchase date"
            columns(a.purchase_date) {
                priority = -10
            }
        }
        
        val purchaseCost = visit(DECIMAL(21, 2), at(13, 1..2)) {
            label = "Purchase Cost (EUR)"
            help = "Asset purchase cost"
            columns(a.purchase_cost)
        }
        
        val warranty = visit(INT(11), at(14, 1)) {
            label = "Warranty (months)"
            help = "Warranty period in months"
            columns(a.warranty_months)
        }
        
        val notes = visit(STRING(100, 10, 5, Fixed.OFF), at(19, 1..3)) {
            label = "Notes"
            help = "Additional notes"
            columns(a.notes)
        }
        
        val userId = hidden(INT(11)) {
            columns(a.user_id)
            trigger(PREINS, PREUPD) {
                value = getUserID()
            }
        }
        
        val user = skipped(STRING(50), at(21, 1..2)) {
            label = "User"
            options(FieldOption.TRANSIENT)
        }
        
        val createdAt = skipped(DATETIME, at(22, 1)) {
            label = "Created"
            help = "Creation date"
            columns(a.created_at)
        }
        
        val modifiedAt = skipped(DATETIME, at(23, 1)) {
            label = "Modified"
            help = "Last modification date"
            columns(a.updated_at)
        }
        
        val deletedAt = hidden(DATETIME) {
            label = "Deleted"
            help = "Deletion date"
            columns(a.deleted_at)
        }
        
        val modelImage = skipped(IMAGE(300, 300), at(1..15, 4)) {
            label = "Model Image"
            help = "Model photo"
            columns(m.image_source)
            options(FieldOption.TRANSIENT)
        }
        
        val image = visit(IMAGE(300, 300), at(1..15, 5)) {
            label = "Image"
            help = "Asset photo"
            columns(a.image_source)
        }

        /**
         * Delete record: Update deleted_at field
         */
        private fun deleteAsset(b: VBlock) {
            b.validate()
            transaction {
                assets.update({ assets.id eq this@AssetBlock.id.value!! }) {
                    it[user_id] = getUserID()
                    it[updated_at] = LocalDateTime.now()
                    it[deleted_at] = LocalDateTime.now()
                }
            }
            b.form.reset()
        }

        /**
         * Copy asset
         */
        private fun copyAsset() {
            id.clear(0)
            assetTag.clear(0)
            serialNumber.clear(0)
            assignedId.clear(0)
            assignmentType.clear(0)
            assignedTo.clear(0)
            userId.clear(0)
            user.clear(0)
            createdAt.clear(0)
            modifiedAt.clear(0)
            deletedAt.clear(0)
            licensePage.clear()
            setRecordFetched(0, false)
            gotoFirstField()
            setMode(Mode.INSERT)
        }

        /**
         * Load asset assignment details
         */
        private fun loadAssetAssignment(id: Int?, assignmentType: String?): String? {
            return id?.let {
                when (assignmentType) {
                    "App\\Models\\User" -> Utils.loadUser(it)
                    "App\\Models\\Location" -> Utils.loadLocationName(it)
                    "App\\Models\\Asset" -> Utils.loadAssetDescription(it)
                    else -> null
                }
            }
        }
    }

    inner class LicenseBlock : Block("License", 100, 20) {
        init {
            options(BlockOption.NODETAIL)
            border = Border.LINE

            command(item = unassign, Mode.QUERY) {
                if (isRecordFilled(currentRecord)) {
                    releaseLicenseSeat(currentRecord)
                } else {
                    throw VExecFailedException("No license selected")
                }
            }

            trigger(PREQRY) {
                // Force condition: deleted_at IS NULL
                deletedAt.vField.setSearchOperator(VConstants.SOP_LE)
            }
            
            trigger(POSTQRY) {
                (0 until block.bufferSize).filter { i -> isRecordFilled(i) }.forEach { i ->
                    status[i] = "Assigned"
                    status.setColor(null, VColor(255, 204, 204))
                }
            }
            
            trigger(ACCESS) {
                assetPage.getMode() != Mode.QUERY.value
            }
        }

        val p = table(license_seats)
        val l = table(licenses)

        val id = hidden(INT(11)){
            columns(p.id)
        }
        
        val assetId = hidden(INT(11)) {
            alias = assetPage.id
            columns(p.asset_id)
        }
        
        val license = skipped(Licenses(), at(1)) {
            label = "Name"
            columns(l.id, nullable(p.license_id))
            FieldOption.NOEDIT
        }
        
        val key = skipped(STRING(38, 5, 1, Fixed.OFF), at(1)) {
            label = "Product Key"
            columns(l.serial)
            FieldOption.NOEDIT
        }
        
        val status = visit(STRING(10), at(1)) {
            label = "Status"
            options(FieldOption.TRANSIENT)
        }
        
        val reassignable = hidden(BOOL) {
            columns(l.reassignable)
        }
        
        val deletedAt = hidden(DATETIME) {
            columns(p.deleted_at)
        }

        /**
         * Release license seat
         */
        fun releaseLicenseSeat(rec: Int) {
            val assetId = assetPage.id.value

            if (reassignable[rec] != true) {
                throw VExecFailedException("License cannot be reassigned")
            }
            
            WindowController.windowController.doModal(
                ReleaseLicenseSeat(license[rec]!!, id[rec], false)
            )

            assetPage.clear()
            assetPage.id.value = assetId
            transaction { assetPage.load() }
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

### Performance Issues

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

---

## Conclusion

The Galite framework provides a powerful, type-safe approach to building enterprise applications with Kotlin. Its DSL-based architecture enables rapid development while maintaining code quality and database integrity. The comprehensive API coverage ensures that developers can build complex business applications with forms, reports, charts, and pivot tables efficiently.

For additional support and updates, visit the [Galite GitHub repository](https://github.com/kopiLeft/Galite) or check the [Maven Central releases](https://mvnrepository.com/artifact/org.kopi/galite-core).

---

*This documentation covers Galite Framework version 1.3.0. For the latest updates and examples, visit the official repository.*