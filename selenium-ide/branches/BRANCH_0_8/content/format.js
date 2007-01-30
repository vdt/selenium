/*
 * Copyright 2005 Shinya Kasatani
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * FormatCollection: manages collection of formats.
 */

function FormatCollection(options) {
	this.options = options;
	
	this.presetFormats = [new InternalFormat(options, "default", "HTML", "html.js"),
						  new InternalFormat(options, "java-rc", "Java - Selenium RC", "java-rc.js"),
						  new InternalFormat(options, "cs-rc", "C# - Selenium RC", "cs-rc.js"),
						  new InternalFormat(options, "perl-rc", "Perl - Selenium RC", "perl-rc.js"),
						  new InternalFormat(options, "php-rc", "PHP - Selenium RC", "php-rc.js"),
						  new InternalFormat(options, "python-rc", "Python - Selenium RC", "python-rc.js"),
						  new InternalFormat(options, "ruby-rc", "Ruby - Selenium RC", "ruby-rc.js"),
						  new InternalFormat(options, "ruby", "Ruby - Selenium IDE 0.7 (deprecated)", "ruby.js")
						  ];
	this.reloadFormats();
}

FormatCollection.log = FormatCollection.prototype.log = new Log('FormatCollection');

FormatCollection.getFormatDir = function() {
	var formatDir = FileUtils.getProfileDir();
	formatDir.append("selenium-ide-scripts");
	if (!formatDir.exists()) {
		formatDir.create(Components.interfaces.nsIFile.DIRECTORY_TYPE, 0755);
	}
	formatDir.append("formats");
	if (!formatDir.exists()) {
		formatDir.create(Components.interfaces.nsIFile.DIRECTORY_TYPE, 0755);
	}
	return formatDir;
}

FormatCollection.loadUserFormats = function(options) {
	var formatFile = FormatCollection.getFormatDir();
	formatFile.append("index.txt");
	
	if (!formatFile.exists()) {
		return [];
	}
	var text = FileUtils.readFile(formatFile);
	var conv = FileUtils.getUnicodeConverter('UTF-8');
	text = conv.ConvertToUnicode(text);
	var formats = [];
	while (text.length > 0) {
		var r = /^(\d+),(.*)\n?/.exec(text);
		if (r) {
			formats.push(new UserFormat(options, r[1], r[2]));
			text = text.substr(r[0].length);
		} else {
			break;
		}
	}
	return formats;
}

FormatCollection.saveUserFormats = function(formats) {
	var text = '';
	for (var i = 0; i < formats.length; i++) {
		text += formats[i].id + ',' + formats[i].name + "\n";
	}
	var conv = FileUtils.getUnicodeConverter('UTF-8');
	text = conv.ConvertFromUnicode(text);
	
	var formatFile = FormatCollection.getFormatDir();
	formatFile.append("index.txt");
	var stream = FileUtils.openFileOutputStream(formatFile);
	stream.write(text, text.length);
	var fin = conv.Finish();
	if (fin.length > 0) {
		stream.write(fin, fin.length);
	}
	stream.close();
}

FormatCollection.loadFormatter = function(url) {
	const subScriptLoader = Components.classes["@mozilla.org/moz/jssubscript-loader;1"]
	  .getService(Components.interfaces.mozIJSSubScriptLoader);
    
	var format = {};
	format.options = {};
	format.configForm = '';
	format.log = new Log("Format");
	format.playable = true;
	format.remoteControl = false;
	format.load = function(file) {
		subScriptLoader.loadSubScript('chrome://selenium-ide/content/formats/' + file, format);
	}
	for (prop in StringUtils) {
		// copy functions from StringUtils
		format[prop] = StringUtils[prop];
	}
	this.log.debug('loading format from ' + url);
	subScriptLoader.loadSubScript(url, format);
	if (format.configForm && format.configForm.length > 0) {
		function copyElement(doc, element) {
			var copy = doc.createElement(element.nodeName.toLowerCase());
			var atts = element.attributes;
			var i;
			for (i = 0; atts != null && i < atts.length; i++) {
				copy.setAttribute(atts[i].name, atts[i].value);
			}
			var childNodes = element.childNodes;
			for (i = 0; i < childNodes.length; i++) {
				if (childNodes[i].nodeType == 1) { // element
					copy.appendChild(copyElement(doc, childNodes[i]));
				} else if (childNodes[i].nodeType == 3) { // text
					copy.appendChild(doc.createTextNode(childNodes[i].nodeValue));
				}
			}
			return copy;
		}
			
		format.createConfigForm = function(document) {
			var xml = '<vbox id="format-config" xmlns="http://www.mozilla.org/keymaster/gatekeeper/there.is.only.xul">' + format.configForm + '</vbox>';
			var parser = new DOMParser();
			var element = parser.parseFromString(xml, "text/xml").documentElement;
			// If we directly return this element, "permission denied" exception occurs
			// when the user clicks on the buttons or textboxes. I haven't figured out the reason, 
			// but as a workaround I'll just re-create the element and make a deep copy.
			return copyElement(document, element);
		}
	}
	return format;
}


FormatCollection.prototype.reloadFormats = function() {
	this.userFormats = FormatCollection.loadUserFormats(this.options);
	this.formats = this.presetFormats.concat(this.userFormats);
}

FormatCollection.prototype.removeUserFormatAt = function(index) {
	this.userFormats.splice(index, 1);
	this.formats = this.presetFormats.concat(this.userFormats);
}

FormatCollection.prototype.saveFormats = function() {
	FormatCollection.saveUserFormats(this.userFormats);
}

FormatCollection.prototype.selectFormat = function(id) {
	var info = this.findFormat(id);
	if (info) {
		try {
			return info;
		} catch (error) {
			this.log.error("failed to select format: " + id + ", error=" + error);
			return this.formats[0];
		}
	} else {
		//this.log.error("failed to select format: " + id);
		return this.formats[0];
	}
}

FormatCollection.prototype.findFormat = function(id) {
	for (var i = 0; i < this.formats.length; i++) {
		if (id == this.formats[i].id) {
			return this.formats[i];
		}
	}
	return null;
}

FormatCollection.prototype.getDefaultFormat = function() {
	return this.findFormat("default");
}


/*
 * Format
 */

function Format() {
}

Format.TEST_CASE_DIRECTORY_PREF = "testCaseDirectory";
Format.TEST_CASE_EXPORT_DIRECTORY_PREF = "testCaseExportDirectory";

Format.prototype.log = Format.log = new Log('Format');

Format.prototype.getUnicodeConverter = function() {
	return FileUtils.getUnicodeConverter(this.options.encoding);
}

Format.prototype.getFormatter = function() {
	if (!this.formatterCache) {
		this.formatterCache = this.loadFormatter();
		for (name in this.options) {
			var r = new RegExp('formats\.' + this.id + '\.(.*)').exec(name);
			if (r) {
				this.formatterCache.options[r[1]] = this.options[name];
			} else if (name.indexOf('.') < 0) {
				this.formatterCache.options["global." + name] = this.options[name];
			}
		}
	}
	return this.formatterCache;
}

Format.prototype.save = function(testCase) {
	return this.saveAs(testCase, testCase.filename, false);
};

Format.prototype.saveAsNew = function(testCase, exportTest) {
	return this.saveAs(testCase, null, exportTest);
};

Format.prototype.saveAs = function(testCase, filename, exportTest) {
	//log.debug("saveAs: filename=" + filename);
	try {
		var file = null;
		if (filename == null) {
            file = showFilePicker(window, "Save as...",
                                  Components.interfaces.nsIFilePicker.modeSave,
                                  exportTest ? Format.TEST_CASE_EXPORT_DIRECTORY_PREF : Format.TEST_CASE_DIRECTORY_PREF,
                                  function(fp) { return fp.file; });
		} else {
			file = FileUtils.getFile(filename);
		}
		if (file != null) {
			testCase.file = file;
			testCase.filename = file.path;
			testCase.baseFilename = file.leafName;
			testCase.name = file.leafName.replace(/\.\w+$/,'');
			// save the directory so we can continue to load/save files from the current suite?
			var outputStream = Components.classes["@mozilla.org/network/file-output-stream;1"].createInstance( Components.interfaces.nsIFileOutputStream);
			outputStream.init(file, 0x02 | 0x08 | 0x20, 0644, 0);
			var converter = this.getUnicodeConverter();
			var text = converter.ConvertFromUnicode(this.getFormatter().format(testCase, testCase.name, '', true));
			outputStream.write(text, text.length);
			var fin = converter.Finish();
			if (fin.length > 0) {
				outputStream.write(fin, fin.length);
			}
			outputStream.close();
			this.log.info("saved " + file.path);
			testCase.lastModifiedTime = file.lastModifiedTime;
			testCase.clearModified();
			return true;
		} else {
			return false;
		}
	} catch (err) {
		alert("error: " + err);
		return false;
	}
};

Format.prototype.getSourceForTestCase = function(testCase) {
	return this.getFormatter().format(testCase, "New Test", true);
}

Format.prototype.getSourceForCommands = function(commands) {
	return this.getFormatter().formatCommands(commands);
}

Format.prototype.setSource = function(testCase, source) {
	try {
		this.getFormatter().parse(testCase, source);
		testCase.setModified();
	} catch (err) {
		alert("error: " + err);
	}
}

Format.prototype.load = function() {
    var self = this;
    return showFilePicker(window, "Select a File", 
                          Components.interfaces.nsIFilePicker.modeOpen,
                          Format.TEST_CASE_DIRECTORY_PREF,
                          function(fp) { return self.loadFile(fp.file); });
}

Format.prototype.loadFile = function(file, isURL) {
	this.log.debug("start loading: file=" + file);
	
	try {
        var sis;
        if (isURL) {
            sis = FileUtils.openURLInputStream(file);
        } else {
            sis = FileUtils.openFileInputStream(file);
        }
		var text = this.getUnicodeConverter().ConvertToUnicode(sis.read(sis.available()));
		var testCase = new TestCase();
		this.getFormatter().parse(testCase, text);
		
		sis.close();
		testCase.file = file;
        if (!isURL) {
            testCase.lastModifiedTime = file.lastModifiedTime;
            testCase.filename = file.path;
            testCase.baseFilename = file.leafName;
        }
		
		return testCase;
	} catch (err) {
		alert("error: " + err);
		return null;
	}
}


/**
 * Format for preset formats
 */
function InternalFormat(options, id, name, file) {
	this.options = options;
	this.id = id;
	this.name = name;
	this.url = 'chrome://selenium-ide/content/formats/' + file;
}

InternalFormat.prototype = new Format;

InternalFormat.prototype.loadFormatter = function() {
	return FormatCollection.loadFormatter(this.url);
}

InternalFormat.prototype.getSource = function() {
	return FileUtils.readURL(this.url);
}


/**
 * Format created by users
 */
function UserFormat(options, id, name) {
	this.options = options;
	if (id && name) {
		this.id = id;
		this.name = name;
	} else {
		this.id = null;
		this.name = '';
	}
}

UserFormat.prototype = new Format;

UserFormat.prototype.saveFormat = function(source) {
	var formatDir = FormatCollection.getFormatDir();
	var formats = FormatCollection.loadUserFormats(this.options);
	if (!this.id) {
		var entries = formatDir.directoryEntries;
		var max = 0;
		while (entries.hasMoreElements()) {
			var file = entries.getNext().QueryInterface(Components.interfaces.nsIFile);
			var r;
			if ((r = /^(\d+)\.js$/.exec(file.leafName)) != null) {
				var id = parseInt(r[1]);
				if (id > max) max = id;
			}
		}
		max++;
		this.id = '' + max;
		formats.push(this);
	}
	var formatFile = formatDir.clone();
	formatFile.append(this.id + ".js");
	var stream = FileUtils.openFileOutputStream(formatFile);
	stream.write(source, source.length);
	stream.close();

	FormatCollection.saveUserFormats(formats);
}

UserFormat.prototype.getFormatFile = function() {
	var formatDir = FormatCollection.getFormatDir();
	var formatFile = formatDir.clone();
	formatFile.append(this.id + ".js");
	return formatFile;
}

UserFormat.prototype.loadFormatter = function() {
	return FormatCollection.loadFormatter(FileUtils.fileURI(this.getFormatFile()));
}

UserFormat.prototype.getSource = function() {
	if (this.id) {
		return FileUtils.readFile(this.getFormatFile());
	} else {
		return FileUtils.readURL('chrome://selenium-ide/content/formats/blank.js');
	}
}
