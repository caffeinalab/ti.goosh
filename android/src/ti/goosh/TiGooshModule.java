package ti.goosh;

import android.app.Activity;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;


import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.TiConfig;
import org.appcelerator.titanium.TiApplication;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

//import com.google.android.gms.common.ConnectionResult;
//import com.google.android.gms.common.GoogleApiAvailability;

/*import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;*/
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import android.app.NotificationManager;
import android.support.v4.app.NotificationManagerCompat;

@Kroll.module(name="TiGoosh", id="ti.goosh")
public class TiGooshModule extends KrollModule {

	private static final String LCAT = "ti.goosh.TiGooshModule";
	private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

	public static final String INTENT_EXTRA = "tigoosh.notification";
	public static final String TOKEN = "tigoosh.token";

	private static TiGooshModule module = null;

	private KrollFunction successCallback = null;
	private KrollFunction errorCallback = null;
	private KrollFunction messageCallback = null;

	/* Callbacks for topics */
	private KrollFunction successUnsubTopicCallback = null;
	private KrollFunction successTopicCallback = null;
	private KrollFunction errorSubTopicCallback = null;
	private KrollFunction errorTopicCallback = null;
	private KrollFunction topicCallback = null;

	public TiGooshModule() {
		super();
		module = this;

		initFirebase();
	}

	public static TiGooshModule getModule() {
		if (module != null) return module;
		else return new TiGooshModule();
	}

	private void initFirebase() {
		

        Context ctx 	  = TiApplication.getInstance().getApplicationContext();

		if (!FirebaseApp.getApps(ctx).isEmpty()) {
			FirebaseOptions options = new FirebaseOptions.Builder()
			// 	.setApplicationId("com.progress44.test")
			// 	.setServiceAccount(new FileInputStream("path/to/serviceAccountKey.json"))
				.build();
		
			FirebaseApp.initializeApp(ctx, options); // Crashes app
		}
		
		//Service is below
		Intent messagingService = new Intent(ctx, FcmMessagingService.class);
		ctx.startService(messagingService);
		Intent listenerService = new Intent(ctx, FcmListenerService.class);
		ctx.startService(listenerService);
	}

	public void parseBootIntent() {
		try {
			Intent intent = TiApplication.getAppRootOrCurrentActivity().getIntent();
			String notification = intent.getStringExtra(INTENT_EXTRA);
			if (notification != null) {
				sendMessage(notification, true);
				intent.removeExtra(INTENT_EXTRA);
			} else {
				Log.d(LCAT, "Empty notification in Intent");
			}
		} catch (Exception ex) {
			Log.e(LCAT, "parseBootIntent" + ex);
		}
	}

	private boolean checkPlayServices() {
		return true;
	}

	private NotificationManager getNotificationManager() {
		return (NotificationManager) TiApplication.getInstance().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Kroll.method
	public String getSenderId() {
		return TiApplication.getInstance().getAppProperties().getString("gcm.senderid", "");
	}

	@Kroll.method
	public void registerForPushNotifications(HashMap options) {
		Activity activity = TiApplication.getAppRootOrCurrentActivity();
		
		if (false == options.containsKey("callback")) {
			Log.e(LCAT, "You have to specify a callback attribute when calling registerForPushNotifications");
			return;
		}

		messageCallback = (KrollFunction)options.get("callback");

		successCallback = options.containsKey("success") ? (KrollFunction)options.get("success") : null;
		errorCallback = options.containsKey("error") ? (KrollFunction)options.get("error") : null;

		parseBootIntent();
	}

	@Kroll.method
	public void unregisterForPushNotifications() {
		
	}

	@Kroll.method
	public boolean areNotificationsEnabled() {
		try{
			return NotificationManagerCompat.from(TiApplication.getInstance().getApplicationContext()).areNotificationsEnabled();
		}catch(Exception ex){
			return false;
		}
		
	}

	@Kroll.method
	public void cancelAll() {
		getNotificationManager().cancelAll();
	}

	@Kroll.method
	public void cancelWithTag(String tag, int id) {
		getNotificationManager().cancel(tag, -1 * id);
	}

	@Kroll.method
	public void cancel(int id) {
		getNotificationManager().cancel(-1 * id);
	}

	@Kroll.method
	@Kroll.getProperty
	public Boolean isRemoteNotificationsEnabled() {
		return (getRemoteDeviceUUID() != null);
	}

	@Kroll.method
	@Kroll.getProperty
	public String getRemoteDeviceUUID() {
		return getDefaultSharedPreferences().getString(TOKEN, "");
	}

	@Kroll.method
	public void setAppBadge(int count) {
		BadgeUtils.setBadge(TiApplication.getInstance().getApplicationContext(), count);
	}

	@Kroll.method
	public int getAppBadge() {
		return 0;
	}

	// Privates

	private SharedPreferences getDefaultSharedPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(TiApplication.getInstance().getApplicationContext());
	}

	// Public

	public void saveToken(String token) {
		SharedPreferences preferences = getDefaultSharedPreferences();
		preferences.edit().putString(TOKEN, token).apply();
	}

	public void sendSuccess(String token) {
		if (successCallback == null) {
			Log.e(LCAT, "sendSuccess invoked but no successCallback defined");
			return;
		}

		saveToken(token);

		HashMap<String, Object> e = new HashMap<String, Object>();
		e.put("deviceToken", token);

		successCallback.callAsync(getKrollObject(), e);
	}

	public void sendError(Exception ex) {
		if (errorCallback == null) {
			Log.e(LCAT, "sendError invoked but no errorCallback defined");
			return;
		}

		HashMap<String, Object> e = new HashMap<String, Object>();
		e.put("error", ex.getMessage());

		errorCallback.callAsync(getKrollObject(), e);
	}

	public boolean hasMessageCallback() {
		return messageCallback != null;
	}

	public void sendMessage(String data, Boolean inBackground) {
		if (messageCallback == null) {
			Log.e(LCAT, "sendMessage invoked but no messageCallback defined");
			return;
		}

		HashMap<String, Object> e = new HashMap<String, Object>();
		e.put("data", data); // to parse on reverse on JS side
		e.put("inBackground", inBackground);

		messageCallback.callAsync(getKrollObject(), e);
	}

	@Kroll.method
	public void subscribe(final HashMap options) {
		final String topic  = (String) options.get("topic");

		if (options.get("success") != null) {
			successTopicCallback = (KrollFunction) options.get("success");
		}
		if (options.get("error") != null) {
			errorTopicCallback = (KrollFunction) options.get("error");
		}

		if (topic == null || !topic.startsWith("/topics/")) {
			Log.e(LCAT, "No or invalid topic specified, should start with /topics/");
			return;
		}

		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				try {
					String token = getRemoteDeviceUUID();
					GcmPubSub.getInstance(TiApplication.getInstance()).subscribe(token, topic, null);
					if (successTopicCallback != null) {
						// send success callback
						HashMap<String, Object> data = new HashMap<String, Object>();
						data.put("success", true);
						data.put("topic", topic);
						successTopicCallback.callAsync(getKrollObject(), data);
					}
				} catch (Exception ex) {
					// error
					Log.e(LCAT, "Error " + ex.toString());
					if (errorTopicCallback != null) {
						// send error callback
						HashMap<String, Object> data = new HashMap<String, Object>();
						data.put("success", false);
						data.put("topic", topic);
						data.put("error", ex.toString());
						errorTopicCallback.callAsync(getKrollObject(), data);
					}
				}
				return null;
			}
		}.execute();
	}

	@Kroll.method
	public void unsubscribe(final HashMap options) {
		final String topic  = (String) options.get("topic");
		successUnsubTopicCallback = (KrollFunction) options.get("success");
		errorSubTopicCallback = (KrollFunction) options.get("error");

		if (topic == null || !topic.startsWith("/topics/")) {
			Log.e(LCAT, "No or invalid topic specified, should start with /topics/");
			return;
		}

		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				try {
					String token = getRemoteDeviceUUID();
					if (token != null) {
						GcmPubSub.getInstance(TiApplication.getInstance()).unsubscribe(token, topic);
						if (successUnsubTopicCallback != null) {
							HashMap<String, Object> data = new HashMap<String, Object>();
							data.put("success", true);
							data.put("topic", topic);
							successUnsubTopicCallback.callAsync(getKrollObject(), data);
						}
					} else {
						HashMap<String, Object> data = new HashMap<String, Object>();
						data.put("error", true);
						data.put("topic", topic);
						errorSubTopicCallback.callAsync(getKrollObject(), data);
						Log.e(LCAT, "Cannot unsubscribe from topic " + topic);
					}
				} catch (Exception ex) {
					HashMap<String, Object> data = new HashMap<String, Object>();
					data.put("error", ex.toString());
					data.put("topic", topic);
					errorSubTopicCallback.callAsync(getKrollObject(), data);
				}
				return null;
			}
		}.execute();
	}
}
