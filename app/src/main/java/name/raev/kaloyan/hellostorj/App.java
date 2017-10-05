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

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Process;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import io.storj.libstorj.Storj;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        setAppDir();
        setTempDir();
        copyCABundle();
    }

    private void setAppDir() {
        Storj.appDir = getFilesDir().getPath();
    }


    private void setTempDir() {
        try {
            Os.setenv("STORJ_TEMP", getCacheDir().getPath(), true);
        } catch (ErrnoException e) {
            Log.e(App.class.getName(), e.getMessage(), e);
        }
    }

    private void copyCABundle() {
        new Thread() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                String filename = "cacert.pem";
                AssetManager assetManager = getAssets();
                try (InputStream in = assetManager.open(filename);
                    OutputStream out = openFileOutput(filename, Context.MODE_PRIVATE)) {
                    copy(in, out);
                    Os.setenv("STORJ_CAINFO", getFileStreamPath(filename).getPath(), true);
                } catch (IOException | ErrnoException e) {
                    Log.e(App.class.getName(), e.getMessage(), e);
                }
            }
        }.start();
    }

    private void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

}
