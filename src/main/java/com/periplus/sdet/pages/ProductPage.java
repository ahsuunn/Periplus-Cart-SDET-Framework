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

        // Wait for either a success toast or the cart counter to update
        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.visibilityOf(successMessage),
                    ExpectedConditions.urlContains("cart"),
                    driver -> readCartCount() > initialCount
            ));
            log.info("Product successfully added to cart (confirmation signal received).");
        } catch (Exception e) {
            // Graceful degradation — some site versions silently update the cart
            log.warn("No explicit success signal detected; proceeding assuming cart was updated.");
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
     * Returns {@code true} when the "Add to Cart" button is present and enabled,
     * indicating the product is in stock.
     *
     * @return {@code true} if the product can be added to the cart
     */
    public boolean isAddToCartAvailable() {
        try {
            WebElement btn = waitForVisible(addToCartButton);
            return btn.isEnabled();
        } catch (Exception e) {
            log.warn("'Add to Cart' button not found — product may be out of stock.");
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
}
