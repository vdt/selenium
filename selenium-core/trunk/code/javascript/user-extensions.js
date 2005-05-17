/*
 * This file contains a convenient location for adding extensions to Selenium, like
 * new actions, checks and locator-strategies.
 * By default, this file does not contain any code, allowing users to place their extension code
 * in a common location, removing the need to modify the Selenium sources, and hopefully assisting
 * with the upgrade process.
 */

/*
// The following examples try to give an indication of how Selenium can be extended with javascript.

// All do* methods on the Selenium prototype are added as actions.
// Eg add a typeRepeated action to Selenium, which types the text twice into a text box.
// The typeTwiceAndWait command will be available automatically
Selenium.prototype.doTypeRepeated = function(locator, text) {
    // All locator-strategies are automatically handled by "findElement"
    var element = this.page().findElement(locator);

    // Create the text to type
    var valueToType = text + text;

    // Replace the element text with the new text
    this.page().replaceText(element, valueToType);
};

// All assert* methods on the Selenium prototype are added as checks.
// Eg add a assertValueRepeated check, that makes sure that the element value
// consists of the supplied text repeated.
// The verify version will be available automatically.
Selenium.prototype.assertValueRepeated = function(locator, text) {
    // All locator-strategies are automatically handled by "findElement"
    var element = this.page().findElement(locator);

    // Create the text to verify
    var expectedValue = text + text;

    // Get the actual element value
    var actualValue = element.value;

    // Make sure the actual value matches the expected
    this.assertMatches(expectedValue, actualValue);
};

// All locateElementBy* methods are added as locator-strategies.
// Eg add a "valuerepeated=" locator, that finds the first element with the supplied value, repeated.
// The "inDocument" is a the document you are searching.
PageBot.prototype.locateElementByValueRepeated = function(text, inDocument) {
    // Create the text to search for
    var expectedValue = text + text;

    // Loop through all elements, looking for ones that have a value === our expected value
    var allElements = inDocument.getElementsByTagName("*");
    for (var i = 0; i < allElements.length; i++) {
        var testElement = allElements[i];
        if (testElement.value && testElement.value === expectedValue) {
            return testElement;
        }
    }
    return null;
};
*/