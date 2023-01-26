package com.applitools.example;

import com.applitools.eyes.BatchInfo;
import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.TestResultsSummary;
import com.applitools.eyes.config.Configuration;
import com.applitools.eyes.playwright.Eyes;
import com.applitools.eyes.playwright.fluent.Target;
import com.applitools.eyes.playwright.visualgrid.BrowserType;
import com.applitools.eyes.visualgrid.model.DeviceName;
import com.applitools.eyes.visualgrid.model.ScreenOrientation;
import com.applitools.eyes.visualgrid.services.RunnerOptions;
import com.applitools.eyes.playwright.visualgrid.VisualGridRunner;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.*;

import java.time.Duration;

public class AcmeBankTests {
    // This JUnit test case class contains everything needed to run a full visual test against the ACME bank site.
    // It runs the test once locally,
    // and then it performs cross-browser testing against multiple unique browsers in Applitools Ultrafast Grid.

    // Test control inputs to read once and share for all tests
    private static String applitoolsApiKey;
    private static boolean headless;

    // Applitools objects to share for all tests
    private static BatchInfo batch;
    private static Configuration config;
    private static VisualGridRunner runner;

    // Test-specific objects
    private static Playwright playwright;
    private static Browser browser;
    private BrowserContext context;
    private Page page;
    private Eyes eyes;

    @BeforeAll
    public static void setUpConfigAndRunner() {
        // This method sets up the configuration for running visual tests in the Ultrafast Grid.
        // The configuration is shared by all tests in a test suite, so it belongs in a `BeforeAll` method.
        // If you have more than one test class, then you should abstract this configuration to avoid duplication.

        // Read the Applitools API key from an environment variable.
        applitoolsApiKey = System.getenv("APPLITOOLS_API_KEY");

        // Read the headless mode setting from an environment variable.
        // Use headless mode for Continuous Integration (CI) execution.
        // Use headed mode for local development.
        headless = Boolean.parseBoolean(System.getenv().getOrDefault("HEADLESS", "true"));

        // Create the runner for the Ultrafast Grid.
        // Concurrency refers to the number of visual checkpoints Applitools will perform in parallel.
        // Warning: If you have a free account, then concurrency will be limited to 1.
        runner = new VisualGridRunner(new RunnerOptions().testConcurrency(5));

        // Create a new batch for tests.
        // A batch is the collection of visual checkpoints for a test suite.
        // Batches are displayed in the Eyes Test Manager, so use meaningful names.
        batch = new BatchInfo("Example: Playwright Java JUnit with the Ultrafast Grid");

        // Create a configuration for Applitools Eyes.
        config = new Configuration();

        // Set the Applitools API key so test results are uploaded to your account.
        // If you don't explicitly set the API key with this call,
        // then the SDK will automatically read the `APPLITOOLS_API_KEY` environment variable to fetch it.
        config.setApiKey(applitoolsApiKey);

        // Set the batch for the config.
        config.setBatch(batch);

        // Add 3 desktop browsers with different viewports for cross-browser testing in the Ultrafast Grid.
        // Other browsers are also available, like Edge and IE.
        config.addBrowser(800, 600, BrowserType.CHROME);
        config.addBrowser(1600, 1200, BrowserType.FIREFOX);
        config.addBrowser(1024, 768, BrowserType.SAFARI);

        // Add 2 mobile emulation devices with different orientations for cross-browser testing in the Ultrafast Grid.
        // Other mobile devices are available, including iOS.
        config.addDeviceEmulation(DeviceName.Pixel_2, ScreenOrientation.PORTRAIT);
        config.addDeviceEmulation(DeviceName.Nexus_10, ScreenOrientation.LANDSCAPE);

        // Start Playwright and launch the browser.
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new com.microsoft.playwright.BrowserType.LaunchOptions().setHeadless(headless));
    }

    @BeforeEach
    public void openBrowserAndEyes(TestInfo testInfo) {
        // This method sets up each test with its own Page and Applitools Eyes objects.

        // Get a new context from the browser
        context = browser.newContext();

        // Create a new page in the context.
        // Creating a new context is not mandatory and a new page can be created from the browser instance.
        // page = browser.newPage();
        page = context.newPage();

        // Create the Applitools Eyes object connected to the VisualGridRunner and set its configuration.
        eyes = new Eyes(runner);
        eyes.setConfiguration(config);

        // Open Eyes to start visual testing.
        // It is a recommended practice to set all four inputs:
        eyes.open(
                page,                                           // Page object to "watch"
                "ACME Bank Web App",                            // The name of the app under test
                testInfo.getDisplayName(),                      // The name of the test case
                new RectangleSize(1024, 768));      // The viewport size for the local browser
    }

    @Test
    public void logIntoBankAccount() {
        // This test covers login for the Applitools demo site, which is a dummy banking app.
        // The interactions use typical Selenium WebDriver calls,
        // but the verifications use one-line snapshot calls with Applitools Eyes.
        // If the page ever changes, then Applitools will detect the changes and highlight them in the Eyes Test Manager.
        // Traditional assertions that scrape the page for text values are not needed here.

        // Load the login page.
        page.navigate("https://demo.applitools.com");

        // Verify the full login page loaded correctly.
        eyes.check(Target.window().fully().withName("Login page"));

        // Perform login.
        page.locator("#username").fill("andy");
        page.locator("#password").fill("i<3pandas");
        page.locator("#log-in").click();

        // Verify the full main page loaded correctly.
        // This snapshot uses LAYOUT match level to avoid differences in closing time text.
        eyes.check(Target.window().fully().withName("Main page").layout());
    }

    @AfterEach
    public void cleanUpTest() {

        // Close Eyes to tell the server it should display the results.
        eyes.closeAsync();

        // Close the page.
        page.close();

        // Warning: `eyes.closeAsync()` will NOT wait for visual checkpoints to complete.
        // You will need to check the Eyes Test Manager for visual results per checkpoint.
        // Note that "unresolved" and "failed" visual checkpoints will not cause the JUnit test to fail.

        // If you want the JUnit test to wait synchronously for all checkpoints to complete, then use `eyes.close()`.
        // If any checkpoints are unresolved or failed, then `eyes.close()` will make the JUnit test fail.
    }

    @AfterAll
    public static void printResults() {

        // Close the Playwright instance.
        playwright.close();

        // Close the batch and report visual differences to the console.
        // Note that it forces JUnit to wait synchronously for all visual checkpoints to complete.
        TestResultsSummary allTestResults = runner.getAllTestResults();
        System.out.println(allTestResults);
    }
}