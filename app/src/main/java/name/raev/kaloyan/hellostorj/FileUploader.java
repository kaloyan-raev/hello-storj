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

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.storj.libstorj.Bucket;
import io.storj.libstorj.File;
import io.storj.libstorj.Storj;
import io.storj.libstorj.UploadFileCallback;
import io.storj.libstorj.android.StorjAndroid;

class FileUploader implements UploadFileCallback {

    private Activity mActivity;
    private Bucket mBucket;
    private String mFilePath;

    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;

    Map<String, Long> lastNotifiedMap = Collections.synchronizedMap(new HashMap<String, Long>());

    FileUploader(Activity activity, Bucket bucket, String filePath) {
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
                .setContentTitle(new java.io.File(mFilePath).getName())
                .setContentText(mActivity.getResources().getString(R.string.app_name))
                .setOnlyAlertOnce(true)
                .setProgress(0, 0, true);
        mNotifyManager.notify(mFilePath.hashCode(), mBuilder.build());
        // trigger the upload
        try {
            long state = StorjAndroid.getInstance(mActivity, Fragments.URL)
                    .uploadFile(mBucket, mFilePath, FileUploader.this);
            if (state != 0) {
                // intent for cancel action
                Intent intent = new Intent(mActivity, CancelUploadReceiver.class);
                intent.putExtra(CancelUploadReceiver.NOTIFICATION_ID, mFilePath.hashCode());
                intent.putExtra(CancelUploadReceiver.UPLOAD_STATE, state);
                PendingIntent cancelIntent = PendingIntent.getBroadcast(
                        mActivity, mFilePath.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
                // add cancel action to notification
                mBuilder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent);
                mNotifyManager.notify(mFilePath.hashCode(), mBuilder.build());
            }
        } catch (MalformedURLException e) {
            onError(mFilePath, Storj.CURLE_URL_MALFORMAT, "Invalid Bridge URL: " + Fragments.URL);
        }
    }

    @Override
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

    @Override
    public void onComplete(String filePath, File file) {
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
        mNotifyManager.notify(filePath.hashCode(), mBuilder.build());
        // remove from last notified map
        lastNotifiedMap.remove(filePath);
    }

    @Override
    public void onError(String filePath, int code, String message) {
        String msg = (code == Storj.TRANSFER_CANCELED)
                ? "Upload canceled"
                : String.format("Upload failed: %s (%d)", message, code);
        mBuilder.setProgress(0, 0, false)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentText(msg)
                .mActions.clear();
        mNotifyManager.notify(filePath.hashCode(), mBuilder.build());
        // remove from last notified map
        lastNotifiedMap.remove(filePath);
    }
}
