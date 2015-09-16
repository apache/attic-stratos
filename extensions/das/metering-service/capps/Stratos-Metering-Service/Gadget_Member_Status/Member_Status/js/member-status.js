gadgets.HubSettings.onConnect = function() {
    gadgets.Hub.subscribe('time-interval-channel', function(topic, data, subscriberData) {
        console.log("---------------------------------Time interval:" + data + "---------------------------------------");
    });
};


//gadgets.HubSettings.onConnect = function () {
//    gadgets.Hub.subscribe("time-interval-channel", callback);
//};
///* Add the function that needs to be invoked at the subscriber's end. In this sample the message that is published by the publisher gadget is written in the JavaScript callback function. */
//function callback(topic, obj, subscriberData) {
//    console.log("---------------------------------Time interval:" + obj + "---------------------------------------");
//}