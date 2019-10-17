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

import static name.raev.kaloyan.hellostorj.Fragments.SCOPE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.storj.Bucket;
import io.storj.BucketInfo;
import io.storj.ObjectInfo;
import io.storj.Project;
import io.storj.Scope;
import io.storj.StorjException;
import io.storj.Uplink;

class FileDownloader {

    static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    private Activity mActivity;
    private BucketInfo mBucket;
    private ObjectInfo mFile;

    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;

    private Map<ObjectInfo, Long> lastNotifiedMap = Collections.synchronizedMap(new HashMap<ObjectInfo, Long>());

    FileDownloader(Activity activity, BucketInfo bucket, ObjectInfo file) {
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
                .setContentTitle(mFile.getPath())
                .setContentText(mActivity.getResources().getString(R.string.app_name))
                .setOnlyAlertOnce(true)
                .setProgress(0, 0, true);
        mNotifyManager.notify(mFile.hashCode(), mBuilder.build());
        // trigger the download
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String fileName = new File(mFile.getPath()).getName();
        try (Uplink uplink = new Uplink();
             Project project = uplink.openProject(Scope.parse(SCOPE));
             Bucket bucket = project.openBucket(mBucket.getName(), Scope.parse(SCOPE));
             OutputStream out = new FileOutputStream(new File(downloadDir, fileName))) {
            bucket.downloadObject(mFile.getPath(), out);
            // intent for cancel action
            Intent intent = new Intent(mActivity, CancelDownloadReceiver.class);
            intent.putExtra(CancelDownloadReceiver.NOTIFICATION_ID, mFile.hashCode());
//            intent.putExtra(CancelDownloadReceiver.DOWNLOAD_STATE, state);
            PendingIntent cancelIntent = PendingIntent.getBroadcast(mActivity, mFile.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
            // add cancel action to notification
            mBuilder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent);
            mNotifyManager.notify(mFile.hashCode(), mBuilder.build());
        } catch (StorjException | IOException e) {
            onError(mFile, 0, e.getMessage());
        }
    }

    public void onProgress(ObjectInfo file, double progress, long downloadedBytes, long totalBytes) {
        Long lastNotifiedTime = lastNotifiedMap.get(file);
        long now = System.currentTimeMillis();

        // check if 1 second elapsed since last notification or progress it at 100%
        if (progress == 1 || lastNotifiedTime == null || now > lastNotifiedTime + 1150) {
            mBuilder.setProgress(100, (int) (progress * 100), false);
            mNotifyManager.notify(file.hashCode(), mBuilder.build());
            // update last notified map
            lastNotifiedMap.put(file, now);
        }
    }

    public void onComplete(ObjectInfo file, String localPath) {
        java.io.File localFile = new java.io.File(localPath);
        // hide the "download in progress" notification
        mNotifyManager.cancel(file.hashCode());
        // show the "download completed" notification
        DownloadManager dm = (DownloadManager) mActivity.getSystemService(Context.DOWNLOAD_SERVICE);
        dm.addCompletedDownload(localFile.getName(),
                mActivity.getResources().getString(R.string.app_name),
                true,
                getMimeType(localFile),
                localPath,
                file.getSize(),
                true);
        // remove from last notified map
        lastNotifiedMap.remove(file);
    }

    @SuppressLint("RestrictedApi")
    public void onError(ObjectInfo file, int code, String message) {
        String msg = /*(code == Storj.TRANSFER_CANCELED)
                ? "Download canceled"
                : */String.format("Download failed: %s (%d)", message, code);
        mBuilder.setProgress(0, 0, false)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentText(msg)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(msg))
                .mActions.clear();
        mNotifyManager.notify(file.hashCode(), mBuilder.build());
        // remove from last notified map
        lastNotifiedMap.remove(file);
    }

    private String getMimeType(java.io.File file) {
        String mime = URLConnection.guessContentTypeFromName(file.getName());
        if (mime == null || mime.isEmpty()) {
            mime = "application/octet-stream";
        }
        return mime;
    }
}
