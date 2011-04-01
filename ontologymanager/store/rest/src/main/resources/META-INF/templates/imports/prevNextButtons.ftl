<#macro prevNextButtons className>
		<div class = "margined">
			<a class="${className} previousButton" href="javascript:;" onClick="javascript: PAGING.gotoPrevious('${className}')">&lt;&lt; Previous</a>
				<div class="show inline ${className} sizeCombobox">
					<p class = "inline">Show</p> 
					<form class=" inline" id="${className}sizeCombobox"> 
				                    <select name="pagesizes" size="1" 
				   onChange="javascript:  var select = document.getElementById('${className}sizeCombobox').elements['pagesizes']; var value = select.options[select.selectedIndex].value;  PAGING.pageSize['${className}'] = parseInt(value); PAGING.adjustVisibility(
				   '${className}')  "> 
				                    <option selected  value="5">5</option> 
				                    <option value="10">10</option> 
				                    <option value="15">15</option>
				                    <option value="20">20</option>
				                    <option value="30">30</option>
				                    <option value="40">40</option>
				                    <option value="50">50</option> 
				                    </select>
			    </div> 
			 </form>
			<a class="${className} nextButton" href="javascript:;" onClick="javascript: PAGING.gotoNext('${className}')">Next &gt;&gt;</a>
			</br>
		</div>
</#macro>