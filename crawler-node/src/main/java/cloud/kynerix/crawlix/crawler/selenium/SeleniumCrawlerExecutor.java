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
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxDriverLogLevel;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.GeckoDriverService;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class SeleniumCrawlerExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeleniumCrawlerExecutor.class.getName());

    @ConfigProperty(name = "crawler.gecko.driver")
    private String GECKO_DRIVER_PATH;

    @ConfigProperty(name = "crawler.firefox.args")
    private String FIREFOX_ARGS;

    @ConfigProperty(name = "crawler.selenium.timeout.sec")
    private long TIMEOUT_SECS;

    @ConfigProperty(name = "crawler.selenium.wait.sec")
    private long WAIT_SECS;

    @ConfigProperty(name = "crawler.local.directory")
    private String LOCAL_DIRECTORY;

    public static final String INJECT_JS_RESOURCE = "/META-INF/resources/crawlix-plugin-api.js";

    private String injectedJS;

    @Inject
    CrawlingJobsManager crawlingJobsManager;

    @Inject
    ContentManager contentManager;

    public SeleniumCrawlerExecutor() {
        this.injectedJS = loadInjectedJS();
        LOGGER.debug("Injected JS snippet");
    }

    String loadInjectedJS() {
        return new Scanner(this.getClass().getResourceAsStream(INJECT_JS_RESOURCE), "UTF-8").useDelimiter("\\A").next();
    }

    public String getInjectedJS(boolean useCachedJS) {
        return useCachedJS ? injectedJS : loadInjectedJS();
    }

    RemoteWebDriver buildLocalDriver() throws Exception {

        System.setProperty(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY, GECKO_DRIVER_PATH);
        System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE, "/dev/null");

        LOGGER.info("Building local driver with " + GECKO_DRIVER_PATH + " and arguments " + FIREFOX_ARGS);

        FirefoxOptions firefoxOptions = new FirefoxOptions();

        firefoxOptions.addArguments(FIREFOX_ARGS);
        firefoxOptions.setLogLevel(FirefoxDriverLogLevel.DEBUG);
        firefoxOptions.addPreference("browser.tabs.remote.autostart", false);
        firefoxOptions.addPreference("security.sandbox.content.level", 5);

        RemoteWebDriver driver = new FirefoxDriver(firefoxOptions);

        LOGGER.info("Local driver created");

        return driver;
    }

    void releaseDriver(RemoteWebDriver driver) {
        try {
            if (driver != null) {
                LOGGER.info("Releasing local driver");
                driver.quit();
                LOGGER.info("Local driver released");
            }
        } catch (Exception e) {
            LOGGER.error("Error releasing driver", e);
        }
    }

    RemoteWebDriver beginCrawling(Plugin plugin) throws Exception {
        RemoteWebDriver driver = buildLocalDriver();

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

    Object executeJS(RemoteWebDriver driver, String js) {
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

    private String escapeCR(String js) {
        return js
                .replaceAll("\\n", " \\\\n")
                .replace("\"", "\\\"");
    }

    void injectJS(RemoteWebDriver driver, String js) {
        LOGGER.debug("Injecting JS");
        String ijs = "var injectedJS = document.createElement('script');\n" +
                "injectedJS.type = 'text/javascript'; \n" +
                "injectedJS.text = \"" + escapeCR(js) + "\";\n" +
                "document.body.appendChild(injectedJS);";
        executeJS(driver, ijs);
    }

    String getBodyHtml(RemoteWebDriver driver) {
        LOGGER.debug("Retrieving body html");
        Object res = executeJS(driver, "var b = document.body; return b.innerHTML;");
        if (res == null)
            return "";
        else
            return String.valueOf(res);
    }

    List<CrawlingJob> createURLFoundJobs(Workspace workspace, Map jsonParsedResults, Plugin plugin, boolean persist) {
        List<CrawlingJob> jobs = new ArrayList<>();

        List<Map> urls = (List<Map>) jsonParsedResults.get("_urlsFound");
        if (urls != null && !urls.isEmpty()) {
            for (Map urlObject : urls) {
                String url = (String) urlObject.get("url");
                String text = (String) urlObject.get("text");
                String pluginKey = (String) urlObject.get("plugin");

                if (url != null) {
                    CrawlingJob job = crawlingJobsManager.newJob(workspace, pluginKey == null ? plugin.getKey() : pluginKey, url);
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

    public CrawlingResults runBrowserCrawler(Workspace workspace, Plugin plugin, CrawlingJob crawlingJob, boolean persistData) {

        String url = crawlingJob == null ? plugin.getDefaultURL() : crawlingJob.getUrl();

        RemoteWebDriver driver = null;

        CrawlingResults results = new CrawlingResults();

        try {
            results.setSuccess(false);

            if (plugin == null) {
                throw new Exception("Plugin " + plugin + " not found");
            }

            results.setUrl(url);
            results.setPlugin(plugin.getKey());
            if (crawlingJob != null) {
                results.setJobId(crawlingJob.getId());
            }

            driver = beginCrawling(plugin);

            driver.navigate().to(url);
            waitForLoad();

            // Inject JS and plugin JS to initialize context
            String customJS = plugin.getScript() == null ? "" : plugin.getScript();
            injectJS(driver, injectedJS + customJS);
            waitForProcessing();

            // Save results
            LOGGER.debug("Retrieving results");
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
            // End of processing

        } catch (Exception e) {
            results.setError("Error loading " + url + " : " + e.getClass().getName() + " : " + e.getMessage());
            LOGGER.error("Error loading " + url, e);
        } finally {
            releaseDriver(driver);
        }

        return results;
    }
}
