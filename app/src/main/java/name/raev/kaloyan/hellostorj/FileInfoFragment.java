/***************************************************************************
 * Copyright (C) 2017 Kaloyan Raev
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
 ***************************************************************************/
package name.raev.kaloyan.hellostorj;

import android.app.Dialog;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Process;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.text.format.Formatter;

import name.raev.kaloyan.hellostorj.jni.Bucket;
import name.raev.kaloyan.hellostorj.jni.File;
import name.raev.kaloyan.hellostorj.jni.Storj;
import name.raev.kaloyan.hellostorj.jni.callbacks.DownloadFileCallback;

public class FileInfoFragment extends DialogFragment implements DownloadFileCallback {

    public static final String FILE = "file";

    private Context mContext;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bucket bucket = (Bucket) getArguments().getSerializable(FilesFragment.BUCKET);
        final File file = (File) getArguments().getSerializable(FILE);
        // Use the Builder class for convenient dialog construction
        mContext = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(R.string.fileinfo_title)
                .setMessage(String.format("ID: %s\nName: %s\nCreated: %s\nDecrypted: %b\nSize: %s\nMIME Type: %s\nErasure: %s\nIndex: %s\nHMAC: %s",
                                          file.getId(),
                                          file.getName(),
                                          file.getCreated(),
                                          file.isDecrypted(),
                                          Formatter.formatFileSize(getContext(), file.getSize()),
                                          file.getMimeType(),
                                          file.getErasure(),
                                          file.getIndex(),
                                          file.getHMAC()))
                .setPositiveButton(R.string.fileinfo_download, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // show snackbar for user to watch for download notifications
                        Snackbar.make(getActivity().findViewById(R.id.browse_list),
                                R.string.fileinfo_download_in_progress,
                                Snackbar.LENGTH_LONG).show();
                        // init the download notification
                        mNotifyManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                        mBuilder = new NotificationCompat.Builder(mContext)
                                .setSmallIcon(android.R.drawable.stat_sys_download)
                                .setContentTitle(file.getName())
                                .setContentText(mContext.getResources().getString(R.string.app_name))
                                .setProgress(0, 0, true);
                        mNotifyManager.notify(file.getId().hashCode(), mBuilder.build());
                        // trigger the download
                        new Thread() {
                            @Override
                            public void run() {
                                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                                Storj.getInstance().download(bucket, file, FileInfoFragment.this);
                            }
                        }.start();
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public void onProgress(File file, double progress, long downloadedBytes, long totalBytes) {
        mBuilder.setProgress(100, (int) (progress * 100), false);
        mNotifyManager.notify(file.getId().hashCode(), mBuilder.build());
    }

    @Override
    public void onComplete(File file, String localPath) {
        // hide the "download in progress" notification
        mNotifyManager.cancel(file.getId().hashCode());
        // show the "download completed" notification
        DownloadManager dm = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        dm.addCompletedDownload(file.getName(),
                                mContext.getResources().getString(R.string.app_name),
                                true,
                                file.getMimeType(),
                                localPath,
                                file.getSize(),
                                true);
    }

    @Override
    public void onError(File file, String message) {
        mBuilder.setProgress(0, 0, false)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentText(message);
        mNotifyManager.notify(file.getId().hashCode(), mBuilder.build());
    }
}
