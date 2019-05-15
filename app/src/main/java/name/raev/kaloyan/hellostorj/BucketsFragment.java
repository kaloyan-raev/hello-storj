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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.ocpsoft.prettytime.PrettyTime;

import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import io.storj.libstorj.Bucket;
import io.storj.libstorj.CreateBucketCallback;
import io.storj.libstorj.GetBucketsCallback;
import io.storj.libstorj.KeysNotFoundException;
import io.storj.libstorj.Storj;
import io.storj.libstorj.android.StorjAndroid;

/**
 * A placeholder fragment containing a simple view.
 */
public class BucketsFragment extends Fragment implements GetBucketsCallback, CreateBucketCallback {

    private static final int NEW_BUCKET_FRAGMENT = 1;

    private RecyclerView mList;
    private ProgressBar mProgress;
    private TextView mStatus;

    private SimpleItemRecyclerViewAdapter mListAdapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public BucketsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.content_browse, container, false);

        mList = (RecyclerView) rootView.findViewById(R.id.browse_list);
        setupRecyclerView(mList);

        mProgress = (ProgressBar) rootView.findViewById(R.id.progress);
        mStatus = (TextView) rootView.findViewById(R.id.status);

        FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment dialog = new NewBucketFragment();
                dialog.setTargetFragment(BucketsFragment.this, NEW_BUCKET_FRAGMENT);
                dialog.show(getFragmentManager(), NewBucketFragment.class.getName());
            }
        });

        getBuckets();

        return rootView;
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        mListAdapter = new SimpleItemRecyclerViewAdapter();
        recyclerView.setAdapter(mListAdapter);
    }

    private void getBuckets() {
        mProgress.setVisibility(View.VISIBLE);
        mList.setVisibility(View.GONE);
        mStatus.setVisibility(View.GONE);

        new Thread() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                try {
                    try {
                        StorjAndroid.getInstance(getContext(), Fragments.URL)
                                .getBuckets(BucketsFragment.this);
                    } catch (MalformedURLException e) {
                        onError(0, "Invalid Bridge URL: " + Fragments.URL);
                    }
                } catch (KeysNotFoundException e) {
                    showKeysError();
                }
            }
        }.start();
    }

    private void createBucket(final String name) {
        mProgress.setVisibility(View.VISIBLE);
        mList.setVisibility(View.GONE);
        mStatus.setVisibility(View.GONE);

        new Thread() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                try {
                    try {
                        StorjAndroid.getInstance(getContext(), Fragments.URL)
                                .createBucket(name, BucketsFragment.this);
                    } catch (MalformedURLException e) {
                        onError(Storj.CURLE_URL_MALFORMAT, "Invalid Bridge URL: " + Fragments.URL);
                    }
                } catch (KeysNotFoundException e) {
                    showKeysError();
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
    public void onBucketsReceived(final Bucket[] buckets) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgress.setVisibility(View.GONE);
                    if (buckets.length == 0) {
                        mStatus.setText(R.string.browse_no_buckets);
                        mStatus.setVisibility(View.VISIBLE);
                    } else {
                        mList.setVisibility(View.VISIBLE);
                    }
                    Arrays.sort(buckets);
                    mListAdapter.setBuckets(buckets);
                    mListAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    @Override
    public void onBucketCreated(final Bucket bucket) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    openBucket(bucket);
                }
            });
        }
    }

    @Override
    public void onError(final String bucketName, final int code, final String message) {
        onError(code, message);
    }

    @Override
    public void onError(final int code, final String message) {
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

    private void openBucket(Bucket bucket) {
        if (getResources().getBoolean(R.bool.twoPaneMode)) {
            FilesFragment fragment = new FilesFragment();
            Bundle bundle = new Bundle();
            bundle.putSerializable(FilesFragment.BUCKET, bucket);
            fragment.setArguments(bundle);
            getFragmentManager().beginTransaction()
                    .replace(R.id.detail_container, fragment)
                    .commit();
        } else {
            Context context = getContext();
            Intent intent = new Intent(context, FilesActivity.class);
            intent.putExtra(FilesFragment.BUCKET, bucket);
            context.startActivity(intent);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case NEW_BUCKET_FRAGMENT:
                if (resultCode == Activity.RESULT_OK) {
                    String name = data.getStringExtra(NewBucketFragment.BUCKET_NAME);
                    createBucket(name);
                }
                break;
        }
    }

    class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>
            implements View.OnClickListener {

        private Bucket[] mBuckets;

        SimpleItemRecyclerViewAdapter()
        {
            mBuckets = new Bucket[0];
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
            Bucket bucket = mBuckets[position];
            holder.mName.setText(bucket.getName());

            try {
                Date date = SimpleDateFormat.getDateTimeInstance().parse(bucket.getCreated());
                holder.mCreated.setText(new PrettyTime().format(date));
            } catch (ParseException e) {
                holder.mCreated.setText(e.getMessage());
            }
        }

        @Override
        public int getItemCount() {
            return mBuckets.length;
        }

        public void setBuckets(Bucket[] buckets) {
            mBuckets = buckets;
        }

        @Override
        public void onClick(View v) {
            int position = mList.getChildAdapterPosition(v);
            if (position != RecyclerView.NO_POSITION) {
                openBucket(mBuckets[position]);
            }
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView mName;
            final TextView mCreated;

            ViewHolder(View view) {
                super(view);
                mName = (TextView) itemView.findViewById(android.R.id.text1);
                mCreated = (TextView) itemView.findViewById(android.R.id.text2);
            }

            @Override
            public String toString() {
                return super.toString() + mName.getText();
            }
        }
    }

}
