package cloud.kynerix.crawlix.crawler.selenium;

import cloud.kynerix.crawlix.content.Content;
import cloud.kynerix.crawlix.content.ContentManager;
import cloud.kynerix.crawlix.crawler.CrawlJob;
import cloud.kynerix.crawlix.crawler.CrawlJobsManager;
import cloud.kynerix.crawlix.crawler.CrawlResults;
import cloud.kynerix.crawlix.crawler.Crawler;
import cloud.kynerix.crawlix.workspaces.Workspace;
import cloud.kynerix.crawlix.workspaces.WorkspaceManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ALL")
@ApplicationScoped
public class SeleniumCrawlerExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeleniumCrawlerExecutor.class.getName());

    @ConfigProperty(name = "crawler.chrome.driver.path")
    String CHROME_DRIVER_PATH;

    @ConfigProperty(name = "crawler.chrome.browser.path")
    String CHROME_BROWSER_PATH;

    @ConfigProperty(name = "crawler.browser.headless", defaultValue = "true")
    boolean BROWSER_HEADLESS;

    @ConfigProperty(name = "crawler.selenium.timeout.sec")
    long TIMEOUT_SECS;

    @ConfigProperty(name = "crawler.selenium.wait.sec")
    long WAIT_SECS;

    @ConfigProperty(name = "crawler.javascript.lib.path")
    String JAVASCRIPT_LIBRARY;

    @ConfigProperty(name = "crawler.javascript.lib.uri")
    String JAVASCRIPT_URL;

    @Inject
    CrawlJobsManager crawlJobsManager;

    @Inject
    WorkspaceManager workspaceManager;

    @Inject
    ContentManager contentManager;

    public SeleniumCrawlerExecutor() {
        //  this.injectedJS = loadInjectedJS();
        LOGGER.debug("Injected JS snippet");
    }

    WebDriver buildLocalDriver() {

        WebDriver driver = null;

        LOGGER.info("Building local driver with " + CHROME_DRIVER_PATH + " and arguments ");

        System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_PATH);
        //System.setProperty("webdriver.chrome.logfile", "chromedriver.log");

        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.setHeadless(BROWSER_HEADLESS);
        chromeOptions.setBinary(CHROME_BROWSER_PATH);
        chromeOptions.addArguments("--version", "--disable-web-security", "--ignore-certificate-errors", "--incognito");

        try {
            LOGGER.debug("Creating driver with driver at " + CHROME_DRIVER_PATH + " and browser at " + CHROME_BROWSER_PATH);
            driver = new ChromeDriver(chromeOptions);
        } catch (Exception e) {
            LOGGER.error("Can't build Chrome driver", e);
        }

        LOGGER.info("Local CHROME driver created");

        return driver;
    }

    void releaseDriver(WebDriver driver) {
        try {
            if (driver != null && BROWSER_HEADLESS) {
                LOGGER.info("Releasing local driver");
                driver.quit();
                LOGGER.info("Local driver released");
            }
        } catch (Exception e) {
            LOGGER.error("Error releasing driver", e);
        }
    }

    WebDriver beginCrawling(Crawler crawler) throws Exception {
        WebDriver driver = buildLocalDriver();
        if (driver == null) {
            LOGGER.error("Can't build driver");
            return null;
        }

        int height = crawler.getBrowserHeight();
        int width = crawler.getBrowserWidth();

        LOGGER.debug("Screen size set to : " + width + " x " + height);

        driver.manage().window().setSize(new Dimension(width, height));

        // Wait until page loads
        driver.manage().timeouts().pageLoadTimeout(TIMEOUT_SECS, TimeUnit.SECONDS);

        return driver;
    }

    void waitForLoad() {
        try {
            LOGGER.debug("Waiting " + WAIT_SECS + " sec");
            Thread.sleep(WAIT_SECS * 1000);
        } catch (InterruptedException e) {

        }
    }

    void waitForProcessing() {
        try {
            LOGGER.debug("Waiting 500ms");
            Thread.sleep(500);
        } catch (InterruptedException e) {

        }
    }

    Object executeJS(WebDriver driver, String js) {
        Object result = null;
        try {
            result = ((JavascriptExecutor) driver).executeScript(js);
        } catch (Exception e) {
            LOGGER.error("Error executing script: \n" + js + "\n", e);
        }

        // waitForRefresh(1);
        return result;
    }

    String escapeCR(String js) {
        return js
                .replaceAll("\\n", " \\\\n")
                .replace("\"", "\\\"");
    }

    void injectJS(WebDriver driver, String js) {
        LOGGER.debug("Injecting JS");
        String ijs = "var injectedJS = document.createElement('script');\n" +
                "injectedJS.type = 'text/javascript'; \n" +
                "injectedJS.text = \"" + escapeCR(js) + "\";\n" +
                "document.body.appendChild(injectedJS);";
        executeJS(driver, ijs);
    }

    void injectJSLibrary(WebDriver driver, String jsURL, String onLoadJS) {
        String ijs = "(function(d, script) {\n" +
                "script = d.createElement('script');\n" +
                "script.type = 'text/javascript';\n" +
                "script.async = true;\n" +
                "script.src = '" + jsURL + "';\n";
        if (onLoadJS != null) {
            ijs += "script.onload = function(){\n" +
                    onLoadJS +
                    "\n" +
                    "};\n";
        }

        ijs += "d.getElementsByTagName('head')[0].appendChild(script);\n" +
                "}(document));";

        executeJS(driver, ijs);
    }

    String getCurrentLocation(WebDriver driver) {
        String windowLocation = (String) executeJS(driver, "return window.location.href;");
        LOGGER.debug("Current brower location: " + windowLocation);
        return windowLocation;
    }

    String getCurrentBodyText(WebDriver driver) {
        String bodyText = (String) executeJS(driver, "return document.body.innerText.trim();");
        LOGGER.debug("Current body text: " + bodyText);
        return bodyText;
    }

    String getBrowserReportedErrorCode(WebDriver driver) {
        String errorCode = null;
        errorCode = (String) executeJS(driver, "let e = document.querySelector('error-cod'); return e == null ? null : e.innerText");
        if (errorCode != null) {
            LOGGER.debug("Browser error code: " + errorCode);
        }
        return errorCode;
    }

    String collectBrowserInfo(WebDriver driver) {
        String browserInfo = (String) executeJS(driver, "return [window.clientInformation.userAgent, window.clientInformation.appVersion, window.clientInformation.language, window.clientInformation.platform].join(' | ')");
        LOGGER.debug(browserInfo);
        return browserInfo;
    }

    void parseException(WebDriverException ex, WebDriver driver, CrawlResults results) {
        results.setOutcome(CrawlResults.ERROR);

        String msg = ex.getMessage();

        if (msg != null) {
            // Try to parse error from message
            int left = msg.indexOf("ERR_");
            if (left != -1) {
                int right = msg.indexOf("\n", left);
                if (right != -1) {
                    results.setBrowserErrorCode(msg.substring(left, right));
                    results.setError(results.getBrowserErrorCode() + " at " + driver.getCurrentUrl());
                    results.setErrorDetails(msg);
                }
            }
        }

        if (results.getBrowserErrorCode() == null) {
            results.setBrowserErrorCode("UNKNOWN");
            results.setError("Generic error loading " + driver.getCurrentUrl());
        }
    }

    // Unfortunately, the WebDriver provides no easy way of retrieving the http code
    // Some basic heuristics to detect not found are needed instead
    boolean analyzeBrowserResponse(WebDriver driver, CrawlResults results) {
        LOGGER.debug("---- Analysis of initial browser response ----");
        results.setBrowserInfo(collectBrowserInfo(driver));
        results.setBrowserErrorCode(getBrowserReportedErrorCode(driver));

        try {
            String reportedLocation = getCurrentLocation(driver);

            if (reportedLocation != null && reportedLocation.startsWith("chrome-error:") && results.getBrowserErrorCode() == null) {
                results.setError("Unknown browser error: " + reportedLocation);
                results.setErrorDetails(getCurrentBodyText(driver));
                results.setBrowserErrorCode("UNKNOWN");
                results.setOutcome(CrawlResults.ERROR);
                return false;
            } else if (results.getBrowserErrorCode() != null) {
                // Full list at chrome://network-errors/
                if (results.getBrowserErrorCode().startsWith("ERR_NAME")) {
                    results.setOutcome(CrawlResults.NOT_FOUND);
                } else {
                    results.setOutcome(CrawlResults.ERROR);
                    results.setError("Error: " + results.getBrowserErrorCode());
                    results.setErrorDetails(getCurrentBodyText(driver));
                }
                return false;
            } else {
                return true;
            }
        } finally {
            LOGGER.debug("----------------------------------------------");
        }
    }

    String normalizeURL(String url) {
        if (url == null) return null;

        url = url.trim();

        try {
            String normalizedURL = new URL(url).toExternalForm();
            LOGGER.debug("Normalized url: " + normalizedURL);
            return normalizedURL;
        } catch (MalformedURLException e) {
            LOGGER.error("Invalid URL: '" + url + "'");
        }
        return null;
    }

    List<CrawlJob> createURLFoundJobs(Workspace workspace, Map jsonParsedResults, Crawler crawler, boolean persist) {
        List<CrawlJob> jobs = new ArrayList<>();

        List<Map> urls = (List<Map>) jsonParsedResults.get("_urlsFound");
        if (urls != null && !urls.isEmpty()) {
            for (Map urlObject : urls) {
                String url = normalizeURL((String) urlObject.get("url"));
                String text = (String) urlObject.get("title");
                String crawlerKey = (String) urlObject.get("crawlerKey");
                String parent = (String) urlObject.get("parent");
                String action = (String) urlObject.get("action");

                String targetCrawler = crawlerKey == null ? crawler.getKey() : crawlerKey;

                if (url != null                                                             // URL must be valid
                        && !crawlJobsManager.isURLVisited(workspace, targetCrawler, url)  // Do not visit the same URL multiple times over a scan
                        && !crawlJobsManager.existsJob(workspace, targetCrawler, url)     // Do not create the same job for the same URL multiple times
                ) {
                    CrawlJob job = crawlJobsManager.newJob(workspace, targetCrawler, url, parent);
                    if (action != null) {
                        job.setAction(action.toUpperCase());
                    }
                    if (text != null) {
                        job.setContext("[" + text + "]");
                    }
                    if (persist) {
                        crawlJobsManager.save(workspace, job);
                    }
                    jobs.add(job);
                } else {
                    LOGGER.debug("Skipping job creation: " + url);
                }
            }
        }

        return jobs;
    }

    List<Content> createContentFound(Workspace workspace, Map jsonParsedResults, Crawler crawler, boolean persist) {
        List<Map> contentList = (List<Map>) jsonParsedResults.get("_contentFound");
        List<Content> savedContent = new ArrayList<>();
        if (contentList != null && !contentList.isEmpty()) {
            for (Map contentObject : contentList) {
                // Build new content object
                Content content = new Content();
                content.setTitle((String) contentObject.get("title"));
                content.setBody((String) contentObject.get("body"));
                content.setSummary((String) contentObject.get("summary"));
                content.setUrl((String) contentObject.get("url"));
                content.setAuthor((String) contentObject.get("author"));
                content.setKey((String) contentObject.get("key"));
                content.setType((String) contentObject.get("type"));
                content.setFoundTime(new Date());
                content.setCrawlerKey(crawler.getKey());

                if (persist) {
                    contentManager.save(workspace, content);
                }

                savedContent.add(content);
            }
        }

        return savedContent;
    }


    void parsePluginResults(WebDriver driver, Workspace workspace, Crawler crawler, boolean persistData, CrawlResults results) {
        String jsonResults = (String) executeJS(driver, "return crawlix._getResults();");

        if (jsonResults != null && jsonResults.trim().length() > 0) {

            try {
                ObjectMapper om = new ObjectMapper();
                Map parsedResults = om.readValue(jsonResults, HashMap.class);
                boolean successParsing = Boolean.parseBoolean(String.valueOf(parsedResults.get("_success")));
                results.setCrawlerLogs((List<String>) parsedResults.get("_logs"));

                if (successParsing) {
                    List<Content> savedContent = createContentFound(workspace, parsedResults, crawler, persistData);
                    results.setContent(savedContent);

                    // Process parsed results
                    List<CrawlJob> savedJobs = createURLFoundJobs(workspace, parsedResults, crawler, persistData);
                    results.setCrawlJobs(savedJobs);
                    results.setOutcome(CrawlResults.SUCCESS);
                }
            } catch (Exception e) {
                LOGGER.error("Error parsing results", e);
            }
        } else {
            results.setError("Error injecting Javascript. Please, check the syntax for crawler " + crawler.getKey());
        }
    }

    public CrawlResults executeBrowser(
            String url,
            Crawler crawler,
            boolean runParser,
            boolean persistData) {

        assert url != null;
        assert crawler != null;

        Workspace workspace = workspaceManager.getWorkspaceByKey(crawler.getWorkspaceKey());

        assert workspace != null;

        WebDriver driver = null;

        CrawlResults results = new CrawlResults();
        results.setDate(new Date());
        results.setOutcome(CrawlResults.ERROR);
        results.setUrl(url);
        results.setCrawlerKey(crawler.getKey());

        if (persistData && crawlJobsManager.isURLVisited(workspace, url, crawler.getKey())) {
            results.setOutcome(CrawlResults.ALREADY_VISITED);
        } else {
            try {

                // Start crawling
                driver = beginCrawling(crawler);

                if (driver == null) {
                    results.setError("Failed to create browser driver.");
                    return results;
                }

                driver.navigate().to(url);

                waitForLoad();

                if (!analyzeBrowserResponse(driver, results)) {
                    // Fail now
                    return results;
                }

                if (!runParser) {
                    // If the job is just about checking the existence of the page but no parsing, stop here
                    results.setOutcome(CrawlResults.SUCCESS);
                    return results;
                }

                // Inject context JS (optional), library and crawler JS
                String pluginJS = crawler.getScript() == null ? null : crawler.getScript();
                String libraryURL = JAVASCRIPT_URL + JAVASCRIPT_LIBRARY;
                if (crawler.getContextScript() != null) {
                    injectJS(driver, crawler.getContextScript());
                }
                injectJSLibrary(driver, libraryURL, pluginJS);
                waitForProcessing();

                // Save results
                LOGGER.debug("Retrieving results");

                parsePluginResults(driver, workspace, crawler, persistData, results);

            } catch (WebDriverException e) {
                // CATCH DNS errors and others
                parseException(e, driver, results);
                LOGGER.error("Error loading " + driver.getCurrentUrl(), e);
            } catch (Exception e) {
                results.setError("Unknown error " + url + " : " + e.getClass().getName() + " : " + e.getMessage());
                LOGGER.error("Unknown error " + url, e);
            } finally {
                releaseDriver(driver);
            }
        }

        return results;
    }

    public String getJavascriptLibraryContent() {
        try {
            return new Scanner(this.getClass().getResourceAsStream("/META-INF/resources" + JAVASCRIPT_LIBRARY), StandardCharsets.UTF_8).useDelimiter("\\A").next();
        } catch (Exception e) {
            LOGGER.error("Error retrieving Javascript", e);
            return null;
        }
    }
}
