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
import android.os.Bundle;
import android.os.Process;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.storj.libstorj.RegisterCallback;
import io.storj.libstorj.android.StorjAndroid;

/**
 * A placeholder fragment containing a simple view.
 */
public class RegisterFragment extends Fragment implements RegisterCallback {

    private Button mButton;
    private ProgressBar mProgress;
    private TextView mStatus;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public RegisterFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.content_register, container, false);

        final EditText userEdit = (EditText) rootView.findViewById(R.id.edit_user);
        final EditText passEdit = (EditText) rootView.findViewById(R.id.edit_pass);
        final EditText confirmPassEdit = (EditText) rootView.findViewById(R.id.edit_confirm_pass);

        mButton = (Button) rootView.findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String user = userEdit.getText().toString();
                String pass = passEdit.getText().toString();
                String confirmPass = confirmPassEdit.getText().toString();

                boolean error = false;

                if (!isValidEmail(user)) {
                    userEdit.setError(getText(R.string.error_keys_user));
                    error = true;
                } else {
                    userEdit.setError(null);
                }

                if (!isValidPassword(pass)) {
                    passEdit.setError(getText(R.string.error_keys_pass));
                    error = true;
                } else {
                    passEdit.setError(null);
                }

                if (!doPasswordsMatch(pass, confirmPass)) {
                    confirmPassEdit.setError(getText(R.string.error_register_pass_dont_match));
                    error = true;
                } else {
                    confirmPassEdit.setError(null);
                }

                if (!error) {
                    register(user, pass);
                }
            }
        });

        mProgress = (ProgressBar) rootView.findViewById(R.id.progress);
        mStatus = (TextView) rootView.findViewById(R.id.status);

        return rootView;
    }

    private boolean isValidEmail(String email) {
        String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
        Pattern pattern = Pattern.compile(EMAIL_PATTERN);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    private boolean isValidPassword(String pass) {
        return pass != null && pass.length() > 0;
    }

    private boolean doPasswordsMatch(String pass, String confirmPass) {
        return TextUtils.equals(pass, confirmPass);
    }

    private void register(final String user, final String pass) {
        mButton.setEnabled(false);
        mProgress.setVisibility(View.VISIBLE);
        mStatus.setVisibility(View.GONE);

        new Thread() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                StorjAndroid.getInstance(getContext())
                        .register(user, pass, RegisterFragment.this);
            }
        }.start();
    }

    @Override
    public void onConfirmationPending(final String email) {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String message = String.format(
                            activity.getResources().getString(R.string.register_confirmation_sent),
                            email);
                    mStatus.setText(message);
                    mButton.setEnabled(true);
                    mProgress.setVisibility(View.GONE);
                    mStatus.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    @Override
    public void onError(final int code, final String message) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mStatus.setText(message);
                    mButton.setEnabled(true);
                    mProgress.setVisibility(View.GONE);
                    mStatus.setVisibility(View.VISIBLE);
                }
            });
        }
    }

}
