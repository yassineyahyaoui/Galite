# Galite Framework - User Documentation

## Table of Contents
1. [Framework Overview](#framework-overview)
2. [Getting Started](#getting-started)
3. [Core Components](#core-components)
4. [DSL Reference](#dsl-reference)
5. [API Reference](#api-reference)
6. [Quick Start Guide](#quick-start-guide)
7. [Testing Utilities](#testing-utilities)
8. [Advanced Topics](#advanced-topics)

## Framework Overview

**Galite** is a Kotlin-based framework for building business applications with an expressive, elegant DSL syntax. It provides comprehensive tools for creating forms, reports, charts, and pivot tables with database integration through Jetbrains Exposed.

### Key Features
- **Kotlin DSL**: Type-safe builders for application components
- **Database Integration**: Built-in support for database operations with Exposed
- **Vaadin-based UI**: Modern web-based user interface
- **Strongly Typed Fields**: Compile-time type checking for all data fields
- **Multi-format Export**: Support for PDF, CSV, Excel exports
- **Internationalization**: Built-in localization support

### Gradle Dependency Information

Add the following to your `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("org.kopi:galite-core:1.3.0")
    implementation("org.kopi:galite-data:1.3.0")
    implementation("org.kopi:galite-util:1.3.0")
    
    // Optional: For testing
    testImplementation("org.kopi:galite-testing:1.3.0")
}
```

## Getting Started

### Basic Application Setup

To create a Galite application, extend `VApplication` and implement required configuration:

```kotlin
@Route("")
class MyApp : VApplication(Registry(domain = "GALITE", parent = null)) {

    override val sologanImage get() = "my-slogan.png"
    override val logoImage get() = "my-logo.png"
    override val logoHref get() = "https://mycompany.com"
    override val alternateLocale get() = Locale.UK
    override val supportedLocales get() = arrayOf(Locale.UK, Locale.FRANCE)

    companion object {
        init {
            ApplicationConfiguration.setConfiguration(MyConfigurationManager)
        }
    }

    object MyConfigurationManager : ApplicationConfiguration() {
        override val version: String = "1.0.0"
        override val applicationName: String = "My Business App"
        override val informationText: String = "Business Application"
        override val logFile: String = "app.log"
        override val debugMailRecipient: String = "admin@mycompany.com"
        override fun getSMTPServer(): String = "smtp.mycompany.com"
        override val faxServer: String = "fax.mycompany.com"
        override fun mailErrors(): Boolean = false
        override fun logErrors(): Boolean = true
        override fun debugMessageInTransaction(): Boolean = true
    }
}
```

## Core Components

### 1. Application Configuration

The `ApplicationConfiguration` abstract class defines application-wide settings:

```kotlin
abstract class ApplicationConfiguration {
    abstract val version: String
    abstract val applicationName: String
    abstract val informationText: String
    abstract val logFile: String
    abstract val debugMailRecipient: String
    abstract fun getSMTPServer(): String
    abstract val faxServer: String
    abstract fun mailErrors(): Boolean
    abstract fun logErrors(): Boolean
    abstract fun debugMessageInTransaction(): Boolean
}
```

### 2. Domain Types

Galite provides strongly-typed domain classes for different data types:

#### Numeric Domains
```kotlin
// Integer types
val idField = LONG(10) // Long integer with width 10
val countField = INT(5) // Integer with width 5

// Decimal types  
val priceField = DECIMAL(10, 2) // Decimal with width 10, scale 2
val fractionField = FRACTION(8) // Fraction with width 8
```

#### Text Domains
```kotlin
// String types
val nameField = STRING(50) // Simple string, width 50
val descriptionField = STRING(100, 5, Fixed.ON) // Multi-line string
val styledField = STRING(200, styled = true) // Rich text support

// Text areas
val notesField = TEXT(80, 10) // Text area, 80 chars wide, 10 lines high
```

#### Date and Time Domains
```kotlin
val dateField = DATE // LocalDate
val timeField = TIME // LocalTime  
val timestampField = TIMESTAMP // Instant
val datetimeField = DATETIME // LocalDateTime
val monthField = MONTH // Month type
val weekField = WEEK // Week type
```

#### Special Domains
```kotlin
val booleanField = BOOL // Boolean
val colorField = COLOR // Color picker
val imageField = IMAGE(200, 150) // Image with dimensions
```

### 3. Forms

Forms are the primary UI component for data entry and display:

```kotlin
class CustomerForm : Form(title = "Customer Management", locale = Locale.UK) {
    
    // Define menus
    val fileMenu = menu("File")
    val editMenu = menu("Edit")
    
    // Define actors (menu items/toolbar buttons)
    val saveActor = actor(
        menu = fileMenu,
        label = "Save", 
        help = "Save customer data"
    ) {
        key = Key.F2
        icon = Icon.SAVE
    }
    
    // Define commands
    val saveCommand = command(item = saveActor) {
        // Save logic here
        model.save()
    }
    
    // Define pages
    val mainPage = page("Customer Details")
    
    // Define blocks
    val customerBlock = mainPage.insertBlock(CustomerBlock())
}

class CustomerBlock : Block("Customer", buffer = 1, visible = 1) {
    
    // Link to database table
    val table = table(Customers, idColumn = Customers.id, sequence = Sequence("CUSTOMER_ID_SEQ"))
    
    // Define fields
    val customerId = visit(domain = LONG(10), position = at(1, 1)) {
        label = "Customer ID"
        help = "Unique customer identifier"
        columns(table.id)
    }
    
    val customerName = visit(domain = STRING(50), position = at(2, 1)) {
        label = "Name"
        help = "Customer full name"
        columns(table.name)
    }
    
    val email = visit(domain = STRING(100), position = at(3, 1)) {
        label = "Email"
        help = "Customer email address"
        columns(table.email)
    }
    
    val isActive = visit(domain = BOOL, position = at(4, 1)) {
        label = "Active"
        help = "Is customer account active?"
        columns(table.active)
    }
}
```

### 4. Reports

Reports display tabular data with grouping and export capabilities:

```kotlin
class SalesReport : Report(title = "Sales Summary", locale = Locale.UK) {
    
    val exportMenu = menu("Export")
    
    val csvExport = actor(
        menu = exportMenu,
        label = "CSV",
        help = "Export to CSV"
    ) {
        key = Key.F8
        icon = Icon.EXPORT_CSV
    }
    
    val csvCommand = command(item = csvExport) {
        model.export(VReport.TYP_CSV)
    }
    
    // Define report fields
    val productName = field(domain = STRING(50)) {
        label = "Product"
        help = "Product name"
        group = category
    }
    
    val category = field(domain = STRING(30)) {
        label = "Category" 
        help = "Product category"
        group = totalSales
    }
    
    val totalSales = field(domain = DECIMAL(12, 2)) {
        label = "Total Sales"
        help = "Total sales amount"
        compute { 
            // Computation logic
            quantity * unitPrice
        }
    }
    
    init {
        // Load data
        transaction {
            Sales.selectAll().forEach { row ->
                add {
                    this[productName] = row[Sales.productName]
                    this[category] = row[Sales.category]
                    this[totalSales] = row[Sales.amount]
                }
            }
        }
    }
}
```

### 5. Charts

Charts provide data visualization capabilities:

```kotlin
class SalesChart : Chart(
    title = "Sales by Region",
    help = "Regional sales performance chart",
    locale = Locale.UK
) {
    
    val exportMenu = menu("Export")
    
    // Define dimension (x-axis)
    val region = dimension(STRING(20)) {
        label = "Region"
        format { value ->
            value?.uppercase()
        }
    }
    
    // Define measures (y-axis)
    val sales = measure(DECIMAL(10, 2)) {
        label = "Sales Amount"
        color {
            VColor.BLUE
        }
    }
    
    val orders = measure(LONG(8)) {
        label = "Order Count"
        color {
            VColor.GREEN  
        }
    }
    
    // Set chart type
    val chartType = trigger(CHARTTYPE) {
        VChartType.BAR
    }
    
    init {
        // Add data
        region.add("North") {
            this[sales] = BigDecimal("125000.50")
            this[orders] = 450L
        }
        
        region.add("South") {
            this[sales] = BigDecimal("98000.25") 
            this[orders] = 320L
        }
    }
}
```

### 6. Pivot Tables

Pivot tables allow dynamic data analysis:

```kotlin
class SalesPivotTable : PivotTable(title = "Sales Analysis", locale = Locale.UK) {
    
    val actionMenu = menu("Action")
    
    // Define dimensions
    val product = dimension(STRING(50), Position.ROW) {
        label = "Product"
        help = "Product name"
    }
    
    val region = dimension(STRING(20), Position.COLUMN) {
        label = "Region"
        help = "Sales region"
    }
    
    val quarter = dimension(STRING(10), Position.COLUMN) {
        label = "Quarter"
        help = "Sales quarter"
    }
    
    // Define measures
    val revenue = measure(DECIMAL(12, 2)) {
        label = "Revenue"
        help = "Total revenue"
    }
    
    init {
        // Load data
        transaction {
            SalesData.selectAll().forEach { row ->
                add {
                    this[product] = row[SalesData.productName]
                    this[region] = row[SalesData.region]
                    this[quarter] = row[SalesData.quarter]
                    this[revenue] = row[SalesData.revenue]
                }
            }
        }
    }
}
```

## DSL Reference

### Form DSL

#### Block Definition
```kotlin
// Simple block
val block = block("Block Title", buffer = 10, visible = 5) {
    // Block configuration
}

// Block with database table
val block = insertBlock(MyBlock())

class MyBlock : Block("Title", buffer = 1, visible = 1) {
    val table = table(MyTable, idColumn = MyTable.id)
    
    // Field definitions
    val field = visit(domain = STRING(50), position = at(1, 1)) {
        label = "Field Label"
        help = "Field help text"
        columns(table.columnName)
    }
}
```

#### Field Positioning
```kotlin
// Position field at row 1, column 1
position = at(1, 1)

// Position field spanning multiple columns
position = at(1, 1..3)

// Position field with specific alignment
position = at(2, 1) {
    align = FieldAlignment.LEFT
}
```

#### Field Options
```kotlin
val field = visit(domain = STRING(50), position = at(1, 1)) {
    label = "Field Label"
    help = "Help text"
    
    // Field options
    options(FieldOption.NOECHO) // Password field
    options(FieldOption.HIDDEN) // Hidden field
    options(FieldOption.MANDATORY) // Required field
    
    // Field triggers
    trigger(PREFLD) {
        // Pre-field logic
    }
    
    trigger(POSTFLD) {
        // Post-field logic  
    }
    
    // Value formatting
    format { value ->
        value?.uppercase()
    }
}
```

### Report DSL

#### Field Definition
```kotlin
val field = field(domain = STRING(50)) {
    label = "Column Header"
    help = "Column description"
    
    // Grouping
    group = parentField
    
    // Formatting
    format { value ->
        value?.let { "Formatted: $it" }
    }
    
    // Computation
    compute {
        // Calculate field value
        otherField1 + otherField2
    }
}
```

### Chart DSL

#### Dimension and Measures
```kotlin
// Chart dimension (categories)
val dimension = dimension(STRING(20)) {
    label = "Category"
    format { value -> value?.uppercase() }
}

// Chart measure (values)
val measure = measure(DECIMAL(10, 2)) {
    label = "Amount"
    color { VColor.BLUE }
}
```

### Common DSL Elements

#### Menu and Actor Definition
```kotlin
// Define menu
val menu = menu("Menu Label")

// Define actor
val actor = actor(
    menu = menu,
    label = "Action Label",
    help = "Action description"
) {
    key = Key.F1
    icon = Icon.SAVE
}

// Define command
val command = command(item = actor) {
    // Command logic
    performAction()
}
```

#### Triggers
```kotlin
// Form triggers
trigger(INIT) {
    // Initialization logic
}

trigger(PREQRY) {
    // Pre-query logic  
}

// Field triggers
trigger(PREFLD, POSTFLD) {
    // Field-level logic
}
```

## API Reference

### org.kopi.galite.visual.dsl.form

#### Form
```kotlin
abstract class Form(title: String, locale: Locale? = null) : Window(title, locale)

// Methods
fun block(title: String, buffer: Int, visible: Int, init: Block.() -> Unit): Block
fun <T : Block> insertBlock(block: T, init: (T.() -> Unit)? = null): T  
fun page(title: String): FormPage
```

#### Block
```kotlin
open class Block(val title: String, var buffer: Int, var visible: Int)

// Methods  
fun table(table: Table, idColumn: Column<Int>? = null, sequence: Sequence? = null): FormBlockTable
fun <T> visit(domain: Domain<T>, position: FormPosition, init: FormField<T>.() -> Unit): FormField<T>
fun trigger(vararg events: FormTriggerEvent<*>, method: () -> Unit): Trigger
```

#### FormField
```kotlin
class FormField<T>(val domain: Domain<T>)

// Properties
var label: String
var help: String
var options: FieldOption

// Methods
fun columns(vararg columns: Column<*>)
fun trigger(vararg events: FieldTriggerEvent<*>, method: () -> Unit): Trigger
fun format(formatter: (T?) -> String): Unit
```

### org.kopi.galite.visual.dsl.report

#### Report
```kotlin
abstract class Report(title: String, help: String?, locale: Locale? = null) : Window(title, locale)

// Methods
fun <T : Comparable<T>?> field(domain: Domain<T>, init: ReportField<T>.() -> Unit): ReportField<T>
fun add(init: ReportRow.() -> Unit): Unit
```

#### ReportField  
```kotlin
class ReportField<T>(val domain: Domain<T>)

// Properties
var label: String
var help: String
var group: ReportField<*>?

// Methods
fun format(formatter: (T?) -> String?): Unit
fun compute(computation: () -> T): Unit
```

### org.kopi.galite.visual.dsl.chart

#### Chart
```kotlin
abstract class Chart(title: String, help: String?, locale: Locale? = null) : Window(title, locale)

// Methods
fun <T : Comparable<T>?> dimension(domain: Domain<T>, init: ChartDimension<T>.() -> Unit): ChartDimension<T>
fun <T : Comparable<T>?> measure(domain: Domain<T>, init: ChartMeasure<T>.() -> Unit): ChartMeasure<T>
```

#### ChartDimension
```kotlin
class ChartDimension<T>(val domain: Domain<T>)

// Methods
fun add(value: T, init: DimensionData.() -> Unit): Unit
fun format(formatter: (T?) -> String?): Unit
```

#### ChartMeasure
```kotlin
class ChartMeasure<T>(val domain: Domain<T>)

// Properties  
var label: String
var help: String

// Methods
fun color(colorProvider: () -> VColor): Unit
```

### org.kopi.galite.visual.dsl.pivottable

#### PivotTable
```kotlin
abstract class PivotTable(title: String, help: String?, locale: Locale? = null) : Window(title, locale)

// Methods
fun <T : Comparable<T>?> dimension(domain: Domain<T>, position: Position, init: Dimension<T>.() -> Unit): Dimension<T>
fun <T : Comparable<T>?> measure(domain: Domain<T>, init: Measure<T>.() -> Unit): Measure<T>
fun add(init: PivotTableRow.() -> Unit): Unit
```

### org.kopi.galite.visual.domain

#### Domain Types
```kotlin
// Numeric domains
class LONG(width: Int, init: Domain<Long>.() -> Unit = {})
class INT(width: Int, init: Domain<Int>.() -> Unit = {})  
class DECIMAL(width: Int, scale: Int, init: Domain<BigDecimal>.() -> Unit = {})
class FRACTION(width: Int, init: Domain<BigDecimal>.() -> Unit = {})

// Text domains
class STRING(width: Int, height: Int = 1, visibleHeight: Int = 0, fixed: Fixed = Fixed.UNDEFINED, convert: Convert = Convert.NONE, styled: Boolean = false)
class TEXT(width: Int, height: Int, visibleHeight: Int = 0, fixed: Fixed = Fixed.UNDEFINED, styled: Boolean = false)

// Date/time domains
object DATE : Domain<LocalDate>()
object TIME : Domain<LocalTime>()
object TIMESTAMP : Domain<Instant>()
object DATETIME : Domain<LocalDateTime>()
object MONTH : Domain<Month>()
object WEEK : Domain<Week>()

// Special domains
object BOOL : Domain<Boolean>()
object COLOR : Domain<Color>()
class IMAGE(width: Int, height: Int) : Domain<Image>(width, height)
```

### org.kopi.galite.visual.dsl.common

#### Window
```kotlin
abstract class Window(val title: String, val locale: Locale?)

// Methods
fun menu(label: String): Menu
fun actor(menu: Menu, label: String, help: String, init: Actor.() -> Unit = {}): Actor
fun command(item: Actor, init: () -> Unit): Command
fun <T> trigger(vararg events: TriggerEvent<T>, method: () -> T): Trigger
```

#### Actor
```kotlin
open class Actor(val menu: Menu, val label: String, help: String)

// Properties
var key: Key?
var icon: Icon?
```

#### Command
```kotlin
class Command(val actor: Actor, val action: () -> Unit)
```

## Quick Start Guide

### 1. Project Setup

Create a new Kotlin project and add Galite dependencies:

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "1.9.0"
    id("org.springframework.boot") version "2.7.0"
}

dependencies {
    implementation("org.kopi:galite-core:1.3.0")
    implementation("org.kopi:galite-data:1.3.0")
    implementation("org.jetbrains.exposed:exposed-core:0.40.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.40.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.40.1")
    implementation("com.h2database:h2:2.1.214")
}
```

### 2. Database Setup

Define your database schema using Exposed:

```kotlin
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

object Customers : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100)
    val email = varchar("email", 100)
    val phone = varchar("phone", 20).nullable()
    val registrationDate = date("registration_date")
    val isActive = bool("is_active").default(true)
    
    override val primaryKey = PrimaryKey(id)
}
```

### 3. Create Application Class

```kotlin
@Route("")
class CustomerApp : VApplication(Registry(domain = "CUSTOMER_APP", parent = null)) {
    
    override val sologanImage get() = "logo.png"
    override val logoImage get() = "company-logo.png"
    override val supportedLocales get() = arrayOf(Locale.UK, Locale.US)
    
    companion object {
        init {
            ApplicationConfiguration.setConfiguration(AppConfig)
        }
    }
    
    object AppConfig : ApplicationConfiguration() {
        override val version = "1.0.0"
        override val applicationName = "Customer Management"
        override val informationText = "Customer Management System"
        override val logFile = "app.log"
        override val debugMailRecipient = "admin@company.com"
        override fun getSMTPServer() = "localhost"
        override val faxServer = "localhost"
        override fun mailErrors() = false
        override fun logErrors() = true
        override fun debugMessageInTransaction() = false
    }
}
```

### 4. Create a Simple Form

```kotlin
class CustomerForm : Form("Customer Management") {
    
    val fileMenu = menu("File")
    val saveActor = actor(fileMenu, "Save", "Save customer") {
        key = Key.F2
        icon = Icon.SAVE
    }
    
    val saveCommand = command(saveActor) {
        model.save()
        model.notice("Customer saved successfully!")
    }
    
    val mainPage = page("Customer Details")
    val customerBlock = mainPage.insertBlock(CustomerBlock())
}

class CustomerBlock : Block("Customer", 1, 1) {
    
    val u = table(Customers, idColumn = Customers.id)
    
    val id = visit(LONG(10), at(1, 1)) {
        label = "ID"
        help = "Customer ID"
        columns(u.id)
    }
    
    val name = visit(STRING(100), at(2, 1)) {
        label = "Name"
        help = "Customer name"
        columns(u.name)
        options(FieldOption.MANDATORY)
    }
    
    val email = visit(STRING(100), at(3, 1)) {
        label = "Email"
        help = "Email address"
        columns(u.email)
    }
    
    val phone = visit(STRING(20), at(4, 1)) {
        label = "Phone"
        help = "Phone number"
        columns(u.phone)
    }
    
    val isActive = visit(BOOL, at(5, 1)) {
        label = "Active"
        help = "Is customer active?"
        columns(u.isActive)
    }
}
```

### 5. Run the Application

```kotlin
fun main() {
    runApplication<CustomerApp>()
}
```

### 6. Minimal Working Example

Here's a complete minimal example:

```kotlin
// Main.kt
import org.springframework.boot.runApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class Application

fun main() {
    runApplication<Application>()
}

// App.kt  
import com.vaadin.flow.router.Route
import org.kopi.galite.visual.ui.vaadin.visual.VApplication
import org.kopi.galite.visual.Registry
import org.kopi.galite.visual.ApplicationConfiguration
import java.util.Locale

@Route("")
class SimpleApp : VApplication(Registry("SIMPLE", null)) {
    
    override val supportedLocales get() = arrayOf(Locale.UK)
    
    companion object {
        init {
            ApplicationConfiguration.setConfiguration(object : ApplicationConfiguration() {
                override val version = "1.0"
                override val applicationName = "Simple App"
                override val informationText = "Simple Galite App"
                override val logFile = "app.log"
                override val debugMailRecipient = "admin@localhost"
                override fun getSMTPServer() = "localhost"
                override val faxServer = "localhost"
                override fun mailErrors() = false
                override fun logErrors() = true
                override fun debugMessageInTransaction() = false
            })
        }
    }
}
```

## Testing Utilities

Galite provides testing utilities in the `galite-testing` module:

```kotlin
// Add to build.gradle.kts
testImplementation("org.kopi:galite-testing:1.3.0")
```

### Form Testing

```kotlin
import org.kopi.galite.testing.*

class CustomerFormTest {
    
    @Test
    fun testFormCreation() {
        val form = CustomerForm()
        
        // Test form initialization
        form.testInit()
        
        // Test field access
        val nameField = form.customerBlock.name
        nameField.testSetValue("John Doe")
        assertEquals("John Doe", nameField.testGetValue())
        
        // Test form validation
        form.testValidate()
    }
    
    @Test
    fun testFormSave() {
        val form = CustomerForm()
        form.testInit()
        
        // Set field values
        form.customerBlock.name.testSetValue("Jane Smith")
        form.customerBlock.email.testSetValue("jane@example.com")
        
        // Test save operation
        form.testSave()
    }
}
```

### Report Testing

```kotlin
class SalesReportTest {
    
    @Test
    fun testReportGeneration() {
        val report = SalesReport()
        
        // Test report initialization
        report.testInit()
        
        // Test data loading
        assertTrue(report.testGetRowCount() > 0)
        
        // Test export
        report.testExport(VReport.TYP_CSV)
    }
}
```

## Advanced Topics

### Custom Domain Types

Create custom domain types for specialized data:

```kotlin
class EmailDomain : Domain<String>(100) {
    init {
        // Email validation logic
        constraint { value ->
            value?.matches(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) == true
        }
    }
}

// Usage
val email = visit(EmailDomain(), at(1, 1)) {
    label = "Email"
    help = "Valid email address"
}
```

### Code Domains

Define lookup tables with code domains:

```kotlin
object StatusDomain : CodeDomain<String>() {
    init {
        "Active" keyOf "A"
        "Inactive" keyOf "I" 
        "Pending" keyOf "P"
    }
}

// Usage
val status = visit(StatusDomain, at(1, 1)) {
    label = "Status"
    help = "Customer status"
}
```

### List Domains

Create dropdown lists from database queries:

```kotlin
object CategoryDomain : ListDomain<Int>(50) {
    init {
        "categories" keyOf {
            Categories.selectAll().map { 
                it[Categories.id] to it[Categories.name] 
            }
        }
    }
}
```

### Complex Triggers

Implement complex business logic with triggers:

```kotlin
// Form-level trigger
trigger(PREQRY) {
    // Pre-query validation
    if (!hasPermission()) {
        throw VException("Access denied")
    }
}

// Field-level trigger with validation
val amountField = visit(DECIMAL(10, 2), at(1, 1)) {
    label = "Amount"
    
    trigger(POSTFLD) {
        val value = getValue() as? BigDecimal
        if (value != null && value < BigDecimal.ZERO) {
            throw VFieldException("Amount cannot be negative")
        }
    }
}
```

### Database Transactions

Handle database operations with proper transaction management:

```kotlin
// In form save logic
command(saveActor) {
    transaction {
        try {
            model.save()
            // Additional business logic
            updateAuditLog()
            model.commitWork()
            model.notice("Data saved successfully")
        } catch (e: SQLException) {
            model.abortWork()
            model.error("Save failed: ${e.message}")
        }
    }
}
```

### Internationalization

Support multiple languages:

```kotlin
// Create localization files
// messages_en.properties
// messages_fr.properties

// Use in application
class MultilingualForm : Form("form.title") {
    
    val nameField = visit(STRING(50), at(1, 1)) {
        label = "field.name.label"
        help = "field.name.help"
    }
}
```

This completes the comprehensive documentation for the Galite Framework, covering all major aspects from basic setup to advanced usage patterns.