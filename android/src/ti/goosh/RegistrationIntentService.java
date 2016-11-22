package ti.goosh;

import java.io.IOException;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import org.appcelerator.titanium.util.TiRHelper;

public class RegistrationIntentService extends IntentService {

	private static final String LCAT = "ti.goosh.RegistrationIntentService";

	public RegistrationIntentService() {
		super(LCAT);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		TiGooshModule module = TiGooshModule.getModule();
		if (module == null) {
			Log.e(LCAT, "Intent handled but no TiGoosh instance module found");
			return;
		}

		try {

			String senderId = module.getSenderId();
			String token = InstanceID.getInstance(this).getToken(senderId, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);

			Log.i(LCAT, "Sender ID: " + senderId);
			Log.i(LCAT, "Device Token: " + token);

			module.sendSuccess(token);

		} catch (Exception ex) {

			Log.e(LCAT, "Failed to get GCM Registration Token:" + ex.getMessage());
			module.sendError(ex);
			
		}
	}
}
