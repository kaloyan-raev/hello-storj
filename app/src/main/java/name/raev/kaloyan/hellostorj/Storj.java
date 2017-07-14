package name.raev.kaloyan.hellostorj;

/**
 * Created by kraev on 7/14/17.
 */
public class Storj {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    public static native long getTimestamp();

    public static native String generateMnemonic(int strength);

}
