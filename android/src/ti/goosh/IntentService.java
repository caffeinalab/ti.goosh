package ti.goosh;

import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.reflect.Type;
import java.lang.Math;
import java.util.Random;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.util.TiRHelper;

import com.google.android.gms.gcm.GcmListenerService;

public class IntentService extends GcmListenerService {

	private static final String LCAT = "ti.goosh.IntentService";
	private static final AtomicInteger atomic = new AtomicInteger(0);

	@Override
	public void onMessageReceived(String from, Bundle bundle) {
		Log.d(LCAT, "Push notification received from: " + from);
		for (String key : bundle.keySet()) {
			Object value = bundle.get(key);
			Log.d(LCAT, String.format("Notification key : %s => %s (%s)", key, value.toString(), value.getClass().getName()));
		}

		parseNotification(bundle);
	}

	private int getResource(String type, String name) {
		int icon = 0;
		if (name != null) {
			int index = name.lastIndexOf(".");
			if (index > 0) name = name.substring(0, index);
			try {
				icon = TiRHelper.getApplicationResource(type + "." + name);
			} catch (TiRHelper.ResourceNotFoundException ex) {
				Log.e(LCAT, type + "." + name + " not found; make sure it's in platform/android/res/" + type);
			}
		}

		return icon;
	}

	private Bitmap getBitmapFromURL(String src) throws Exception {
		HttpURLConnection connection = (HttpURLConnection)(new URL(src)).openConnection();
		connection.setDoInput(true);
		connection.setUseCaches(false); // Android BUG
		connection.connect();
		return BitmapFactory.decodeStream( new BufferedInputStream( connection.getInputStream() ) );
	}

	private void parseNotification(Bundle bundle) {
		TiGooshModule module = TiGooshModule.getModule();

		Context context = getApplicationContext();
		Boolean appInBackground = !TiApplication.isCurrentActivityInForeground();

		// Flag that determine if the message should be broadcasted to TiGooshModule and call the callback
		Boolean sendMessage = !appInBackground;

		// Flag to show the system alert
		Boolean showNotification = appInBackground;

		// the title and alert
		String title = bundle.getString("title", "");
		String alert = bundle.getString("data",
			TiApplication.getInstance().getAppInfo().getName());

		// get the `data` or fallback for `custom` (OneSignal)
		String jsonData = bundle.getString("data", bundle.getString("custom"));
		JsonObject data = null; // empty json

		try {
			data = (JsonObject) new Gson().fromJson(jsonData, JsonObject.class);
		} catch (Exception ex) {
			Log.e(LCAT, "Error parsing data JSON: " + ex.getMessage());
			data = (JsonObject) new Gson().fromJson("", JsonObject.class);
		}

		// OneSignal does not send as `alert`, but `a` instead.
		if (data.has("alert") == false && data.has("a") == true) {
			data = data.getAsJsonObject("a");
		}

		// overwrite the alert
		if (data.has("title")) {
			title = data.getAsJsonPrimitive("title").getAsString();
		}

		// overwrite the alert
		if (data.has("alert")) {
			alert = data.getAsJsonPrimitive("alert").getAsString();
		}

		if (!appInBackground) {
			if (data.has("force_show_in_foreground")) {
				JsonPrimitive forceShowInForeground = data.getAsJsonPrimitive("force_show_in_foreground");
				showNotification = ((forceShowInForeground.isBoolean() && forceShowInForeground.getAsBoolean() == true));
			} else {
				showNotification = false;
			}
		}

		if (data.has("badge") == true) {
			int badge = data.getAsJsonPrimitive("badge").getAsInt();
			BadgeUtils.setBadge(context, badge);
		}

		if (sendMessage) {
			module.sendMessage(jsonData, appInBackground);
		}

		if (showNotification) {
			Log.w(LCAT, "Show Notification: TRUE");

			Intent notificationIntent = new Intent(this, PushHandlerActivity.class);
			notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
			notificationIntent.putExtra(TiGooshModule.INTENT_EXTRA, jsonData);

			PendingIntent contentIntent = PendingIntent.getActivity(this, new Random().nextInt(), notificationIntent, PendingIntent.FLAG_ONE_SHOT);

			// Start building notification

			NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
			int builder_defaults = 0;
			builder.setContentIntent(contentIntent);
			builder.setAutoCancel(true);
			builder.setPriority(2);

			// Title
			builder.setContentTitle(title);

			// alert
			builder.setContentText(alert);
			builder.setTicker(alert);

			// BigText
			if (data.has("big_text")) {
				NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
				bigTextStyle.bigText(data.getAsJsonPrimitive("big_text").getAsString());

				if (data.has("big_text_summary")) {
					bigTextStyle.setSummaryText( data.getAsJsonPrimitive("big_text_summary").getAsString() );
				}

				builder.setStyle(bigTextStyle);
			}

			// Icons
			try {
				int smallIcon = this.getResource("drawable", "notificationicon");
				if (smallIcon > 0) {
					builder.setSmallIcon(smallIcon);
				}
			} catch (Exception ex) {
				Log.e(LCAT, "Smallicon exception: " + ex.getMessage());
			}

			// Large icon
			if (data.has("icon")) {
				try {
					Bitmap icon = this.getBitmapFromURL( data.getAsJsonPrimitive("icon").getAsString() );
					builder.setLargeIcon(icon);
				} catch (Exception ex) {
					Log.e(LCAT, "Icon exception: " + ex.getMessage());
				}
			}

			// Color
			if (data.has("color")) {
				try {
					int color = Color.parseColor( data.getAsJsonPrimitive("color").getAsString() );
					builder.setColor( color );
				} catch (Exception ex) {
					Log.e(LCAT, "Color exception: " + ex.getMessage());
				}
			}

			// Badge
			if (data.has("badge")) {
				int badge = data.getAsJsonPrimitive("badge").getAsInt();
				BadgeUtils.setBadge(context, badge);
				builder.setNumber(badge);
			}

			// Sound
			if (data.has("sound")) {
				JsonPrimitive sound = data.getAsJsonPrimitive("sound");
				if ( ("default".equals(sound.getAsString())) || (sound.isBoolean() && sound.getAsBoolean() == true) ) {
					builder_defaults |= Notification.DEFAULT_SOUND;
				} else {
					int resource = getResource("raw", data.getAsJsonPrimitive("sound").getAsString());
					builder.setSound( Uri.parse("android.resource://" + context.getPackageName() + "/" + resource) );
				}
			}

			// Vibration
			if (data.has("vibrate")) {
				try {
					JsonElement vibrateJson = data.get("vibrate");

					if (vibrateJson.isJsonPrimitive()) {
						JsonPrimitive vibrate = vibrateJson.getAsJsonPrimitive();

						if (vibrate.isBoolean() && vibrate.getAsBoolean() == true) {
							builder_defaults |= Notification.DEFAULT_VIBRATE;
						}
					} else if (vibrateJson.isJsonArray()) {
						JsonArray vibrate = vibrateJson.getAsJsonArray();

						if (vibrate.size() > 0) {
							long[] pattern = new long[vibrate.size()];
							int i = 0;

							for(i = 0; i < vibrate.size(); i++) {
								pattern[i] = vibrate.get(i).getAsLong();
							}

							builder.setVibrate(pattern);
						}
					}
				} catch(Exception ex) {
					Log.e(LCAT, "Vibrate exception: " + ex.getMessage());
				}
			}


			// Lights
			if (data.has("lights")) {
				try {
					JsonElement lightsJson = data.get("lights");

					if (lightsJson.isJsonObject()) {
						JsonObject lights = lightsJson.getAsJsonObject();
						int argb = Color.parseColor(lights.get("argb").getAsString());
						int onMs = lights.get("onMs").getAsInt();
						int offMs = lights.get("offMs").getAsInt();

						if (-1 != argb && -1 != onMs && -1 != offMs) {
							builder.setLights(argb, onMs, offMs);
						}
					}
				} catch(Exception ex) {
					Log.e(LCAT, "Lights exception: " + ex.getMessage());
				}
			} else {
				builder_defaults |= Notification.DEFAULT_LIGHTS;
			}


			// Ongoing
			if (data.has("ongoing")) {
				try {
					JsonElement ongoingJson = data.get("ongoing");

					if (ongoingJson.isJsonPrimitive()) {
						Boolean ongoing = ongoingJson.getAsBoolean();
						builder.setOngoing(ongoing);
					}

				} catch(Exception ex) {
					Log.e(LCAT, "Ongoing exception: " + ex.getMessage());
				}
			} else {
				builder_defaults |= Notification.DEFAULT_LIGHTS;
			}

			// Group
			if (data.has("group")) {
				try {
					JsonElement groupJson = data.get("group");

					if (groupJson.isJsonPrimitive()) {
						String group = groupJson.getAsString();
						builder.setGroup(group);
					}
				} catch(Exception ex) {
					Log.e(LCAT, "Group exception: " + ex.getMessage());
				}
			} else {
				builder_defaults |= Notification.DEFAULT_LIGHTS;
			}


			// GroupSummary
			if (data.has("group_summary")) {
				try {
					JsonElement groupsumJson = data.get("group_summary");

					if (groupsumJson.isJsonPrimitive()) {
						Boolean groupsum = groupsumJson.getAsBoolean();
						builder.setGroupSummary(groupsum);
					}
				} catch(Exception ex) {
					Log.e(LCAT, "Group summary exception: " + ex.getMessage());
				}
			} else {
				builder_defaults |= Notification.DEFAULT_LIGHTS;
			}


			// When
			if (data.has("when")) {
				try {
					JsonElement whenJson = data.get("when");

					if (whenJson.isJsonPrimitive()) {
						int when = whenJson.getAsInt();
						builder.setWhen(when);
					}
				} catch(Exception ex) {
					Log.e(LCAT, "When exception: " + ex.getMessage());
				}
			} else {
				builder_defaults |= Notification.DEFAULT_LIGHTS;
			}


			// Only alert once
			if (data.has("only_alert_once")) {
				try {
					JsonElement oaoJson = data.get("only_alert_once");

					if (oaoJson.isJsonPrimitive()) {
						Boolean oao = oaoJson.getAsBoolean();
						builder.setOnlyAlertOnce(oao);
					}
				} catch(Exception ex) {
					Log.e(LCAT, "Only alert once exception: " + ex.getMessage());
				}
			} else {
				builder_defaults |= Notification.DEFAULT_LIGHTS;
			}

			// Builder defaults OR
			builder.setDefaults(builder_defaults);


			// Tag
			String tag = null;
			if (data.has("tag")) {
				tag = data.getAsJsonPrimitive("tag").getAsString();
			}

			// Nid
			int id = 0;
			if (data.has("id")) {
				// ensure that the id sent from the server is negative to prevent
				// collision with the atomic integer
				id = -1 * Math.abs(data.getAsJsonPrimitive("id").getAsInt());
			} else {
				id = atomic.getAndIncrement();
			}

			// Send
			NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.notify(tag, id, builder.build());
		} else {
			Log.w(LCAT, "Show Notification: FALSE");
		}
	}

}
