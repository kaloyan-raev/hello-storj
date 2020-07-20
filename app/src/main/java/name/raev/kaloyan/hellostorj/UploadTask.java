package name.raev.kaloyan.hellostorj;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import io.storj.BucketInfo;
import io.storj.ObjectOutputStream;
import io.storj.Project;
import io.storj.StorjException;
import io.storj.Uplink;
import io.storj.UplinkOption;

public class UploadTask extends AsyncTask<Void, Long, Throwable> {

    private static final String TAG = "UploadTask";

    private Activity mActivity;
    private BucketInfo mBucket;
    private String mFilePath;
    private long mFileSize;
    private String mAppName;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private int mNotificationId;
    private long mUploadedBytes;
    private long mLastNotifiedTime;

    private ObjectOutputStream mOutputStream;

    UploadTask(Activity activity, BucketInfo bucket, String filePath) {
        mActivity = activity;
        mBucket = bucket;
        mFilePath = filePath;
        mFileSize = new File(mFilePath).length();
        mAppName = activity.getResources().getString(R.string.app_name);
        mNotificationId = mFilePath.hashCode();
    }

    @Override
    protected void onPreExecute() {
        CancelUploadReceiver.tasks.put(mNotificationId, this);
        // show snackbar for user to watch for upload notifications
        Snackbar.make(mActivity.findViewById(R.id.browse_list),
                R.string.upload_in_progress,
                Snackbar.LENGTH_LONG).show();
        // intent for cancel action
        Intent intent = new Intent(mActivity, CancelUploadReceiver.class);
        intent.putExtra(CancelUploadReceiver.NOTIFICATION_ID, mNotificationId);
        PendingIntent cancelIntent = PendingIntent.getBroadcast(mActivity, mNotificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        // init the download notification
        mNotifyManager = (NotificationManager) mActivity.getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(mActivity, FileTransferChannel.ID)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setColor(ContextCompat.getColor(mActivity, R.color.colorNotification))
                .setContentTitle(new File(mFilePath).getName())
                .setContentText(mAppName)
                .setOnlyAlertOnce(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
                .setProgress(0, 0, true);
        mNotifyManager.notify(mNotificationId, mBuilder.build());
    }

    @Override
    protected Exception doInBackground(Void... params) {
        String tempDir = mActivity.getCacheDir().getPath();
        Uplink uplink = new Uplink(UplinkOption.tempDir(tempDir));
        try (Project project = uplink.openProject(AccessManager.getAccess(mActivity));
             InputStream in = new FileInputStream(mFilePath);
             ObjectOutputStream out = project.uploadObject(mBucket.getName(), new File(mFilePath).getName())) {
            mOutputStream = out;
            byte[] buffer = new byte[128 * 1024];
            int len;
            try {
                while ((len = in.read(buffer)) != -1) {
                    if (isCancelled()) {
                        return null;
                    }
                    out.write(buffer, 0, len);
                    if (isCancelled()) {
                        return null;
                    }
                    publishProgress((long) len);
                }
                out.commit();
            } catch (IOException e) {
                throw e;
            }
        } catch (StorjException | IOException e) {
            return e;
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Long... params) {
        long increment = params[0];
        mUploadedBytes += increment;

        long now = System.currentTimeMillis();

        int progress = (mFileSize == 0) ? 100 : (int) ((mUploadedBytes * 100) / mFileSize);

        // check if 1 second elapsed since last notification or progress is at 100%
        if (progress == 100 || mLastNotifiedTime == 0 || now > mLastNotifiedTime + 1150) {
            mBuilder.setProgress(100, progress, false);
            mNotifyManager.notify(mNotificationId, mBuilder.build());
            mLastNotifiedTime = now;
        }
    }

    @Override
    protected void onPostExecute(Throwable t) {
        CancelUploadReceiver.tasks.remove(mNotificationId);

        if (t != null) {
            setNotificationText(
                    String.format("Upload failed: %s", t.getMessage()),
                    android.R.drawable.stat_notify_error,
                    null
            );
            return;
        }

        Intent intent = new Intent(mActivity, FilesActivity.class);
        intent.putExtra(FilesFragment.BUCKET, mBucket);
        PendingIntent resultIntent =
                PendingIntent.getActivity(
                        mActivity,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        setNotificationText(
                mActivity.getResources().getString(R.string.upload_complete),
                android.R.drawable.stat_sys_upload_done,
                resultIntent
        );
    }

    protected void onCancelled(Throwable t) {
        CancelUploadReceiver.tasks.remove(mNotificationId);
        setNotificationText("Upload canceled", android.R.drawable.stat_notify_error, null);
    }

    void cancel() {
        this.cancel(false);
        try {
            mOutputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Error aborting upload", e);
        }
    }

    @SuppressLint("RestrictedApi")
    private void setNotificationText(String text, int icon, PendingIntent intent) {
        mBuilder.setProgress(0, 0, false)
                .setSmallIcon(icon)
                .setContentText(text)
                .setContentIntent(intent)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .mActions.clear();
        mNotifyManager.notify(mNotificationId, mBuilder.build());
    }
}
