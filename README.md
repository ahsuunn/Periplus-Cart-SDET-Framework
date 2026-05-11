# Periplus Cart SDET Framework

[![Java Version](https://img.shields.io/badge/Java-17-orange.svg?style=flat-square&logo=java)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Selenium Version](https://img.shields.io/badge/Selenium-4.43.0-green.svg?style=flat-square&logo=selenium)](https://www.selenium.dev/)
[![TestNG](https://img.shields.io/badge/TestNG-7.10.0-blue.svg?style=flat-square)](https://testng.org/)
[![Maven](https://img.shields.io/badge/Maven-3.8+-red.svg?style=flat-square&logo=apache-maven)](https://maven.apache.org/)

A production-grade **Selenium + TestNG** automation framework designed for comprehensive testing of the [Periplus.com](https://www.periplus.com/) shopping cart. This framework implements the **Page Object Model (POM)** pattern, providing a scalable, maintainable, and robust solution for e-commerce functional validation.

---

## Key Features

*   **Page Object Model (POM):** Clean separation between page-specific logic and test scripts.
*   **Thread-Safe Driver Management:** Parallel-ready `DriverFactory` using `ThreadLocal` for isolated browser instances.
*   **Rich Reporting:** Integrated with **ExtentReports** for beautiful HTML dashboards and detailed step-level logs.
*   **Centralised Configuration:** Environment-agnostic setup via `config.properties`, environment variables, or System properties.
*   **Multi-Suite Support:** Pre-configured `testng-smoke.xml` and `testng-regression.xml` for CI/CD integration.
*   **Robust Wait Strategy:** Utilises Explicit Waits and Fluent Waits to handle AJAX/DOM mutations gracefully.
*   **Cross-Browser Support:** Seamless execution on **Chrome**, **Firefox**, and **Edge** with headless mode support.

---

## Technology Stack

| Category | Tools / Libraries |
| :--- | :--- |
| **Language** | Java 17 |
| **Automation** | Selenium WebDriver 4.43.0 |
| **Test Engine** | TestNG 7.10.0 |
| **Build Tool** | Apache Maven |
| **Reporting** | AventStack ExtentReports 5.1.1 |
| **Binary Mgmt** | WebDriverManager (io.github.bonigarcia) |
| **Logging** | SLF4J + Logback |

---

## Project Structure

```text
Periplus-Cart-SDET-Framework
├── src
│   ├── main/java/com/periplus/sdet
│   │   ├── config     # ConfigManager (Singleton properties loader)
│   │   ├── driver     # DriverFactory (ThreadLocal driver lifecycle)
│   │   └── pages      # Page Objects (HomePage, CartPage, etc.)
│   └── test/java/com/periplus/sdet
│       ├── base       # BaseTest (Setup/Teardown, ExtentReport init)
│       └── tests      # Test Suites (AddToCartTest, etc.)
├── src/test/resources
│   ├── config.properties  # Global environment settings
│   └── logback-test.xml   # Logging configuration
├── testng.xml             # Main suite definition
├── testng-regression.xml  # Regression suite definition
├── testng-smoke.xml       # Smoke suite definition
└── pom.xml                # Project dependencies & build config
```

---

## Test Coverage

The framework currently focuses on the **Shopping Cart** critical path:

| ID | Test Case | Description |
| :--- | :--- | :--- |
| **TC-ATC-001** | Happy Path: Add Product | Search → Select → Add → Verify Cart Integrity |
| **TC-ATC-002** | Cart Item Increment | Validates badge and line-item count synchronization |
| **TC-ATC-003** | URL Accessibility | Ensures cart page routing is correct post-addition |
| **TC-ATC-004** | Data Integrity | Asserts PDP product title matches Cart line-item title |
| **TC-ATC-005** | Math Verification | Audits Subtotal calculation after Quantity updates |
| **TC-ATC-006** | State Persistence | Verifies cart contents survive browser refresh |
| **TC-ATC-007** | Teardown/Empty State | Validates badge reset and "Empty Cart" message |
| **TC-ATC-008** | Boundary Limits | Handles maximum quantity inputs (e.g., 9999) |
| **TC-ATC-009** | Inventory Constraints | Graceful handling of Out-of-Stock items |

---

## Setup & Installation

### 1. Prerequisites
*   **JDK 17** installed and configured in `JAVA_HOME`.
*   **Maven 3.8+** installed.
*   Browsers (Chrome/Firefox/Edge) installed on the local machine.

### 2. Configuration
The framework loads settings from `src/test/resources/config.properties`. You can also override these via Environment Variables or System Properties.

```properties
app.base.url=https://www.periplus.com/
user.email=your_email@example.com
user.password=your_password
browser=chrome
browser.headless=false
test.search.keyword=java programming
```

---

## Running Tests

### Execute Default Suite (Regression)
```bash
mvn test
```

### Execute Smoke Suite
```bash
mvn test -DsuiteXmlFile=testng-smoke.xml
```

### Execute with System Property Overrides
```bash
mvn test -Dbrowser=firefox -Dbrowser.headless=true
```

---

## Reports & Logs

After test execution, comprehensive reports are generated:

*   **Extent Report:** `target/reports/test-report-YYYYMMDD_HHMMSS.html`
    *   *Includes step-by-step logs, pass/fail status, and embedded failure screenshots.*
*   **Application Logs:** `test_run.log`
    *   *Detailed debug logs showing driver initialization, page transitions, and element interactions.*
*   **Test Results Summary:** `test_results.txt`
    *   *A flat file summary of the last test execution.*

---

## Framework Best Practices
*   **Explicit Waits Only:** Avoid `Thread.sleep()`. All waiting logic is encapsulated within Page Objects using `WebDriverWait`.
*   **Thread Isolation:** `DriverFactory` ensures that each thread has its own driver instance, preventing cross-test interference.
*   **Failure Analysis:** On any test failure, the framework automatically captures a Base64 screenshot and embeds it directly into the HTML report for instant debugging.