package com.periplus.sdet.pages;

import com.periplus.sdet.driver.DriverFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static com.periplus.sdet.config.ConfigManager.getInstance;

/**
 * {@code BasePage} is the root of the Page Object Model hierarchy.
 *
 * <p>Every concrete page class extends {@code BasePage}, which provides:
 * <ul>
 *   <li>A shared {@link WebDriver} reference via {@link DriverFactory}</li>
 *   <li>A pre-configured {@link WebDriverWait} for explicit waits</li>
 *   <li>Convenience wrapper methods so that page classes contain zero
 *       raw Selenium boilerplate</li>
 * </ul>
 * </p>
 */
public abstract class BasePage {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final WebDriver driver;
    protected final WebDriverWait wait;

    // ── Constructor ────────────────────────────────────────────────────────

    protected BasePage() {
        this.driver = DriverFactory.getDriver();
        this.wait   = new WebDriverWait(driver,
                Duration.ofSeconds(getInstance().getExplicitWaitSeconds()));
        PageFactory.initElements(driver, this);
        log.debug("Initialised page object: {}", getClass().getSimpleName());
    }

    // ── Navigation helpers ─────────────────────────────────────────────────

    /** Navigates to the given absolute URL. */
    protected void navigateTo(String url) {
        log.info("Navigating to: {}", url);
        driver.get(url);
    }

    // ── Wait helpers ───────────────────────────────────────────────────────

    /**
     * Waits until the element is visible on the DOM.
     *
     * @param element the element to wait for
     * @return the element once visible
     */
    protected WebElement waitForVisible(WebElement element) {
        return wait.until(ExpectedConditions.visibilityOf(element));
    }

    /**
     * Waits until the element is clickable (visible + enabled).
     *
     * @param element the element to wait for
     * @return the element once clickable
     */
    protected WebElement waitForClickable(WebElement element) {
        return wait.until(ExpectedConditions.elementToBeClickable(element));
    }

    /**
     * Waits until the page title contains the given substring.
     *
     * @param titlePart partial title string
     */
    protected void waitForTitleContains(String titlePart) {
        wait.until(ExpectedConditions.titleContains(titlePart));
    }

    /**
     * Waits until the current URL contains the given substring.
     *
     * @param urlPart partial URL string
     */
    protected void waitForUrlContains(String urlPart) {
        wait.until(ExpectedConditions.urlContains(urlPart));
    }

    /**
     * Waits until the current URL no longer contains the provided fragment.
     *
     * @param urlPart fragment that should disappear from the current URL
     */
    protected void waitForUrlNotContains(String urlPart) {
        wait.until(ExpectedConditions.not(ExpectedConditions.urlContains(urlPart)));
    }

    /**
     * Waits for any preloader overlay to become invisible, with graceful
     * degradation if no preloader is found.
     */
    protected void dismissPreloaderIfPresent() {
        try {
            waitForPreloaderToDisappear();
        } catch (Exception e) {
            log.debug("No preloader found or already invisible; proceeding.");
        }
    }

    // ── Interaction helpers ────────────────────────────────────────────────

    /**
     * Clears the field and types the given text.
     *
     * @param element the target input field
     * @param text    the text to enter
     */
    protected void typeInto(WebElement element, String text) {
        waitForClickable(element).clear();
        element.sendKeys(text);
        log.debug("Typed '{}' into element.", text);
    }

    /**
     * Clicks an element, scrolling it into view first if necessary.
     *
     * @param element the element to click
     */
    protected void click(WebElement element) {
        WebElement el = waitForClickable(element);
        scrollIntoView(el);
        try {
            el.click();
        } catch (ElementClickInterceptedException e) {
            // Retry once after overlays/preloaders clear
            waitForPreloaderToDisappear();
            el = waitForClickable(element);
            scrollIntoView(el);
            el.click();
        }
        log.debug("Clicked element: {}.", element);
    }

    private void waitForPreloaderToDisappear() {
        if (!driver.findElements(By.cssSelector("div.preloader, .preloader")).isEmpty()) {
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector("div.preloader, .preloader")));
            log.debug("Preloader has been dismissed.");
        }
    }

    /**
     * Scrolls the element into the visible viewport using JavaScript.
     *
     * @param element the element to scroll to
     */
    protected void scrollIntoView(WebElement element) {
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView({block:'center'});", element);
    }

    /**
     * Returns the current page URL.
     *
     * @return current URL string
     */
    protected String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    /**
     * Returns the current page title.
     *
     * @return current page title string
     */
    protected String getPageTitle() {
        return driver.getTitle();
    }

    /**
     * Accepts any open JavaScript alert if it exists.
     */
    protected void handleAlertIfPresent() {
        try {
            wait.until(ExpectedConditions.alertIsPresent());
            String text = driver.switchTo().alert().getText();
            log.warn("Dismissing unexpected alert: '{}'", text);
            driver.switchTo().alert().accept();
        } catch (Exception e) {
            log.debug("No alert present to handle.");
        }
    }

    /**
     * Overwrites content of an input field using CTRL+A + Type instead of clear().
     * This avoids validation alerts triggered by an 'empty' state.
     *
     * @param element the target input
     * @param text    the text to enter
     */
    protected void forceTypeInto(WebElement element, String text) {
        waitForClickable(element).click();
        element.sendKeys(org.openqa.selenium.Keys.CONTROL + "a");
        element.sendKeys(org.openqa.selenium.Keys.BACK_SPACE);
        element.sendKeys(text);
        log.debug("Force-typed '{}' into element.", text);
    }
}
