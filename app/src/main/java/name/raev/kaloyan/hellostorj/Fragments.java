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

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;

public enum Fragments {
    
    BROWSE(R.string.title_buckets, BucketsFragment.class),
    KEYS(R.string.title_keys, KeysFragment.class),
    BRIDGE_INFO(R.string.title_bridge_info, BridgeInfoFragment.class),
    MNEMONIC(R.string.title_mnemonic, MnemonicFragment.class),
    TIMESTAMP(R.string.title_timestamp, TimestampFragment.class),
    LIBS(R.string.title_libs, LibsFragment.class);

    private static final String TAG = "Fragments";

    private int title;
    private Class<? extends Fragment> clazz;
    
    Fragments(int title, Class<? extends Fragment> clazz) {
        this.title = title;
        this.clazz = clazz;
    }

    public static Integer[] getTitles() {
        Fragments[] fragments = values();
        Integer[] titles = new Integer[fragments.length];
        for (int i = 0; i < titles.length; i++) {
            titles[i] = fragments[i].getTitle();
        }
        return titles;
    }

    public int getTitle() {
        return title;
    }
    
    @Nullable
    public Fragment newInstance() {
        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            Log.e(TAG, "newInstance: ", e);
            return null;
        }
    }
    
}
