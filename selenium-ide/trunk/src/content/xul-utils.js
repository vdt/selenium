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

var XulUtils = {
    clearChildren: function(e) {
        var i;
        for (i = e.childNodes.length - 1; i >= 0; i--) {
            e.removeChild(e.childNodes[i]);
        }
    },

    appendMenuItem: function(e, attributes) {
		var menuitem = document.createElement("menuitem");
        for (key in attributes) {
            if (attributes[key] != null) {
                menuitem.setAttribute(key, attributes[key]);
            }
        }
		e.appendChild(menuitem);
    },
    
	atomService: Components.classes["@mozilla.org/atom-service;1"].
		getService(Components.interfaces.nsIAtomService)
}