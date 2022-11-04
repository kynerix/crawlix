"use strict";

var dataSet = [];

function setDataSet(newDataSet) {
    dataSet = newDataSet;
} 

function getDataSet() {
    return dataSet;
}

function sendRequest(method, url, callback) {
    $.ajax({
        beforeSend: function (request) {
            request.setRequestHeader("Authorization", '00-DEFAULT-ADMIN-TOKEN-00');
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
        error: function ( request, textStatus, error) {
            operationError( textStatus + " " + error);
        }
    });
}

function operationResult(data) {
    if( data.success ) {
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
/* Admin API calls                                                                                                                                 */

function startNode(node) {
    sendRequest("PUT", "/admin/start-node?node=" + node,
        function (data) { refreshTable(); operationResult(data); }
    );
}

function stopNode(node) {
    sendRequest("PUT", "/admin/stop-node?node=" + node,
        function (data) { refreshTable(); operationResult(data); }
    );
}

function generateToken(workspace) {
    sendRequest("PUT", "/admin/generate-token?workspace=" + workspace,
        function (data) { refreshTable(); operationResult(data); }
    );
}

function deleteWorkspace(workspace) {
    sendRequest("DELETE", "/admin/delete-workspace?key=" + workspace,
        function (data) { refreshTable(); operationResult(data); }
    );
}
