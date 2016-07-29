package ti.goosh;

import android.app.Application.ActivityLifecycleCallbacks;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiRootActivity;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

final class TiGooshActivityLifecycleCallbacks implements ActivityLifecycleCallbacks {

	private static final String LCAT = "ti.goosh.TiGooshModule";

	public void onActivityDestroyed(Activity activity) {}
	public void onActivityPaused(Activity activity) {}
	public void onActivityCreated(Activity activity, Bundle bundle) {}
	public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
	public void onActivityStopped(Activity activity) {}

	public void onActivityStarted(Activity activity) {
		if (activity.getLocalClassName().equals("org.appcelerator.titanium.TiActivity") == false) return;

		TiGooshModule instance = TiGooshModule.getInstance();
		if (instance == null || instance.registered == false) return;

		instance.parseIncomingNotificationIntent();
	}

	public void onActivityResumed(Activity activity) {
		if (activity.getLocalClassName().equals("org.appcelerator.titanium.TiActivity") == false) return;

		TiGooshModule instance = TiGooshModule.getInstance();
		if (instance == null || instance.registered == false) return;

		instance.parseIncomingNotificationIntent();
	}

}