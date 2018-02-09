package ti.goosh;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import org.appcelerator.titanium.TiApplication;
import org.appcelerator.kroll.KrollRuntime;

public class PushHandlerActivity extends Activity {

	private static String LCAT = "ti.goosh.TiGooshPushHandlerActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		try {
			Log.d(LCAT, "started");
			super.onCreate(savedInstanceState);
			finish();

			TiGooshModule module = TiGooshModule.getModule();
			Context context = getApplicationContext();
			String notification = getIntent().getStringExtra(TiGooshModule.INTENT_EXTRA);

			Intent launcherIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
			launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
			launcherIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			if (module.hasMessageCallback() && KrollRuntime.getInstance().getRuntimeState() != KrollRuntime.State.DISPOSED) {
				module.sendMessage(notification, true);
			} else {
				launcherIntent.putExtra(TiGooshModule.INTENT_EXTRA, notification);
			}

			startActivity(launcherIntent);

		} catch (Exception e) {
			// noop
		} finally {
			finish();
		}
	}

	@Override protected void onResume() {
		Log.d(LCAT, "resumed");
		super.onResume();
	}
	
}