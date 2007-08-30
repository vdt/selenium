function Utils() {
}

Utils.newInstance = function(className, interfaceName) {
    var clazz = Components.classes[className];
    var iface = Components.interfaces[interfaceName];
    return clazz.createInstance(iface);
};

Utils.getService = function(className, serviceName) {
    var clazz = Components.classes[className];
    if (clazz == undefined) {
        throw new Exception();
    }

    return clazz.getService(Components.interfaces[serviceName]);
};

Utils.getServer = function() {
    var handle = Utils.newInstance("@thoughtworks.com/webdriver/fxdriver;1", "nsISupports");
    return handle.wrappedJSObject;
}

Utils.getBrowser = function(context) {
    return context.fxbrowser;
};

Utils.getDocument = function(context) {
    return context.fxdocument;
};

function getTextFromNode(node, toReturn, textSoFar, isPreformatted) {
	var children = node.childNodes;

	for (var i = 0; i < children.length; i++) {
		var child = children[i];
		
		// Do we need to collapse the text so far?
		if (child["tagName"] && child.tagName == "PRE") {
			toReturn += collapseWhitespace(textSoFar);
			textSoFar = "";
			var bits = getTextFromNode(child, toReturn, "", true);
			toReturn += bits[1];
			continue;
		}

		// Or is this just plain text?
		if (child.nodeName == "#text") {
			var textToAdd = child.nodeValue;
			textToAdd = textToAdd.replace(new RegExp(String.fromCharCode(160), "gm"), " ");
			textSoFar += textToAdd;
			continue;
		}
		
		// Treat as another child node. 
		var bits = getTextFromNode(child, toReturn, textSoFar, false);
		toReturn = bits[0];
		textSoFar = bits[1];
	}
	
	if (isBlockLevel(node)) {
		if (node["tagName"] && node.tagName != "PRE") {
			toReturn += collapseWhitespace(textSoFar) + "\n";
			textSoFar = "";
		} else {
			toReturn += "\n";
		}
	}
	return [toReturn, textSoFar];
};

function isBlockLevel(node) {
	if (node["tagName"] && node.tagName == "BR")
		return true;

	try {
		// Should we think about getting hold of the current document?
		return "block" == Utils.getStyleProperty(node, "display");
	} catch (e) {
		return false;
	}
}

Utils.getStyleProperty = function(node, propertyName) {
	return node.ownerDocument.defaultView.getComputedStyle(node, null).getPropertyValue(propertyName);
};

function collapseWhitespace(textSoFar) {
	return textSoFar.replace(/\s+/g, " ");
};

function getPreformattedText(node) {
	var textToAdd = "";
	return getTextFromNode(node, "", textToAdd, true)[1];
};

function isWhiteSpace(character) {
	return character == '\n' || character == ' ' || character == '\t' || character == '\r';
}

Utils.getText = function(element) {
	var bits = getTextFromNode(element, "", "", element.tagName == "PRE");
	
	var text = collapseWhitespace(bits[1]) + bits[0];
	var index = text.length - 1;
	while (isWhiteSpace(text[index])) {
		index--;
	}

	return text.slice(0, index+1);
};

Utils.addToKnownElements = function(element, context) {
    var doc = Utils.getDocument(context);
    if (!doc.fxdriver_elements) {
        doc.fxdriver_elements = new Array();
    }
    var start = doc.fxdriver_elements.length;
    doc.fxdriver_elements.push(element);
    return start;
};

Utils.getElementAt = function(index, context) {
    // Convert to a number if we're dealing with a string....
    index = index - 0;

    var doc = Utils.getDocument(context);
    if (doc.fxdriver_elements)
        return doc.fxdriver_elements[index];
    return undefined;
};

Utils.type = function(context, element, text) {
    var isTextField = element["value"];

    var value = "";
    if (isTextField) {
		element.value = value;
	} else {
		element.setAttribute("value", value);
	}
	
    for (var i = 0; i < text.length; i++) {
        var character = text.charAt(i);
        value += character;

        Utils.keyDownOrUp(context, element, true, character);
        Utils.keyPress(context, element, character);
        if (isTextField) {
			element.value = value;
		} else {
          	element.setAttribute("value", value);
		}
        Utils.keyDownOrUp(context, element, false, character);
	}
};

Utils.keyPress = function(context, element, text) {
    var event = Utils.getDocument(context).createEvent('KeyEvents');
    event.initKeyEvent('keypress', true, true, Utils.getBrowser(context).contentWindow, 0, 0, 0, 0, 0, text.charCodeAt(0));
	element.dispatchEvent(event);
};

Utils.keyDownOrUp = function(context, element, down, text) {
    var keyCode = text;
    // We should do something clever with non-text characters

    var event = Utils.getDocument(context).createEvent('KeyEvents');
    event.initKeyEvent(down ? 'keydown' : 'keyup', true, true, Utils.getBrowser(context).contentWindow, 0, 0, 0, 0, keyCode, 0);
    element.dispatchEvent(event);
};

Utils.fireHtmlEvent = function(context, element, eventName) {
    var doc = Utils.getDocument(context);
    var e = doc.createEvent("HTMLEvents");
	e.initEvent("change", true, true);
    element.dispatchEvent(e);
}

Utils.findForm = function(element) {
    // Are we already on an element that can be used to submit the form?
    try {
        element.QueryInterface(Components.interfaces.nsIDOMHTMLButtonElement);
        return element;
    } catch(e) {
    }

    try {
        var input = element.QueryInterface(Components.interfaces.nsIDOMHTMLInputElement);
        if (input.type == "image" || input.type == "submit")
            return input;
    } catch(e) {
    }

    var form = element;
    while (form) {
        if (form["submit"])
            return form;
        form = form.parentNode;
    }
    return undefined;
}

Utils.fireMouseEventOn = function(context, element, eventName) {
    var event = Utils.getDocument(context).createEvent("MouseEvents");
    event.initMouseEvent(eventName, true, true, null, 1, 0, 0, 0, 0, false, false, false, false, 0, null);
    element.dispatchEvent(event);
}

Utils.dump = function(element) {
    dump("=============\n");

    dump("Supported interfaces: ");
    for (var i in Components.interfaces) {
        try {
            var view = element.QueryInterface(Components.interfaces[i]);
            dump(i + ", ");
        } catch (e) {
            // Doesn't support the interface
        }
    }
    dump("\n------------\n");
    var rows = [];
    try {
        Utils.dumpProperties(element, rows);
    } catch (e) {
        dump("caught an exception: " + e);
    }

    rows.sort();
    for (var i in rows) {
        dump(rows[i] + "\n");
    }

    dump("=============\n\n\n");
}

Utils.dumpProperties = function(view, rows) {
    for (var i in view) {
        var value = "\t" + i + ": ";
        try {

            if (typeof(view[i])  == typeof(Function)) {
                value += " function()";
            } else {
                    value += String(view[i]);
            }
        } catch (e) {
            value += " Cannot obtain value";
        }
        rows.push(value);
    }
}

Utils.stackTrace = function() {
    var stack = Components.stack;
    var i = 5;
    while (i && stack.caller) {
        stack = stack.caller;
        dump(stack + "\n");
    }
}
