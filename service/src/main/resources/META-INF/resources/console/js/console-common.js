"use strict";

var dataSet = [];
var adminToken = '00-DEFAULT-ADMIN-TOKEN-00';

function refreshTable() {
    $("#data-table").DataTable().ajax.reload();
}

/* ----------------------------------------------------------------------------------------------------------------------------------------------- */
// Actions handling
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

/* ----------------------------------------------------------------------------------------------------------------------------------------------- */
// Dropdown menus handling
/* ----------------------------------------------------------------------------------------------------------------------------------------------- */
function toogleMenu(clickButton) {
    if (clickButton == null) return;

    let menu = clickButton.parentNode.querySelector("ul");

    if (menu == null) return;

    if (menu.style.visibility == "hidden") {
        menu.style.visibility = "visible";
    } else {
        menu.style.visibility = "hidden";
    }

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
        menuHtml += '<li><a class="pf-c-dropdown__menu-item" href="#" onClick="javascript: hideDropdownMenus(); ' + option.action + '">' + option.title + '</a></li>';
    }

    menuHtml += '</ul></div>';

    return menuHtml;
}

/* ----------------------------------------------------------------------------------------------------------------------------------------------- */

function sendRequest(method, url, callback) {
    $.ajax({
        beforeSend: function (request) {
            request.setRequestHeader("Authorization", adminToken);
        },
        method: method,
        dataType: "json",
        url: url,
        success: function (data) {
            console.log("- Request: " + url);
            console.log("- Response");
            console.log(data);
            console.log("");
            callback(data);
        },
        error: function (request, textStatus, error) {
            operationError(textStatus + " " + error);
        }
    });
}


/* ----------------------------------------------------------------------------------------------------------------------------------------------- */
/* Admin API calls                                                                                                                                 */

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

function deleteWorkspace(workspace, callback = nullworkspace) {
    sendRequest("DELETE", "/admin/delete-workspace?key=" + workspace,
        function (data) { operationResult(data); if (callback) callback(data); }
    );
}

