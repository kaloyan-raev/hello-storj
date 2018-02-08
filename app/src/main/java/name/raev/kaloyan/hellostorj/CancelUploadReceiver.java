/*
 * Copyright (C) 2018 Kaloyan Raev
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

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import io.storj.libstorj.android.StorjAndroid;

public class CancelUploadReceiver extends BroadcastReceiver {

    public static final String NOTIFICATION_ID = "notificationId";
    public static final String UPLOAD_STATE = "uploadState";

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            int id = extras.getInt(NOTIFICATION_ID);
            long state = extras.getLong(UPLOAD_STATE);

            StorjAndroid.getInstance(context).cancelUpload(state);

            NotificationManager notifyManager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            notifyManager.cancel(id);
        }
    }
}
