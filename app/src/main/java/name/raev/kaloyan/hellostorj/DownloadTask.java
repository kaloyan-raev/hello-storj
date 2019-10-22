package name.raev.kaloyan.hellostorj;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLConnection;

import io.storj.Bucket;
import io.storj.ObjectInfo;
import io.storj.ObjectInputStream;
import io.storj.Project;
import io.storj.StorjException;
import io.storj.Uplink;

public class DownloadTask extends AsyncTask<Void, Long, Throwable> {

    private Activity mActivity;
    private ObjectInfo mFile;
    private File mLocalFile;
    private String mAppName;
    private DownloadManager mDownloadManager;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private int mNotificationId;
    private long mDownloadedBytes;
    private long mLastNotifiedTime;

    private ObjectInputStream mInputStream;

    DownloadTask(Activity activity, ObjectInfo file) {
        mActivity = activity;
        mFile = file;

        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String fileName = new File(mFile.getPath()).getName();
        mLocalFile = new File(downloadDir, fileName);

        mAppName = activity.getResources().getString(R.string.app_name);
        mDownloadManager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        mNotificationId = mFile.hashCode();
    }

    @Override
    protected void onPreExecute() {
        CancelDownloadReceiver.tasks.put(mNotificationId, this);
        // show snackbar for user to watch for download notifications
        Snackbar.make(mActivity.findViewById(R.id.browse_list),
                R.string.download_in_progress,
                Snackbar.LENGTH_LONG).show();
        // intent for cancel action
        Intent intent = new Intent(mActivity, CancelDownloadReceiver.class);
        intent.putExtra(CancelDownloadReceiver.NOTIFICATION_ID, mNotificationId);
        PendingIntent cancelIntent = PendingIntent.getBroadcast(mActivity, mNotificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        // init the download notification
        mNotifyManager = (NotificationManager) mActivity.getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(mActivity, FileTransferChannel.ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setColor(ContextCompat.getColor(mActivity, R.color.colorNotification))
                .setContentTitle(mFile.getPath())
                .setContentText(mAppName)
                .setOnlyAlertOnce(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
                .setProgress(0, 0, true);
        mNotifyManager.notify(mNotificationId, mBuilder.build());
    }

    @Override
    protected Exception doInBackground(Void... params) {
        try (Uplink uplink = new Uplink();
             Project project = uplink.openProject(ScopeManager.getScope(mActivity));
             Bucket bucket = project.openBucket(mFile.getBucket(), ScopeManager.getScope(mActivity));
             ObjectInputStream in = new ObjectInputStream(bucket, mFile.getPath());
             OutputStream out = new FileOutputStream(mLocalFile)) {
            mInputStream = in;
            byte[] buffer = new byte[128 * 1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                if (isCancelled()) {
                    in.cancel();
                    return null;
                }
                out.write(buffer, 0, len);
                if (isCancelled()) {
                    in.cancel();
                    return null;
                }
                publishProgress((long) len);
            }
        } catch (StorjException | IOException e) {
            return e;
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Long... params) {
        long increment = params[0];
        mDownloadedBytes += increment;

        long now = System.currentTimeMillis();

        int progress = (int) ((mDownloadedBytes * 100) / mFile.getSize());

        // check if 1 second elapsed since last notification or progress is at 100%
        if (progress == 100 || mLastNotifiedTime == 0 || now > mLastNotifiedTime + 1150) {
            mBuilder.setProgress(100, progress, false);
            mNotifyManager.notify(mNotificationId, mBuilder.build());
            // update last notified map
            mLastNotifiedTime = now;
        }
    }

    @Override
    protected void onPostExecute(Throwable t) {
        CancelDownloadReceiver.tasks.remove(mNotificationId);

        if (t != null) {
            setNotificationText(String.format("Download failed: %s", t.getMessage()));
            return;
        }

        // hide the "download in progress" notification
        mNotifyManager.cancel(mNotificationId);
        // show the "download completed" notification
        mDownloadManager.addCompletedDownload(mLocalFile.getName(),
                mAppName,
                true,
                getMimeType(mLocalFile),
                mLocalFile.getPath(),
                mFile.getSize(),
                true);
    }

    protected void onCancelled(Throwable t) {
        CancelDownloadReceiver.tasks.remove(mNotificationId);
        setNotificationText("Download canceled");
    }

    void cancel() {
        this.cancel(false);
        mInputStream.cancel();
    }

    private String getMimeType(java.io.File file) {
        String mime = URLConnection.guessContentTypeFromName(file.getName());
        if (mime == null || mime.isEmpty()) {
            mime = "application/octet-stream";
        }
        return mime;
    }

    @SuppressLint("RestrictedApi")
    private void setNotificationText(String text) {
        mBuilder.setProgress(0, 0, false)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .mActions.clear();
        mNotifyManager.notify(mNotificationId, mBuilder.build());
    }
}
