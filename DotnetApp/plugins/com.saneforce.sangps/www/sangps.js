﻿    var exec = require('cordova/exec');

    var sangps = {
        echoGPS: function (param) {
            var success = function (resp) { };
            var error = function (message) { alert("Oopsie! " + message); };
            cordova.exec(success, error, "SANGPS", "Get_Location", [param]);
        },
        intiGPS: function (param) {
            var success = function (resp) { };
            var error = function (message) { alert("Oopsie! " + message); };
            cordova.exec(success, error, "SANGPS", "init", [param]);
        },
        CheckGPS: function (param) {
            var success = function (resp) { };
            var error = function (message) { alert("Oopsie! " + message); };
            cordova.exec(success, error, "SANGPS", "check_gps", [param]);
        },
        getDateTime: function (success) {
            var error = function (message) { alert("Oopsie! " + message); };
            cordova.exec(success, error, "SANGPS", "getdate", []);
        }
    }

    module.exports = sangps;