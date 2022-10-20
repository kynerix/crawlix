/* CrawliX

   Lightweight DSL for common parsing operations in plugins. Feel free to use the full browser API, if needed.
*/

class CrawliX {

    constructor() {
        this._reset();
    }

    _getResults() {
        return JSON.stringify(this);
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

    setFoundContent( arrayOfContents ) {
        this._contentFound = arrayOfContents;
        return this;
    }

    setFoundLinks( arrayOfLinksFound ) {
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
     * Links finding for navigation
     */
    findLinks(cssSelector = "a", plugin = null) {
        let urls = Array.from(document.querySelectorAll(cssSelector));
            
        let linkObjects = urls.map(link => {
                return {
                    url: link.href,
                    title: link.innerText,
                    plugin: plugin
                }
            });

        // Remove duplicates
        linkObjects.filter( (link, index) => {
            for( let i = index+1; i < linkObjects.length; i++ ) {
                if( linkObjects[i].url === link.url) return false;
            }
            return true;
        });

        this.log("findLinks - Found " + linkObjects.length + " links");

        this._urlsFound = this._urlsFound.concat(linkObjects);

        return this;
    }

    filterLinks(hrefInclude = null, hrefExclude = null) {
        
        this.filterLinksExpr(
            link => {
                return (hrefInclude == null || link.url.includes(hrefInclude))
                    && (hrefExclude == null || !link.url.includes(hrefExclude))
            }
        )
        return this;
    }

    filterLinksExpr( evalFunction ) {
        let initialCount = this._urlsFound.length;

        if( evalFunction != null ) {         
            this._urlsFound = this._urlsFound.filter(evalFunction);
        }

        this.log("filterLinks - " + this._urlsFound.length + " links out of " + initialCount);
    }

    // ------------------------------------------------------------------------------------------------------------------------

    /*
     * Single content extraction
     */
    parseAttributeValue(cssSelector, attribute ) {
        return this.parse( cssSelector, attribute);
    }

    parseFirst(cssSelector) {
        return this.parse( cssSelector, null, false);
    }

    parseMultiple(cssSelector) {
        return this.parse( cssSelector, null, true);
    }

    parse(cssSelector, attribute = null, selectMultipleEntities = false) {
        let selectedEntities = Array.from(document.querySelectorAll(cssSelector));
        if( !selectMultipleEntities && selectedEntities.length > 1 ) {
            selectedEntities = [ selectedEntities[0] ];
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
        this._currentContent.forEach( c=> { c[this._currentField] = value;} )
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
                            .filter( v=> {return v;});   // Remove undefined

            joinContent[k] = values.join( this._getSeparator() );
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
}

var crawlix = new CrawliX();

crawlix.log('JS has been injected...');

// End of Crawlix JS injection
// ---------------------------------------------------------------------------------------------------------------------
