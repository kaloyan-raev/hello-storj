/*
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
 */
package name.raev.kaloyan.hellostorj;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.format.Formatter;

import java.util.Locale;
import java.util.Objects;

import io.storj.BucketInfo;
import io.storj.ObjectInfo;

public class FileInfoFragment extends DialogFragment {

    public static final String BUCKET = "bucket";
    public static final String FILE = "file";

    interface DownloadListener {
        void onDownload(String bucket, ObjectInfo file);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String bucket = (String) getArguments().getSerializable(BUCKET);
        final ObjectInfo file = (ObjectInfo) getArguments().getSerializable(FILE);
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.title_fileinfo)
                .setMessage(String.format(Locale.getDefault(),
                                          "Bucket: %s\nKey: %s\nSize: %s\nCreated: %s\nExpires: %s\nMetadata: %s",
                                          bucket,
                                          file.getKey(),
                                          Formatter.formatFileSize(getContext(), file.getSystemMetadata().getContentLength()),
                                          file.getSystemMetadata().getCreated(),
                                          file.getSystemMetadata().getExpires(),
                                          Objects.toString(file.getCustomMetadata())))
                .setPositiveButton(R.string.button_download, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ((DownloadListener) getActivity()).onDownload(bucket, file);
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
