/***************************************************************************
 * Copyright (C) 2017 Kaloyan Raev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ***************************************************************************/
package name.raev.kaloyan.hellostorj.jni;

import android.os.Environment;

import name.raev.kaloyan.hellostorj.jni.callbacks.CreateBucketCallback;
import name.raev.kaloyan.hellostorj.jni.callbacks.DownloadFileCallback;
import name.raev.kaloyan.hellostorj.jni.callbacks.GetBucketsCallback;
import name.raev.kaloyan.hellostorj.jni.callbacks.GetInfoCallback;
import name.raev.kaloyan.hellostorj.jni.callbacks.ListFilesCallback;

public class Storj {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private static final String HOST = "api.storj.io";

    public static String appDir;

    private static Storj instance;

    private Keys keys;

    private Storj() {
        keys = null;
    }

    public static Storj getInstance() {
        if (instance == null) {
            instance = new Storj();
        }
        return instance;
    }

    public boolean keysExist() {
        return getAuthFile().exists();
    }

    public Keys getKeys(String passphrase) {
        if (keys == null) {
            keys = exportKeys(getAuthFile().getPath(), passphrase);
        }
        return keys;
    }

    /**
     *
     * @param keys
     * @param passphrase
     * @return <code>true</code> if importing keys was successful, <code>false</code> otherwise
     */
    public boolean importKeys(Keys keys, String passphrase) {
        boolean success = writeAuthFile(getAuthFile().getPath(), keys.getUser(), keys.getPass(), keys.getMnemonic(), passphrase);
        if (success) {
            this.keys = keys;
        }
        return success;
    }

    private java.io.File getAuthFile() {
        if (appDir == null) {
            throw new IllegalStateException("appDir is not set");
        }

        return new java.io.File(appDir, HOST + ".json");
    }

    public void download(Bucket bucket, File file, DownloadFileCallback callback) {
        java.io.File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String path = new java.io.File(downloads, file.getName()).getPath();
        Keys keys = getKeys("");
        downloadFile(bucket.getId(), file, path, keys.getUser(), keys.getPass(), keys.getMnemonic(), callback);
    }

    public static native void downloadFile(String bucketId, File file, String path, String user, String pass, String mnemonic, DownloadFileCallback callback);

    public static native Keys exportKeys(String location, String passphrase);

    public static native boolean writeAuthFile(String location, String user, String pass, String mnemonic, String passphrase);

    public static native void getBuckets(String user, String pass, String mnemonic, GetBucketsCallback callback);

    public static native void createBucket(String user, String pass, String mnemonic, String bucketName, CreateBucketCallback callback);

    public static native void listFiles(String user, String pass, String mnemonic, String bucketId, ListFilesCallback callback);

    public static native void getInfo(GetInfoCallback callback);

    public static native void setCAInfoPath(String path);

    public static native boolean checkMnemonic(String mnemonic);

    public static native String generateMnemonic(int strength);

    public static native long getTimestamp();
}
