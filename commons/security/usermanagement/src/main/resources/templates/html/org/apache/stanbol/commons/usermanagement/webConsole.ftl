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
<@namespace dc="http://purl.org/dc/elements/1.1/" />



<!--
<p class="statline ui-state-highlight">There are X amount of users blah blah blah</p>
-->
<br />

<p>
    <button id="create-user" onClick="addUser()"> Create New User </button>
    <button id="create-role" onClick="addRole()"> Create New Role </button>
</p>
<br />


<div  title="Edit User" id="editUserForm">
</div>
<div  title="Edit Role" id="editRoleForm">
</div>
<!-- #include "/html/editUserForm.ftl" -->

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
        $.ajaxSetup({
            dataType:"html",
            traditional: true //this prevents [] to be added to array parameter name with jquery > 1.4
        }); // set default
        $("#tabs").tabs();
        showUserList();
        showRoleList();
        showPermissionList();
    });
    
    $("#editUserForm").dialog({
        autoOpen: false,
        minHeight: 400,
        autoResize:true,
        width: 500,
        modal: true,
        buttons: {
            "Submit": function() {  
                var newLogin = $("#newLogin");
                var currentLogin = $("#currentLogin");
                var fullName = $("#fullName");
                var email = $("#email");
                var password = $("#password");
                var tips = $("#validateTips");
                var allFields = $([]).add(newLogin).add(fullName).add(email).add(password);
                allFields.removeClass("ui-state-error");
        
                console.log("calling validate");
        
                if(validate(newLogin, email, password)) {
                    var formData = {
                        "currentLogin": currentLogin.val(), 
                        "newLogin": newLogin.val(), 
                        "fullName": fullName.val(),
                        "email": email.val(),
                        "password": password.val()
                        
                    };
                        
                    // gather role checkbox values into array, to provide format
                    // roles=BasePermissionsRole&roles=CommunityUser etc.
                    var roleList = new Array();
                    var index = 0;
                    var roles = $(".role"); // .role,input:checkbox
                    //  console.log("roles = "+roles);
                  
                    for (var attrname in roles) { 
                        console.log("roles[attrname] = "+roles[attrname]);
                        if(roles[attrname].checked) {
                            roleList[index++] = roles[attrname].name;
                        };
                    };
                    roleList[index++] = "BasePermissionsRole";
                    formData["roles"] = roleList;
                    // console.log("ROLES = "+roleList);
                    
                    /////////////
                    var permissionList = new Array();
                    var index = 0;
                    var permissions = $(".checkboxPermission");  
                    // console.log("permissions = "+permissions);
                    for (var attrname in permissions) { 
                        console.log("attrname = "+attrname);
                        if(permissions[attrname].checked) {
                            permissionList[index++] = permissions[attrname].name;
                        };
                    };

                    $(".inputPermission").each(function(){
                        permissionList[index++] = $(this).val();
                    });
            
                    formData["permissions"] = permissionList;
                    // console.log("PERMISSIONS = "+permissionList);
                    
                    $.ajax({
                        type: 'POST',
                        url: '/user-management/store-user',
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
                $(this).dialog("close");
            }
        } 
    });
    
    $("#editRoleForm").dialog({
        autoOpen: false,
        minHeight: 400,
        autoResize:true,
        width: 500,
        modal: true,
        buttons: {
            "Submit": function() {  

               
                var formData = {
                    "roleName": $("#roleName").val(),
                    "comment": $("#comment").val()
                };
                        
                // gather permission checkbox values into array, to provide format
                var permissionList = new Array();
                var index = 0;
                var permissions = $(".checkboxPermission"); // .labelCheckbox 
                // console.log("permissions = "+permissions);
                for (var attrname in permissions) { 
                    // console.log("attrname = "+attrname);
                    if(permissions[attrname].checked) {
                        permissionList[index++] = permissions[attrname].name;
                    };
                };
                $(".inputPermission").each(function(){
                    // alert($(this).val());
                    permissionList[index++] = $(this).val();
                });
                    
                formData["permissions"] = permissionList;
                // console.log("PERMISSIONS = "+permissionList);
                    
                $.ajax({
                    type: 'POST',
                    url: '/user-management/store-role',
                    data: formData,
                    success: function(data) {
                        close();
                        location.reload();
                    }
                });
        
                $(this).dialog("close");
            }
        },
        "Cancel": function() {
            // close();
            $(this).dialog("close");
        }
    } 
);
        
    function validate(login, email, password) {
        //        console.log("validate called");
        //        console.log("login = "+login.val());
        //        console.log("email = "+email.val());
        //        console.log("password = "+password.val());
        var valid = true;

        valid = valid && checkLength(login,"login", 3, 16);
        valid = valid && checkLength(email,"email", 6, 80);
        console.log('$("create-or-edit") = '+$("#create-or-edit"));
        console.log('$("create-or-edit").val() = '+$("#create-or-edit").val());
        console.log('$("create-or-edit").valueOf() = '+$("#create-or-edit").valueOf());
        if($("#create-or-edit").val() != "edit") {
            valid = valid && checkLength(password,"password", 5, 16);
        }
        
        // From jqueryUI examples, attributed to joern & Scott Gonzalez: http://projects.scottsplayground.com/email_address_validation/
        valid = valid && checkRegexp(login, /^[a-z]([0-9a-z_])+$/i,"Login name may only contain a-z, 0-9, underscores, begin with a letter.");
        valid = valid && checkRegexp(email, /^((([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+(\.([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+)*)|((\x22)((((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(([\x01-\x08\x0b\x0c\x0e-\x1f\x7f]|\x21|[\x23-\x5b]|[\x5d-\x7e]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(\\([\x01-\x09\x0b\x0c\x0d-\x7f]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]))))*(((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(\x22)))@((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?$/i,"eg. person@jexample.com");
        // valid = valid && checkRegexp(password, /^([0-9a-zA-Z])+$/,"Password may only contain : a-z 0-9");
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
     *  
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
     * 
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
    
    function addUser(){
        $.ajax({
            url: '/user-management/create-form',
            dataType: 'html',
            success: function(data) {
                $("#editUserForm").html(data);
                $("#editUserForm").title = "Create User"; 
                $("#editUserForm").dialog("open");
                
            }
        });
    }
    
    function addRole(){
        $.ajax({
            url: '/user-management/create-role',
            dataType: 'html',
            success: function(data) {
                $("#editRoleForm").html(data);
                $("#editRoleForm").title = "Create Role"; 
                $("#editRoleForm").dialog("open");
                
            }
        });
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
                console.log(data);
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

    function editUser(userName){
        $.ajax({
            url: '/user-management/users/edit/'+userName,
            dataType: "html",
            success: function(data) {
                $("#editUserForm").html(data);
                $("#password-label").html("<label for='password' id='password-label'>Password (leave blank to retain existing password)</label>");
                

                $.ajax({url: "/user-management/users/"+userName+"/rolesCheckboxes"}).done(
                            function(data){
                                $("#roles-checkboxes").html(data);
                            });
                /* for some reason this cause: Error: cannot call methods on dialog prior to initialization; attempted to call method 'destroy'
                $.get("/user-management/users/"+userName+"/rolesCheckboxes",
                            function(data){
                    $("#roles-checkboxes").html(data);
                }, "text/html");*/
                
                $("#editUserForm").dialog("open");
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
    
    function editRole(roleName){
        $.ajax({
            url: '/user-management/roles/edit/'+roleName,
            dataType: "html",
            success: function(data) {
                $("#editRoleForm").html(data);      
                
                $("#editRoleForm").dialog("open");
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
    
    function addPermissionField(){
        $("#permission-inputs").append("<input type='text' class='inputPermission' /><br />");
    }
</script>