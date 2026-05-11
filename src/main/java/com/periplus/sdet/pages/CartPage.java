package com.periplus.sdet.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

/**
 * {@code CartPage} models the Periplus shopping cart page.
 *
 * <p>URL pattern: {@code https://www.periplus.com/cart} or
 * {@code /checkout/cart}</p>
 *
 * <p><b>TC Factors:</b>
 * <ul>
 *   <li>Session state: cart contents are tied to the authenticated session.
 *       Anonymous (guest) carts behave differently and are out of scope here.</li>
 *   <li>Cart persistence: items added in one session should persist if the
 *       browser is closed and re-opened while the session cookie is valid.</li>
 * </ul>
 * </p>
 *
 * <p><b>Dynamic Interactions:</b>
 * <ul>
 *   <li>Quantity updates and item removals trigger AJAX calls; the page DOM
 *       is mutated in place — all assertions must use explicit waits.</li>
 *   <li>Cart total is recalculated server-side after each mutation and
 *       injected into the DOM asynchronously.</li>
 * </ul>
 * </p>
 *
 * <p><b>Verification Calls (Test Oracles):</b>
 * <ul>
 *   <li>{@link #getItemCount()} — asserts the expected number of unique line items.</li>
 *   <li>{@link #containsItemWithTitle(String)} — asserts a specific product is present.</li>
 *   <li>{@link #getItemQuantity(String)} — asserts correct quantity for a given item.</li>
 *   <li>{@link #getSubtotal()} — asserts the subtotal equals expected price * qty.</li>
 * </ul>
 * </p>
 */
public class CartPage extends BasePage {

    // ── Locators ───────────────────────────────────────────────────────────

    /** The cart items table / container that holds all line items. */
    @FindBy(css = "table.data-table, "
            + "form[action*='cart'], "
            + "div.cart-items, "
            + "ul.cart-list, "
            + "#shopping-cart-table")
    private WebElement cartTable;

    /** All product row elements within the cart. */
    @FindBy(css = "tr.item-info, .cart-item, div.cart-product")
    private List<WebElement> cartItems;

    /** Empty cart message — rendered when there are no line items. */
    @FindBy(css = ".cart-empty, "
            + "p[class*='empty'], "
            + "div[class*='empty-cart'], "
            + "div.cart-is-empty")
    private WebElement emptyCartMessage;

    /** Cart subtotal amount element. */
    @FindBy(css = "div.cart-total td:nth-child(2)")
    private WebElement subtotalEl;

    /** "Proceed to Checkout" button. */
    @FindBy(css = "button[onclick*='checkout'], "
            + "a[href*='checkout'], "
            + "button[class*='checkout'], "
            + ".btn-proceed-checkout")
    private WebElement checkoutButton;

    // ── Actions ────────────────────────────────────────────────────────────

    /**
     * Waits for the cart page to fully load by polling until either the
     * cart table or the empty cart message is visible.
     *
     * @return {@code this} for fluent chaining
     */
    public CartPage waitForCartToLoad() {
        log.info("Waiting for cart page content to load.");
        wait.until(ExpectedConditions.or(
                ExpectedConditions.visibilityOf(cartTable),
                ExpectedConditions.visibilityOf(emptyCartMessage)
        ));
        return this;
    }

    // ── Verification Calls ─────────────────────────────────────────────────

    /**
     * Returns the number of unique product line items in the cart.
     *
     * <p><b>Oracle:</b> After a single add-to-cart action on an empty cart,
     * this should return {@code 1}.</p>
     *
     * @return count of line items
     */
    public int getItemCount() {
        wait.until(ExpectedConditions.visibilityOf(cartTable));
        int count = cartItems.size();
        log.info("Cart contains {} line item(s).", count);
        return count;
    }

    /**
     * Checks whether the cart contains a line item whose title matches
     * (case-insensitive, substring) the given product name.
     *
     * <p><b>Oracle:</b> After adding "Atomic Habits", this should return
     * {@code true} when called with {@code "Atomic Habits"}.</p>
     *
     * @param productTitle the expected product title (substring match)
     * @return {@code true} if a matching item is found
     */
    public boolean containsItemWithTitle(String productTitle) {
        wait.until(ExpectedConditions.visibilityOf(cartTable));
        boolean found = cartItems.stream().anyMatch(item -> {
            boolean matches = item.findElements(By.cssSelector(
                    "td.product-name a, "
                    + ".product-name, "
                    + "span[class*='product-name'], "
                    + "a[class*='product-link']"
            )).stream()
              .findFirst()
              .map(el -> el.getText().trim())
              .orElse("")
              .toLowerCase()
              .contains(productTitle.toLowerCase());
            return matches;
        });
        log.info("Cart contains item with title '{}': {}", productTitle, found);
        return found;
    }

    /**
     * Returns the quantity of a specific cart line item identified by its
     * product title substring.
     *
     * <p><b>Oracle:</b> Asserts that qty = 1 after a single add-to-cart event.</p>
     *
     * @param productTitle the product title to look up (substring, case-insensitive)
     * @return the quantity as an integer, or {@code -1} if the item is not found
     */
    public int getItemQuantity(String productTitle) {
        for (WebElement item : cartItems) {
            String nameText = item.findElements(By.cssSelector(
                    "td.product-name a, .product-name, a[class*='product-link']"
            )).stream()
              .findFirst()
              .map(el -> el.getText().trim())
              .orElse("");

            if (nameText.toLowerCase().contains(productTitle.toLowerCase())) {
                String qtyText = item.findElements(By.cssSelector(
                        "input[id^='qty_'], input.input-number"
                )).stream()
                  .findFirst()
                  .map(el -> {
                      String val = el.getAttribute("value");
                      return (val != null) ? val : el.getText().trim();
                  })
                  .orElse("0");
                try {
                    int qty = Integer.parseInt(qtyText.trim());
                    log.info("Item '{}' has quantity {}.", productTitle, qty);
                    return qty;
                } catch (NumberFormatException e) {
                    log.warn("Could not parse quantity '{}' for item '{}'.", qtyText, productTitle);
                    return -1;
                }
            }
        }
        log.warn("Item '{}' not found in cart.", productTitle);
        return -1;
    }

    /**
     * Returns the cart subtotal text as displayed (e.g., "Rp 150,000").
     *
     * <p><b>Oracle:</b> Compare with {@code product price × quantity} to
     * detect subtotal miscalculation bugs.</p>
     *
     * @return the subtotal string
     */
    public String getSubtotal() {
        String subtotal = waitForVisible(subtotalEl).getText().trim();
        log.info("Cart subtotal: '{}'", subtotal);
        return subtotal;
    }

    /**
     * Returns {@code true} if the empty cart message is displayed.
     *
     * @return {@code true} when the cart has zero items
     */
    public boolean isCartEmpty() {
        try {
            boolean empty = emptyCartMessage.isDisplayed();
            log.info("Cart empty state: {}", empty);
            return empty;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Clicks the "Proceed to Checkout" button.
     * Out-of-scope for the current test suite but provided for completeness.
     */
    public void proceedToCheckout() {
        log.info("Proceeding to checkout.");
        click(checkoutButton);
    }
}
