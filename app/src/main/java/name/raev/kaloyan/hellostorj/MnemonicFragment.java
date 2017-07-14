package name.raev.kaloyan.hellostorj;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import name.raev.kaloyan.hellostorj.jni.Storj;

/**
 * A placeholder fragment containing a simple view.
 */
public class MnemonicFragment extends Fragment {

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MnemonicFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.content_mnemonic, container, false);

        final TextView valueView = (TextView) rootView.findViewById(R.id.value);
        valueView.setText(Storj.generateMnemonic(256));

        final Button button = (Button) rootView.findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                valueView.setText(Storj.generateMnemonic(256));
            }
        });

        return rootView;
    }

}
