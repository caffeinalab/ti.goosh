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

import android.app.NotificationManager;

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

	public TiGooshModule() {
		super();
		module = this;
	}

	public static TiGooshModule getModule() {
		return module;
	}

	public void parseBootIntent() {
		try {
			Intent intent = TiApplication.getAppRootOrCurrentActivity().getIntent();

			if (intent.hasExtra(INTENT_EXTRA)) {

				String notification = intent.getStringExtra(INTENT_EXTRA);

				intent.removeExtra(INTENT_EXTRA);
				sendMessage(notification, true);

			} else {
				Log.d(LCAT, "No notification in Intent");
			}
		} catch (Exception ex) {
			Log.e(LCAT, ex.getMessage());
		}
	}

	private boolean checkPlayServices() {
		/*Activity activity = TiApplication.getAppRootOrCurrentActivity();

		GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
		int resultCode = apiAvailability.isGooglePlayServicesAvailable(activity);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (apiAvailability.isUserResolvableError(resultCode)) {
				apiAvailability.getErrorDialog(activity, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {
				Log.e(LCAT, "This device is not supported.");
			}
			return false;
		}*/
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
		/*final String senderId = getSenderId();
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
		}.execute();*/
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

	private void saveToken(String token) {
		SharedPreferences preferences = getDefaultSharedPreferences();
		preferences.edit().putString(TOKEN, token).apply();
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

		HashMap<String, Object> e = new HashMap<String, Object>();
		e.put("data", data); // to parse on reverse on JS side
		e.put("inBackground", inBackground);

		messageCallback.callAsync(getKrollObject(), e);
	}

}

