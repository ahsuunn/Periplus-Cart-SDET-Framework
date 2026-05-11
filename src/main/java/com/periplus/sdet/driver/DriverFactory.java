package com.periplus.sdet.driver;

import com.periplus.sdet.config.ConfigManager;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * {@code DriverFactory} manages the lifecycle of {@link WebDriver} instances.
 *
 * <p>A {@link ThreadLocal} store ensures that concurrent test threads each get
 * their own isolated driver instance — a prerequisite for parallel execution.</p>
 *
 * <p>Usage pattern:</p>
 * <pre>{@code
 *   // In @BeforeMethod:
 *   DriverFactory.initDriver();
 *
 *   // In any page object or test:
 *   WebDriver driver = DriverFactory.getDriver();
 *
 *   // In @AfterMethod:
 *   DriverFactory.quitDriver();
 * }</pre>
 */
public final class DriverFactory {

    private static final Logger log = LoggerFactory.getLogger(DriverFactory.class);
    private static final ThreadLocal<WebDriver> DRIVER_STORE = new ThreadLocal<>();

    private DriverFactory() { /* utility class — no instantiation */ }

    // ── Lifecycle ──────────────────────────────────────────────────────────

    /**
     * Initialises a fresh WebDriver for the current thread based on the
     * {@code browser} property in {@code config.properties}.
     */
    public static void initDriver() {
        ConfigManager cfg = ConfigManager.getInstance();
        String browser = cfg.getBrowser();
        boolean headless = cfg.isHeadless();

        log.info("Initialising '{}' driver (headless={}) for thread {}.",
                browser, headless, Thread.currentThread().getId());

        WebDriver driver;
        switch (browser) {
            case "chrome":
                driver = buildChromeDriver(headless);
                break;
            case "firefox":
                driver = buildFirefoxDriver(headless);
                break;
            case "edge":
                driver = buildEdgeDriver(headless);
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported browser '" + browser + "'. Use: chrome | firefox | edge");
        }

        // Global timeouts — explicit waits are handled in page objects
        driver.manage().timeouts().pageLoadTimeout(
                Duration.ofSeconds(cfg.getPageLoadTimeoutSeconds()));
        driver.manage().window().maximize();

        DRIVER_STORE.set(driver);
        log.debug("WebDriver stored for thread {}.", Thread.currentThread().getId());
    }

    /**
     * Returns the {@link WebDriver} bound to the current thread.
     *
     * @return the active driver
     * @throws IllegalStateException if {@link #initDriver()} was not called first
     */
    public static WebDriver getDriver() {
        WebDriver driver = DRIVER_STORE.get();
        if (driver == null) {
            throw new IllegalStateException(
                    "No WebDriver found for thread " + Thread.currentThread().getId()
                    + ". Call DriverFactory.initDriver() before calling getDriver().");
        }
        return driver;
    }

    /**
     * Quits the driver and removes the ThreadLocal reference to prevent
     * memory leaks — critical for test suite runs.
     */
    public static void quitDriver() {
        WebDriver driver = DRIVER_STORE.get();
        if (driver != null) {
            log.info("Quitting WebDriver for thread {}.", Thread.currentThread().getId());
            driver.quit();
            DRIVER_STORE.remove();
        }
    }

    // ── Browser builders ───────────────────────────────────────────────────

    private static WebDriver buildChromeDriver(boolean headless) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions opts = new ChromeOptions();
        if (headless) {
            opts.addArguments("--headless=new");
        }
        opts.addArguments(
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--window-size=1920,1080",
                // Disable Chrome's automation detection banner
                "--disable-blink-features=AutomationControlled"
        );
        opts.setExperimentalOption("excludeSwitches",
                new String[]{"enable-automation"});
        opts.setExperimentalOption("useAutomationExtension", false);
        return new ChromeDriver(opts);
    }

    private static WebDriver buildFirefoxDriver(boolean headless) {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions opts = new FirefoxOptions();
        if (headless) {
            opts.addArguments("-headless");
        }
        return new FirefoxDriver(opts);
    }

    private static WebDriver buildEdgeDriver(boolean headless) {
        WebDriverManager.edgedriver().setup();
        EdgeOptions opts = new EdgeOptions();
        if (headless) {
            opts.addArguments("--headless=new");
        }
        return new EdgeDriver(opts);
    }
}
