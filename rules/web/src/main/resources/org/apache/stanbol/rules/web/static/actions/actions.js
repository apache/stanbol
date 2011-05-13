function Interaction() {
	
}

Interaction.prototype.hide = function(){
	$('#syntax-body').hide('slow');
	$("#show-sytanx-button").show();
	$("#hide-sytanx-button").hide();
	/*
	$('#syntax-button').attr('value', 'show');
	$("#sytanx-button").unbind('click.hide');
	$("#sytanx-button").bind('click.show', function() {var interaction = new Interaction(); interaction.hide();});
	*/
}

Interaction.prototype.show = function(){
	$('#syntax-body').show('slow');
	$("#show-sytanx-button").hide();
	$("#hide-sytanx-button").show();
	/*
	$("#sytanx-button").unbind('click.show');
	$("#sytanx-button").bind('click.hide', function() {var interaction = new Interaction(); interaction.hide();});
	$('#syntax-button').val('hide');
	alert($('#syntax-button').val());
	*/
}