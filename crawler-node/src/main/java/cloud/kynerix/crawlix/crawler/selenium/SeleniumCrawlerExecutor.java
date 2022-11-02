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
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxDriverLogLevel;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.GeckoDriverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.net.HttpURLConnection;
import java.util.*;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class SeleniumCrawlerExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeleniumCrawlerExecutor.class.getName());

    @ConfigProperty(name = "crawler.gecko.driver")
    String GECKO_DRIVER_PATH;

    @ConfigProperty(name = "crawler.firefox.args")
    String FIREFOX_ARGS;

    @ConfigProperty(name = "crawler.selenium.timeout.sec")
    long TIMEOUT_SECS;

    @ConfigProperty(name = "crawler.selenium.wait.sec")
    long WAIT_SECS;

    @ConfigProperty(name = "crawler.browser.close", defaultValue = "true")
    boolean CLOSE_BROWSER;

    @ConfigProperty(name = "crawler.javascript.lib.path")
    String JAVASCRIPT_LIBRARY;

    @ConfigProperty(name = "crawler.javascript.lib.url")
    String JAVASCRIPT_URL;

    @Inject
    CrawlingJobsManager crawlingJobsManager;

    @Inject
    ContentManager contentManager;

    public SeleniumCrawlerExecutor() {
        //  this.injectedJS = loadInjectedJS();
        LOGGER.debug("Injected JS snippet");
    }

    WebDriver buildLocalDriver() throws Exception {

        System.setProperty(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY, GECKO_DRIVER_PATH);
        System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE, "/dev/null");

        LOGGER.info("Building local driver with " + GECKO_DRIVER_PATH + " and arguments " + FIREFOX_ARGS);

        FirefoxOptions firefoxOptions = new FirefoxOptions();

        firefoxOptions.addArguments(FIREFOX_ARGS);
        firefoxOptions.setLogLevel(FirefoxDriverLogLevel.DEBUG);
        firefoxOptions.addPreference("browser.tabs.remote.autostart", false);
        firefoxOptions.addPreference("security.sandbox.content.level", 5);

        WebDriver driver = new FirefoxDriver(firefoxOptions);

        LOGGER.info("Local driver created");

        return driver;
    }

    void releaseDriver(WebDriver driver) {
        try {
            if (driver != null && CLOSE_BROWSER) {
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
            Thread.currentThread().sleep(WAIT_SECS * 1000);
        } catch (InterruptedException e) {

        }
    }

    void waitForProcessing() {
        try {
            LOGGER.debug("Waiting 500ms");
            Thread.currentThread().sleep(500);
        } catch (InterruptedException e) {

        }
    }

    Object executeJS(WebDriver driver, String js) {
        String script = js;
        Object result = null;
        try {
            result = ((JavascriptExecutor) driver).executeScript(script);
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

    int analyzeHttpCode(WebDriver driver) {
        String title = driver.getTitle();
        if (title != null && title.contains("404")) {
            return HttpURLConnection.HTTP_NOT_FOUND;
        }

        // TODO: Confirm not found or http code, extend for other codes

        return HttpURLConnection.HTTP_OK;
    }

    List<CrawlingJob> createURLFoundJobs(Workspace workspace, Map jsonParsedResults, Plugin plugin, boolean persist) {
        List<CrawlingJob> jobs = new ArrayList<>();

        List<Map> urls = (List<Map>) jsonParsedResults.get("_urlsFound");
        if (urls != null && !urls.isEmpty()) {
            for (Map urlObject : urls) {
                String url = (String) urlObject.get("url");
                String text = (String) urlObject.get("text");
                String pluginKey = (String) urlObject.get("plugin");
                String parent = (String) urlObject.get("parent");
                String action = (String) urlObject.get("action");

                String targetPlugin = pluginKey == null ? plugin.getKey() : pluginKey;

                if (url != null && !crawlingJobsManager.isURLVisited(workspace, targetPlugin, url)) {
                    CrawlingJob job = crawlingJobsManager.newJob(workspace, targetPlugin, url, parent);
                    if (action != null) {
                        job.setAction(action.toUpperCase());
                    }
                    if (persist) {
                        crawlingJobsManager.save(workspace, job);
                    }
                    jobs.add(job);
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

            driver.navigate().to(url);

            waitForLoad();

            // Unfortunately, the WebDriver provides no easy way of retrieving the http code
            // Some basic heuristics to detect not found are needed instead
            int httpCode = analyzeHttpCode(driver);
            results.setHttpCode(httpCode);

            if (httpCode != HttpURLConnection.HTTP_OK) {
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
            if (e.getMessage() != null && e.getMessage().contains("about:neterror?e=dnsNotFound")) {
                // DNS not found error
                LOGGER.error("DNS Error detected");
                results.setError("DNS error: " + url);
            } else {
                results.setError("Error loading: " + url + " : " + e.getMessage());
            }
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
            return new Scanner(this.getClass().getResourceAsStream("/META-INF/resources" + JAVASCRIPT_LIBRARY), "UTF-8").useDelimiter("\\A").next();
        } catch (Exception e) {
            LOGGER.error("Error retrieving Javascript", e);
            return null;
        }
    }
}
