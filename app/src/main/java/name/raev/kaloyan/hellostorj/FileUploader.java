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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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

class FileUploader {

    private Activity mActivity;
    private BucketInfo mBucket;
    private String mFilePath;

    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;

    Map<String, Long> lastNotifiedMap = Collections.synchronizedMap(new HashMap<String, Long>());

    FileUploader(Activity activity, BucketInfo bucket, String filePath) {
        mActivity = activity;
        mBucket = bucket;
        mFilePath = filePath;
    }

    public void upload() {
        // show snackbar for user to watch for upload notifications
        Snackbar.make(mActivity.findViewById(R.id.browse_list),
                R.string.upload_in_progress,
                Snackbar.LENGTH_LONG).show();
        // init the upload notification
        mNotifyManager = (NotificationManager) mActivity.getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(mActivity, FileTransferChannel.ID)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setColor(ContextCompat.getColor(mActivity, R.color.colorNotification))
                .setContentTitle(new File(mFilePath).getName())
                .setContentText(mActivity.getResources().getString(R.string.app_name))
                .setOnlyAlertOnce(true)
                .setProgress(0, 0, true);
        mNotifyManager.notify(mFilePath.hashCode(), mBuilder.build());
        // trigger the upload
        try (Uplink uplink = new Uplink();
             Project project = uplink.openProject(Scope.parse(SCOPE));
             Bucket bucket = project.openBucket(mBucket.getName(), Scope.parse(SCOPE));
             InputStream file = new FileInputStream(mFilePath)) {
            bucket.uploadObject(new File(mFilePath).getName(), file);
            // intent for cancel action
            Intent intent = new Intent(mActivity, CancelUploadReceiver.class);
            intent.putExtra(CancelUploadReceiver.NOTIFICATION_ID, mFilePath.hashCode());
//            intent.putExtra(CancelUploadReceiver.UPLOAD_STATE, state);
            PendingIntent cancelIntent = PendingIntent.getBroadcast(
                    mActivity, mFilePath.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
            // add cancel action to notification
            mBuilder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent);
            mNotifyManager.notify(mFilePath.hashCode(), mBuilder.build());
        } catch (StorjException | IOException e) {
            onError(mFilePath, 0, e.getMessage());
        }
    }

    public void onProgress(String filePath, double progress, long uploadedBytes, long totalBytes) {
        Long lastNotifiedTime = lastNotifiedMap.get(filePath);
        long now = System.currentTimeMillis();

        // check if 1 second elapsed since last notification or progress it at 100%
        if (progress == 1 || lastNotifiedTime == null || now > lastNotifiedTime + 1150) {
            mBuilder.setProgress(100, (int) (progress * 100), false);
            mNotifyManager.notify(filePath.hashCode(), mBuilder.build());
            // update last notified map
            lastNotifiedMap.put(filePath, now);
        }
    }

    @SuppressLint("RestrictedApi")
    public void onComplete(String filePath, ObjectInfo file) {
        Intent intent = new Intent(mActivity, FilesActivity.class);
        intent.putExtra(FilesFragment.BUCKET, mBucket);
        PendingIntent resultIntent =
                PendingIntent.getActivity(
                        mActivity,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        mBuilder.setProgress(0, 0, false)
                .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                .setContentText(mActivity.getResources().getString(R.string.upload_complete))
                .setContentIntent(resultIntent)
                .setAutoCancel(true)
                .mActions.clear();
        mNotifyManager.notify(file.hashCode(), mBuilder.build());
        // remove from last notified map
        lastNotifiedMap.remove(filePath);
    }

    @SuppressLint("RestrictedApi")
    public void onError(String filePath, int code, String message) {
        String msg = /*(code == Storj.TRANSFER_CANCELED)
                ? "Upload canceled"
                :*/ String.format("Upload failed: %s (%d)", message, code);
        mBuilder.setProgress(0, 0, false)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentText(msg)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(msg))
                .mActions.clear();
        mNotifyManager.notify(filePath.hashCode(), mBuilder.build());
        // remove from last notified map
        lastNotifiedMap.remove(filePath);
    }
}
