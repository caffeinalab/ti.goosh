package ti.goosh;

import android.annotation.SuppressLint;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.support.annotation.RequiresApi;

import java.lang.ref.WeakReference;

@RequiresApi(api = Build.VERSION_CODES.O)
public class GcmJobService extends JobService {

	private static String LCAT = "ti.goosh.GcmJobService";
	private static final int JOB_ID = Integer.MAX_VALUE - 4321;

	private GcmAsyncTask mAsyncTask;

	@RequiresApi(api = Build.VERSION_CODES.O)
	static void scheduleJob(Context context, Bundle extras) {
		ComponentName jobComponentName = new ComponentName(context.getPackageName(), GcmJobService.class.getName());
		JobScheduler mJobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
		JobInfo existingInfo = mJobScheduler.getPendingJob(JOB_ID);
		if (existingInfo != null) {
			mJobScheduler.cancel(JOB_ID);
		}

		JobInfo.Builder jobBuilder = new JobInfo.Builder(JOB_ID, jobComponentName)
				.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setTransientExtras(extras);
		int result = mJobScheduler.schedule(jobBuilder.build());
		if (result != JobScheduler.RESULT_SUCCESS) {
			Log.e(LCAT, "Could not start job, error code: " + result);
		}
	}

	@Override
	public boolean onStartJob(JobParameters params) {
		mAsyncTask = new GcmAsyncTask(getApplicationContext());
		mAsyncTask.execute(params);
		return true;
	}

	@Override
	public boolean onStopJob(JobParameters params) {
		if (mAsyncTask != null) {
			mAsyncTask.cancel(true);
			mAsyncTask = null;
		}
		return false;
	}

	private class GcmAsyncTask extends AsyncTask<JobParameters, Void, Void> {
		private JobParameters params;
		private WeakReference<Context> contextRef;

		public GcmAsyncTask(Context context) {
			contextRef = new WeakReference<>(context);
		}

		@Override
		protected Void doInBackground(JobParameters... params) {
			this.params = params[0];

			Bundle extras = this.params.getTransientExtras();
			try {
				new IntentService().handleMessage(contextRef.get(), extras);
			} catch (Exception ex) {
				Log.e(LCAT, "Exception while handling message: " + ex.getMessage());
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);
			jobFinished(params, false);
		}
	};
}