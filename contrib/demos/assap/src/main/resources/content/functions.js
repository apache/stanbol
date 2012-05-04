function extractTextContent(element) {
  var element_clone = element.cloneNode(true);
  var script_tags = element_clone.getElementsByTagName("script");
    
  for (var i = 0; i < script_tags.length ; i++) {
	  script_tags[i].innerHTML = "";
  }

	var style_tags = element_clone.getElementsByTagName("style");
  for (var i = 0; i < style_tags.length ; i++) {
	  style_tags[i].innerHTML = "";
  }
	
	element_clone.innerHTML = insertSpacerXHTML(element_clone.innerHTML, "br");
	element_clone.innerHTML = insertSpacerHTML(element_clone.innerHTML, "p");
	element_clone.innerHTML = insertSpacerHTML(element_clone.innerHTML, "div");
	element_clone.innerHTML = insertSpacerHTML(element_clone.innerHTML, "li");
	element_clone.innerHTML = insertSpacerHTML(element_clone.innerHTML, "h1");
	element_clone.innerHTML = insertSpacerHTML(element_clone.innerHTML, "h2");
	element_clone.innerHTML = insertSpacerHTML(element_clone.innerHTML, "h3");
	element_clone.innerHTML = insertSpacerHTML(element_clone.innerHTML, "h4");
	element_clone.innerHTML = insertSpacerHTML(element_clone.innerHTML, "h5");
	element_clone.innerHTML = insertSpacerHTML(element_clone.innerHTML, "h6");
	
	var output = element_clone.textContent;
  output = output.replace(/<\S[^><]*>/g, "");

  output = output.replace(/\n\n/g, " ");
  output = output.replace(/\t/g, " ");
  
	return output;
}

/* Erweitert ein HTML Tagende um einen Zeilenumbruch */
function insertSpacerHTML(input, tag)
{
   input = input.split("</" + tag.toUpperCase() +">").join("</" + tag + ">\n\n");
   return input.split("</" + tag +">").join("</" + tag + ">\n\n");
}

/* Erweitert ein XHTML Tagende um einen Zeilenumbruch */
function insertSpacerXHTML(input, tag)
{
   input = input.split("<" + tag.toUpperCase() +">").join("<" + tag + ">\n");
   input = input.split("<" + tag +">").join("</" + tag + ">\n");
   input = input.split("<" + tag.toUpperCase() +"/>").join("<" + tag + ">\n");
   input = input.split("<" + tag +"/>").join("<" + tag + ">\n");   
   input = input.split("<" + tag.toUpperCase() +" />").join("<" + tag + ">\n");
   return input.split("<" + tag +" />").join("<" + tag + ">\n");   
}