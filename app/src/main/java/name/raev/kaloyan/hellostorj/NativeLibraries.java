package name.raev.kaloyan.hellostorj;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kraev on 7/14/17.
 */

public class NativeLibraries {

    public static Map<String, String> getVersions() {
        Map<String, String> map = new HashMap<>();
        map.put("lib3", "1.2.3");
        map.put("lib1", "0.99");
        map.put("lib2", "3.0.2");
        return map;
    }

}
