var cordova = require('cordova');

var SandboxFileServerPlugin = function () {};

SandboxFileServerPlugin.prototype.openPrivateFtpServer = function (successCallback, errorCallback, options) {
    if (errorCallback == null) {
        errorCallback = function () {}
    }

    if (typeof errorCallback != "function") {
        console.log("RVTPOWERPlugin.prsentCardView: failure: failure parameter not a function");
        return
    }

    if (typeof successCallback != "function") {
        console.log("RVTPOWERPlugin.prsentCardView: success callback parameter must be a function");
        return
    }

    cordova.exec(successCallback, errorCallback, 'SandboxFileServerPlugin', 'openPrivateFtpServer', [options]);
};

module.exports = new SandboxFileServerPlugin();