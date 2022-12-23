/* CrawliX library for creating crawler plugins.

   To test your plugin in your local browser, open the Javascript console and paste the following JS snippet.
  
   See examples of plugins at https://github.com/kynerix/crawlix-lib

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

        this._currentContent = [{}];                    // Active list of contents
        this._logs = [];                                // Plugin logs for later analylis
    }

    requiredParam(param, defaultValue = null) {
        try {
            if (typeof eval(param) !== 'undefined') {
                this.log("PARAMETER: '" + param + "' = '" + eval(param) + "'");
                return this;
            }
        } catch (e) {
            // Do nothing
        }

        if (defaultValue != null) {
            this.log("Setting default value to parameter " + param + " = " + defaultValue);
            window[param] = defaultValue;
            return this;
        } else {
            this.fail();
            this.log("REQUIRED parameter '" + param + "' is not defined.");
            return this.end();
        }
    }

    linksParser() {
        return new CrawlixLinksParser(this);
    }

    blocksParser(cssSelector, filterFunction) {
        return new CrawlixBlocksParser(this, cssSelector, filterFunction);
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

        this.log("Discovered Links : " + this._urlsFound.length);
        this._urlsFound.forEach(link => console.log(link));
        this.log("");

        this.log("-----------------------------------------------------------------------------------");
        this.log("Contents parsed : " + this._contentFound.length);
        this._contentFound.forEach(c => console.log(c));
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


    setValue(field, value, index = 0) {
        this._currentContent[index][field] = value;
        return this;
    }

    setValues(field, value) {
        this._currentContent.forEach(c => { c[field] = value; })
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
    // Current content manipulation
    // ------------------------------------------------------------------------------------------------------------------------

    removeIfEmpty(field) {
        return this.removeIf(field, f => {
            return !f;
        })
    }

    removeIf(field, removeFieldFunction) {
        this._currentContent = this._currentContent.filter(
            c => { return !removeFieldFunction(c[field]) }
        );
        return this;
    }

    applyToField(field, functionToApply) {
        for (let i = 0; i < this._currentContent.length; i++) {
            let value = this._currentContent[i][field];
            if (value) {
                this.setValue(field, functionToApply(value), i);
            }
        }

        return this;
    }

    cutBetween(field, left, right) {
        return this.applyToField(
            field,
            value => {
                let index1 = left == null ? 0 : (value.indexOf(left) + left.length);
                let index2 = right == null ? value.length - 1 : value.indexOf(right);

                if (index1 != -1 && index2 != -1) {
                    return value.substring(index1, index2);
                } else {
                    return null;
                }
            }
        );
    }

    trim(field) {
        return this.applyToField(
            field,
            value => {
                return value.trim();
            }
        );
    }

    appendLeft(field, leftStr) {
        return this.applyToField(
            field,
            value => {
                return leftStr + value;
            }
        );
    }

    appendRight(field, rightStr) {
        return this.applyToField(
            field,
            value => {
                return value + rightStr;
            }
        );
    }

    replaceAll(field, regexp, replaceValue) {
        if (regexp == null) return this;

        if (replaceValue == null) replaceValue = "";

        return this.applyToField(
            field,
            value => {
                return value.replaceAll(regexp, replaceValue);
            }
        );
    }

    split(field, separator, keepValue = null) {
        if (separator == null) return this;

        return this.applyToField(
            field,
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

    uppercase(field) {
        return this.applyToField(
            field,
            value => {
                return value.toUpperCase();
            }
        );
    }

    lowercase(field) {
        return this.applyToField(
            field,
            value => {
                return value.toLowerCase();
            }
        );
    }

    // ------------------------------------------------------------------------------------------------------------------------

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
            this._contentFound.push(this._currentContent[0]);
            this._currentContent.shift();
        }

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

    select(cssSelector, filterFunction = null, startNode = null) {
        let hits = [];
        let root = startNode == null ? document : startNode;

        if (cssSelector == null) return hits;

        root.querySelectorAll(cssSelector).forEach(
            c => {
                if (filterFunction == null || filterFunction(c)) {
                    hits.push(c);
                }
            });
        this.log("   · [selector] " + cssSelector + " hits: " + hits.length);
        return hits;
    }

    // ------------------------------------------------------------------------------------------------------------------------

    removeTags(cssSelector, filterFunction = null) {
        let tags = this.select(cssSelector, filterFunction);
        for (let tag of tags) {
            tag.remove();
        }

        this.log("[removeTags] - removed " + tags.length);
        return this;
    }
}

// ------------------------------------------------------------------------------------------------------------------------
// Parses content per block, typically an article, product or post. 

class CrawlixBlocksParser {
    constructor(crawlix, cssSelector, filterFunction = null) {
        this.crawlix = crawlix;
        this._selectors = {};
        this._filters = {};
        this._parseAttr = {};
        this._blockSelector = cssSelector;
        this._blockFilter = filterFunction;
    }

    select(field, cssSelector, parseAttribute = null, filterFunction = null) {
        this._selectors[field] = cssSelector;
        this._filters[field] = filterFunction;
        this._parseAttr[field] = parseAttribute;
        this.crawlix.log("[Blocks] Parsing " + field + " as " + cssSelector);
        return this;
    }

    parse() {
        let blocks = this.crawlix.select(this._blockSelector, this._blockFilter);
        let contentsFound = 0;

        for (let i = 0; i < blocks.length; i++) {
            let block = blocks[i];
            let newContent = {};
            let fields = Object.keys(this._selectors);
            this.crawlix.log("----- Block #" + i + " -----");

            for (let field of fields) {
                let values = this.crawlix.select(
                    this._selectors[field],
                    this._filters[field],
                    block);

                if (values.length > 0) {
                    let parseAttr = this._parseAttr[field];
                    let extractedContents = values.map(
                        // Extract either attribute value or tag content via innerText or others
                        c => { return parseAttr != null ? c.getAttribute(parseAttr) : c[this.crawlix._optionExtractProperty] }
                    );

                    // Append all texts into one
                    newContent[field] = extractedContents.join(this.crawlix._getSeparator());
                }
            }

            if (Object.keys(newContent).length > 0) {
                this.crawlix._currentContent.push(newContent);
                contentsFound++;
            }
        }

        this.crawlix.log("[Blocks] Blocks found: " + blocks.length + " contents: " + contentsFound);

        return this.crawlix;
    }
}

// ------------------------------------------------------------------------------------------------------------------------

class CrawlixLinksParser {
    constructor(crawlix) {
        this.crawlix = crawlix;
        this._currentURLs = [];      // Current list of links found
    }

    find(cssSelector = "a", plugin = null, filterFunction = null) {
        let urls = this.crawlix.select(cssSelector, filterFunction);

        let linkObjects = urls.map(link => {
            return {
                url: link.href,
                title: link.innerText.trim(),
                plugin: plugin,
                parent: location.href,
                action: "parse"
            }
        });

        this.crawlix.log("[link parser] - Found " + linkObjects.length + " links");

        this._currentURLs = this._currentURLs.concat(linkObjects);

        return this;
    }

    include(field, includeStr) {
        this.crawlix.log("[link parser] including links if field '" + field + "' contains '" + includeStr + "'");
        return this.filter(field, includeStr);
    }

    exclude(field, excludeStr) {
        this.crawlix.log("[link parser] excluding links if field '" + field + "' contains '" + excludeStr + "'");
        return this.filter(field, null, excludeStr);
    }

    removeIfEmpty(field) {
        this.crawlix.log("[link parser] removing links if field '" + field + "' is empty");
        return this.filterByExpr(
            link => { return link[field]; }
        );
    }

    removeDuplicates() {
        
        this._currentURLs = this._currentURLs.filter((link, index) => {
            for (let i = index + 1; i < this._currentURLs.length; i++) {
                if (this._currentURLs[i].url === link.url) return false;
            }
            return true;
        });

        this.crawlix.log("[link parser] removed duplicates - size: " + this._currentURLs.length);

        return this;
    }

    filter(field, includeStr = null, excludeStr = null) {
        return this.filterByExpr(
            link => {
                return (includeStr == null || link[field].includes(includeStr))
                    && (excludeStr == null || !link[field].includes(excludeStr))
            }
        );
    }

    filterByExpr(evalFunction) {
        let initialCount = this._currentURLs.length;

        if (evalFunction != null) {
            this._currentURLs = this._currentURLs.filter(evalFunction);
        }

        this.crawlix.log("    · [filter by expr] " + this._currentURLs.length + " links out of " + initialCount);
        return this;
    }

    check() {
        return this._action("check");
    }

    visit() {
        return this._action("parse");
    }

    _action(linkAction) {
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

console.log("*".repeat(80));
console.log('CrawliX JS has been injected');
console.log("*".repeat(80));

