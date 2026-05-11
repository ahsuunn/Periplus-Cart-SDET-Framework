package com.periplus.sdet.pages;

import com.periplus.sdet.config.ConfigManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * {@code ProductPage} models a Periplus product detail page.
 *
 * <p>URL pattern: {@code https://www.periplus.com/p/<product-slug>/<product-id>}</p>
 *
 * <p><b>TC Factors:</b>
 * <ul>
 *   <li>Stock availability: the "Add to Cart" button is only rendered when
 *       inventory {@code qty > 0}. An out-of-stock product renders a
 *       "Notify Me" or "Out of Stock" label instead.</li>
 *   <li>Product type: digital/e-book products may have a different CTA
 *       ("Buy Now" vs "Add to Cart").</li>
 * </ul>
 * </p>
 *
 * <p><b>Dynamic Interactions:</b>
 * <ul>
 *   <li>Clicking "Add to Cart" fires a {@code POST /cart/add} AJAX request.
 *       The cart counter in the nav bar updates asynchronously; tests must
 *       wait for the counter change, not a page reload.</li>
 *   <li>A confirmation toast / modal may appear — it must be dismissed or
 *       waited upon before further navigation.</li>
 * </ul>
 * </p>
 */
public class ProductPage extends BasePage {

    // ── Locators ───────────────────────────────────────────────────────────

    /** The main product title / book name displayed on the detail page. */
    @FindBy(css = "h2, h1")
    private WebElement productTitle;

    /** The "Add to Cart" CTA button on the product detail page. */
    @FindBy(css = "button.btn-add-to-cart")
    private WebElement addToCartButton;

    /** The quantity selector input (defaults to 1). */
    @FindBy(css = "input#qty, input[name='qty'], input[class*='qty']")
    private WebElement quantityInput;

    /** The product price element — used in cart price verification. */
    @FindBy(css = "span[itemprop='price'], .price, "
            + "p.special-price .price, div.price-box span.price")
    private WebElement productPrice;

    /** The cart link in the navigation bar. */
    @FindBy(css = "a[href*='/checkout/cart']")
    private WebElement cartLink;

    /** Success / confirmation message or modal after adding to cart. */
    @FindBy(css = ".ti-check, .modal-text ")
    private WebElement successMessage;

    /** "Notify Me" or "Out of Stock" element. */
    @FindBy(css = "button[class*='notify'], .out-of-stock, .availability.out-of-stock")
    private WebElement notifyMeButton;

    /** Out-of-stock alert modal / overlay. */
    @FindBy(css = ".ti-alert.modal-alert, .ti-alert, .modal-alert, .ti-modal, .modal-backdrop, [class*='alert-modal']")
    private WebElement outOfStockAlert;

    // ── Actions ────────────────────────────────────────────────────────────

    /**
     * Returns the displayed product title.
     *
     * <p><b>Verification Call:</b> Capture this value before adding to cart,
     * then assert it matches the cart line item title.</p>
     *
     * @return trimmed product title string
     */
    public String getProductTitle() {
        String title = waitForVisible(productTitle).getText().trim();
        log.info("Product page title: '{}'", title);
        return title;
    }

    /**
     * Returns the product price text (e.g., "Rp 150,000").
     *
     * <p><b>Verification Call:</b> Assert this matches the line item price
     * shown in the cart to detect price-injection regressions.</p>
     *
     * @return price string as displayed
     */
    public String getProductPrice() {
        String price = waitForVisible(productPrice).getText().trim();
        log.info("Product price: '{}'", price);
        return price;
    }

    /**
     * Sets the product quantity before adding to cart.
     *
     * @param quantity the desired quantity (must be ≥ 1)
     * @return {@code this} for fluent chaining
     */
    public ProductPage setQuantity(int quantity) {
        log.info("Setting quantity to {}.", quantity);
        typeInto(quantityInput, String.valueOf(quantity));
        return this;
    }

    /**
     * Clicks the "Add to Cart" button and waits for the AJAX success signal.
     *
     * <p>Two outcomes are handled:
     * <ol>
     *   <li>A success confirmation toast/modal appears — method waits for it.</li>
     *   <li>The page navigates to the cart — the {@link CartPage} handles this.</li>
     * </ol>
     * </p>
     *
     * @return {@code this} to allow chained cart navigation via
     *         {@link #proceedToCart()}
     */
    public ProductPage addToCart() {
        log.info("Clicking 'Add to Cart' button.");
        dismissPreloaderIfPresent();
        int initialCount = readCartCount();
        click(addToCartButton);

        // Wait for a signal: Success, Out-of-Stock Alert, URL change, or Count change
        try {
            wait.until(d -> 
                isOutOfStockAlertVisible() || 
                (driver.findElements(By.cssSelector(".ti-check, .modal-text")).stream().anyMatch(WebElement::isDisplayed)) ||
                driver.getCurrentUrl().contains("cart") || 
                readCartCount() > initialCount
            );
            log.info("Signal received after 'Add to Cart' click.");
        } catch (Exception e) {
            log.warn("No explicit signal detected within timeout.");
        }
        return this;
    }

    /**
     * Navigates to the cart page by clicking the cart icon in the navigation.
     *
     * @return a new {@link CartPage} instance
     */
    public CartPage proceedToCart() {
        log.info("Navigating to the cart page.");
        dismissPreloaderIfPresent();
        try {
            click(cartLink);
        } catch (Exception e) {
            log.warn("Could not click cart link directly, trying to navigate via URL.");
            String baseUrl = ConfigManager.getInstance().getBaseUrl().replaceAll("/+$", "");
            navigateTo(baseUrl + "/checkout/cart");
        }
        waitForUrlContains("cart");
        return new CartPage();
    }

    @FindBy(id = "cart_total")
    private WebElement cartBadge;
    private int readCartCount() {
        try {
            String rawText = waitForVisible(cartBadge).getText().trim();
            if (rawText.isEmpty()) {
            return 0;
        }
        
            return Integer.parseInt(rawText);
        } catch (Exception e) {
            System.err.println("Failed to parse cart count. Raw text was: " + cartBadge.getText());
            return 0;
        }
    }

    /**
     * Returns {@code true} when the "Add to Cart" button is present, enabled,
     * and not functionally blocked by an "Out of Stock" state.
     *
     * <p>Note: On some versions of Periplus, the button remains enabled but
     * triggers an alert modal (e.g., .ti-alert) instead of adding to cart.</p>
     *
     * @return {@code true} if the product can be added to the cart
     */
    public boolean isAddToCartAvailable() {
        try {
            // 1. If 'Notify Me' or 'Out of Stock' labels are visible, it's not available
            if (isNotifyMeDisplayed()) {
                log.info("Product is NOT available: 'Notify Me' button or OOS label detected.");
                return false;
            }

            // 2. Check the main button
            WebElement btn = waitForVisible(addToCartButton);
            if (!btn.isDisplayed() || !btn.isEnabled()) {
                return false;
            }

            // 3. Check for specific CSS indicators that imply it's out of stock even if enabled
            String classAttr = btn.getAttribute("class");
            if (classAttr != null && (classAttr.contains("disabled") || classAttr.contains("oos"))) {
                return false;
            }

            return true;
        } catch (Exception e) {
            log.warn("'Add to Cart' button check failed — product may be out of stock: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Returns the text of the success confirmation message after adding to cart.
     *
     * @return the success message string, or empty string if not present
     */
    public String getSuccessMessageText() {
        try {
            return waitForVisible(successMessage).getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Returns {@code true} if the "Notify Me" button or "Out of Stock" label is displayed.
     *
     * @return {@code true} if product is out of stock
     */
    /**
     * Returns {@code true} if the "Notify Me" button or "Out of Stock" label is displayed.
     *
     * @return {@code true} if product is out of stock
     */
    public boolean isNotifyMeDisplayed() {
        try {
            return notifyMeButton.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns {@code true} if the out-of-stock alert modal is displayed.
     *
     * @return {@code true} if the alert is visible
     */
    /**
     * Returns {@code true} if an out-of-stock alert or modal is present.
     * Uses both Selenium and JavaScript checks to handle transient or animated overlays.
     *
     * @return {@code true} if the alert is detected
     */
    public boolean isOutOfStockAlertVisible() {
        try {
            // 1. Standard Selenium check with a short wait
            if (driver.findElements(By.cssSelector(".ti-alert, .modal-alert, .ti-modal, [class*='alert']")).stream()
                    .anyMatch(WebElement::isDisplayed)) {
                return true;
            }

            // 2. JavaScript fallback: Check if any element with these classes exists and is potentially visible
            String script = "return Array.from(document.querySelectorAll('.ti-alert, .modal-alert, .ti-modal'))" +
                            ".some(el => window.getComputedStyle(el).display !== 'none' && el.offsetHeight > 0);";
            return (Boolean) ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(script);

        } catch (Exception e) {
            return false;
        }
    }
}
