<img src="logo.jpg" />

#### Android Titanium module to work easily with Google Cloud Messaging push notification service... and it's Parse and Parse Server compatible!

## Version compatibility

### v3.* is for Titanium SDK 6.X - branch: release/titanium-6.x

### v4.* is for Titanium SDK 7.X - branch: master

## Install the module

Unzip the latest release in your module directory and add to tiapp modules, or just type:

```
gittio install ti.goosh
```

## Set the sender ID

In your tiapp.xml, insert

```xml
<property name="gcm.senderid">YOUR_SENDER_ID</property>
```

To get GCM sender ID:

* Open the Google Developers Console.
* If you haven't created an API project yet, click Create Project.
* Find your API project and click Project Name.
* You can find Project Number. You will use it as the GCM sender ID.

### Crashes

If your app crashes on launch, try adding these in your tiapp.xml.

```xml
<property name="ti.android.bug2373.disableDetection" type="bool">true</property>
<property name="ti.android.bug2373.finishfalseroot" type="bool">true</property>
<property name="ti.android.bug2373.restartDelay" type="int">500</property>
<property name="ti.android.bug2373.finishDelay" type="int">0</property>
<property name="ti.android.bug2373.skipAlert" type="bool">true</property>
<property name="ti.android.bug2373.message">Initializing</property>
<property name="ti.android.bug2373.title">Restart Required</property>
<property name="ti.android.bug2373.buttonText">Continue</property>
```

## Set the Google Play Services SDK version

In your  `tiapp.xml`, in the node **android > manifest > application**, insert:

```xml
<meta-data android:name="com.google.android.gms.version" android:value="@integer/google_play_services_version" />
```

If you have an error at compile time, please first read this article to clarify your idea about this bug: [https://medium.com/all-titanium/deal-with-titanium-modules-and-its-missing-support-for-android-multidex-546de5486d13#.wfo7v4akx](https://medium.com/all-titanium/deal-with-titanium-modules-and-its-missing-support-for-android-multidex-546de5486d13#.wfo7v4akx).

Once done, visit the `gps-jars` directory in this repository.

## Register for Push notifications

We kept the same syntax of `Ti.Network` for iOS notifications, hope you like this choice :)

The behaviour is the same of iOS:

* if the app is in **background**, the standard OS view for incoming notifications is shown. If you click on that banner, the callback is invoked with the notification payload + the property `inBackground = true`.
* if the app is in **foreground**, nothing is shown and you have to handle manually in the app (you can anyway override this behaviour).

```js
var TiGoosh = require('ti.goosh');
TiGoosh.registerForPushNotifications({


	// The callback to invoke when a notification arrives.
	callback: function(e) {
	
		var data = JSON.parse(e.data || '');
	
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
```

## Unregister

```js
TiGoosh.unregisterForPushNotifications();
```

## Subscribe/unsubscribe to topics
```js
// subscribe

TiGoosh.subscribe({
	topic: "/topics/myTopic",
	success: function(e) {
		console.log("unsub done " + e);
	},
	error: function(e){
		console.error("error")
	}
})

// unsubscribe

TiGoosh.unsubscribe({
	topic: "/topics/myTopic",
	success: function(e) {
		console.log("unsub done " + e);
	},
	error: function(e){
		console.error("error")
	}
})
```
## Are notifications enabled
It will return false if users have disabled notifications from the settings for the app, work from to API 19 (Android 4.4).
Not work for android before API 19 (Android 4.4), return true.

```js
TiGoosh.areNotificationsEnabled();
```
## Usage with Trimethyl

[Trimethyl](http://trimethyl.github.io/trimethyl/) uses this module for its *Notifications* library. [http://trimethyl.github.io/trimethyl/notifications.js.html#line155](http://trimethyl.github.io/trimethyl/notifications.js.html#line155), so you can just type:

```js
Notifications.subscribe();
```

to activate the notifications for both iOS / Android.

## Properties

Property | Type | Description
--- | --- | ----
remoteNotificationsEnabled | Boolean | Check if the notifications are registered with `registerForPushNotifications`.
remoteDeviceUUID | String | Get the device token previously obtained.

## Set the badge

*Due system limitations, currently the badge over the icon is supported only on Samsung and Sony devices. This is why there's no an "Android official method" to draw that badge, but only via private API.*

```js
gcm.setAppBadge(2);
```

## Cancel received notification

Cancel all notifications

```
TiGoosh.cancelAll();
```

Cancel notification By id

```
TiGoosh.cancel(int id);
```

Cancel notification with tag and id

```
TiGoosh.cancelWithTag(String tag, int id);
```

## Set the icon for the tray

The module sets the notification tray icon taking it from `/platform/android/res/drawable-*/notificationicon.png`.

It should be flat (no gradients), white and face-on perspective. 

**NB: You have to generate the icon with all resolutions.**

```
22 × 22 area in 24 × 24 (mdpi)
33 × 33 area in 36 × 36 (hdpi)
44 × 44 area in 48 × 48 (xhdpi)
66 × 66 area in 72 × 72 (xxhdpi)
88 × 88 area in 96 × 96 (xxxhdpi)
```

You can use this script to generate it **once you put** the icon in `drawable-xxxhdpi/notificationicon.png`.

```sh
#!/bin/sh

ICON_SOURCE="app/platform/android/res/drawable-xxxhdpi/notificationicon.png"
if [ -f "$ICON_SOURCE" ]; then
	mkdir -p "app/platform/android/res/drawable-xxhdpi"
	mkdir -p "app/platform/android/res/drawable-xhdpi"
	mkdir -p "app/platform/android/res/drawable-hdpi"
	mkdir -p "app/platform/android/res/drawable-mdpi"
	convert "$ICON_SOURCE" -resize 72x72 "app/platform/android/res/drawable-xxhdpi/notificationicon.png"
	convert "$ICON_SOURCE" -resize 48x48 "app/platform/android/res/drawable-xhdpi/notificationicon.png"
	convert "$ICON_SOURCE" -resize 36x36 "app/platform/android/res/drawable-hdpi/notificationicon.png"
	convert "$ICON_SOURCE" -resize 24x24 "app/platform/android/res/drawable-mdpi/notificationicon.png"
else
	echo "No notificationicon.png found"
fi
```

## Send the notification from your server

All properties must be wrapper with a `data` object.

The payload of the notification is compatible with *Parse server*.

Property | Type | Default | Description
--- | --- | --- | ----
alert | String | `null` | The message to show in the notification center and in the status bar. 
title | String | App Title | The title to show in the notification center.
sound | String | `false` | The sound of the notification. If `true` or `default` the default sound is used, otherwise you can specificy an audio file path located in the app bundle (`/assets/`)
vibrate | Boolean | `false` | Control the vibration of the phone.
vibrate | Array | `null` | This property can also be an array with a pattern. When the notification is received the device will vibrate following that pattern
lights | Object | `null` | This optional property sets the LED light color and frequency. Check out the property description below on how to send it
badge | Number | `null` | The icon on the launchscreen will display this number on the right-top corner if supported.
icon | String | `null` | A URL represting a large icon to show. 
color | String | `null` | Background color of the notification icon
tag | String | `null` | Tag of this notification.
id | Number | `null` | ID of this notification.
force_show_in_foreground | Boolean | `false` | Control if notification must be shown as alert even if app is in foreground.
ongoing | Boolean | `false` | Set whether this is an ongoing notification.
group | String | `null` | Set this notification to be part of a group of notifications sharing the same key. 
group_summary | Boolean | `null` | Sets whether this notification is the main one for it's group
when | Number | `null` | Set the time that the event occurred. Notifications in the panel are sorted by this time.
only_alert_once | Boolean | `null` | Set this flag if you would only like the sound, vibrate and ticker to be played if the notification is not already showing.
big_text | String | `null` | The longer text to be displayed in the big form of the template in place of the content text.
big_text_summary | String | `null` | The first line of text after the detail section in the big form of the template.

### Lights Object

Property | Type | Default | Description
--- | --- | --- | ----
argb | String | `null` | [Required] an ARGB or RGB string of the color to set the LED light to. i.e. `#ff00ff` or `#50ff00ff`
onMs | Number | `null` | [Required] The number of milliseconds you want the light to stay on
offMs | Number | `null` | [Required] The number of milliseconds you want it to stay off. The light will loop between on and off

If any of the above properties are missing, the device default color and frequency will be set


Notes:

* If `alert` is not present, no message is shown in the notification center.
* The pair (`tag`, `id`) identifies this notification from your app to the system, so that pair should be unique within your app. If you call one of the notify methods with a (tag, id) pair that is currently active and a new set of notification parameters, it will be updated.

## A PHP/Shell Example

```php
<?php

define('GOOGLE_KEY', '');

$json = [
  "registration_ids" => [
  "cAU7A9Xo8T8:APA91bF9tkp5XeBmAdQqpemHupAhzXL9RDR727yRl9TEzl3mnQugxhaTi1POYepV74AszvlyN7dxbyJV2VJhW52m0-hWjZIfcIMmy3Zf11y0dHYgBHZ_GeAhhCpviR5CZaMUFcanq7ze"
  ],
  "data"    => [
    "data"  => [
      "alert"     => "Testing " . time(),
      "title"     => "The title",
      "vibrate"   => [100,50,50,50,100],
      "badge"     => 1,
      "tag"       => "APP",
    ]
  ]
];

echo json_encode($json, JSON_PRETTY_PRINT);

echo exec("curl -X POST -H 'Authorization: key=" . GOOGLE_KEY . "' -H 'Content-Type: application/json' --data " . escapeshellarg(json_encode($json)) . ' https://android.googleapis.com/gcm/send');
```

Sending a message to a topic
```php
<?php
	// API access key from Google API's Console
	define('API_ACCESS_KEY', '.......');
	// prep the bundle
	$msg = array
	(
		'body' => 'This is a message sent from my http server',
		'title' => 'From server side',
		'priority' => 'high',
		'sound' => 'default',
		'time_to_live' => 3600
	);
	$fields = array('to' => '/topics/myTopic', 'notification' => $msg);

	$headers = array
	(
		'Authorization: key=' . API_ACCESS_KEY,
		'Content-Type: application/json'
	);

	$ch = curl_init();
	curl_setopt($ch, CURLOPT_URL, 'https://fcm.googleapis.com/fcm/send');
	curl_setopt($ch, CURLOPT_POST, true);
	curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
	curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
	curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
	curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($fields));
	$result = curl_exec($ch);
	curl_close($ch);
	echo $result;
```

## Handle the notification on the app

The payload of the notifications is the same that comes from your server. 

**NB: You have to parse the `data` object on the javascript side:** 

```
function(e) {
	e.data = JSON.parse(e.data);
}
```

The object passed in the `callback` contain also:

#### inBackground

A boolean value indicating if the notification has come when the app was in background, and the user has explicited clicked on the banner.

## LICENSE

MIT.
