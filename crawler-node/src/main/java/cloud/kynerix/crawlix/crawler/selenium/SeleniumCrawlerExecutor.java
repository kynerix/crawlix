package cloud.kynerix.crawlix.crawler.selenium;

import cloud.kynerix.crawlix.content.Content;
import cloud.kynerix.crawlix.content.ContentManager;
import cloud.kynerix.crawlix.crawler.CrawlingJob;
import cloud.kynerix.crawlix.crawler.CrawlingJobsManager;
import cloud.kynerix.crawlix.crawler.CrawlingResults;
import cloud.kynerix.crawlix.crawler.Plugin;
import cloud.kynerix.crawlix.workspaces.Workspace;
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
import java.net.HttpURLConnection;
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
    CrawlingJobsManager crawlingJobsManager;

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

    WebDriver beginCrawling(Plugin plugin) throws Exception {
        WebDriver driver = buildLocalDriver();
        if (driver == null) {
            LOGGER.error("Can't build driver");
            return null;
        }

        int height = plugin.getBrowserHeight();
        int width = plugin.getBrowserWidth();

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

    void parseException(WebDriverException ex, WebDriver driver, CrawlingResults results) {
        results.setSuccess(false);

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

    void analyzeBrowerResponse(WebDriver driver, CrawlingResults results) {
        LOGGER.debug("---- Analysis of initial browser response ----");
        results.setBrowserInfo(collectBrowserInfo(driver));
        results.setBrowserErrorCode(getBrowserReportedErrorCode(driver));

        String reportedLocation = getCurrentLocation(driver);

        int httpCode = HttpURLConnection.HTTP_OK;

        if (reportedLocation != null && reportedLocation.startsWith("chrome-error:") && results.getBrowserErrorCode() == null) {
            httpCode = HttpURLConnection.HTTP_INTERNAL_ERROR;
            results.setError("Unknown browser error: " + reportedLocation);
            results.setErrorDetails(getCurrentBodyText(driver));
            results.setBrowserErrorCode("UNKNOWN");
            results.setSuccess(false);
        } else if (results.getBrowserErrorCode() != null) {
            // Full list at chrome://network-errors/
            if (results.getBrowserErrorCode().startsWith("ERR_NAME")) {
                httpCode = HttpURLConnection.HTTP_NOT_FOUND;
            }
            results.setError("Browser error: " + results.getBrowserErrorCode());
            results.setErrorDetails(getCurrentBodyText(driver));
            results.setSuccess(false);
        } else {
            results.setHttpCode(HttpURLConnection.HTTP_OK);
        }

        LOGGER.debug("----------------------------------------------");
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

    List<CrawlingJob> createURLFoundJobs(Workspace workspace, Map jsonParsedResults, Plugin plugin, boolean persist) {
        List<CrawlingJob> jobs = new ArrayList<>();

        List<Map> urls = (List<Map>) jsonParsedResults.get("_urlsFound");
        if (urls != null && !urls.isEmpty()) {
            for (Map urlObject : urls) {
                String url = normalizeURL((String) urlObject.get("url"));
                String text = (String) urlObject.get("title");
                String pluginKey = (String) urlObject.get("plugin");
                String parent = (String) urlObject.get("parent");
                String action = (String) urlObject.get("action");

                String targetPlugin = pluginKey == null ? plugin.getKey() : pluginKey;

                if (url != null                                                             // URL must be valid
                        && !crawlingJobsManager.isURLVisited(workspace, targetPlugin, url)  // Do not visit the same URL multiple times over a scan
                        && !crawlingJobsManager.existsJob(workspace, targetPlugin, url)     // Do not create the same job for the same URL multiple times
                ) {
                    CrawlingJob job = crawlingJobsManager.newJob(workspace, targetPlugin, url, parent);
                    if (action != null) {
                        job.setAction(action.toUpperCase());
                    }
                    if (text != null) {
                        job.setContext("[" + text + "]");
                    }
                    if (persist) {
                        crawlingJobsManager.save(workspace, job);
                    }
                    jobs.add(job);
                } else {
                    LOGGER.debug("Skipping job creation: " + url);
                }
            }
        }

        return jobs;
    }

    List<Content> createContentFound(Workspace workspace, Map jsonParsedResults, Plugin plugin, boolean persist) {
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
                content.setPlugin(plugin.getKey());

                if (persist) {
                    contentManager.save(workspace, content);
                }

                savedContent.add(content);
            }
        }

        return savedContent;
    }

    public CrawlingResults runCrawler(Workspace workspace, Plugin plugin, CrawlingJob crawlingJob, boolean persistData) {

        String url = crawlingJob == null ? plugin.getDefaultURL() : crawlingJob.getURL();

        WebDriver driver = null;

        CrawlingResults results = new CrawlingResults();
        results.setSuccess(false);
        if (url == null) {
            results.setError("URL is null");
            return results;
        }

        try {

            if (plugin == null) {
                throw new Exception("Plugin " + plugin + " not found");
            }

            results.setUrl(url);
            results.setPlugin(plugin.getKey());
            if (crawlingJob != null) {
                results.setJobId(crawlingJob.getId());
            }

            // Start crawling
            driver = beginCrawling(plugin);

            if (driver == null) {
                results.setError("Failed to create browser driver.");
                results.setSuccess(false);
                return results;
            }

            driver.navigate().to(url);

            waitForLoad();

            // Unfortunately, the WebDriver provides no easy way of retrieving the http code
            // Some basic heuristics to detect not found are needed instead

            analyzeBrowerResponse(driver, results);

            if (results.getHttpCode() != HttpURLConnection.HTTP_OK) {
                return results;
            }

            if (crawlingJob != null && CrawlingJob.ACTION_CHECK.equals(crawlingJob.getAction())) {
                // If the job is just about checking the existence of the page but no parsing, stop here
                results.setSuccess(true);
                return results;
            }

            // Inject context JS (optional), library and plugin JS
            String pluginJS = plugin.getScript() == null ? null : plugin.getScript();
            String libraryURL = JAVASCRIPT_URL + JAVASCRIPT_LIBRARY;
            if (plugin.getContextScript() != null) {
                injectJS(driver, plugin.getContextScript());
            }
            injectJSLibrary(driver, libraryURL, pluginJS);
            waitForProcessing();

            // Save results
            LOGGER.debug("Retrieving results");

            parseBrowserResults(driver, workspace, plugin, persistData, results);

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

        return results;
    }

    private void parseBrowserResults(WebDriver driver, Workspace workspace, Plugin plugin, boolean persistData, CrawlingResults results) {
        String jsonResults = (String) executeJS(driver, "return crawlix._getResults();");

        if (jsonResults != null && jsonResults.trim().length() > 0) {

            try {
                ObjectMapper om = new ObjectMapper();
                Map parsedResults = om.readValue(jsonResults, HashMap.class);
                boolean successParsing = Boolean.parseBoolean(String.valueOf(parsedResults.get("_success")));
                results.setPluginLogs((List<String>) parsedResults.get("_logs"));

                if (successParsing) {
                    List<Content> savedContent = createContentFound(workspace, parsedResults, plugin, persistData);
                    results.setContent(savedContent);

                    // Process parsed results
                    List<CrawlingJob> savedJobs = createURLFoundJobs(workspace, parsedResults, plugin, persistData);
                    results.setCrawlingJobs(savedJobs);
                    results.setSuccess(true);
                }
            } catch (Exception e) {
                LOGGER.error("Error parsing results", e);
            }
        } else {
            results.setError("Error injecting Javascript. Please, check the syntax for plugin " + plugin.getKey());
        }
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
