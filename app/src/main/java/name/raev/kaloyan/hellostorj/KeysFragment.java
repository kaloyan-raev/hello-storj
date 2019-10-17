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
import java.io.IOException;
import java.io.PrintWriter;

import io.storj.ApiKey;
import io.storj.EncryptionAccess;
import io.storj.Key;
import io.storj.Project;
import io.storj.Scope;
import io.storj.StorjException;
import io.storj.Uplink;

/**
 * A placeholder fragment containing a simple view.
 */
public class KeysFragment extends Fragment {

    private static final String TAG = "KeyFragment";

    private Button button;
    private ProgressBar progress;
    private Context appContext;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public KeysFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.content_keys, container, false);

        final EditText satelliteAddressEdit = rootView.findViewById(R.id.edit_satellite_addr);
        final EditText apiKeyEdit = rootView.findViewById(R.id.edit_api_key);
        final EditText passphraseEdit = rootView.findViewById(R.id.edit_passphrase);

        button = rootView.findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String satelliteAddress = satelliteAddressEdit.getText().toString();
                String serializedApiKey = apiKeyEdit.getText().toString();
                String passphrase = passphraseEdit.getText().toString();

                boolean error = false;

                if (isEmpty(satelliteAddress)) {
                    satelliteAddressEdit.setError(getText(R.string.error_keys_satellite_addr));
                    error = true;
                } else {
                    satelliteAddressEdit.setError(null);
                }

                if (isEmpty(serializedApiKey)) {
                    apiKeyEdit.setError(getText(R.string.error_keys_api_key));
                    error = true;
                } else {
                    apiKeyEdit.setError(null);
                }

                if (isEmpty(passphrase)) {
                    passphraseEdit.setError(getText(R.string.error_keys_passphrase));
                    error = true;
                } else {
                    passphraseEdit.setError(null);
                }

                if (error) {
                    return;
                }

                button.setEnabled(false);
                progress.setVisibility(View.VISIBLE);
                new ImportKeysTask().execute(satelliteAddress, serializedApiKey, passphrase);
            }
        });

        progress = rootView.findViewById(R.id.progress);

        appContext = getActivity().getApplicationContext();

        return rootView;
    }

    private boolean isEmpty(String pass) {
        return pass == null || pass.length() == 0;
    }

    private Scope getScope(String satelliteAddress, String serializedApiKey, String passphrase) throws StorjException {
        ApiKey apiKey = ApiKey.parse(serializedApiKey);
        try (Uplink uplink = new Uplink();
             Project project = uplink.openProject(satelliteAddress, apiKey)) {
            Key saltedKey = Key.getSaltedKeyFromPassphrase(project, passphrase);
            EncryptionAccess access = new EncryptionAccess(saltedKey);
            return new Scope(satelliteAddress, apiKey, access);
        }
    }

    private class ImportKeysTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String satelliteAddress = params[0];
            String serializedApiKey = params[1];
            String passphrase = params[2];

            try {
                Scope scope = getScope(satelliteAddress, serializedApiKey, passphrase);
                try (PrintWriter file = new PrintWriter(getContext().getFilesDir(), "scope")) {
                    file.print(scope.serialize());
                }
            } catch (StorjException | IOException e) {
                Log.e(TAG, "Error importing keys", e);
                return "Error importing keys: " + e.getMessage();
            }

            return "Keys imported";
        }

        @Override
        protected void onPostExecute(String message) {
            button.setEnabled(true);
            progress.setVisibility(View.GONE);

            Toast.makeText(appContext, message, Toast.LENGTH_LONG).show();
        }
    }

}
