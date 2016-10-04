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
import java.lang.reflect.Method;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import android.app.NotificationManager;
import android.service.notification.StatusBarNotification;
import android.app.PendingIntent;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

@Kroll.module(name="TiGoosh", id="ti.goosh")
public class TiGooshModule extends KrollModule {

	private static final String LCAT = "ti.goosh.TiGooshModule";
	private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

	private static TiGooshModule instance = null;

	private KrollFunction successCallback = null;
	private KrollFunction errorCallback = null;
	private KrollFunction messageCallback = null;

	public Boolean registered = false;

	public TiGooshModule() {
		super();
		instance = this;
	}

	public static TiGooshModule getInstance() {
		return instance;
	}

	public void parseIncomingNotificationIntent() {
		try {
			Activity root = TiApplication.getAppRootOrCurrentActivity();
			Intent intent = root.getIntent();

			if (intent.hasExtra("tigoosh.notification")) {

				TiGooshModule.getInstance().sendMessage(intent.getStringExtra("tigoosh.notification"), true);
				intent.removeExtra("tigoosh.notification");

			} else {
				Log.d(LCAT, "No notification in Intent");
			}
		} catch (Exception ex) {
			Log.e(LCAT, ex.getMessage());
		}
	}

	private boolean checkPlayServices() {
		Activity activity = TiApplication.getAppRootOrCurrentActivity();

		GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
		int resultCode = apiAvailability.isGooglePlayServicesAvailable(activity);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (apiAvailability.isUserResolvableError(resultCode)) {
				apiAvailability.getErrorDialog(activity, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {
				Log.e(LCAT, "This device is not supported.");
			}
			return false;
		}
		return true;
	}

	private static NotificationManager getNotificationManager() {
		return (NotificationManager) TiApplication.getInstance().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
	}

	private static Intent getIntent(PendingIntent pendingIntent) {
		try {
			Method getIntent = PendingIntent.class.getDeclaredMethod("getIntent");
			return (Intent) getIntent.invoke(pendingIntent);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Kroll.method
	public String getSenderId() {
		return TiApplication.getInstance().getAppProperties().getString("gcm.senderid", "");
	}

	@Kroll.method
	public void registerForPushNotifications(HashMap options) {
		Activity activity = TiApplication.getAppRootOrCurrentActivity();

		successCallback = options.containsKey("success") ? (KrollFunction)options.get("success") : null;
		errorCallback = options.containsKey("error") ? (KrollFunction)options.get("error") : null;
		messageCallback = options.containsKey("callback") ? (KrollFunction)options.get("callback") : null;

		registered = true;
		parseIncomingNotificationIntent();

		if (checkPlayServices()) {
			activity.startService( new Intent(activity, RegistrationIntentService.class) );
		}
	}

	@Kroll.method
	public void unregisterForPushNotifications() {
		final String senderId = getSenderId();
		final Context context = TiApplication.getInstance().getApplicationContext();

		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				try {
					InstanceID.getInstance(context).deleteToken(senderId, GoogleCloudMessaging.INSTANCE_ID_SCOPE);
					Log.d(LCAT, "delete instanceid succeeded");
				} catch (final IOException e) {
					Log.e(LCAT, "remove token failed - error: " + e.getMessage());
				}
				return null;
			}
		}.execute();
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
	public Object getActiveNotifications()  throws JSONException {
		ArrayList<Object> list = new ArrayList<Object>();
		for (StatusBarNotification sbn : getNotificationManager().getActiveNotifications()){
			JSONObject jsonObject = new JSONObject(getIntent(sbn.getNotification().contentIntent).getStringExtra("tigoosh.notification"));
			list.add(jsonToMap(jsonObject));
		}
		return list.toArray();
	}

	@Kroll.method
	@Kroll.getProperty
	public Boolean isRemoteNotificationsEnabled() {
		return getDefaultSharedPreferences().contains("tigoosh.token");
	}

	@Kroll.method
	@Kroll.getProperty
	public String getRemoteDeviceUUID() {
		return getDefaultSharedPreferences().getString("tigoosh.token", "");
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

	private void saveToken(String token) {
		getDefaultSharedPreferences().edit().putString("tigoosh.token", token).apply();
	}

	// Public

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

	public void sendMessage(String data, Boolean inBackground) {
		if (messageCallback == null) {
			Log.e(LCAT, "sendMessage invoked but no messageCallback defined");
			return;
		}

		try {
		
			HashMap<String, Object> e = new HashMap<String, Object>();
			JSONObject jsonObject = new JSONObject(data);
			e.put("data", jsonToMap(jsonObject));
			e.put("inBackground", inBackground);

			messageCallback.callAsync(getKrollObject(), e);

		} catch (Exception ex) {
			Log.e(LCAT, "Error sending gmessage to JS: " + ex.getMessage());
		}
	}

	public static Map<String, Object> jsonToMap(JSONObject json) throws JSONException {
	    Map<String, Object> retMap = new HashMap<String, Object>();

	    if(json != JSONObject.NULL) {
	        retMap = toMap(json);
	    }
	    return retMap;
	}

	public static Map<String, Object> toMap(JSONObject object) throws JSONException {
	    Map<String, Object> map = new HashMap<String, Object>();

	    Iterator<String> keysItr = object.keys();
	    while(keysItr.hasNext()) {
	        String key = keysItr.next();
	        Object value = object.get(key);

	        if(value instanceof JSONArray) {
	            value = toList((JSONArray) value);
	        }

	        else if(value instanceof JSONObject) {
	            value = toMap((JSONObject) value);
	        }
	        map.put(key, value);
	    }
	    return map;
	}

	public static List<Object> toList(JSONArray array) throws JSONException {
	    List<Object> list = new ArrayList<Object>();
	    for(int i = 0; i < array.length(); i++) {
	        Object value = array.get(i);
	        if(value instanceof JSONArray) {
	            value = toList((JSONArray) value);
	        }

	        else if(value instanceof JSONObject) {
	            value = toMap((JSONObject) value);
	        }
	        list.add(value);
	    }
	    return list;
	}

}

