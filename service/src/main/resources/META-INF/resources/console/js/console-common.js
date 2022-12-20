"use strict";

const TOKEN_KEY_STORAGE = "admintoken";
const USER_KEY_STORAGE = "username";

var _dataSet = [];
var _adminToken = null;
var _userName = null;

/* ----------------------------------------------------------------------------------------------------------------------------------------------- */
// Authentication
/* ----------------------------------------------------------------------------------------------------------------------------------------------- */
function login(user, password) {
    sendRequest("POST", "/admin/auth",
        function (data) {
            if (data.success === false) {
                operationError(data.message);
            } else {
                _updateAdminToken(data.result);
                _updateUserName(user);
                _afterLoginSuccessful();
            }
        },
        // Service parameters
        {
            user: user,
            password: password
        }
    );
}

function logout() {
    _updateAdminToken(null);
    _updateUserName(null);
    _showLoginPage();
}

function _updateAdminToken(newToken) {
    _adminToken = newToken;
    if (newToken != null) {
        sessionStorage.setItem(TOKEN_KEY_STORAGE, newToken);
    } else {
        sessionStorage.removeItem(TOKEN_KEY_STORAGE);
    }
}

function _updateUserName(newUserName) {
    if (newUserName != null) {
        sessionStorage.setItem(USER_KEY_STORAGE, newUserName);
        $("#username").text(newUserName);
    } else {
        sessionStorage.removeItem(USER_KEY_STORAGE);
    }
}

function _showLoginPage() {
    document.location = "/console/login.html";
}

function checkAuthentication() {
    _adminToken = sessionStorage.getItem(TOKEN_KEY_STORAGE);
    _userName = sessionStorage.getItem(USER_KEY_STORAGE);

    if (_adminToken == null || _userName == null) {
        logout();
        return false;
    } else {
        $("#username").text(_userName);
        return true;
    }
}

function _afterLoginSuccessful() {
    document.location = "/console/plugins.html";
}

/* ----------------------------------------------------------------------------------------------------------------------------------------------- */
// Operation results
/* ----------------------------------------------------------------------------------------------------------------------------------------------- */

function operationResult(data) {
    if (data.success) {
        operationSuccess();
    } else {
        operationError(data.message);
    }
}

function operationSuccess() {
    $("#successMsg").show();
    setTimeout(function () { $("#successMsg").hide(); }, 5000);
}

function operationError(msg) {
    $("#errorTxt").text(msg);
    $("#errorMsg").show();
    setTimeout(function () { $("#errorMsg").hide(); }, 5000);
}

function renderDate(dateStr) {
    if (dateStr == null) return "-";
    var d = new Date(dateStr.replace("[UTC]", ""));
    return d.toLocaleString();
}

function hide(elementId) {
    let e = document.getElementById(elementId);
    if (e == null) return;
    e.style.visibility = "hidden";
}

function show(elementId) {
    let e = document.getElementById(elementId);
    if (e == null) return;
    e.style.visibility = "visible";
}

function update(elementId, innerContent)  {
    let e = document.getElementById(elementId);
    if (e == null) return;
    e.innerHTML = innerContent;
}

function toogle(elementId) {
    let e = document.getElementById(elementId);
    if (e == null) return;
    if (e.style.visibility == "hidden") {
        e.style.visibility = "visible";
    } else {
        e.style.visibility = "hidden";
    }
}

/* ----------------------------------------------------------------------------------------------------------------------------------------------- */
// Data table
/* ----------------------------------------------------------------------------------------------------------------------------------------------- */

function refreshTable() {
    $("#data-table").DataTable().ajax.reload();
}

/* ----------------------------------------------------------------------------------------------------------------------------------------------- */
// Dropdown menus
/* ----------------------------------------------------------------------------------------------------------------------------------------------- */

function toogleMenu(clickButton) {
    if (clickButton == null) return;

    let menu = clickButton.parentNode.querySelector("ul");

    if (menu == null) return;

    let visibility = menu.style.visibility == "hidden" ? "visible" : "hidden";

    menu.style.visibility = visibility;
    menu.querySelectorAll(".pf-c-divider").forEach( e=> {e.style.visibility=visibility;})
}

function hideDropdownMenus() {
    document.querySelectorAll(".pf-c-dropdown__menu").forEach(n => { n.style.visibility = "hidden" });
}

function renderMenu(menuOptions) {
    let menuHtml = '<div class="pf-c-dropdown pf-m-expanded">' +
        '<button class="pf-c-dropdown__toggle pf-m-plain" id="dropdown-kebab-expanded-button" aria-expanded="false" type="button" aria-label="Actions" onClick="toogleMenu(this);">' +
        '<i class="fas fa-ellipsis-v" aria-hidden="true"></i></button>' +
        '<ul class="pf-c-dropdown__menu" aria-labelledby="dropdown-kebab-expanded-button" style="visibility:hidden">';

    for (let option of menuOptions) {
        if( option.separator ) {
            menuHtml += "<li class='pf-c-divider' style='visibility:hidden'></li>";
        } else {
            menuHtml += '<li><a class="pf-c-dropdown__menu-item" href="#" onClick="javascript: hideDropdownMenus(); ' + option.action + '">' + option.title + '</a></li>';
        }
    }

    menuHtml += '</ul></div>';

    return menuHtml;
}

function renderIcon(data, readyStatus, errorStatus = [], warnStatus = []) {
    if (data == null) return "";
    
    if (readyStatus.includes(data)) {
        return "<i class='fas fa-check-circle pf-u-success-color-100' aria-hidden='true'></i> ";
    } else if (errorStatus.includes(data)) {
        return "<i class='fas fa-exclamation-circle pf-u-danger-color-100' aria-hidden='true'></i> ";
    } else if (warnStatus.includes(data)) {
        return "<i class='fas fa-exclamation-triangle pf-u-warning-color-100' aria-hidden='true'></i> ";
    }

    return "";
}

/* ----------------------------------------------------------------------------------------------------------------------------------------------- */
// Send request
/* ----------------------------------------------------------------------------------------------------------------------------------------------- */

function sendRequest(method, url, callback, jsonData = null) {
    $.ajax({
        beforeSend: function (request) {
            if (_adminToken) {
                request.setRequestHeader("Authorization", _adminToken);
            }
        },
        contentType: "application/json",
        method: method,
        data: JSON.stringify(jsonData),
        dataType: "json",
        url: url,
        success: function (data) {
            if( data.success == false ) {
                operationError("Error " + request.message);    
            } else {
                callback(data);
            }
        },
        error: function (request, textStatus, error) {
            if (request.status == 403) {
                // Forbidden
                console.log("[Request FORBIDDEN] - Forcing authentication");
                logout();
                //operationError("Error: " + error);
            } else {
                console.log("[Request error] " + error);
                operationError("Error: " + request.status + " " + error);
            }
        }
    });
}

/* ----------------------------------------------------------------------------------------------------------------------------------------------- */
// API calls
/* ----------------------------------------------------------------------------------------------------------------------------------------------- */

function startNode(node, callback = null) {
    sendRequest("PUT", "/admin/start-node?node=" + node,
        function (data) { operationResult(data); if (callback) callback(data); }
    );
}

function stopNode(node, callback = null) {
    sendRequest("PUT", "/admin/stop-node?node=" + node,
        function (data) {
            operationResult(data);
            if (callback) callback(data);
        }
    );
}

function generateToken(workspace, callback = null) {
    sendRequest("PUT", "/admin/generate-token?workspace=" + workspace,
        function (data) { operationResult(data); if (callback) callback(data); }
    );
}

function deleteWorkspace(workspace, callback = null) {
    sendRequest("DELETE", "/admin/delete-workspace?key=" + workspace,
        function (data) { operationResult(data); if (callback) callback(data); }
    );
}

function enablePlugin(workspace, plugin, callback = null) {
    sendRequest("PUT", "/crawlix/" + workspace + "/enable-plugin?plugin=" + plugin,
            function (data) { operationResult(data); if (callback) callback(data); }
    );
}

function disablePlugin(workspace, plugin, callback = null) {
    sendRequest("PUT", "/crawlix/" + workspace + "/disable-plugin?plugin=" + plugin,
            function (data) { operationResult(data); if (callback) callback(data); }
    );
}

function testPlugin(workspace, plugin, callback = null) {
    sendRequest("PUT", "/crawlix/" + workspace + "/execute?plugin=" + plugin,
            function (data) { operationResult(data); if (callback) callback(data); }
    );
}

function executePlugin(workspace, plugin, callback = null) {
    sendRequest("PUT", "/crawlix/" + workspace + "/execute?plugin=" + plugin + "&store-results=true",
            function (data) { operationResult(data); if (callback) callback(data); }
    );
}

function loadPlugin(workspace, plugin, callback = null) {
    sendRequest("GET", "/crawlix/" + workspace + "/get-plugin?plugin=" + plugin,
            function (data) { if (callback) callback(data); }
    );
}