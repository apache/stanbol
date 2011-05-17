var tutorialPage = 0;
var totalPages = 5;

function Interaction() {
	
}

Interaction.prototype.hide = function(elementID){
	$("#" + elementID + "-body").hide('slow');
	$("#show-" + elementID + "-button").show();
	$("#hide-" + elementID + "-button").hide();
}

Interaction.prototype.show = function(elementID){
	$("#" + elementID + "-body").show('slow');
	$("#show-" + elementID + "-button").hide();
	$("#hide-" + elementID + "-button").show();
	/*
	$("#sytanx-button").unbind('click.show');
	$("#sytanx-button").bind('click.hide', function() {var interaction = new Interaction(); interaction.hide();});
	$('#syntax-button').val('hide');
	alert($('#syntax-button').val());
	*/
}

Interaction.prototype.previousTutorial = function(){

	//set the current page as inactive
	$("#tutorial" + tutorialPage).removeClass("active");
	$("#tutorial" + tutorialPage).addClass("inactive");


	//set the previous page as active
	tutorialPage = tutorialPage - 1;
	
	$("#tutorial" + tutorialPage).removeClass("inactive");
	$("#tutorial" + tutorialPage).addClass("active");
	
	if(tutorialPage == 0){
		$("#previous").hide();
	}
	if(tutorialPage < (totalPages-1)){
		$("#next").show();
	}
}


Interaction.prototype.nextTutorial = function(){

	//set the current page as inactive
	$("#tutorial" + tutorialPage).removeClass("active");
	$("#tutorial" + tutorialPage).addClass("inactive");



	//set the next page as active
	tutorialPage = tutorialPage + 1;
	
	$("#tutorial" + tutorialPage).removeClass("inactive");
	$("#tutorial" + tutorialPage).addClass("active");
	
	if(tutorialPage > 0){
		$("#previous").show();
	}
	
	if(tutorialPage == (totalPages-1)){
		$("#next").hide();
	}
}

Interaction.prototype.getTutorial = function(page){

	
	//set all the pages as inactive
	
	for(var i=0; i<totalPages; i++){
		$("#tutorial" + i).removeClass("active");
		$("#tutorial" + i).addClass("inactive");
	}
	
	//set the wanted page as active
	
	$("#tutorial" + page).removeClass("inactive");
	$("#tutorial" + page).addClass("active");
	
	if(page > 0){
		$("#previous").show();
	}
	
	if(page == (totalPages-1)){
		$("#next").hide();
	}
	
	tutorialPage = page;

}