/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*

 Takes className as an argument and 
 makes visible "pageSize" number of  elements starting from startIndex
 which has class attribute in from class="className index"

 */
//Create PAGING object if not exist
if (!this.PAGING) {
	this.PAGING = {};
}
// Create startIndex and pageSize variables if not exist
if (!this.PAGING.startIndex) {
	this.PAGING.startIndex = {};
}

if (!this.PAGING.pageSize) {
	this.PAGING.pageSize = {};
}

(function() {
	PAGING.adjustVisibility = function(className) {
		PAGING.showVisibility(className)
		if (PAGING.hasNext(className)) {
			PAGING.getNextButton(className).removeClass("invisible");
		} else {
			PAGING.getNextButton(className).addClass("invisible");
		}
		if (PAGING.hasPrevious(className)) {
			PAGING.getPreviousButton(className).removeClass("invisible");
		} else {
			PAGING.getPreviousButton(className).addClass("invisible");
		}
	}

	PAGING.gotoNext = function(className) {
		PAGING.startIndex[className] += PAGING.pageSize[className];
		PAGING.adjustVisibility(className);
	}

	PAGING.gotoPrevious = function(className) {
		PAGING.startIndex[className] -= PAGING.pageSize[className];
		if (PAGING.startIndex[className] < 0)
			PAGING.startIndex[className] = 0;
		PAGING.adjustVisibility(className);
	}

	PAGING.count = function(className) {
		var c = $("." + className).length;
		return c;
	}

	PAGING.hasPrevious = function(className) {
		if (PAGING.startIndex[className] > 0) {
			return true;
		} else {
			return false;
		}
	}

	// FIXME - 3 needed because two buttons (next and previous) and combobox
	// also has this class
	// attribute
	PAGING.hasNext = function(className) {
		if (PAGING.count(className) - 3 > PAGING.startIndex[className]
				+ PAGING.pageSize[className]) {
			return true;
		} else {
			return false;
		}
	}

	PAGING.getNextButton = function(className) {
		return $("." + className).filter(".nextButton");
	}

	PAGING.getPreviousButton = function(className) {
		return $("." + className).filter(".previousButton");
	}

	PAGING.showVisibility = function(className) {

		// Initialize 'undefined' parameters
		if (typeof (PAGING.startIndex[className]) == 'undefined') {
			PAGING.startIndex[className] = 0;
		}

		if (typeof (PAGING.pageSize[className]) == 'undefined') {
			PAGING.pageSize[className] = 5;
		}

		var elems = $("." + className);
		elems.each(function() {
			$(this).addClass("invisible");

			for (i = 0; i < PAGING.pageSize[className]; i++) {
				if ($(this).hasClass(PAGING.startIndex[className] + i)) {
					$(this).removeClass("invisible");
					break;
				}
			}
			if ($(this).hasClass("sizeCombobox")) {
				if ($(this).parent().hasClass("margined")) {
					$(this).parent().removeClass("margined");
				}
				if ($(this).hasClass("inline")) {
					$(this).removeClass("inline");
				}
				if (PAGING.hasNext(className) || PAGING.hasPrevious(className)
						|| PAGING.pageSize[className] != 5) {
					$(this).addClass("inline");
					$(this).removeClass("invisible");
					$(this).parent().addClass("margined");
				}
			}
		});
	}
}());
