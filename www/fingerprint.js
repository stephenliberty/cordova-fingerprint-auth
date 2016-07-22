function FingerprintAuth() {
}

FingerprintAuth.prototype.show = function (params, successCallback, errorCallback) {


    cordova.exec(
        function (result) {
            if(successCallback) {
                successCallback(result === "OK");
            }
        },
        errorCallback,
        "FingerprintAuth",
        "show",
        []
    );
};

FingerprintAuth.prototype.isAvailable = function (successCallback, errorCallback) {
    cordova.exec(
        function (result) {
            if(successCallback) {
                successCallback(JSON.parse(result));
            }
        },
        errorCallback,
        "FingerprintAuth",
        "isAvailable",
        []
    );
};

FingerprintAuth.prototype.getSupport = function (successCallback, errorCallback) {
    cordova.exec(
        function (result) {
            if(successCallback) {
                successCallback(JSON.parse(result));
            }
        },
        errorCallback,
        "FingerprintAuth",
        "getSupport",
        []
    );
};

FingerprintAuth = new FingerprintAuth();
module.exports = FingerprintAuth;