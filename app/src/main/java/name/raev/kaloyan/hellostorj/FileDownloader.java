/*
 * Copyright (C) 2017-2018 Kaloyan Raev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package name.raev.kaloyan.hellostorj;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import java.net.URLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.storj.libstorj.Bucket;
import io.storj.libstorj.DownloadFileCallback;
import io.storj.libstorj.File;
import io.storj.libstorj.Storj;
import io.storj.libstorj.android.StorjAndroid;

class FileDownloader implements DownloadFileCallback {

    static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    private Activity mActivity;
    private Bucket mBucket;
    private File mFile;

    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;

    Map<String, Long> lastNotifiedMap = Collections.synchronizedMap(new HashMap<String, Long>());

    FileDownloader(Activity activity, Bucket bucket, File file) {
        mActivity = activity;
        mBucket = bucket;
        mFile = file;
    }

    public void download() {
        if (isPermissionGranted()) {
            doDownload();
        } else {
            requestPermission();
        }
    }

    private boolean isPermissionGranted() {
        return ContextCompat.checkSelfPermission(mActivity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(mActivity,
                new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
    }

    void onRequestPermissionsResult(@NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            doDownload();
        } else {
            Snackbar.make(mActivity.findViewById(R.id.browse_list),
                    R.string.download_permission_denied,
                    Snackbar.LENGTH_LONG).show();
        }
    }

    private void doDownload() {
        // show snackbar for user to watch for download notifications
        Snackbar.make(mActivity.findViewById(R.id.browse_list),
                R.string.download_in_progress,
                Snackbar.LENGTH_LONG).show();
        // init the download notification
        mNotifyManager = (NotificationManager) mActivity.getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(mActivity, FileTransferChannel.ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setColor(ContextCompat.getColor(mActivity, R.color.colorNotification))
                .setContentTitle(mFile.getName())
                .setContentText(mActivity.getResources().getString(R.string.app_name))
                .setProgress(0, 0, true);
        mNotifyManager.notify(mFile.getId().hashCode(), mBuilder.build());
        // trigger the download
        long state = StorjAndroid.getInstance(mActivity)
                .downloadFile(mBucket, mFile, FileDownloader.this);
        if (state != 0) {
            // intent for cancel action
            Intent intent = new Intent(mActivity, CancelDownloadReceiver.class);
            intent.putExtra(CancelDownloadReceiver.NOTIFICATION_ID, mFile.getId().hashCode());
            intent.putExtra(CancelDownloadReceiver.DOWNLOAD_STATE, state);
            PendingIntent cancelIntent = PendingIntent.getBroadcast(mActivity, mFile.getId().hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
            // add cancel action to notification
            mBuilder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent);
            mNotifyManager.notify(mFile.getId().hashCode(), mBuilder.build());
        }
    }

    @Override
    public void onProgress(String fileId, double progress, long downloadedBytes, long totalBytes) {
        Long lastNotifiedTime = lastNotifiedMap.get(fileId);
        long now = System.currentTimeMillis();

        // check if 1 second elapsed since last notification or progress it at 100%
        if (progress == 1 || lastNotifiedTime == null || now > lastNotifiedTime + 1150) {
            mBuilder.setProgress(100, (int) (progress * 100), false);
            mNotifyManager.notify(fileId.hashCode(), mBuilder.build());
            // update last notified map
            lastNotifiedMap.put(fileId, now);
        }
    }

    @Override
    public void onComplete(String fileId, String localPath) {
        java.io.File file = new java.io.File(localPath);
        // hide the "download in progress" notification
        mNotifyManager.cancel(fileId.hashCode());
        // show the "download completed" notification
        DownloadManager dm = (DownloadManager) mActivity.getSystemService(Context.DOWNLOAD_SERVICE);
        dm.addCompletedDownload(file.getName(),
                mActivity.getResources().getString(R.string.app_name),
                true,
                getMimeType(file),
                localPath,
                file.length(),
                true);
        // remove from last notified map
        lastNotifiedMap.remove(fileId);
    }

    @Override
    public void onError(String fileId, int code, String message) {
        String msg = (code == Storj.TRANSFER_CANCELED)
                ? "Download canceled"
                : String.format("Download failed: %s (%d)", message, code);
        mBuilder.setProgress(0, 0, false)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentText(msg)
                .mActions.clear();
        mNotifyManager.notify(fileId.hashCode(), mBuilder.build());
        // remove from last notified map
        lastNotifiedMap.remove(fileId);
    }

    private String getMimeType(java.io.File file) {
        String mime = URLConnection.guessContentTypeFromName(file.getName());
        if (mime == null || mime.isEmpty()) {
            mime = "application/octet-stream";
        }
        return mime;
    }
}
