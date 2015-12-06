var exec = require('cordova/exec');

exports.connect = function(args, success, error){
    exec(success, error, "MqTTPlugin", "connect", [args.url, args.clientId, args.cleanSession, args.userName, args.password]);
};

exports.publish = function(args, success, error){
    exec(success, error, "MqTTPlugin", "publish", [args.topicName, args.qos, args.message]);
};

exports.subscribe = function(args, success, error){
    exec(success, error, "MqTTPlugin", "subscribe", [args.topicName, args.qos]);
};

exports.disconnect = function(success, error){
    exec(success, error, "MqTTPlugin", "disconnect", []);
};

exports.onMessage = function(data){

};
