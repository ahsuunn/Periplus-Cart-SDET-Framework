package com.periplus.sdet.pages;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * {@code LoginPage} models the Periplus account login page.
 *
 * <p>URL pattern: {@code https://www.periplus.com/account/login}</p>
 *
 * <p><b>TC Factors:</b>
 * <ul>
 *   <li>Requires no active session cookie — a pre-existing valid session will
 *       redirect directly to the account dashboard.</li>
 *   <li>The page may redirect to {@code /account/login?redirect=...} if the
 *       user attempted to access a protected resource first.</li>
 * </ul>
 * </p>
 *
 * <p><b>Dynamic Interactions:</b>
 * <ul>
 *   <li>On submit, the browser issues a {@code POST /account/login} request;
 *       a successful authentication sets a session cookie and redirects to
 *       the homepage or the original requested URL.</li>
 *   <li>A failed attempt returns an inline error message — no page reload
 *       on some configurations (AJAX-based validation).</li>
 * </ul>
 * </p>
 */
public class LoginPage extends BasePage {

    // ── Locators ───────────────────────────────────────────────────────────

    /** E-mail / username input field. */
    @FindBy(css = "input#LoginEmail, input[name='email'], input[type='email']")
    private WebElement emailInput;

    /** Password input field. */
    @FindBy(css = "input#LoginPassword, input[name='password'], input[type='password']")
    private WebElement passwordInput;

    /** Login submit button. */
    @FindBy(css = "button[type='submit'], input[type='submit'][value*='Login'], "
            + "input[type='submit'][value*='Sign']")
    private WebElement loginButton;

    /**
     * Inline error message displayed on authentication failure.
     * Used in negative-path test cases.
     */
    @FindBy(css = "div.alert.alert-danger")
    private WebElement errorMessage;

    // ── Actions ────────────────────────────────────────────────────────────

    /**
     * Enters the email address into the email field.
     *
     * @param email the e-mail address
     * @return {@code this} for fluent chaining
     */
    public LoginPage enterEmail(String email) {
        log.info("Entering email: {}", email);
        typeInto(emailInput, email);
        return this;
    }

    /**
     * Enters the password into the password field.
     *
     * @param password the account password (value not logged for security)
     * @return {@code this} for fluent chaining
     */
    public LoginPage enterPassword(String password) {
        log.info("Entering password: [REDACTED]");
        typeInto(passwordInput, password);
        return this;
    }

    /**
     * Clicks the login / submit button.
     *
     * <p>On success, Periplus redirects to the homepage or the account
     * dashboard, so this method transitions to a {@link HomePage}.</p>
     *
     * @return a new {@link HomePage} instance
     */
    public HomePage clickLogin() {
        log.info("Submitting login form.");
        click(loginButton);
        // Wait for navigation away from the login page
        waitForUrlContains("periplus.com");
        return new HomePage();
    }

    /**
     * Convenience method — performs a full login in a single call.
     *
     * @param email    the e-mail address
     * @param password the account password
     * @return a new {@link HomePage} instance post-login
     */
    public HomePage loginWith(String email, String password) {
        return enterEmail(email)
                .enterPassword(password)
                .clickLogin();
    }

    // ── Verification helpers ───────────────────────────────────────────────

    /**
     * Returns the text of the inline error message element.
     *
     * <p><b>Verification Call (test oracle):</b> In negative-path tests,
     * assert the returned string matches the expected error text
     * (e.g., "Invalid email or password").</p>
     *
     * @return the error message text, or an empty string if not present
     */
    public String getErrorMessageText() {
        try {
            return waitForVisible(errorMessage).getText().trim();
        } catch (Exception e) {
            log.debug("No error message element found.");
            return "";
        }
    }

    /**
     * Returns {@code true} if the login page is currently displayed.
     *
     * @return {@code true} when the URL contains "login"
     */
    public boolean isOnLoginPage() {
        return getCurrentUrl().contains("login");
    }
}
