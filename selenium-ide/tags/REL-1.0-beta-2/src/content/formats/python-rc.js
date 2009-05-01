/*
 * Format for Selenium Remote Control Python client.
 */

load('remoteControl.js');

this.name = "python-rc";

function testMethodName(testName) {
	return "test_" + underscore(testName);
}

notOperator = function() {
	return "not ";
}

string = function(value) {
	value = value.replace(/\\/g, '\\\\');
	value = value.replace(/\"/g, '\\"');
	value = value.replace(/\r/g, '\\r');
	value = value.replace(/\n/g, '\\n');
	var unicode = false;
	for (var i = 0; i < value.length; i++) {
		if (value.charCodeAt(i) >= 128) {
			unicode = true;
		}
	}
	return (unicode ? 'u' : '') + '"' + value + '"';
}

function assertTrue(expression) {
	return "self.failUnless(" + expression.toString() + ")";
}

function assertFalse(expression) {
	return "self.failIf(" + expression.toString() + ")";
}

function verify(statement) {
	return "try: " + statement + "\n" +
		"except AssertionError, e: self.verificationErrors.append(str(e))";
}

function verifyTrue(expression) {
	return verify(assertTrue(expression));
}

function verifyFalse(expression) {
	return verify(assertFalse(expression));
}

function assignToVariable(type, variable, expression) {
	return variable + " = " + expression.toString();
}

function waitFor(expression) {
	return "for i in range(60):\n" +
		indents(1) + "try:\n" +
        indents(2) + "if " + expression.toString() + ": break\n" +
		indents(1) + "except: pass\n" +
		indents(1) + 'time.sleep(1)\n' +
        'else: self.fail("time out")';
}

function assertOrVerifyFailure(line, isAssert) {
	return "try: " + line + "\n" +
		"except: pass\n" +
		'else: self.fail("expected failure")';
}

Equals.prototype.toString = function() {
	return this.e1.toString() + " == " + this.e2.toString();
}

Equals.prototype.assert = function() {
	return "self.assertEqual(" + this.e1.toString() + ", " + this.e2.toString() + ")";
}

Equals.prototype.verify = function() {
	return verify(this.assert());
}

NotEquals.prototype.toString = function() {
	return this.e1.toString() + " != " + this.e2.toString();
}

NotEquals.prototype.assert = function() {
	return "self.assertNotEqual(" + this.e1.toString() + ", " + this.e2.toString() + ")";
}

NotEquals.prototype.verify = function() {
	return verify(this.assert());
}

RegexpMatch.prototype.toString = function() {
	var str = this.pattern;
	if (str.match(/\"/) || str.match(/\n/)) {
		str = str.replace(/\\/g, "\\\\");
		str = str.replace(/\"/g, '\\"');
		str = str.replace(/\n/g, '\\n');
		return '"' + str + '"';
	} else {
		str = 'r"' + str + '"';
	}
	return "re.search(" + str + ", " + this.expression + ")";
}

EqualsArray.prototype.length = function() {
	return "len(" + this.variableName + ")";
}

EqualsArray.prototype.item = function(index) {
	return this.variableName + "[" + index + "]";
}

function pause(milliseconds) {
	return "time.sleep(" + (parseInt(milliseconds) / 1000) + ")";
}

function echo(message) {
	return "print(" + xlateArgument(message) + ")"
}

function statement(expression) {
	return expression.toString();
}

function array(value) {
	var str = '[';
	for (var i = 0; i < value.length; i++) {
		str += string(value[i]);
		if (i < value.length - 1) str += ", ";
	}
	str += ']';
	return str;
}

CallSelenium.prototype.toString = function() {
	var result = '';
	if (this.negative) {
		result += 'not ';
	}
	if (options.receiver) {
		result += options.receiver + '.';
	}
	result += underscore(this.message);
	result += '(';
	for (var i = 0; i < this.args.length; i++) {
		result += this.args[i];
		if (i < this.args.length - 1) {
			result += ', ';
		}
	}
	result += ')';
	return result;
}

function formatComment(comment) {
	return comment.comment.replace(/.+/mg, function(str) {
			return "# " + str;
		});
}

this.options = {
	receiver: "sel",
	header:
	'from selenium import selenium\n' +
	'import unittest, time, re\n' +
	'\n' +
	'class ${className}(unittest.TestCase):\n' +
	'    def setUp(self):\n' +
	'        self.verificationErrors = []\n' +
	'        self.selenium = selenium("localhost", 4444, "*chrome", "${baseURL}")\n' +
	'        self.selenium.start()\n' +
	'    \n' +
	'    def ${methodName}(self):\n' +
	'        sel = self.selenium\n',
	footer:
	'    \n' +
	'    def tearDown(self):\n' +
	'        self.selenium.stop()\n' +
	'        self.assertEqual([], self.verificationErrors)\n' +
	'\n' +
	'if __name__ == "__main__":\n' +
	'    unittest.main()\n',
    indent:	'4',
	initialIndents: '2'
};

this.configForm = 
	'<description>Variable for Selenium instance</description>' +
	'<textbox id="options_receiver" />' +
	'<description>Header</description>' +
	'<textbox id="options_header" multiline="true" flex="1" rows="4"/>' +
	'<description>Footer</description>' +
	'<textbox id="options_footer" multiline="true" flex="1" rows="4"/>' +
	'<description>Indent</description>' +
	'<menulist id="options_indent"><menupopup>' +
	'<menuitem label="Tab" value="tab"/>' +
	'<menuitem label="1 space" value="1"/>' +
	'<menuitem label="2 spaces" value="2"/>' +
	'<menuitem label="3 spaces" value="3"/>' +
	'<menuitem label="4 spaces" value="4"/>' +
	'<menuitem label="5 spaces" value="5"/>' +
	'<menuitem label="6 spaces" value="6"/>' +
	'<menuitem label="7 spaces" value="7"/>' +
	'<menuitem label="8 spaces" value="8"/>' +
	'</menupopup></menulist>';

