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

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import io.storj.StorjException;

/**
 * A placeholder fragment containing a simple view.
 */
public class AccessFragment extends Fragment {

    private static final String TAG = "AccessFragment";

    private Button button;
    private ProgressBar progress;
    private Context appContext;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AccessFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.content_access, container, false);

        final EditText serializedAccessEdit = rootView.findViewById(R.id.edit_serialized_scope);

        button = rootView.findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String serializedAccess = serializedAccessEdit.getText().toString();

                boolean error = false;

                if (isEmpty(serializedAccess)) {
                    serializedAccessEdit.setError(getText(R.string.error_access_serialized));
                    error = true;
                } else {
                    serializedAccessEdit.setError(null);
                }

                if (error) {
                    return;
                }

                button.setEnabled(false);
                progress.setVisibility(View.VISIBLE);
                new ImportScopeTask().execute(serializedAccess);
            }
        });

        progress = rootView.findViewById(R.id.progress);

        appContext = getActivity().getApplicationContext();

        return rootView;
    }

    private boolean isEmpty(String pass) {
        return pass == null || pass.length() == 0;
    }

    private class ImportScopeTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String serializedScope = params[0];

            try {
                AccessManager.setAccess(getContext(), serializedScope);
            } catch (StorjException e) {
                Log.e(TAG, "Error importing scope", e);
                return "Error importing scope: " + e.getMessage();
            }

            return "Scope imported";
        }

        @Override
        protected void onPostExecute(String message) {
            button.setEnabled(true);
            progress.setVisibility(View.GONE);

            Toast.makeText(appContext, message, Toast.LENGTH_LONG).show();
        }
    }

}
