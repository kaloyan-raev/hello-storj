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

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.format.Formatter;

import name.raev.kaloyan.hellostorj.jni.Bucket;
import name.raev.kaloyan.hellostorj.jni.File;

public class FileInfoFragment extends DialogFragment {

    public static final String FILE = "file";

    private Bucket mBucket;
    private File mFile;

    public interface DownloadListener {
        void onDownload(Bucket bucket, File file);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mBucket = (Bucket) getArguments().getSerializable(FilesFragment.BUCKET);
        mFile = (File) getArguments().getSerializable(FILE);
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.title_fileinfo)
                .setMessage(String.format("ID: %s\nName: %s\nCreated: %s\nDecrypted: %b\nSize: %s\nMIME Type: %s\nErasure: %s\nIndex: %s\nHMAC: %s",
                                          mFile.getId(),
                                          mFile.getName(),
                                          mFile.getCreated(),
                                          mFile.isDecrypted(),
                                          Formatter.formatFileSize(getContext(), mFile.getSize()),
                                          mFile.getMimeType(),
                                          mFile.getErasure(),
                                          mFile.getIndex(),
                                          mFile.getHMAC()))
                .setPositiveButton(R.string.button_download, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ((DownloadListener) getActivity()).onDownload(mBucket, mFile);
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
