package ti.goosh;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import org.appcelerator.titanium.TiApplication;

public class FcmIntent extends Activity {
	private static String LCAT = "ti.goosh.FcmIntent";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		try {
			Log.d(LCAT, "started");
			super.onCreate(savedInstanceState);

			if (getIntent().getExtras() != null) {
	            for (String key : getIntent().getExtras().keySet()) {
	                Object value = getIntent().getExtras().get(key);
	                Log.d(LCAT, "Key: " + key + " Value: " + value);
	            }
	        }
			finish();	        
		} catch (Exception e) {
			// noop
			finish();
		} finally {
			finish();
		}
	}
}