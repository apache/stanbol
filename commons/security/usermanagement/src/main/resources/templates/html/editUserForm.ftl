
<#assign userName>
    <@ldpath path="platform:userName :: xsd:string"/>
</#assign>	

<div  title="Edit User" id="editUserForm">
    
 
</div>
<script>
    //  method="post" action="/user-management/store-user"
    
    $("#editUserForm").dialog({
        autoOpen: false,
        minHeight: 400,
        autoResize:true,
        width: 350,
        modal: true,
        buttons: {
            "Submit": function() {                            
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
                        console.log("ROLES = "+roleList);
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
                // close();
                $(this).dialog("close");
            }
        } 
    });
</script>