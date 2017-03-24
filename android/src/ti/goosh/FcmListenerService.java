package ti.goosh;

import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.util.TiRHelper;
import java.util.concurrent.atomic.AtomicInteger;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

public class FcmListenerService extends FirebaseInstanceIdService {

	private static final String LCAT = "ti.goosh.FcmListenerService";
	private static final AtomicInteger atomic = new AtomicInteger(0);

	/**
	* Called if InstanceID token is updated. This may occur if the security of
	* the previous token had been compromised. Note that this is also called
	* when the InstanceID token is initially generated, so this is where
	* you retrieve the token.
	*/
	// [START refresh_token]
	@Override
	public void onTokenRefresh() {
		Log.e(LCAT, "Is this function being called or not?");
		TiGooshModule module = TiGooshModule.getModule();
		// Get updated InstanceID token.
		String refreshedToken = FirebaseInstanceId.getInstance().getToken();
		Log.d(LCAT, "Refreshed token: " + refreshedToken);

		if (module == null) {
			Log.e(LCAT, "Intent handled but no TiGoosh instance module found");
			return;
		} else {
			module.saveToken(refreshedToken);
		}
	}
}
