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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.storj.BucketInfo;
import io.storj.Project;
import io.storj.StorjException;
import io.storj.Uplink;

/**
 * A placeholder fragment containing a simple view.
 */
public class BucketsFragment extends Fragment {

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

        mList = rootView.findViewById(R.id.browse_list);
        setupRecyclerView(mList);

        mProgress = rootView.findViewById(R.id.progress);
        mStatus = rootView.findViewById(R.id.status);

        FloatingActionButton fab = rootView.findViewById(R.id.fab);
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
                try (Uplink uplink = new Uplink();
                     Project project = uplink.openProject(ScopeManager.getScope(getContext()))) {
                    onBucketsReceived(project.listBuckets());
                } catch (StorjException e) {
                    onError(0, e.getMessage());
                } catch (ScopeNotFoundException e) {
                    showScopeError();
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
                try (Uplink uplink = new Uplink();
                     Project project = uplink.openProject(ScopeManager.getScope(getContext()))) {
                    onBucketCreated(project.createBucket(name));
                } catch (StorjException e) {
                    onError(0, e.getMessage());
                } catch (ScopeNotFoundException e) {
                    showScopeError();
                }
            }
        }.start();
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
                                intent.putExtra(DetailActivity.EXTRA_INDEX, Fragments.SCOPE.ordinal());
                                context.startActivity(intent);
                            }
                        }
                    });
                    snackbar.show();
                }
            });
        }
    }

    public void onBucketsReceived(final Iterable<BucketInfo> buckets) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgress.setVisibility(View.GONE);
                    if (!buckets.iterator().hasNext()) {
                        mStatus.setText(R.string.browse_no_buckets);
                        mStatus.setVisibility(View.VISIBLE);
                    } else {
                        mList.setVisibility(View.VISIBLE);
                    }
                    mListAdapter.setBuckets(buckets);
                    mListAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    public void onBucketCreated(final BucketInfo bucket) {
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

    public void onError(final String bucketName, final int code, final String message) {
        onError(code, message);
    }

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

    private void openBucket(BucketInfo bucket) {
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

        private List<BucketInfo> mBuckets;

        SimpleItemRecyclerViewAdapter()
        {
            mBuckets = new ArrayList<>();
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
            BucketInfo bucket = mBuckets.get(position);
            holder.mName.setText(bucket.getName());
            holder.mCreated.setText(new PrettyTime().format(bucket.getCreated()));
        }

        @Override
        public int getItemCount() {
            return mBuckets.size();
        }

        void setBuckets(Iterable<BucketInfo> buckets) {
            mBuckets.clear();
            for (BucketInfo bucket : buckets) {
                mBuckets.add(bucket);
            }
            Collections.sort(mBuckets);
        }

        @Override
        public void onClick(View v) {
            int position = mList.getChildAdapterPosition(v);
            if (position != RecyclerView.NO_POSITION) {
                openBucket(mBuckets.get(position));
            }
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView mName;
            final TextView mCreated;

            ViewHolder(View view) {
                super(view);
                mName = itemView.findViewById(android.R.id.text1);
                mCreated = itemView.findViewById(android.R.id.text2);
            }

            @Override
            public String toString() {
                return super.toString() + mName.getText();
            }
        }
    }

}
