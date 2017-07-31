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

import java.io.File;

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

    public static String caInfoPath;

    public static boolean keysExist() {
        return getAuthFile().exists();
    }

    public static Keys getKeys(String passphrase) {
        return exportKeys(getAuthFile().getPath(), passphrase);
    }

    /**
     *
     * @param keys
     * @param passphrase
     * @return <code>true</code> if importing keys was successful, <code>false</code> otherwise
     */
    public static boolean importKeys(Keys keys, String passphrase) {
        return writeAuthFile(getAuthFile().getPath(), keys.getUser(), keys.getPass(), keys.getMnemonic(), passphrase);
    }

    private static File getAuthFile() {
        if (appDir == null) {
            throw new IllegalStateException("appDir is not set");
        }

        return new File(appDir, HOST + ".json");
    }

    public static native Keys exportKeys(String location, String passphrase);

    public static native boolean writeAuthFile(String location, String user, String pass, String mnemonic, String passphrase);

    public static native void getBuckets(String user, String pass, String mnemonic, GetBucketsCallback callback);

    public static native void listFiles(String user, String pass, String mnemonic, String bucketId, ListFilesCallback callback);

    public static native void getInfo(GetInfoCallback callback);

    public static native boolean checkMnemonic(String mnemonic);

    public static native String generateMnemonic(int strength);

    public static native long getTimestamp();
}
