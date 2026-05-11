package com.periplus.sdet.pages;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;
import java.util.stream.Collectors;

/**
 * {@code SearchResultsPage} models the search-results listing page on Periplus.
 *
 * <p>URL pattern: {@code https://www.periplus.com/p/English-Books?filter_name=<keyword>}
 * or similar variants depending on the category.</p>
 *
 * <p><b>TC Factors:</b>
 * <ul>
 *   <li>Search results depend on live inventory — the number of items returned
 *       may vary. Tests must be resilient to a variable result count.</li>
 *   <li>Pagination: if the keyword returns many products, only the first page
 *       is relevant for the primary happy-path scenario.</li>
 * </ul>
 * </p>
 *
 * <p><b>Dynamic Interactions:</b>
 * <ul>
 *   <li>Results are fetched via an AJAX call after the search form is submitted.
 *       The product grid must be waited upon before any interaction.</li>
 *   <li>Individual product "Add to Cart" buttons may trigger a mini-cart
 *       AJAX update without navigating to a new page.</li>
 * </ul>
 * </p>
 */
public class SearchResultsPage extends BasePage {

    // ── Locators ───────────────────────────────────────────────────────────

    /**
     * Visible product title links rendered on the results page.
     */
    @FindBy(css = "h3 a[href*='/p/']")
    private List<WebElement> productTitleLinks;

    /** "No results" message rendered when the search returns 0 products. */
    @FindBy(css = ".no-results, .alert-warning")
    private WebElement noResultsMessage;

    // ── Actions ────────────────────────────────────────────────────────────

    /**
     * Waits until at least one product card is present in the DOM, then
     * returns the total count of products on the page.
     *
     * @return the count of products returned by the search
     */
    public int getProductCount() {
        int count = (int) productTitleLinks.stream()
                .filter(WebElement::isDisplayed)
                .count();
        log.info("Search results page shows {} product(s).", count);
        return count;
    }

    /**
     * Returns the title text of the first product in the results list.
     *
     * <p><b>Verification Call:</b> Used in step assertions to confirm that the
     * correct product title appears in the cart after adding.</p>
     *
     * @return the product title string
     */
    public String getFirstProductTitle() {
        WebElement titleEl = getFirstVisibleProductTitleLink();
        String title = titleEl.getText().trim();
        log.info("First product title resolved as: '{}'", title);
        return title;
    }

    /**
     * Clicks "Add to Cart" on the first product in the results grid.
     *
     * <p>If the button is not directly visible (e.g., behind a hover layer),
     * a JavaScript hover is performed before clicking.</p>
     *
     * @return a new {@link ProductPage} if the click navigates to a product
     *         detail page, or {@code this} if it is an inline add.
     */
    public ProductPage clickFirstProductTitle() {
        log.info("Clicking on first product to navigate to its detail page.");
        // Wait for any preloader overlay to disappear
        dismissPreloaderIfPresent();
        WebElement link = getFirstVisibleProductTitleLink();
        String href = link.getAttribute("href");
        if (href != null && !href.isBlank()) {
            log.info("Navigating directly to product URL: {}", href);
            navigateTo(href);
        } else {
            click(link);
        }
        return new ProductPage();
    }

    /**
     * Checks whether the "no results" message is visible on the page.
     *
     * @return {@code true} if zero results were returned
     */
    public boolean hasNoResults() {
        try {
            return noResultsMessage.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────

    /**
     * Returns the first visible product title link on the page.
     *
     * @return the first visible product title link
     * @throws org.openqa.selenium.TimeoutException if no visible product links appear
     */
    private WebElement getFirstVisibleProductTitleLink() {
        return wait.until(driver -> productTitleLinks.stream()
                .filter(WebElement::isDisplayed)
                .findFirst()
                .orElse(null));
    }
}
