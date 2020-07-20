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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.storj.BucketInfo;
import io.storj.ObjectInfo;
import io.storj.ObjectIterator;
import io.storj.ObjectListOption;
import io.storj.Project;
import io.storj.StorjException;
import io.storj.Uplink;
import name.raev.kaloyan.hellostorj.utils.FileUtils;

import static android.app.Activity.RESULT_OK;

/**
 * A placeholder fragment containing a simple view.
 */
public class FilesFragment extends Fragment {

    private static final String TAG = "FilesFragment";
    public static final String BUCKET = "bucket";

    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    private static final int READ_REQUEST_CODE = 1;

    private RecyclerView mList;
    private ProgressBar mProgress;
    private TextView mStatus;

    private BucketInfo mBucket;
    private Project mProject;
    private ObjectIterator mObjects;

    private SimpleItemRecyclerViewAdapter mListAdapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FilesFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.content_browse, container, false);

        mList = rootView.findViewById(R.id.browse_list);
        setupRecyclerView(mList);

        mProgress = rootView.findViewById(R.id.progress);
        mStatus = rootView.findViewById(R.id.status);

        FloatingActionButton fab = rootView.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isReadPermissionGranted()) {
                    pickFile();
                } else {
                    requestReadPermission();
                }
            }
        });

        mBucket = (BucketInfo) getArguments().getSerializable(BUCKET);
        listFiles(mBucket);

        return rootView;
    }

    @Override
    public void onDestroyView() {
        new Thread() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                if (mObjects != null) {
                    try {
                        mObjects.close();
                        mObjects = null;
                    } catch (StorjException e) {
                        Log.e(TAG, "Error closing iterator", e);
                    }
                }
                if (mProject != null) {
                    try {
                        mProject.close();
                        mProject = null;
                    } catch (StorjException e) {
                        Log.e(TAG, "Error closing project", e);
                    }
                }
            }
        }.start();

        super.onDestroyView();
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        mListAdapter = new SimpleItemRecyclerViewAdapter();
        recyclerView.setAdapter(mListAdapter);
    }

    private void listFiles(final BucketInfo bucketInfo) {
        mProgress.setVisibility(View.VISIBLE);
        mList.setVisibility(View.GONE);
        mStatus.setVisibility(View.GONE);

        new Thread() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                try {
                    mProject = new Uplink().openProject(AccessManager.getAccess(getContext()));
                    mObjects = mProject.listObjects(bucketInfo.getName(), ObjectListOption.recursive(), ObjectListOption.system());
                    onFilesReceived(mObjects);
                } catch (StorjException e) {
                    onError(e.getMessage());
                } catch (ScopeNotFoundException e) {
                    showScopeError();
                }
            }
        }.start();
    }

    private void pickFile() {
        startActivityForResult(FileUtils.createGetContentIntent(), READ_REQUEST_CODE);
    }

    private void showScopeError() {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgress.setVisibility(View.GONE);
                    mStatus.setText(R.string.keys_export_fail);
                    mStatus.setVisibility(View.VISIBLE);
                    Snackbar snackbar = Snackbar.make(mProgress, R.string.keys_imported, Snackbar.LENGTH_LONG);
                    snackbar.setAction(R.string.keys_import_action, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (getResources().getBoolean(R.bool.twoPaneMode)) {
                                Fragment fragment = new KeysFragment();
                                getFragmentManager().beginTransaction()
                                        .replace(R.id.detail_container, fragment)
                                        .commit();
                            } else {
                                Context context = v.getContext();
                                Intent intent = new Intent(context, DetailActivity.class);
                                intent.putExtra(DetailActivity.EXTRA_INDEX, Fragments.ACCESS.ordinal());
                                context.startActivity(intent);
                            }
                        }
                    });
                    snackbar.show();
                }
            });
        }
    }

    private boolean isReadPermissionGranted() {
        return ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestReadPermission() {
        requestPermissions(new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                           PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickFile();
                } else {
                    Snackbar.make(getActivity().findViewById(R.id.browse_list),
                            R.string.upload_permission_denied,
                            Snackbar.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    public void onFilesReceived(final Iterable<ObjectInfo> files) {
        Activity activity = getActivity();
        if (activity != null) {
            final boolean empty = !files.iterator().hasNext();
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgress.setVisibility(View.GONE);
                    if (empty) {
                        mStatus.setText(R.string.browse_no_files);
                        mStatus.setVisibility(View.VISIBLE);
                    } else {
                        mList.setVisibility(View.VISIBLE);
                    }
                    mListAdapter.setFiles(files);
                    mListAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    public void onError(final String message) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgress.setVisibility(View.GONE);
                    mStatus.setText(message);
                    mStatus.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == READ_REQUEST_CODE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            String path = FileUtils.getPath(getContext(), uri);
            if (FileUtils.isLocal(path)) {
                new UploadTask(getActivity(), mBucket, path).execute();
            } else {
                Snackbar.make(getActivity().findViewById(R.id.browse_list),
                        R.string.upload_not_supported,
                        Snackbar.LENGTH_LONG).show();
            }
        }
    }

    class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>
            implements View.OnClickListener {

        private List<ObjectInfo> mFiles;

        SimpleItemRecyclerViewAdapter()
        {
            mFiles = new ArrayList<>();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            view.setOnClickListener(this);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            ObjectInfo file = mFiles.get(position);
            holder.mName.setText(file.getKey());
            holder.mInfo.setText(getContext().getString(R.string.file_list_info,
                    Formatter.formatFileSize(getContext(), file.getSystemMetadata().getContentLength()),
                    new PrettyTime().format(file.getSystemMetadata().getCreated())));
        }

        @Override
        public int getItemCount() {
            return mFiles.size();
        }

        void setFiles(Iterable<ObjectInfo> files) {
            mFiles.clear();
            for (ObjectInfo file : files) {
                mFiles.add(file);
            }
            Collections.sort(mFiles);
        }

        @Override
        public void onClick(View v) {
            int position = mList.getChildAdapterPosition(v);
            if (position != RecyclerView.NO_POSITION) {
                ObjectInfo file = mFiles.get(position);
                Bundle args = new Bundle();
                args.putSerializable(FileInfoFragment.BUCKET, mBucket.getName());
                args.putSerializable(FileInfoFragment.FILE, file);

                DialogFragment dialog = new FileInfoFragment();
                dialog.setArguments(args);
                dialog.show(getFragmentManager(), FileInfoFragment.class.getName());
            }
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView mName;
            final TextView mInfo;

            ViewHolder(View view) {
                super(view);
                mName = itemView.findViewById(android.R.id.text1);
                mInfo = itemView.findViewById(android.R.id.text2);
            }

            @Override
            public String toString() {
                return super.toString() + " " + mName.getText();
            }
        }
    }

}
