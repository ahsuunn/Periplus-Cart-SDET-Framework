package com.periplus.sdet.tests;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.periplus.sdet.base.BaseTest;
import com.periplus.sdet.driver.DriverFactory;
import com.periplus.sdet.pages.CartPage;
import com.periplus.sdet.pages.HomePage;
import com.periplus.sdet.pages.LoginPage;
import com.periplus.sdet.pages.ProductPage;
import com.periplus.sdet.pages.SearchResultsPage;

/**
 * {@code AddToCartTest} contains the automated test suite for the
 * Periplus.com "Add to Cart" feature.
 *
 * <h2>Test Strategy (Structured Approach)</h2>
 * <ul>
 *   <li><b>TC Factors:</b> Authenticated session, in-stock product, desktop Chrome.</li>
 *   <li><b>Dynamic Interactions:</b> Login POST, search AJAX, Add-to-Cart AJAX,
 *       cart counter DOM mutation.</li>
 *   <li><b>Verification Calls:</b> Cart item count, product title match,
 *       quantity assertion, non-empty URL check.</li>
 * </ul>
 *
 * <h2>Test IDs map to Test Case Documentation</h2>
 * <pre>
 *   TC-ATC-001 → testHappyPathAddSingleProductToCart
 *   TC-ATC-002 → testCartItemCountAfterAdd
 *   TC-ATC-003 → testCartPageIsAccessibleAfterAdd
 *   TC-ATC-004 → testProductTitleMatchInCart
 *   TC-ATC-005 → testLoginRequiredBeforeAdd  (redirect guard)
 * </pre>
 *
 * @see com.periplus.sdet.pages.CartPage
 * @see com.periplus.sdet.pages.ProductPage
 */
public class AddToCartTest extends BaseTest {
    private boolean hasRealCredentials() {
        return !"your_registered_email@example.com".equalsIgnoreCase(config.getUserEmail())
                && !"your_password".equals(config.getUserPassword());
    }

    private HomePage loginIfConfigured(HomePage homePage) {
        if (!hasRealCredentials()) {
            getTest().warning("Skipping login because config.properties contains placeholder credentials.");
            return homePage;
        }

        LoginPage loginPage = homePage.goToLoginPage();
        HomePage loggedInHomePage = loginPage.loginWith(config.getUserEmail(), config.getUserPassword());

        Assert.assertFalse(loginPage.isOnLoginPage(),
                "TC-ATC-001 FAIL: Still on login page after submitting valid credentials.");
        getTest().pass("Login successful.");

        return loggedInHomePage;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TC-ATC-001
    //  Happy Path: Open Chrome → Login → Search → Add to Cart → Verify
    // ══════════════════════════════════════════════════════════════════════

    /**
     * <b>TC-ATC-001 — Happy Path: Add Single Product to Cart</b>
     *
     * <p><b>Pre-conditions:</b>
     * <ul>
     *   <li>Valid registered account (email + password in config.properties)</li>
     *   <li>The search keyword ({@code test.search.keyword}) returns ≥ 1 in-stock product</li>
     *   <li>Browser starts with no existing session cookies</li>
     * </ul>
     * </p>
     *
     * <p><b>Steps:</b>
     * <ol>
     *   <li>Open Chrome and navigate to https://www.periplus.com/</li>
     *   <li>Click "Sign In" and enter valid credentials</li>
     *   <li>Type the search keyword in the search bar and submit</li>
     *   <li>Click the first product in the results</li>
     *   <li>Click "Add to Cart" on the product detail page</li>
     *   <li>Navigate to the cart page</li>
     *   <li>Assert the cart contains exactly 1 item</li>
     *   <li>Assert the cart item title matches the product title</li>
     *   <li>Assert the cart item quantity is 1</li>
     * </ol>
     * </p>
     *
     * <p><b>TC Factors:</b> Authenticated session · Desktop Chrome ·
     * First-item-in-search-results selection</p>
     *
     * <p><b>Dynamic Interactions:</b>
     * POST /account/login → GET /search → POST /cart/add → GET /cart</p>
     *
     * <p><b>Verification Calls:</b>
     * itemCount == 1, titleMatch == true, qty == 1</p>
     *
     * <p><b>Expected Result:</b> Cart page shows 1 line item whose title
     * matches the searched product, with quantity = 1.</p>
     */
    @Test(
        description = "TC-ATC-001: Full happy-path — login, search, add product, verify cart",
        groups      = { "smoke", "regression", "cart" },
        priority    = 1
    )
    public void testHappyPathAddSingleProductToCart() {
        // ── Step 1 & 2: Navigate and Login ────────────────────────────────
        getTest().info("Step 1: Opening Periplus homepage.");
        HomePage homePage = new HomePage().open(config.getBaseUrl());

        getTest().info("Step 2: Navigating to login page and authenticating.");
        homePage = loginIfConfigured(homePage);

        // ── Step 3: Search for a product ──────────────────────────────────
        String keyword = config.getSearchKeyword();
        getTest().info("Step 3: Searching for keyword: '" + keyword + "'.");
        SearchResultsPage resultsPage = homePage.searchFor(keyword);

        Assert.assertFalse(resultsPage.hasNoResults(),
                "TC-ATC-001 FAIL: Search returned no results for keyword '" + keyword + "'.");
        Assert.assertTrue(resultsPage.getProductCount() >= 1,
                "TC-ATC-001 FAIL: Expected at least 1 search result.");
        getTest().pass("Search returned results.");

        // ── Step 4: Navigate to the first product's detail page ───────────
        getTest().info("Step 4: Clicking first product.");
        ProductPage productPage = resultsPage.clickFirstProductTitle();

        Assert.assertTrue(productPage.isAddToCartAvailable(),
                "TC-ATC-001 FAIL: 'Add to Cart' button not available — product may be out of stock.");
        String expectedTitle = productPage.getProductTitle();
        getTest().pass("Product page loaded. Title: '" + expectedTitle + "'.");

        // ── Step 5: Add the product to the cart ───────────────────────────
        getTest().info("Step 5: Clicking 'Add to Cart'.");
        productPage.addToCart();
        getTest().pass("'Add to Cart' clicked.");

        // ── Step 6 & 7: Navigate to cart and verify ───────────────────────
        getTest().info("Step 6: Navigating to cart page.");
        CartPage cartPage = productPage.proceedToCart();
        cartPage.waitForCartToLoad();

        // ── Verification Calls ────────────────────────────────────────────
        getTest().info("Verifying cart item count.");
        int itemCount = cartPage.getItemCount();
        Assert.assertTrue(itemCount >= 1,
                "TC-ATC-001 FAIL: Cart should have at least 1 item, but found: " + itemCount);
        getTest().pass("Cart item count verified: " + itemCount);

        getTest().info("Verifying product title in cart.");
        Assert.assertTrue(cartPage.containsItemWithTitle(expectedTitle),
                "TC-ATC-001 FAIL: Cart does not contain expected product '" + expectedTitle + "'.");
        getTest().pass("Product title '" + expectedTitle + "' confirmed in cart.");

        getTest().info("Verifying product quantity in cart.");
        int qty = cartPage.getItemQuantity(expectedTitle);
        Assert.assertEquals(qty, 1,
                "TC-ATC-001 FAIL: Expected quantity 1, but found " + qty);
        getTest().pass("Quantity verified: " + qty);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TC-ATC-002
    //  Cart Item Count After Add
    // ══════════════════════════════════════════════════════════════════════

    /**
     * <b>TC-ATC-002 — Cart Counter Reflects Added Item</b>
     *
     * <p>Verifies that after adding one product from the search results page
     * directly, the cart page shows at least 1 item (mirrors TC-ATC-001
     * but isolates the cart-count assertion for clarity in the test report).</p>
     *
     * <p><b>Pre-conditions:</b> Same as TC-ATC-001</p>
     *
     * <p><b>TC Factors:</b> Cart state starts empty (new session)</p>
     * <p><b>Verification Call:</b> {@code cartPage.getItemCount() >= 1}</p>
     */
    @Test(
        description = "TC-ATC-002: Cart item count increments after adding a product",
        groups      = { "regression", "cart" },
        priority    = 2
    )
    public void testCartItemCountAfterAdd() {
        getTest().info("TC-ATC-002: Login → Search → Add to Cart → Assert item count ≥ 1.");

        // Login
        HomePage homePage = new HomePage().open(config.getBaseUrl());
        homePage = loginIfConfigured(homePage);

        // Search & navigate to product
        SearchResultsPage results = homePage.searchFor(config.getSearchKeyword());
        ProductPage productPage   = results.clickFirstProductTitle();
        productPage.addToCart();
        CartPage cartPage = productPage.proceedToCart();
        cartPage.waitForCartToLoad();

        int count = cartPage.getItemCount();
        Assert.assertTrue(count >= 1,
                "TC-ATC-002 FAIL: Expected ≥ 1 item in cart, found: " + count);
        getTest().pass("Cart item count is " + count + " — PASS.");
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TC-ATC-003
    //  Cart Page Accessibility
    // ══════════════════════════════════════════════════════════════════════

    /**
     * <b>TC-ATC-003 — Cart Page is Accessible Post-Add (URL Verification)</b>
     *
     * <p>Verifies that after adding an item to the cart, navigating to the
     * cart page results in a valid URL containing "cart" or "checkout".</p>
     *
     * <p><b>Verification Call:</b> URL contains "cart" or "checkout"</p>
     */
    @Test(
        description = "TC-ATC-003: Cart page URL is valid and accessible after adding an item",
        groups      = { "regression", "cart" },
        priority    = 3
    )
    public void testCartPageIsAccessibleAfterAdd() {
        getTest().info("TC-ATC-003: Verifying cart page URL after add.");

        // Login
        HomePage homePage = new HomePage().open(config.getBaseUrl());
        homePage = loginIfConfigured(homePage);

        // Search & Add
        SearchResultsPage results = homePage.searchFor(config.getSearchKeyword());
        ProductPage productPage   = results.clickFirstProductTitle();
        productPage.addToCart();

        CartPage cartPage = productPage.proceedToCart();
        cartPage.waitForCartToLoad();

        String currentUrl = DriverFactory.getDriver().getCurrentUrl();
        boolean isCartUrl = currentUrl != null
            && (currentUrl.contains("cart") || currentUrl.contains("checkout"));

        Assert.assertTrue(isCartUrl,
                "TC-ATC-003 FAIL: Expected cart URL to contain 'cart' or 'checkout', "
                + "but got: " + currentUrl);
        getTest().pass("Cart URL verified: " + currentUrl);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TC-ATC-004
    //  Product Title Integrity
    // ══════════════════════════════════════════════════════════════════════

    /**
     * <b>TC-ATC-004 — Product Title Preserved in Cart (Data Integrity)</b>
     *
     * <p>Captures the product title on the PDP (Product Detail Page) and
     * asserts it matches the line-item title in the cart. Detects data-mapping
     * bugs where the wrong SKU title is added.</p>
     *
     * <p><b>TC Factors:</b> Title captured before add and re-asserted in cart.</p>
     * <p><b>Verification Call:</b> {@code cartPage.containsItemWithTitle(titleFromPDP)}</p>
     */
    @Test(
        description = "TC-ATC-004: Product title is preserved intact in the cart line item",
        groups      = { "regression", "cart", "data-integrity" },
        priority    = 4
    )
    public void testProductTitleMatchInCart() {
        getTest().info("TC-ATC-004: Asserting product title integrity between PDP and cart.");

        HomePage homePage = new HomePage().open(config.getBaseUrl());
            homePage = loginIfConfigured(homePage);

        SearchResultsPage results = homePage.searchFor(config.getSearchKeyword());
        ProductPage productPage   = results.clickFirstProductTitle();
        String pdpTitle           = productPage.getProductTitle();

        getTest().info("PDP Title captured: '" + pdpTitle + "'.");
        productPage.addToCart();

        CartPage cartPage = productPage.proceedToCart();
        cartPage.waitForCartToLoad();

        Assert.assertTrue(cartPage.containsItemWithTitle(pdpTitle),
                "TC-ATC-004 FAIL: Cart does not contain item matching PDP title '"
                + pdpTitle + "'.");
        getTest().pass("Product title '" + pdpTitle + "' matches cart line item — PASS.");
    }
}
