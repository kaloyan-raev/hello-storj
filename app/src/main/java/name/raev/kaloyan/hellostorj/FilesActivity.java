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

import android.Manifest;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import name.raev.kaloyan.hellostorj.jni.Bucket;
import name.raev.kaloyan.hellostorj.jni.File;
import name.raev.kaloyan.hellostorj.jni.Storj;
import name.raev.kaloyan.hellostorj.jni.callbacks.DownloadFileCallback;

public class FilesActivity extends AppCompatActivity implements FileInfoFragment.DownloadListener, DownloadFileCallback {

    public static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    private Bucket mBucket;
    private File mFile;

    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Show the Up button in new Intent(this, DetailActivity.class)the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity_main
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity_main
            // using a fragment transaction.
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                Bucket bucket = (Bucket) extras.getSerializable(FilesFragment.BUCKET);
                setTitle(bucket.getName());

                FilesFragment fragment = new FilesFragment();
                Bundle bundle = new Bundle();
                bundle.putSerializable(FilesFragment.BUCKET, bucket);
                fragment.setArguments(bundle);
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.detail_container, fragment)
                        .commit();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity_main, the Up button is shown. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra(DetailActivity.EXTRA_INDEX, Fragments.BROWSE.ordinal());
            navigateUpTo(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    download();
                } else {
                    Snackbar.make(findViewById(R.id.browse_list),
                            R.string.download_permission_denied,
                            Snackbar.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    @Override
    public void onDownload(Bucket bucket, File file) {
        mBucket = bucket;
        mFile = file;
        // check if write permission is already granted
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                    PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        } else {
            download();
        }
    }

    private void download() {
        // show snackbar for user to watch for download notifications
        Snackbar.make(findViewById(R.id.browse_list),
                R.string.download_in_progress,
                Snackbar.LENGTH_LONG).show();
        // init the download notification
        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(mFile.getName())
                .setContentText(getResources().getString(R.string.app_name))
                .setProgress(0, 0, true);
        mNotifyManager.notify(mFile.getId().hashCode(), mBuilder.build());
        // trigger the download
        new Thread() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                Storj.getInstance().download(mBucket, mFile, FilesActivity.this);
            }
        }.start();
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
        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        dm.addCompletedDownload(file.getName(),
                getResources().getString(R.string.app_name),
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
