<#--
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the"License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an"AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

<@namespace platform="http://clerezza.org/2009/08/platform#" />
<@namespace permission="http://clerezza.org/2008/10/permission#" />
<@namespace sioc="http://rdfs.org/sioc/ns#" />

<!--
<p class="statline ui-state-highlight">There are X amount of users blah blah blah</p>
-->
<br />
<!-- couldn't get all the jQueryUI magic to work, so using onClick() instead -->
<p><button id="create-user" onClick="addUser()"> Create New User </button></p>
<br />
<div id="createUserForm" title="Create New User">
    <p id="validateTips" class="important">* required fields</p>
    <form>
        <fieldset>
            <label for="login">Login <span class="important">*</span></label>
            <input type="text" name="login" id="login" class="text ui-widget-content ui-corner-all" />
            <label for="fullName">Full Name</label>
            <input type="text" name="fullName" id="fullName" class="text ui-widget-content ui-corner-all" />
            <label for="email">Email</label>
            <input type="text" name="email" id="email" value="" class="text ui-widget-content ui-corner-all" />
            <label for="password">Password <span class="important">*</span></label>
            <input type="password" name="password" id="password" value="" class="text ui-widget-content ui-corner-all" />
        </fieldset>


        <fieldset id="roles-checkboxes" class="labelCheckbox">
            <legend>Roles</legend>
            <input type="hidden" id="BasePermissionsRole" name="BasePermissionsRole" value="BasePermissionsRole" />
        </fieldset> 
<br/>
        <fieldset id="permissions-checkboxes" class="labelCheckbox">
            <legend>Direct Permissions</legend>
        </fieldset> 
    </form>
</div>


<!-- #include "*/editUserForm.html" -->

<#include "/html/included.ftl">

<div id="tabs">
    <ul>
        <li><a href="#tabs-users">Users</a></li>
        <li><a href="#tabs-roles">Roles</a></li>
        <li><a href="#tabs-permissions">Permissions</a></li>
    </ul>
    <div id="tabs-users">loading User List</div>
    <div id="tabs-roles">loading Roles</div>
    <div id="tabs-permissions">loading Permissions</div>
</div>


<script>
        
    $(function() {
        $.get("/user-management/rolesCheckboxes",
        function(data){
            $("#roles-checkboxes").html(data);
        }, "text/html");
        
        $.get("/user-management/permissionsCheckboxes",
        function(data){
            $("#permissions-checkboxes").html(data);
        }, "text/html");

   
        $("#createUserForm").dialog({
            autoOpen: false,
            minHeight: 400,
            autoResize:true,
            width: 350,
            modal: true,
            buttons: {
                "Create account": function() {                            
                    var login = $("#login");
                    var fullName = $("#fullName");
                    var email = $("#email");
                    var password = $("#password");
                    var tips = $("#validateTips");
                    var allFields = $([]).add(login).add(fullName).add(email).add(password);
                    allFields.removeClass("ui-state-error");
        
 
                    if(validate(login, email, password)) {
                        var formData = {
                            "login": login.val(), 
                            "fullName": fullName.val(),
                            "email": email.val(),
                            "password": password.val()
                        };
                        
                        // gather role checkbox values into array, to provide format
                        // roles=BasePermissionsRole&roles=CommunityUser etc.
                        var roleList = new Array();
                        var index = 0;
                        var roles = $(".labelCheckbox input");
                        for (var attrname in roles) { 
                            if(roles[attrname].checked) {
                                roleList[index++] = roles[attrname].name;
                            };
                        };
                        formData["roles"] = roleList;
                        
                        $.ajax({
                            type: 'POST',
                            url: '/user-management/create-user',
                            data: formData,
                            success: function(data) {
                                close();
                                location.reload();
                            }
                        });
        
                        $(this).dialog("close");
                    }
                },
                Cancel: function() {
                    // close();
                    $(this).dialog("close");
                }
            } 
        });
        
        function validate(login, email, password) {
            var valid = true;

            valid = valid && checkLength(login,"login", 3, 16);
            valid = valid && checkLength(email,"email", 6, 80);
            valid = valid && checkLength(password,"password", 5, 16);
        
            // From jqueryUI examples, attributed to joern & Scott Gonzalez: http://projects.scottsplayground.com/email_address_validation/
            valid = valid && checkRegexp(login, /^[a-z]([0-9a-z_])+$/i,"Login name may only contain a-z, 0-9, underscores, begin with a letter.");
            valid = valid && checkRegexp(email, /^((([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+(\.([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+)*)|((\x22)((((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(([\x01-\x08\x0b\x0c\x0e-\x1f\x7f]|\x21|[\x23-\x5b]|[\x5d-\x7e]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(\\([\x01-\x09\x0b\x0c\x0d-\x7f]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]))))*(((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(\x22)))@((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?$/i,"eg. person@jexample.com");
            valid = valid && checkRegexp(password, /^([0-9a-zA-Z])+$/,"Password may only contain : a-z 0-9");
            return valid;
        }
     
        function updateTips(t) {
            var tips = $("#validateTips");
            tips
            .text(t)
            .addClass("ui-state-highlight");
            setTimeout(function() {
                tips.removeClass("ui-state-highlight", 1500);
            }, 500);
        }
        
        /*
         * for popup fields 
         */
        function checkLength(o, n, min, max) {
            if(o.val().length > max || o.val().length < min) {
                o.addClass("ui-state-error");
                updateTips(n +" must be between " +
                    min +" and " + max +" characters");
                return false;
            } else {
                return true;
            }
        }

        /*
         * for popup fields 
         */
        function checkRegexp(o, regexp, n) {
            if(!(regexp.test(o.val()))) {
                o.addClass("ui-state-error");
                updateTips(n);
                return false;
            } else {
                return true;
            }
        }
    
        $("#tabs").tabs();
        showUserList();
        showRoleList();
        showPermissionList();
    });
    
    function addUser(){
        $("#createUserForm").dialog("open");
    }


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
    
    function showPermissionList(){  
        $.ajax({
            url: '/user-management/permissions',
            success: function(data) {
                $("div#tabs-permissions").html(data);
                $("#permission-table").tablesorter();
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
        // console.log("Remove user ="+name);

        $("#remove"+name).dialog({
            resizable: false,
            height:140,
            modal: true,
            title: "Delete",
            buttons: {
                "Delete User": function() {
                    console.log("deleting user ="+name); 
                    
                    $.ajax({
                        type: 'POST',
                        url: '/user-management/delete',
                        data: {"user" : name},
                        success: function(data) {
                            close();
                            location.reload();
                        }
                    });
                },
                Cancel: function() {
                    $(this).dialog("close");
                    location.reload();
                }
            }
        });
    }
    

</script>

