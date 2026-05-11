package com.periplus.sdet.base;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.periplus.sdet.config.ConfigManager;
import com.periplus.sdet.driver.DriverFactory;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * {@code BaseTest} is the root of the test class hierarchy.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Initialise and tear down the {@link WebDriver} before/after each test method.</li>
 *   <li>Initialise and flush the {@link ExtentReports} HTML report before/after the suite.</li>
 *   <li>Automatically capture and embed a screenshot in the report on test failure.</li>
 *   <li>Provide a {@code config} reference so that all subclasses can access
 *       test configuration without boilerplate.</li>
 * </ul>
 * </p>
 *
 * <p>All concrete test classes must extend this class and avoid declaring their
 * own {@code @BeforeMethod} / {@code @AfterMethod} driver lifecycle hooks.</p>
 */
public abstract class BaseTest {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final ConfigManager config = ConfigManager.getInstance();

    // ── ExtentReports (class-level, shared across threads via ThreadLocal test) ──
    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> extentTest = new ThreadLocal<>();

    // ── Suite lifecycle ────────────────────────────────────────────────────

    /**
     * Initialises the ExtentReports HTML reporter once before the entire test suite.
     * Report is saved to {@code target/reports/index.html}.
     */
    @BeforeSuite(alwaysRun = true)
    public synchronized void setUpSuite() {
        try {
            Files.createDirectories(Paths.get("target/reports"));
        } catch (IOException e) {
            System.err.println("Failed to create reports directory: " + e.getMessage());
        }

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String reportPath = "target/reports/test-report-" + timestamp + ".html";

        ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);
        spark.config().setDocumentTitle("Periplus Cart Test Report");
        spark.config().setReportName("Shopping Cart Regression Suite");

        extent = new ExtentReports();
        extent.attachReporter(spark);
        extent.setSystemInfo("Framework",    "Selenium 4 + TestNG + POM");
        extent.setSystemInfo("Browser",      config.getBrowser());
        extent.setSystemInfo("BaseURL",      config.getBaseUrl());
        extent.setSystemInfo("Environment",  "Staging/Production");
        extent.setSystemInfo("Tester",       "SDET Automation Framework");

        log.info("ExtentReports initialised. Report will be saved to: {}", reportPath);
    }

    /**
     * Flushes all pending report data to disk after all tests have completed.
     */
    @AfterSuite(alwaysRun = true)
    public synchronized void tearDownSuite() {
        if (extent != null) {
            extent.flush();
            log.info("ExtentReports flushed and saved.");
        }
    }

    // ── Test lifecycle ─────────────────────────────────────────────────────

    /**
     * Initialises a fresh {@link WebDriver} and creates an ExtentTest node
     * before each test method.
     *
     * @param method the TestNG method object — used to extract the test name
     */
    @BeforeMethod(alwaysRun = true)
    public void setUp(Method method) {
        DriverFactory.initDriver();

        // Create a test node in the report
        ExtentTest test = extent.createTest(method.getName());
        extentTest.set(test);

        log.info("═══════════════════════════════════════════════════");
        log.info("  Starting test: {}", method.getName());
        log.info("═══════════════════════════════════════════════════");
    }

    /**
     * Quits the {@link WebDriver} and logs pass/fail status after each test method.
     *
     * <p>On failure, a screenshot is automatically captured and embedded in the
     * ExtentReports HTML report for diagnosis.</p>
     *
     * @param result the TestNG result object carrying pass/fail/skip information
     */
    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        try {
            ExtentTest test = extentTest.get();
            if (result.getStatus() == ITestResult.FAILURE) {
                log.error("TEST FAILED: {} — {}", result.getName(),
                        result.getThrowable().getMessage());
                if (test != null) {
                    test.fail(result.getThrowable());
                    String screenshotBase64 = captureScreenshotAsBase64();
                    if (!screenshotBase64.isEmpty()) {
                        test.addScreenCaptureFromBase64String(screenshotBase64,
                                "Failure Screenshot");
                    }
                }
            } else if (result.getStatus() == ITestResult.SUCCESS) {
                log.info("TEST PASSED: {}", result.getName());
                if (test != null) test.pass("Test passed.");
            } else {
                log.warn("TEST SKIPPED: {}", result.getName());
                if (test != null) test.skip("Test skipped.");
            }
        } finally {
            DriverFactory.quitDriver();
            log.info("═══════════════════════════════════════════════════");
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /**
     * Returns the currently active ExtentTest node for this thread.
     * Subclasses may use this to add custom log steps.
     *
     * @return the thread-local {@link ExtentTest} node
     */
    protected ExtentTest getTest() {
        return extentTest.get();
    }

    /**
     * Captures the current browser viewport as a Base64-encoded PNG string.
     *
     * @return Base64 PNG string, or empty string if capture fails
     */
    private String captureScreenshotAsBase64() {
        try {
            WebDriver driver = DriverFactory.getDriver();
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
        } catch (Exception e) {
            log.warn("Could not capture screenshot: {}", e.getMessage());
            return "";
        }
    }
}
