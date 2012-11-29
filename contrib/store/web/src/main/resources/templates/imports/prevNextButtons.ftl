<#--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
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