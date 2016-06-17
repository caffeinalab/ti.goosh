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
		Context context = TiApplication.getInstance().getApplicationContext();
		Boolean appInBackground = !TiApplication.isCurrentActivityInForeground();

		Boolean showNotification = true;

		String jsonData = bundle.getString("data");
		JsonObject data = null;

		try {
			data = (JsonObject) new Gson().fromJson(jsonData, JsonObject.class);
		} catch (Exception ex) {
			Log.e(LCAT, "Error parsing data JSON: " + ex.getMessage());
			return;
		}

		if (data != null && data.has("alert") == true) {
			if (appInBackground) {
				showNotification = true;
			} else {
				if (data.has("force_show_in_foreground")) {
					JsonPrimitive showInFore = data.getAsJsonPrimitive("force_show_in_foreground");
					showNotification = ((showInFore.isBoolean() && showInFore.getAsBoolean() == true));
				} else {
					showNotification = false;
				}
			}
		} else {
			Log.i(LCAT, "Not showing notification cause missing data.alert");
			showNotification = false;
		}

		if (showNotification) {

			Intent launcherIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
			launcherIntent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
			launcherIntent.putExtra("tigoosh.notification", jsonData);

			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, launcherIntent, PendingIntent.FLAG_ONE_SHOT);

			// Start building notification

			NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
			int builder_defaults = 0;
			builder.setContentIntent(contentIntent);
			builder.setAutoCancel(true);
			builder.setPriority(2);

			// Body 

			String alert = null;
			if (data.has("alert")) {
				alert = data.getAsJsonPrimitive("alert").getAsString();
				builder.setContentText(alert);
				builder.setTicker(alert);
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

			// Title

			if (data.has("title")) {
				builder.setContentTitle( data.getAsJsonPrimitive("title").getAsString() );
			} else {
				builder.setContentTitle( TiApplication.getInstance().getAppInfo().getName() );
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

			try {
				if (data.has("vibrate")) {
					JsonPrimitive vibrate = data.getAsJsonPrimitive("vibrate");
					if (vibrate.isBoolean() && vibrate.getAsBoolean() == true) {
						builder_defaults |= Notification.DEFAULT_VIBRATE;
					}
				}
			} catch(Exception ex) {
				Log.e(LCAT, "Vibrate exception: " + ex.getMessage());
			}

			// Build

			builder_defaults |= Notification.DEFAULT_LIGHTS;
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


			NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.notify(tag, id, builder.build());
		}

		if (TiGooshModule.getInstance() != null) {
			TiGooshModule.getInstance().sendMessage(jsonData, appInBackground);
		}
	}

}
