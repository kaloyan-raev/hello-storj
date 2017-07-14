package name.raev.kaloyan.hellostorj;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Map;
import java.util.TreeMap;

import name.raev.kaloyan.hellostorj.jni.NativeLibraries;

/**
 * A placeholder fragment containing a simple view.
 */
public class LibsFragment extends Fragment {

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public LibsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.content_libs, container, false);

        View recyclerView = rootView.findViewById(R.id.lib_list);
        assert recyclerView != null;
        setupRecyclerView((RecyclerView) recyclerView);

        return rootView;
    }
    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        Map<String, String> versions = NativeLibraries.getVersions();
        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(new TreeMap<>(versions)));
    }

    public class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final Map<String, String> mValues;

        public SimpleItemRecyclerViewAdapter(Map<String, String> items) {
            mValues = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            String key = mValues.keySet().toArray(new String[] {})[position];
            holder.mLibrary.setText(key + " version");
            holder.mVersion.setText(mValues.get(key));
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final TextView mLibrary;
            public final TextView mVersion;

            public ViewHolder(View view) {
                super(view);
                mLibrary = (TextView) itemView.findViewById(android.R.id.text1);
                mVersion = (TextView) itemView.findViewById(android.R.id.text2);
            }

            @Override
            public String toString() {
                return super.toString() + " '" + mLibrary.getText() + ": " + mVersion.getText() + "'";
            }
        }
    }

}
