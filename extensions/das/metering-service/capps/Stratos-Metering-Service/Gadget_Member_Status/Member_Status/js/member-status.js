gadgets.HubSettings.onConnect = function () {
    gadgets.Hub.subscribe('member-status-filter', function (topic, data, subscriberData) {
        clusterId = data['clusterId'];
        applicationId = data['applicationId'];
        timeInterval = data['timeInterval'];
        console.log("Member Filter Value: " + JSON.stringify(data));
    });
};

