package com.periplus.sdet.pages;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.Assert;

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

    private static final By CART_ITEM_ROW = By.cssSelector(
            "div.col-lg-10.col-9 a[href*='checkout/cart?remove']");
    private static final By EMPTY_CART = By.cssSelector(
            ".cart-empty, p[class*='empty'], div[class*='empty-cart'], div.cart-is-empty");
    /** The cart items table / container that holds all line items. */
    @FindBy(css = "table.data-table, "
            + "form[action*='cart'], "
            + "div.cart-items, "
            + "ul.cart-list, "
            + "#shopping-cart-table, "
            + "#content, "
            + ".shopping-cart, "
            + ".cart-content, "
            + ".checkout-cart")
    private WebElement cartTable;

    /** All product row elements within the cart. */
    @FindBy(css = "div.col-lg-10.col-9, tr.item-info, .cart-item, div.cart-product")
    private List<WebElement> cartItems;

    /** Cart subtotal amount element. */
    @FindBy(id = "sub_total")
    private WebElement subtotalEl;

    /** "Proceed to Checkout" button. */
    @FindBy(css = "button[onclick*='checkout'], "
            + "a[href*='checkout'], "
            + "button[class*='checkout'], "
            + ".btn-proceed-checkout")
    private WebElement checkoutButton;

    /** Cart count badge in header. */
    @FindBy(css = "span.cart-counter, .cart-qty, #cart-count")
    private WebElement cartCountBadge;

    /** "Your cart is empty" message. */
    @FindBy(css = ".content")
    private WebElement emptyCartMessage;

    // ── Actions ────────────────────────────────────────────────────────────



/**
 * Extracts the exact Rupiah value from messy strings containing points or extra text.
 * Example: "Rp 458,000 or 1832 Points" -> Extracts "458,000" -> Returns 458000
 */
public int parsePriceToInteger(String rawPriceText) {
    if (rawPriceText == null || rawPriceText.isEmpty() || rawPriceText.equals("0")) {
        return 0;
    }

    try {
        // Regex: Find "Rp" followed by any spaces, then capture all digits, commas, and dots
        Pattern pattern = Pattern.compile("Rp\\s*([\\d,\\.]+)");
        Matcher matcher = pattern.matcher(rawPriceText);

        if (matcher.find()) {
            // matcher.group(1) gives us exactly "458,000" (ignoring the " or 1832 Points")
            String cleanNumberString = matcher.group(1); 
            
            // Now strip the commas and dots to get pure math digits
            String justDigits = cleanNumberString.replaceAll("\\D+", "");
            return Integer.parseInt(justDigits);
        }
        
        // Fallback if "Rp" is missing
        return Integer.parseInt(rawPriceText.replaceAll("\\D+", ""));
        
    } catch (Exception e) {
        log.error("Failed to parse price from text: " + rawPriceText, e);
        return 0;
    }
}
    /**
     * Waits for the cart page to fully load by polling until either the
     * cart table or the empty cart message is visible.
     *
     * @return {@code this} for fluent chaining
     */
    public CartPage waitForCartToLoad() {
        log.info("Waiting for cart page content to load.");
        wait.until(ExpectedConditions.or(
                ExpectedConditions.visibilityOfElementLocated(CART_ITEM_ROW),
                ExpectedConditions.visibilityOfElementLocated(EMPTY_CART),
                ExpectedConditions.visibilityOf(cartTable)
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
        int count = getLineItems().size();
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
        boolean found = getLineItems().stream().anyMatch(item -> {
            boolean matches = item.findElements(By.cssSelector(
                    "td.product-name a, "
                    + ".product-name, "
                    + "span[class*='product-name'], "
                    + "a[class*='product-link'], "
                    + "a[href*='/p/']"
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
        WebElement item = findItemRowByTitle(productTitle);
        if (item != null) {
            String qtyText = item.findElements(By.cssSelector(
                    "input[id^='qty_'], input[name^='quantity['], input.input-number"
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
        log.warn("Item '{}' not found in cart.", productTitle);
        return -1;
    }

    /**
     * Updates the quantity for a specific product and waits for the AJAX
     * update to complete.
     *
     * @param productTitle the product to update
     * @param qty          the new quantity
     * @return {@code this} for fluent chaining
     */
    public CartPage changeQuantity(String productTitle, int qty) {
        log.info("Changing quantity of '{}' to {}.", productTitle, qty);
        WebElement item = findItemRowByTitle(productTitle);
        if (item == null) {
            throw new RuntimeException("Product '" + productTitle + "' not found in cart.");
        }

        WebElement qtyInput = item.findElement(By.cssSelector(
                "input[id^='qty_'], input[name^='quantity['], input.input-number"
        ));
        
        try {
            forceTypeInto(qtyInput, String.valueOf(qty));
            qtyInput.sendKeys(org.openqa.selenium.Keys.ENTER);
        } catch (Exception e) {
            log.warn("Caught unexpected alert during quantity change: {}", e.getMessage());
            handleAlertIfPresent();
            // Retry once after alert is dismissed
            forceTypeInto(qtyInput, String.valueOf(qty));
            qtyInput.sendKeys(org.openqa.selenium.Keys.ENTER);
        }

        // Wait for preloader and then wait for the subtotal to reflect changes
        dismissPreloaderIfPresent();
        return this;
    }

    /**
     * Retrieves the unit price of a specific product.
     *
     * @param productTitle the product title
     * @return the unit price as a string (e.g., "Rp 150.000")
     */
    public String getUnitPrice(String productTitle) {
        WebElement item = findItemRowByTitle(productTitle);
        if (item == null) {
            throw new RuntimeException("Product '" + productTitle + "' not found in cart.");
        }

        String rawPriceText = item.findElements(By.xpath(".//div[contains(@class, 'row') and contains(text(), 'Rp')]"))
            .stream()
            .findFirst()
            .map(el -> el.getText().trim())
            .orElse("0");

        log.info("Raw unit price text for '{}': {}", productTitle, rawPriceText);
        parsePriceToInteger(rawPriceText);
        return rawPriceText;
    }

    /**
     * Removes a specific product from the cart.
     *
     * @param productTitle the product to remove
     * @return {@code this} for fluent chaining
     */
    public CartPage removeItem(String productTitle) {
        log.info("Removing '{}' from cart.", productTitle);
        WebElement item = findItemRowByTitle(productTitle);
        if (item == null) {
            throw new RuntimeException("Product '" + productTitle + "' not found in cart.");
        }

        WebElement removeBtn = item.findElement(By.cssSelector(
                "a[href*='checkout/cart?remove'], .btn-remove, .action-delete"
        ));
        click(removeBtn);
        dismissPreloaderIfPresent();
        return this;
    }

    /**
     * Parses a currency string (e.g., "Rp 150.000") into a numeric value.
     * Handles Indonesian Rupiah formatting.
     *
     * @param currencyText the text to parse
     * @return the numeric value as a long
     */
    public long parseCurrency(String currencyText) {
        if (currencyText == null || currencyText.isEmpty()) return 0;
        // Remove non-digit characters except for commas/dots if they are used as separators
        // Periplus usually uses '.' as thousand separator and no decimals for IDR
        String cleaned = currencyText.replaceAll("[^0-9]", "");
        try {
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            log.error("Failed to parse currency text: '{}'", currencyText);
            return 0;
        }
    }

    /**
     * Checks if the "Your cart is empty" message is visible.
     *
     * @return {@code true} if the message is displayed
     */
    public boolean isCartEmptyMessageDisplayed() {
        try {
            boolean displayed = waitForVisible(emptyCartMessage).isDisplayed();
            log.info("Is cart empty message displayed: {}", displayed);
            return displayed;
        } catch (Exception e) {
            System.err.println("Failed to find empty cart message: " + e.getClass().getSimpleName());
            return false;
        }
    }

    /**
     * Returns the current cart badge count from the header.
     *
     * @return the badge count as an integer
     */
    public int getCartBadgeCount() {
        try {
            String text = cartCountBadge.getText().trim();
            if (text.isEmpty()) return 0;
            return Integer.parseInt(text.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private WebElement findItemRowByTitle(String productTitle) {
        return getLineItems().stream()
                .filter(item -> {
                    String nameText = item.findElements(By.cssSelector(
                            "td.product-name a, .product-name, a[class*='product-link'], a[href*='/p/']"
                    )).stream()
                      .findFirst()
                      .map(el -> el.getText().trim())
                      .orElse("");
                    return nameText.toLowerCase().contains(productTitle.toLowerCase());
                })
                .findFirst()
                .orElse(null);
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
            boolean empty = !driver.findElements(EMPTY_CART).isEmpty()
                    && driver.findElement(EMPTY_CART).isDisplayed();
            log.info("Cart empty state: {}", empty);
            return empty;
        } catch (Exception e) {
            return false;
        }
    }

    private List<WebElement> getLineItems() {
        return cartItems.stream()
                .filter(item -> !item.findElements(By.cssSelector(
                        "a[href*='checkout/cart?remove']")).isEmpty())
                .toList();
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
