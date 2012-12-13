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
<@namespace platform="http://clerezza.org/2009/08/platform#" />
<@namespace permission="http://clerezza.org/2008/10/permission#" />
<@namespace sioc="http://rdfs.org/sioc/ns#" />

<script>
$(function() {
    $("#tabs").tabs();
    showUserList();
    showRoleList();
});

function showUserList(){  
    $.ajax({
        url: '/user-management/users',
        success: function(data) {
            $("div#tabs-users").html(data);
            $("#user-table").tablesorter();
        }
    });
}

function showRoleList(){  
    $.ajax({
        url: '/user-management/roles',
        success: function(data) {
            $("div#tabs-roles").html(data);
            $("#role-table").tablesorter();
        }
    });
}

function editUser(name){
    var back = ("<div style='float:right;'><href='#' onClick='showUserList()'>&lt;&lt; back to user list</a></div>");    
    $.ajax({
        url: '/user-management/user/'+name,
        success: function(data) {
            $("div#tabs-users").html(back);
            $("div#tabs-users").append(data);
        }
    });
}

function removeUser(name){
    $.dialog({
        resizable: false,
        height:140,
        modal: true,
        buttons: {
            "Delete User": function() {
                $( this ).dialog( "close" );
            },
            Cancel: function() {
                $( this ).dialog( "close" );
            }
        }
    });
}



</script>


<p class="statline ui-state-highlight">There are X amount of users blah blah blah</p>
<div id="tabs">
    <ul>
        <li><a href="#tabs-users">Users</a></li>
        <li><a href="#tabs-roles">Roles</a></li>
        <li><a href="#tabs-permisions">Permissions</a></li>
    </ul>
    <div id="tabs-users">loading User List</div>
    <div id="tabs-roles">roles</div>
    <div id="tabs-permisions">permissions</div>
</div>

<!--
                Permssions: 
                <ul>
                    <@ldpath path="fn:sort(permission:hasPermission)">
                        <li class="permission" style="list-style-type: disc;">
                            <@ldpath path="permission:javaPermissionEntry :: xsd:string"/>
                        </li>
                    </@ldpath>
                </ul>
-->