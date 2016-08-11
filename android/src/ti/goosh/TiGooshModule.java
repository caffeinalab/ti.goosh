package ti.goosh;

import android.app.Activity;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

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

	@Kroll.onAppCreate
	public static void onAppCreate(TiApplication app) {
		// Register the events to ensure the Intent parsing on resume
		TiApplication.getInstance().registerActivityLifecycleCallbacks(new TiGooshActivityLifecycleCallbacks());
		Log.d(LCAT, "onAppCreate " + app + " (" + (instance != null) + ")");
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

		this.registered = true;
		this.parseIncomingNotificationIntent();

		if (checkPlayServices()) {
			activity.startService( new Intent(activity, RegistrationIntentService.class) );
		}
	}

	@Kroll.method
	public void unregisterForPushNotifications() {
		// TODO
	}

	@Kroll.method
	@Kroll.getProperty
	public Boolean isRemoteNotificationsEnabled() {
		return this.getDefaultSharedPreferences().contains("tigoosh.token");
	}

	@Kroll.method
	@Kroll.getProperty
	public String getRemoteDeviceUUID() {
		return this.getDefaultSharedPreferences().getString("tigoosh.token", "");
	}

	@Kroll.method
	public void setAppBadge(int count) {
		BadgeUtils.setBadge(TiApplication.getInstance().getApplicationContext(), count);
	}

	@Kroll.method
	public int getAppBadge() {
		return 0;
	}


	// Private

	public SharedPreferences getDefaultSharedPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(TiApplication.getInstance().getApplicationContext());
	}

	public void saveToken(String token) {
		this.getDefaultSharedPreferences().edit().putString("tigoosh.token", token).apply();
	}
 

	public void sendSuccess(String token) {
		if (successCallback == null) {
			Log.e(LCAT, "sendSuccess invoked but no successCallback defined");
			return;
		}

		this.saveToken(token);

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
			e.put("data", data); // to parse on reverse on JS side
			e.put("inBackground", inBackground);

			messageCallback.call(getKrollObject(), e);

		} catch (Exception ex) {
			Log.e(LCAT, "Error sending gmessage to JS: " + ex.getMessage());
		}
	}

}

