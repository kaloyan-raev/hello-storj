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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

import name.raev.kaloyan.hellostorj.jni.File;
import name.raev.kaloyan.hellostorj.jni.Keys;
import name.raev.kaloyan.hellostorj.jni.Storj;
import name.raev.kaloyan.hellostorj.jni.callbacks.ListFilesCallback;

/**
 * A placeholder fragment containing a simple view.
 */
public class FilesFragment extends Fragment implements ListFilesCallback {

    public static final String BUCKET_ID = "bucketId";

    private RecyclerView mList;
    private ProgressBar mProgress;

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

        mList = (RecyclerView) rootView.findViewById(R.id.buckets_list);
        setupRecyclerView(mList);

        mProgress = (ProgressBar) rootView.findViewById(R.id.progress);

        Bundle bundle = getArguments();
        String bucketId = bundle.getString(BUCKET_ID);

        listFiles(bucketId);

        return rootView;
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        mListAdapter = new SimpleItemRecyclerViewAdapter();
        recyclerView.setAdapter(mListAdapter);
    }

    private void listFiles(final String bucketId) {
        mProgress.setVisibility(View.VISIBLE);
        mList.setVisibility(View.GONE);

        new Thread() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                Keys keys = Storj.getKeys("");
                if (keys == null) {
                    showKeysError();
                } else {
                    Storj.listFiles(keys.getUser(), keys.getPass(), keys.getMnemonic(), bucketId, FilesFragment.this);
                }
            }
        }.start();
    }

    private void showKeysError() {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgress.setVisibility(View.GONE);
                    Snackbar snackbar = Snackbar.make(mProgress, R.string.keys_export_fail, Snackbar.LENGTH_LONG);
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
                                intent.putExtra(DetailActivity.EXTRA_INDEX, Fragments.KEYS.ordinal());
                                context.startActivity(intent);
                            }
                        }
                    });
                    snackbar.show();
                }
            });
        }
    }

    @Override
    public void onFilesReceived(final File[] files) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Arrays.sort(files);
                    mListAdapter.setFiles(files);
                    mListAdapter.notifyDataSetChanged();
                    mProgress.setVisibility(View.GONE);
                    mList.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    @Override
    public void onError(final String message) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgress.setVisibility(View.GONE);
                    mList.setVisibility(View.VISIBLE);
                    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private File[] mFiles;

        public SimpleItemRecyclerViewAdapter()
        {
            mFiles = new File[0];
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            File file = mFiles[position];
            holder.mId.setText(file.getId());
            holder.mName.setText(file.getName());
        }

        @Override
        public int getItemCount() {
            return mFiles.length;
        }

        public void setFiles(File[] files) {
            mFiles = files;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final TextView mId;
            public final TextView mName;

            public ViewHolder(View view) {
                super(view);
                mId = (TextView) itemView.findViewById(android.R.id.text2);
                mName = (TextView) itemView.findViewById(android.R.id.text1);
            }

            @Override
            public String toString() {
                return super.toString() + " " + mId.getText() + " " + mName.getText();
            }
        }
    }

}
