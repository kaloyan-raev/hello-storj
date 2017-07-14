package name.raev.kaloyan.hellostorj;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kraev on 7/14/17.
 */

public class NativeLibraries {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    public static Map<String, String> getVersions() {
        Map<String, String> map = new HashMap<>();
        map.put("JSON-C", getJsonCVersion());
        map.put("cURL", getCurlVersion());
        map.put("libuv", getLibuvVersion());
        map.put("Nettle", getNettleVersion());
        map.put("Microhttpd", getMHDVersion());
        return map;
    }

    public static native String getJsonCVersion();
    public static native String getCurlVersion();
    public static native String getLibuvVersion();
    public static native String getNettleVersion();
    public static native String getMHDVersion();

}
