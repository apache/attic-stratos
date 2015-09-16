gadgets.HubSettings.onConnect = function() {
    gadgets.Hub.subscribe('time-interval-channel', function(topic, data, subscriberData) {
        console.log("Time interval:" + data);
    });
};