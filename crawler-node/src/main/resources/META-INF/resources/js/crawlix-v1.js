/* CrawliX library for common parsing operations in crawler plugins.

   To test your plugin in your local browser, open the Javascript console and paste the following JS snippet:

(function(d, script) {
script = d.createElement('script');
script.type = 'text/javascript';
script.async = true;
// Replace URL, if different from localhost
script.src = 'http://localhost:8079/crawlix/javascript';
d.getElementsByTagName('head')[0].appendChild(script);
}(document));

*/

class CrawliX {

    constructor() {
        this._reset();
    }

    _getResults() {
        return JSON.stringify({
            _logs: this._logs,
            _contentFound: this._contentFound,
            _urlsFound: this._urlsFound,
            _success: this._success,
            _logs: this._logs
        });
    }

    _getSeparator() {
        return (this._optionSeparator != null) ? this._optionSeparator : "";
    }

    _reset() {
        this._urlsFound = [];      // Subsequent discovered navigation path to be crawled
        this._contentFound = [];   // List of consolidated content objects
        this._success = null;      // Report of plugin success

        // Parameters controlling the extract options
        this._optionExtractProperty = 'innerText';      // Extract plain text
        this._optionSeparator = ', ';                   // Separator when joining multiple strings into one

        this._currentField = 'body';                    // Active content field
        this._currentContent = [{}];                    // Active list of contents parsed
        this._logs = [];                                // Plugin logs for later analylis

        this._links = new CrawliXLinks(this);
    }

    links() {
        return this._links;
    }

    linkCount() {
        return this._urlsFound.length;
    }

    contentCount() {
        return this._contentFound.length;
    }

    isSuccessful() {
        return this._success;
    }

    log(msg) {
        console.log("[CrawliX] " + msg);
        this._logs.push(msg);
        return this;
    }

    charCR() {
        return String.fromCharCode(0xA);
    }

    // ------------------------------------------------------------------------------------------------------------------------

    begin() {
        this._reset();
        return this;
    }

    end() {
        if (this._success == null) {
            this._success = true;
        }

        this.log("End of parsing");
        this.log("-----------------------------------------------------------------------------------");
        this.log("** Contents parsed : " + this._contentFound.length);
        this._contentFound.forEach(c => this.log(" - " + JSON.stringify(c)));
        this.log("");

        this.log("** Discovered URLs : " + this._urlsFound.length);
        this._urlsFound.forEach(c => this.log(" - " + JSON.stringify(c)));
        this.log("");

        this.log("** SUCCESS: " + this._success);
        this.log("-----------------------------------------------------------------------------------");

        return this._success;
    }

    setFoundContent(arrayOfContents) {
        this._contentFound = arrayOfContents;
        return this;
    }

    setFoundLinks(arrayOfLinksFound) {
        this._urlsFound = arrayOfLinksFound;
        return this;
    }

    // ------------------------------------------------------------------------------------------------------------------------

    field(activeField) {
        this._currentField = activeField;
        return this;
    }

    body() {
        return this.field("body");
    }

    title() {
        return this.field("title");
    }

    key() {
        return this.field("key");
    }

    summary() {
        return this.field("summary");
    }

    author() {
        return this.field("author");
    }

    url() {
        return this.field("url");
    }

    type() {
        return this.field("type");
    }

    currentContent(index = 0) {
        while (index >= this._currentContent.length) {
            this._currentContent.push({});
        }

        return this._currentContent[index];
    }

    currentValue(index = 0) {
        return this.currentContent(index)[this._currentField];
    }

    // ------------------------------------------------------------------------------------------------------------------------

    /*
     * Single content extraction
     */
    parseAttributeValue(cssSelector, attribute) {
        return this.parse(cssSelector, attribute);
    }

    parseFirst(cssSelector) {
        return this.parse(cssSelector, null, false);
    }

    parseMultiple(cssSelector) {
        return this.parse(cssSelector, null, true);
    }

    parse(cssSelector, attribute = null, selectMultipleEntities = false) {
        let selectedEntities = Array.from(document.querySelectorAll(cssSelector));
        if (!selectMultipleEntities && selectedEntities.length > 1) {
            selectedEntities = [selectedEntities[0]];
        }
        let contentHit = selectedEntities.map(
            // Extract either attribute value or tag content
            c => { return attribute != null ? c.getAttribute(attribute) : c[this._optionExtractProperty] }
        );

        for (let i = 0; i < contentHit.length; i++) {
            this.setValue(contentHit[i], i);
        }

        this.log("parse - field: " + this._currentField + " cssSelector: " + cssSelector + " retrieving: " +
            (attribute != null ? ("attribute " + attribute) : this._optionExtractProperty)
        );
        this.log("parse - hits: " + contentHit.length);

        return this;
    }

    setValue(value, index = 0) {
        this.currentContent(index)[this._currentField] = value;
        return this;
    }

    setValues(value) {
        this._currentContent.forEach(c => { c[this._currentField] = value; })
        return this;
    }

    setOptionExtractText() {
        this._optionExtractProperty = "innerText";
        return this;
    }

    setOptionExtractHtml() {
        this._optionExtractProperty = "innerHTML";
        return this;
    }

    setOptionSeparator(newSeparator) {
        this._optionSeparator = newSeparator;
        return this;
    }

    // ------------------------------------------------------------------------------------------------------------------------

    applyToContents(functionToApply) {
        for (let i = 0; i < this._currentContent.length; i++) {
            let value = this.currentValue(i);
            if (value != null) {
                let newValue = functionToApply(value);
                if (newValue != null) {
                    this.setValue(newValue, i);
                }
            }
        }

        return this;
    }

    cutBetween(left, right) {
        return this.applyToContents(
            value => {
                let index1 = left == null ? 0 : value.indexOf(left);
                let index2 = right == null ? value.length - 1 : value.indexOf(right);

                if (index1 != -1 && index2 != -1) {
                    return value.substring(index1, index2);
                } else {
                    return null;
                }
            }
        );
    }

    trim() {
        return this.applyToContents(
            value => {
                return value.trim();
            }
        );
    }

    appendLeft(leftStr) {
        return this.applyToContents(
            value => {
                return leftStr + value;
            }
        );
    }

    appendRight(rightStr) {
        return this.applyToContents(
            value => {
                return value + rightStr;
            }
        );
    }

    replaceAll(regexp, replaceValue) {
        if (regexp == null) return this;

        if (replaceValue == null) replaceValue = "";

        return this.applyToContents(
            value => {
                return value.replaceAll(regexp, replaceValue);
            }
        );
    }

    split(separator, keepValue = null) {
        if (separator == null) return this;

        return this.applyToContents(
            value => {
                let values = value.split(separator);
                if (keepValue != null && keepValue < values.length) {
                    return values[keepValue];
                } else {
                    return values.join();
                }
            }
        );

    }

    uppercase() {
        return this.applyToContents(
            value => {
                return value.toUpperCase();
            }
        );
    }

    lowercase() {
        return this.applyToContents(
            value => {
                return value.toLowerCase();
            }
        );
    }

    // ------------------------------------------------------------------------------------------------------------------------

    assertNotEmpty() {
        this.log("assertNotEmpty");
        return this.failIf(this.currentValue() == null || this.currentValue().trim().length == 0);
    }

    assertEquals(value) {
        this.log("assertEquals :" + value);
        return this.failIf(value == null && this.currentValue() != null || value !== this.currentValue());
    }

    assertContains(value) {
        this.log("assertContains :" + value);
        return this.failIf(value == null || this.currentValue() == null || this.currentValue().indexOf(value) == -1);
    }

    assertContentCount(n) {
        this.log("assertContentCount = " + n);
        return this.assert(this._contentFound.length == n);
    }

    assertMinContentCount(n) {
        this.log("assertMinContentCount >= " + n);
        return this.assert(this._contentFound.length >= n);
    }

    assert(condition) {
        this.log("assert condition");
        return this.failIf(!condition);
    }

    fail() {
        this.log("fail");
        this.failIf(null);
        return this;
    }

    failIf(conditionToFail) {
        if (conditionToFail == null || conditionToFail) {
            this._success = false;
            this.log("failIf - Assertion failed : " + this._currentField);
        }
        return this;
    }

    // ------------------------------------------------------------------------------------------------------------------------

    addContent() {
        this.log("addContent - #" + this._contentFound.length);

        if (this._currentContent.length > 0) {
            this._contentFound.push(this.currentContent());
            this._currentContent.shift();
        }

        return this;
    }

    joinContents(filterFunction = null) {
        this.log("joinContents - Aggregating " + this._currentContent.length) + " objects into one";

        if (this._currentContent.length < 2) {
            this.log("joinContents - no multiple contents found. Ignoring.");
            return this;
        }

        let joinContent = {};

        let contents = this._currentContent;
        if (filterFunction != null) {
            contents = contents.filter(filterFunction);
        }

        if (contents.length == 0) {
            this.log("joinContents - Filtered ALL contents. Ignoring.");
            return this;
        }

        let keys = Object.keys(contents[0]);

        keys.forEach(k => {
            // Aggregate all values for all objects properties under k
            let values = contents
                .map(v => { return v[k]; })
                .filter(v => { return v; });   // Remove undefined

            joinContent[k] = values.join(this._getSeparator());
        });

        this._currentContent = [joinContent];

        this.addContent();

        return this;
    }

    addContents(filterFunction = null) {
        this.log("addContents - Parsed: " + this._currentContent.length);

        this._currentContent.forEach(
            content => {
                if (filterFunction == null || filterFunction(c)) {
                    this._contentFound.push(content);
                }
            }
        );

        this.log("addContents - Total contents: " + this._contentFound.length);

        this._currentContent = [{}];
            
        return this;
    }

    removeTags(cssSelector) {
        let totalRemoved = 0;
        document.querySelectorAll(cssSelector).forEach(
            node => {
                node.remove();
                totalRemoved++;
            }
        );

        this.log("removeTags - " + cssSelector + " : removed " + totalRemoved);
        return this;
    }
}

// ------------------------------------------------------------------------------------------------------------------------

class CrawliXLinks {
    constructor(crawlix) {
        this.crawlix = crawlix;
        this._currentURLs = [];      // Current list of links found
    }

    find(cssSelector = "a", plugin = null) {
        let urls = Array.from(document.querySelectorAll(cssSelector));

        let linkObjects = urls.map(link => {
            return {
                url: link.href,
                title: link.innerText,
                plugin: plugin,
                parent: location.href,
                action: "parse"
            }
        });

        // Remove duplicates
        linkObjects.filter((link, index) => {
            for (let i = index + 1; i < linkObjects.length; i++) {
                if (linkObjects[i].url === link.url) return false;
            }
            return true;
        });

        this.crawlix.log("findLinks - Found " + linkObjects.length + " links");

        this._currentURLs = this._currentURLs.concat(linkObjects);

        return this;
    }

    include(hrefInclude) {
        return this.filter(hrefInclude);
    }

    exclude(hrefExclude) {
        return this.filter(null, hrefExclude);
    }

    filter(hrefInclude = null, hrefExclude = null) {
        return this.filterByExpr(
            link => {
                return (hrefInclude == null || link.url.includes(hrefInclude))
                    && (hrefExclude == null || !link.url.includes(hrefExclude))
            }
        )
    }

    filterByExpr(evalFunction) {
        let initialCount = this._currentURLs.length;

        if (evalFunction != null) {
            this._currentURLs = this._currentURLs.filter(evalFunction);
        }

        this.crawlix.log("filterLinksExpr - " + this._currentURLs.length + " links out of " + initialCount);
        return this;
    }

    check() {
        return this.action("check");
    }

    analyze() {
        return this.action("parse");
    }

    action(linkAction) {
        this._currentURLs.forEach(link => { link.action = linkAction })
        return this;
    }

    add() {
        this.crawlix.log("Adding " + this._currentURLs.length + " links");
        this.crawlix._urlsFound = this.crawlix._urlsFound.concat(this._currentURLs);
        this._currentURLs = [];
        return this.crawlix;
    }
}

var crawlix = new CrawliX();

crawlix.log('JS has been injected...');

// End of Crawlix JS injection
// ---------------------------------------------------------------------------------------------------------------------
