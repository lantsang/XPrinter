var exec = require('cordova/exec');

exports.coolMethod = function (arg0, success, error) {
    exec(success, error, 'XPrinter', 'coolMethod', [arg0]);
};

exports.connect = function (arg0, success, error) {
    exec(success, error, 'XPrinter', 'connect', [arg0]);
};

exports.printTest = function (title, body, footer, success, error) {
    exec(success, error, 'XPrinter', 'printTest', [title, body, footer]);
};

exports.printImage = function (uri, success, error) {
    exec(success, error, 'XPrinter', 'printImage', [uri]);
};