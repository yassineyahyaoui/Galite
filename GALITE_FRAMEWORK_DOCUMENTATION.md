# Galite Framework Documentation

![Galite Logo](docs/logo_galite.png)

## Framework Overview

Galite is a powerful Kotlin-based framework for building enterprise business applications with forms, reports, charts, and pivot tables. It provides a type-safe, expressive DSL (Domain Specific Language) that allows developers to create database-backed applications with minimal boilerplate code.

### Key Features

- **Kotlin DSL**: Type-safe builders for creating forms, reports, charts, and pivot tables
- **Database Integration**: Built on JetBrains Exposed ORM with support for multiple database dialects
- **Vaadin UI**: Modern web-based user interface using Vaadin components
- **Strongly Typed Fields**: Compile-time type checking for all field operations
- **Internationalization**: Built-in support for multiple locales and languages
- **Export Capabilities**: Export reports to CSV, PDF, Excel formats
- **Testing Framework**: Comprehensive testing utilities for UI components

### Gradle Dependencies

Add the following dependencies to your `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("org.kopi:galite-core:1.3.0")
    implementation("org.kopi:galite-data:1.3.0")
    implementation("org.kopi:galite-util:1.3.0")
    
    // For testing
    testImplementation("org.kopi:galite-testing:1.3.0")
}
```

## Core Components

### 1. Application Configuration

#### VApplication
**Package**: `org.kopi.galite.visual.ui.vaadin.visual`

The main application class that handles authentication, routing, and global configuration.

```kotlin
@Route("")
class MyApp : VApplication(Registry(domain = "MYAPP", parent = null)) {
    override val sologanImage get() = "my-slogan.png"
    override val logoImage get() = "my-logo.png"
    override val logoHref get() = "https://mywebsite.com"
    override val alternateLocale get() = Locale.UK
    override val supportedLocales get() = arrayOf(Locale.UK, Locale.FRANCE)
    
    override fun login(database: String, driver: String, username: String, 
                      password: String, schema: String?, maxRetries: Int?, 
                      waitMin: Long?, waitMax: Long?): Connection? {
        return Connection.createConnection(
            url = database,
            driver = driver,
            userName = username,
            password = password,
            lookupUserId = true,
            schema = schema
        )
    }
}
```

#### ApplicationConfiguration
**Package**: `org.kopi.galite.visual`

Configuration management for application settings.

```kotlin
object ConfigurationManager : ApplicationConfiguration() {
    override val version get(): String = "1.0"
    override val applicationName get(): String = "My Application"
    override val informationText get(): String = "Application info"
    override val logFile get(): String = "app.log"
    override fun getSMTPServer(): String = "smtp.server.com"
    override fun mailErrors(): Boolean = true
    override fun logErrors(): Boolean = true
}
```

### 2. Domain Types

**Package**: `org.kopi.galite.visual.domain`

Galite provides strongly-typed domain classes for different data types:

#### Basic Domains

```kotlin
// Numeric types
LONG(width: Int)                    // Long integers
INT(width: Int)                     // Integers  
DECIMAL(width: Int, scale: Int)     // Decimal numbers
FRACTION(width: Int)                // Fraction numbers

// Text types
STRING(width: Int)                  // String fields
TEXT(width: Int, height: Int)       // Multi-line text

// Date/Time types
DATE                                // LocalDate
TIME                                // LocalTime
DATETIME                            // LocalDateTime
TIMESTAMP                           // Instant
MONTH                               // Month type
WEEK                                // Week type

// Other types
BOOL                                // Boolean
COLOR                               // Color picker
IMAGE(width: Int, height: Int)      // Image field
```

#### Advanced Domains

```kotlin
// Code domains for dropdown lists
object Status : CodeDomain<Int>() {
    init {
        "Active" keyOf 1
        "Inactive" keyOf 0
    }
}

// List domains for lookup fields
object ClientList : ListDomain<Int>(30) {
    override val table = Client
    init {
        "ID" keyOf Client.id hasWidth 10
        "Name" keyOf Client.name hasWidth 50
    }
}
```

## DSL Reference

### 1. Forms

**Package**: `org.kopi.galite.visual.dsl.form`

Forms are the primary UI component for data entry and editing.

#### Form Structure

```kotlin
class MyForm : Form(title = "My Form", locale = Locale.UK) {
    
    // Define menus
    val fileMenu = menu("File")
    val actionMenu = menu("Action")
    
    // Define actors (menu items)
    val save = actor(menu = fileMenu, label = "Save", help = "Save record") {
        key = Key.F7
        icon = Icon.SAVE
    }
    
    // Define pages (tabs)
    val mainPage = page("Main Information")
    val detailPage = page("Details")
    
    // Define blocks
    val mainBlock = mainPage.insertBlock(MainBlock())
    
    // Form-level triggers
    init {
        trigger(INIT) {
            // Initialize form
        }
        
        trigger(PREFORM) {
            // Before form display
        }
    }
}
```

#### Block Definition

```kotlin
inner class MainBlock : Block("Main", buffer = 1, visible = 1) {
    // Link to database table
    val client = table(Client, Client.id)
    
    // Define fields with different access levels
    val id = visit(domain = LONG(10), position = at(1, 1)) {
        label = "ID"
        help = "Client identifier"
        columns(client.id) {
            priority = 1
        }
    }
    
    val name = mustFill(domain = STRING(50), position = at(1, 2)) {
        label = "Name"
        help = "Client name"
        columns(client.name)
        
        // Field triggers
        trigger(POSTCHG) {
            // After field change
        }
    }
    
    val email = visit(domain = STRING(100), position = at(2, 1..2)) {
        label = "Email"
        help = "Email address"
        columns(client.email)
    }
    
    val active = visit(domain = BOOL, position = at(3, 1)) {
        label = "Active"
        help = "Is client active?"
        columns(client.active)
    }
    
    // Block triggers
    init {
        trigger(PREBLK) {
            // Before entering block
        }
        
        trigger(POSTQRY) {
            // After database query
        }
        
        // Block commands
        command(item = save) { saveBlock() }
    }
}
```

#### Field Access Levels

```kotlin
// Field access types
mustFill(domain, position) { }    // Required field
visit(domain, position) { }       // Editable field  
skipped(domain, position) { }     // Read-only field
hidden(domain) { }                // Hidden field
```

#### Field Positioning

```kotlin
// Position syntax
at(line = 1, column = 1)              // Single position
at(line = 1, columnRange = 1..3)      // Span columns
at(lineRange = 1..2, column = 1)      // Span lines  
at(lineRange = 1..2, columnRange = 1..3) // Span both
follow(otherField)                     // Position after field
```

### 2. Reports

**Package**: `org.kopi.galite.visual.dsl.report`

Reports display tabular data with grouping, formatting, and export capabilities.

```kotlin
class ProductReport : Report(title = "Products", locale = Locale.UK) {
    
    // Define export actors
    val csv = actor(menu = actionMenu, label = "CSV", help = "Export CSV") {
        key = Key.F8
        icon = Icon.EXPORT_CSV
    }
    
    val pdf = actor(menu = actionMenu, label = "PDF", help = "Export PDF") {
        key = Key.F9
        icon = Icon.EXPORT_PDF
    }
    
    // Export commands
    val csvCmd = command(item = csv) { 
        model.export(VReport.TYP_CSV) 
    }
    
    val pdfCmd = command(item = pdf) { 
        model.export(VReport.TYP_PDF) 
    }
    
    // Define report fields
    val category = field(domain = STRING(20)) {
        label = "Category"
        help = "Product category"
        group = department  // Group by this field
    }
    
    val department = field(domain = STRING(30)) {
        label = "Department" 
        help = "Product department"
        group = description
    }
    
    val description = field(domain = STRING(100)) {
        label = "Description"
        help = "Product description"
        format { value ->
            value?.uppercase()  // Format display value
        }
    }
    
    val price = field(domain = DECIMAL(10, 2)) {
        label = "Price"
        help = "Unit price"
    }
    
    // Load data
    init {
        transaction {
            Product.selectAll().forEach { result ->
                add {
                    this[category] = result[Product.category]
                    this[department] = result[Product.department]
                    this[description] = result[Product.description]
                    this[price] = result[Product.price]
                }
            }
        }
    }
}
```

### 3. Charts

**Package**: `org.kopi.galite.visual.dsl.chart`

Charts provide visual data representation with multiple chart types.

```kotlin
class SalesChart : Chart(
    title = "Sales by Category",
    help = "Sales distribution across product categories",
    locale = Locale.UK
) {
    
    // Chart controls
    val pieView = actor(menu = actionMenu, label = "Pie", help = "Pie chart") {
        key = Key.F5
        icon = Icon.PIE_CHART
    }
    
    val barView = actor(menu = actionMenu, label = "Bar", help = "Bar chart") {
        key = Key.F6  
        icon = Icon.BAR_CHART
    }
    
    // Chart type commands
    val pieCmd = command(item = pieView) {
        model.setType(VChartType.PIE)
    }
    
    val barCmd = command(item = barView) {
        model.setType(VChartType.BAR)
    }
    
    // Define dimension (X-axis)
    val category = dimension(domain = STRING(30)) {
        label = "Category"
        help = "Product category"
        
        format { value ->
            value?.uppercase()
        }
    }
    
    // Define measures (Y-axis values)
    val totalSales = measure(domain = DECIMAL(15, 2)) {
        label = "Total Sales"
        help = "Total sales amount"
        
        color {
            VColor.BLUE
        }
    }
    
    val quantity = measure(domain = LONG(10)) {
        label = "Quantity Sold"
        help = "Number of items sold"
    }
    
    // Set chart type
    val chartTypeInit = trigger(CHARTTYPE) {
        VChartType.COLUMN
    }
    
    // Load chart data
    init {
        transaction {
            // Add data points
            category.add("Electronics") {
                this[totalSales] = BigDecimal("15000.00")
                this[quantity] = 150L
            }
            
            category.add("Clothing") {
                this[totalSales] = BigDecimal("8500.00") 
                this[quantity] = 95L
            }
        }
    }
}
```

### 4. Pivot Tables

**Package**: `org.kopi.galite.visual.dsl.pivottable`

Pivot tables provide interactive data analysis and aggregation.

```kotlin
class SalesPivotTable : PivotTable(title = "Sales Analysis", locale = Locale.UK) {
    
    // Define dimensions
    val product = dimension(domain = STRING(50), Position.ROW) {
        label = "Product"
        help = "Product name"
    }
    
    val region = dimension(domain = STRING(30), Position.COLUMN) {
        label = "Region" 
        help = "Sales region"
    }
    
    val quarter = dimension(domain = STRING(10), Position.ROW) {
        label = "Quarter"
        help = "Sales quarter"
    }
    
    // Define measures
    val revenue = measure(domain = DECIMAL(12, 2)) {
        label = "Revenue"
        help = "Total revenue"
    }
    
    val units = measure(domain = LONG(8)) {
        label = "Units Sold"
        help = "Number of units"
    }
    
    // Configure pivot table
    val initTrigger = trigger(INIT) {
        // Set default aggregator and renderer
        aggregator = Pair("Sum", "")
        defaultRenderer = "Table"
    }
    
    // Load data
    init {
        transaction {
            Sales.selectAll().forEach { result ->
                add {
                    this[product] = result[Sales.productName]
                    this[region] = result[Sales.region]
                    this[quarter] = result[Sales.quarter]
                    this[revenue] = result[Sales.amount]
                    this[units] = result[Sales.quantity]
                }
            }
        }
    }
}
```

## Quick Start Guide

### 1. Basic Setup

Create a new Kotlin project and add Galite dependencies:

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.kopi:galite-core:1.3.0")
    implementation("org.kopi:galite-data:1.3.0")
}
```

### 2. Database Configuration

Define your database tables using Exposed:

```kotlin
// Tables.kt
object Users : Table("USERS") {
    val id = integer("ID").autoIncrement()
    val name = varchar("NAME", 50)
    val email = varchar("EMAIL", 100)
    val active = bool("ACTIVE")
    
    override val primaryKey = PrimaryKey(id)
}
```

### 3. Create Your First Form

```kotlin
// UserForm.kt
class UserForm : Form(title = "User Management", locale = Locale.UK) {
    
    val mainPage = page("User Information")
    val userBlock = mainPage.insertBlock(UserBlock())
    
    inner class UserBlock : Block("Users", buffer = 1, visible = 10) {
        val u = table(Users, Users.id)
        
        val id = visit(domain = LONG(10), position = at(1, 1)) {
            label = "ID"
            help = "User ID"
            columns(u.id) {
                onInsertSkipped()
                onUpdateSkipped()
            }
        }
        
        val name = mustFill(domain = STRING(50), position = at(1, 2)) {
            label = "Name"
            help = "User name"
            columns(u.name)
        }
        
        val email = visit(domain = STRING(100), position = at(2, 1..2)) {
            label = "Email"
            help = "Email address"
            columns(u.email)
        }
        
        val active = visit(domain = BOOL, position = at(3, 1)) {
            label = "Active"
            help = "User status"
            columns(u.active)
        }
        
        init {
            // Add standard commands
            command(item = save) { saveBlock() }
            command(item = insertMode) { insertMode() }
        }
    }
}
```

### 4. Create Application Entry Point

```kotlin
// MyApplication.kt
@Route("")
class MyApplication : VApplication(Registry("MYAPP", null)) {
    
    override val logoImage get() = "logo.png"
    override val supportedLocales get() = arrayOf(Locale.UK)
    
    override fun login(database: String, driver: String, username: String,
                      password: String, schema: String?, maxRetries: Int?,
                      waitMin: Long?, waitMax: Long?): Connection? {
        return Connection.createConnection(
            url = database,
            driver = driver, 
            userName = username,
            password = password
        )
    }
    
    init {
        ApplicationConfiguration.setConfiguration(MyConfigurationManager)
    }
}

object MyConfigurationManager : ApplicationConfiguration() {
    override val applicationName get() = "My App"
    override val version get() = "1.0.0"
    // ... other configuration
}
```

## API Reference

### Core Classes

#### Form API
**Package**: `org.kopi.galite.visual.dsl.form`

```kotlin
abstract class Form(title: String, locale: Locale? = null) : Window {
    // Page management
    fun page(title: String): FormPage
    fun page(title: String, init: FormPage.() -> Unit): FormPage
    
    // Block management  
    fun <T : Block> insertBlock(block: T, init: (T.() -> Unit)? = null): T
    
    // Trigger support
    fun <T> trigger(vararg events: FormTriggerEvent<T>, method: () -> T): Trigger
    
    // Navigation
    fun gotoBlock(target: Block)
    fun showChart(chart: Chart)
    
    // Form operations
    fun resetForm()
    fun quitForm() 
    fun showHelp()
}
```

#### Block API
**Package**: `org.kopi.galite.visual.dsl.form`

```kotlin
open class Block(title: String, buffer: Int, visible: Int) {
    // Table linking
    fun <T : Table> table(table: T, idColumn: Column<Int>? = null, 
                         sequence: Sequence? = null): T
    
    // Field creation
    fun <T> mustFill(domain: Domain<T>, position: FormPosition, 
                    init: FormField<T>.() -> Unit): FormField<T>
    fun <T> visit(domain: Domain<T>, position: FormPosition,
                 init: FormField<T>.() -> Unit): FormField<T>  
    fun <T> skipped(domain: Domain<T>, position: FormPosition,
                   init: FormField<T>.() -> Unit): FormField<T>
    fun <T> hidden(domain: Domain<T>, init: FormField<T>.() -> Unit): FormField<T>
    
    // Position helpers
    fun at(line: Int, column: Int): FormPosition
    fun at(line: Int, columnRange: IntRange): FormPosition
    fun follow(field: FormField<*>): FormPosition
    
    // Block operations
    fun insertMode()
    fun saveBlock()
    fun resetBlock()
    fun load()
    fun gotoFirstRecord()
    fun gotoLastRecord()
}
```

#### Report API
**Package**: `org.kopi.galite.visual.dsl.report`

```kotlin
abstract class Report(title: String, help: String?, locale: Locale? = null) : Window {
    // Field creation
    inline fun <reified T : Comparable<T>?> field(domain: Domain<T>, 
                                                 init: ReportField<T>.() -> Unit): ReportField<T>
    
    // Data management
    fun add(init: ReportRow.() -> Unit)
    fun getRow(rowNumber: Int): MutableMap<ReportField<*>, Any?>
    
    // Export operations
    fun export(type: Int = VReport.TYP_CSV)
    fun export(file: File, type: Int = VReport.TYP_CSV)
    
    // Trigger support
    fun <T> trigger(vararg events: ReportTriggerEvent<T>, method: () -> T): Trigger
}
```

#### Chart API
**Package**: `org.kopi.galite.visual.dsl.chart`

```kotlin
abstract class Chart(title: String, help: String?, locale: Locale? = null) : Window {
    // Dimension and measure creation
    inline fun <reified T : Comparable<T>?> dimension(domain: Domain<T>,
                                                     init: ChartDimension<T>.() -> Unit): ChartDimension<T>
    
    inline fun <reified T> measure(domain: Domain<T>,
                                  init: ChartMeasure<T>.() -> Unit): ChartMeasure<T> 
                                  where T : Comparable<T>?, T : Number?
    
    // Chart type management
    var chartType: VChartType
    
    // Trigger support
    fun <T> trigger(vararg events: ChartTriggerEvent<T>, method: () -> T): Trigger
}
```

### Trigger Events

#### Form Triggers
```kotlin
val INIT        // Form initialization
val PREFORM     // Before form display  
val POSTFORM    // After form close
val RESET       // On form reset
val CHANGED     // Form change detection
val QUITFORM    // Before form quit
```

#### Block Triggers
```kotlin
val PREBLK      // Before block entry
val POSTBLK     // After block exit
val PREREC      // Before record entry
val POSTREC     // After record exit
val PREQRY      // Before database query
val POSTQRY     // After database query
val PRESAVE     // Before record save
val PREINS      // Before record insert
val POSTINS     // After record insert
val PREUPD      // Before record update
val POSTUPD     // After record update
val PREDEL      // Before record delete
val POSTDEL     // After record delete
```

#### Field Triggers
```kotlin
val PREFLD      // Before field entry
val POSTFLD     // After field exit
val POSTCHG     // After field change
val VALFLD      // Field validation
val FORMAT      // Field formatting
val DEFAULT     // Default value
```

### Constants and Enums

#### Chart Types
```kotlin
enum class VChartType {
    BAR, COLUMN, LINE, AREA, PIE, DEFAULT
}
```

#### Report Export Types
```kotlin
object VReport {
    const val TYP_CSV = 1
    const val TYP_PDF = 2  
    const val TYP_XLS = 3
    const val TYP_XLSX = 4
}
```

#### Field Access Modes
```kotlin
object VConstants {
    const val ACS_MUSTFILL = 1    // Required field
    const val ACS_VISIT = 2       // Editable field
    const val ACS_SKIPPED = 3     // Read-only field
    const val ACS_HIDDEN = 4      // Hidden field
}
```

## Testing Framework

**Package**: `org.kopi.galite.testing`

Galite provides comprehensive testing utilities for UI components:

```kotlin
// Testing forms
class UserFormTest {
    @Test
    fun testFormCreation() {
        val form = UserForm()
        
        // Test form properties
        assertEquals("User Management", form.title)
        assertEquals(1, form.blocks.size)
        
        // Test block navigation
        form.userBlock.gotoFirstRecord()
        form.userBlock.insertMode()
        
        // Test field values
        form.userBlock.name.value = "John Doe"
        form.userBlock.email.value = "john@example.com"
        
        assertEquals("John Doe", form.userBlock.name.value)
    }
}

// Testing reports
class ReportTest {
    @Test
    fun testReportGeneration() {
        val report = ProductReport()
        
        // Verify report structure
        assertTrue(report.fields.isNotEmpty())
        
        // Test export functionality
        report.export(VReport.TYP_CSV)
    }
}
```

## Advanced Features

### 1. Custom Domains

Create reusable domain types:

```kotlin
class EmailDomain : STRING(100) {
    init {
        constraint {
            it.contains("@") && it.contains(".")
        }
    }
}

// Usage
val email = visit(domain = EmailDomain(), position = at(1, 1)) {
    label = "Email"
    help = "Valid email address"
}
```

### 2. Field Validation

```kotlin
val age = visit(domain = INT(3), position = at(1, 1)) {
    label = "Age"
    help = "Person's age"
    
    trigger(VALFLD) {
        if (value != null && (value < 0 || value > 150)) {
            throw VFieldException("Age must be between 0 and 150")
        }
    }
}
```

### 3. Dynamic Field Behavior

```kotlin
val country = visit(domain = STRING(50), position = at(1, 1)) {
    label = "Country"
    
    trigger(POSTCHG) {
        // Update state/province list based on country
        when (value) {
            "USA" -> stateField.setList(usStates)
            "Canada" -> stateField.setList(canadianProvinces)
            else -> stateField.clearList()
        }
    }
}
```

### 4. Complex Queries

```kotlin
// In block initialization
init {
    trigger(PREQRY) {
        // Build dynamic WHERE clause
        val conditions = mutableListOf<Op<Boolean>>()
        
        if (searchName.value.isNotEmpty()) {
            conditions.add(Client.name like "%${searchName.value}%")
        }
        
        if (searchActive.value != null) {
            conditions.add(Client.active eq searchActive.value)
        }
        
        // Apply conditions to query
        if (conditions.isNotEmpty()) {
            block.setSearchConditions(conditions.reduce { acc, op -> acc and op })
        }
    }
}
```

## Error Handling

### Exception Types

```kotlin
// Field validation errors
class VFieldException(message: String) : VException(message)

// Database operation errors  
class VExecFailedException(message: String) : VException(message)

// General framework errors
open class VException(message: String) : Exception(message)
```

### Error Handling Patterns

```kotlin
// In triggers
trigger(PRESAVE) {
    try {
        validateBusinessRules()
    } catch (e: BusinessRuleException) {
        throw VExecFailedException("Validation failed: ${e.message}")
    }
}

// In commands
command(item = saveActor) {
    try {
        saveBlock()
        model.notice("Record saved successfully")
    } catch (e: VException) {
        model.error("Save failed: ${e.message}")
    }
}
```

## Performance Considerations

### 1. Database Optimization

```kotlin
// Use proper indexing
val clientIndex = index("Unique client email") {
    Client.email
}

// Optimize queries with proper joins
val clientProducts = table(Client)
val products = table(Product)

// Use exposed query optimization
trigger(PREQRY) {
    Client.innerJoin(Product)
        .select { Client.active eq true }
        .limit(100)
}
```

### 2. Memory Management

```kotlin
// Limit buffer sizes for large datasets
class LargeDataBlock : Block("Data", buffer = 50, visible = 20) {
    // Implementation
}

// Use pagination for reports
class LargeReport : Report("Large Report") {
    init {
        // Load data in chunks
        loadDataInChunks(pageSize = 1000)
    }
}
```

## Migration Guide

### From Version 1.2.x to 1.3.0

1. **Updated Dependencies**: Update Kotlin version to 1.9.0+
2. **API Changes**: Some trigger signatures have changed
3. **New Features**: Pivot table support, enhanced chart types

```kotlin
// Old way (1.2.x)
trigger(POSTCHG) { field ->
    // field parameter was required
}

// New way (1.3.0)  
trigger(POSTCHG) {
    // field accessible via 'this' context
}
```

## Best Practices

### 1. Code Organization

```kotlin
// Separate concerns
class ClientModule {
    class ClientForm : Form(...)
    class ClientReport : Report(...)  
    class ClientChart : Chart(...)
    
    // Shared domains
    object ClientDomains {
        val ClientID = LONG(10)
        val ClientName = STRING(100)
    }
}
```

### 2. Reusable Components

```kotlin
// Base form class
abstract class BaseForm(title: String) : Form(title, Locale.UK) {
    init {
        insertMenus()
        insertCommands()
    }
    
    // Common functionality
    protected fun showSuccessMessage(message: String) {
        model.notice(message)
    }
}

// Extend base form
class ProductForm : BaseForm("Products") {
    // Product-specific implementation
}
```

### 3. Testing Strategy

```kotlin
// Integration tests
@Test
fun testCompleteWorkflow() {
    val form = ClientForm()
    
    // Test data entry
    form.clientBlock.insertMode()
    form.clientBlock.name.value = "Test Client"
    form.clientBlock.saveBlock()
    
    // Verify database state
    transaction {
        val client = Client.select { Client.name eq "Test Client" }.single()
        assertEquals("Test Client", client[Client.name])
    }
}
```

## Troubleshooting

### Common Issues

1. **Database Connection Problems**
   - Verify database URL and credentials
   - Check driver classpath
   - Validate schema permissions

2. **Field Validation Errors**
   - Check domain constraints
   - Verify trigger implementations
   - Review field access permissions

3. **Performance Issues**
   - Optimize database queries
   - Reduce buffer sizes
   - Use appropriate indexing

### Debugging Tips

```kotlin
// Enable debug logging
init {
    if (ApplicationConfiguration.isDebugModeEnabled) {
        println("Form initialized: ${this::class.simpleName}")
    }
}

// Add validation logging
trigger(VALFLD) {
    logger.debug("Validating field ${label} with value: $value")
    // validation logic
}
```

## Contributing

Galite is an open-source project. Contributions are welcome!

- **Repository**: https://github.com/kopiLeft/Galite
- **License**: LGPL v2.1
- **Issues**: Report bugs and feature requests on GitHub

## Resources

- **Official Documentation**: https://kopileft.github.io/Galite/
- **Maven Repository**: https://mvnrepository.com/artifact/org.kopi
- **Examples**: See `galite-demo` module for complete examples
- **API Reference**: Generated KDoc available in releases

---

*This documentation covers Galite Framework version 1.3.0. For the latest updates and examples, visit the official repository.*