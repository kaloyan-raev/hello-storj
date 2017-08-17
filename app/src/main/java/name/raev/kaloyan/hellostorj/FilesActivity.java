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

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import name.raev.kaloyan.hellostorj.jni.Bucket;
import name.raev.kaloyan.hellostorj.jni.File;

public class FilesActivity extends AppCompatActivity implements FileInfoFragment.DownloadListener {

    private FileDownloader mDownloader;

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
            case FileDownloader.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                if (mDownloader != null) {
                    mDownloader.onRequestPermissionsResult(grantResults);
                }
            }
        }
    }

    @Override
    public void onDownload(Bucket bucket, File file) {
        mDownloader = new FileDownloader(this, bucket, file);
        mDownloader.download();
    }
}
