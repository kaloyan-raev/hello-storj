package name.raev.kaloyan.hellostorj;

import java.util.Random;

/**
 * Created by kraev on 7/14/17.
 */

public class Storj {

    public static long getTimestamp() {
        return new Random().nextLong();
    }

    public static String generateMnemonic() {
        String[] mnemonic = { "ala", "bala", "nica", "turska", "panica", "hey", "gidi", "vancho", "nash", "kapitancho" };
        shuffleArray(mnemonic);
        return join(mnemonic);
    }

    private static void shuffleArray(String[] arr)
    {
        Random rnd = new Random();
        for (int i = arr.length - 1; i > 0; i--)
        {
            int index = rnd.nextInt(i + 1);
            // Simple swap
            String a = arr[index];
            arr[index] = arr[i];
            arr[i] = a;
        }
    }

    private static String join(String[] arr) {
        StringBuilder builder = new StringBuilder();
        for (String s : arr) {
            builder.append(s);
            builder.append(' ');
        }
        return builder.toString();
    }

}
