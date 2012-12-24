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

<!-- needs moving to the nearby CSS resource, but that currently isn't getting loaded -->
<style>
    label, input { display:block; }
    input.text { margin-bottom:12px; width:95%; padding: .4em; }
    fieldset { padding:0; border:0; margin-top:25px; }
    .ui-dialog .ui-state-error { padding: .3em; }
    .validateTips { border: 1px solid transparent; padding: 0.3em; }
</style>

<@namespace platform="http://clerezza.org/2009/08/platform#" />
<@namespace permission="http://clerezza.org/2008/10/permission#" />
<@namespace sioc="http://rdfs.org/sioc/ns#" />

<!--
<p class="statline ui-state-highlight">There are X amount of users blah blah blah</p>
-->
<br />
<p><button id="create-user" onClick="addUser()">Create new user</button></p>
<br />
<div id="dialog-form" title="Create new user">
    <p id="validateTips">All form fields are required.</p>
    <form>
        <fieldset>
            <label for="name">Name</label>
            <input type="text" name="name" id="name" class="text ui-widget-content ui-corner-all" />
            <label for="email">Email</label>
            <input type="text" name="email" id="email" value="" class="text ui-widget-content ui-corner-all" />
            <label for="password">Password</label>
            <input type="password" name="password" id="password" value="" class="text ui-widget-content ui-corner-all" />
        </fieldset>
    </form>
</div>
<!-- -->

<div id="tabs">
    <ul>
        <li><a href="#tabs-users">Users</a></li>
        <li><a href="#tabs-roles">Roles</a></li>
        <li><a href="#tabs-permisions">Permissions</a></li>
    </ul>
    <div id="tabs-users">loading User List</div>
    <div id="tabs-roles">roles</div>
    <div id="tabs-permissions">permissions</div>
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


<script>
            
    $(function() {
        var name = $( "#name" ),
        email = $( "#email" ),
        password = $( "#password" ),
        allFields = $( [] ).add( name ).add( email ).add( password ),
        tips = $( "#validateTips" );
            
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

    function addUser(){
        $( "#dialog-form" ).dialog( "open" );
    }

    function updateTips( t ) {
        tips
        .text( t )
        .addClass( "ui-state-highlight" );
        setTimeout(function() {
            tips.removeClass( "ui-state-highlight", 1500 );
        }, 500 );
    }

    function checkLength( o, n, min, max ) {
        if ( o.val().length > max || o.val().length < min ) {
            o.addClass( "ui-state-error" );
            updateTips( "Length of " + n + " must be between " +
                min + " and " + max + "." );
            return false;
        } else {
            return true;
        }
    }

    function checkRegexp( o, regexp, n ) {
        if ( !( regexp.test( o.val() ) ) ) {
            o.addClass( "ui-state-error" );
            updateTips( n );
            return false;
        } else {
            return true;
        }
    }

    $( "#dialog-form" ).dialog({
        autoOpen: false,
        height: 300,
        width: 350,
        modal: true,
        buttons: {
            "Create account": function() {
                                   
            
                var bValid = true;
                allFields.removeClass( "ui-state-error" );
        
                bValid = bValid && checkLength( name, "username", 3, 16 );
                bValid = bValid && checkLength( email, "email", 6, 80 );
                bValid = bValid && checkLength( password, "password", 5, 16 );
        
                // From jqueryUI examples, attributed to joern & Scott Gonzalez: http://projects.scottsplayground.com/email_address_validation/
                bValid = bValid && checkRegexp( name, /^[a-z]([0-9a-z_])+$/i, "Username may consist of a-z, 0-9, underscores, begin with a letter." );
                bValid = bValid && checkRegexp( email, /^((([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+(\.([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+)*)|((\x22)((((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(([\x01-\x08\x0b\x0c\x0e-\x1f\x7f]|\x21|[\x23-\x5b]|[\x5d-\x7e]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(\\([\x01-\x09\x0b\x0c\x0d-\x7f]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]))))*(((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(\x22)))@((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?$/i, "eg. ui@jquery.com" );
                bValid = bValid && checkRegexp( password, /^([0-9a-zA-Z])+$/, "Password field only allow : a-z 0-9" );
        
                if ( bValid ) {
                    //                    $( "#users tbody" ).append( "<tr>" +
                    //                        "<td>" + name.val() + "</td>" + 
                    //                        "<td>" + email.val() + "</td>" + 
                    //                        "<td>" + password.val() + "</td>" +
                    //                        "</tr>" ); 
                    ///////////////////////////////////////////////
                    var back = ("<div style='float:right;'><href='#' onClick='showUserList()'>&lt;&lt; back to user list</a></div>");    
                    $.ajax({
                        url: '/user-management/user/'+name,
                        success: function(data) {
                            $("div#tabs-users").html(back);
                            $("div#tabs-users").append(data);
                        }
                    });
                    /////////////////////////////////////////
        
                    $( this ).dialog( "close" );
                }
            },
            Cancel: function() {
                $( this ).dialog( "close" );
            }
        },
        close: function() {
            allFields.val( "" ).removeClass( "ui-state-error" );
        }
    });
            

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

