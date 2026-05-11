package com.periplus.sdet.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

/**
 * {@code HomePage} models the Periplus.com homepage.
 *
 * <p><b>Responsibilities:</b>
 * <ul>
 *   <li>Navigate to the site</li>
 *   <li>Dismiss any overlays / cookie banners</li>
 *   <li>Access the search bar</li>
 *   <li>Access the login link in the top navigation</li>
 * </ul>
 * </p>
 *
 * <p><b>TC Factors (environment):</b> Assumes a fresh browser session with no
 * pre-existing cookies. If a session cookie is present, the user will be
 * auto-logged in and the login link will not be visible.</p>
 */
public class HomePage extends BasePage {

    // ── Locators ───────────────────────────────────────────────────────────

    /** Primary search input — persistent in the top navigation bar. */
    @FindBy(css = "input#filter_name_desktop")
    private WebElement searchInput;

    /** Search submit button next to the search bar. */
    @FindBy(css = "button.btnn")
    private WebElement searchButton;

    /**
     * Sign-in / Account link in the top navigation.
     */
    @FindBy(css = "a[href*='account/Your-Account']")
    private WebElement signInLink;

    /** Cart icon in the top navigation bar. */
    @FindBy(css = "a.single-icon[href*='checkout/cart']")
    private WebElement cartLink;

    // ── Cookie / overlay dismissal ─────────────────────────────────────────

    /**
     * Dismisses any cookie consent banner or promotional overlay that may
     * block interaction with page elements.
     *
     * <p><b>Dynamic Interaction note:</b> The banner is injected asynchronously;
     * we poll for its presence for up to 5 seconds before giving up gracefully.</p>
     */
    public void dismissOverlaysIfPresent() {
        try {
            List<WebElement> overlayCloseButtons = driver.findElements(
                    By.cssSelector(
                            "button.close, "            // Generic close
                            + ".cookie-consent button, " // Cookie banners
                            + "button[aria-label='Close'], "
                            + "#gdpr-cookie-notice button, "
                            + ".modal-close, "
                            + ".popup-close"
                    )
            );
            for (WebElement btn : overlayCloseButtons) {
                if (btn.isDisplayed()) {
                    log.info("Dismissing overlay/popup.");
                    click(btn);
                }
            }
        } catch (Exception e) {
            log.debug("No dismissible overlay found (this is expected on clean sessions).");
        }
    }

    // ── Actions ────────────────────────────────────────────────────────────

    /**
     * Navigates the browser to the Periplus homepage.
     *
     * @param baseUrl the base URL from {@code config.properties}
     * @return {@code this} for fluent chaining
     */
    public HomePage open(String baseUrl) {
        navigateTo(baseUrl);
        waitForUrlContains("periplus.com");
        waitForVisible(searchInput);
        dismissOverlaysIfPresent();
        log.info("Homepage loaded successfully.");
        return this;
    }

    /**
     * Clicks the "Sign In" link to navigate to the login page.
     *
     * @return a new {@link LoginPage} instance
     */
    public LoginPage goToLoginPage() {
        String loginUrl = getCurrentUrl().replaceAll("/+$", "") + "/account/Login";
        log.info("Navigating to the login page: {}", loginUrl);
        navigateTo(loginUrl);
        waitForUrlContains("account/Login");
        return new LoginPage();
    }

    /**
     * Types the search term into the search bar and submits the form.
     *
     * @param keyword the product search keyword
     * @return a new {@link SearchResultsPage} instance
     */
    public SearchResultsPage searchFor(String keyword) {
        log.info("Searching for: '{}'", keyword);
        typeInto(searchInput, keyword);
        click(searchButton);
        return new SearchResultsPage();
    }

    /**
     * Clicks the cart icon in the top navigation.
     *
     * @return a new {@link CartPage} instance
     */
    public CartPage goToCart() {
        click(cartLink);
        return new CartPage();
    }
}
