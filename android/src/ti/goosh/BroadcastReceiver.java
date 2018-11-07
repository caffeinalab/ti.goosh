package ti.goosh;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.app.Activity;
import org.appcelerator.titanium.util.TiRHelper;

import com.google.android.gms.gcm.GcmListenerService;
import com.google.android.gms.gcm.GcmReceiver;

public class BroadcastReceiver extends GcmReceiver {
	private static String LCAT = "ti.goosh.TiGooshBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        ComponentName comp = new ComponentName(context.getPackageName(),
                IntentService.class.getName());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                GcmJobService.scheduleJob(context, extras);
            }
        } else {
            startWakefulService(context, (intent.setComponent(comp)));
        }
        setResultCode(Activity.RESULT_OK);

        Log.d(LCAT, "started");
    }
}