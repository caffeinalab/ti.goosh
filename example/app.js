var TiGoosh = require('ti.goosh');
TiGoosh.registerForPushNotifications({


    // The callback to invoke when a notification arrives.
    callback: function(e) {

        var data = JSON.parse(e.data || '');
        var notification = JSON.parse(e.notification || '');

    },

    // The callback invoked when you have the device token.
    success: function(e) {

        // Send the e.deviceToken variable to your PUSH server
        Ti.API.log('Notifications: device token is ' + e.deviceToken);

    },

    // The callback invoked on some errors.
    error: function(err) {
        Ti.API.error('Notifications: Retrieve device token failed', err);
    }
});