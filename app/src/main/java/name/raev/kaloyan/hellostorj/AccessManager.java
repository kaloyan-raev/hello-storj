package name.raev.kaloyan.hellostorj;

import android.content.Context;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import io.storj.Access;
import io.storj.StorjException;
import io.storj.Uplink;

class AccessManager {

    private static String AUTH_FILENAME = "auth";
    private static String ACCESS_KEY = "scope"; // keep it as "scope" for backward-compatibility

    private static Access _access;

    static synchronized Access getAccess(Context context) throws StorjException {
        if (_access != null) {
            return _access;
        }

        Properties props = new Properties();
        try {
            props.load(new FileReader(new File(context.getFilesDir(), AUTH_FILENAME)));
        } catch (IOException e) {
            throw new ScopeNotFoundException();
        }

        _access = Access.parse(props.getProperty(ACCESS_KEY));

        return _access;
    }

    static synchronized void setAccess(Context context, String serializedAccess) throws StorjException {
        Access access = Access.parse(serializedAccess);

        Properties props = new Properties();
        props.setProperty(ACCESS_KEY, serializedAccess);
        try {
            props.store(new PrintWriter(new File(context.getFilesDir(), AUTH_FILENAME)), null);
        } catch (IOException e) {
            throw new StorjException(e);
        }

        _access = access;
    }

    static synchronized void setAccess(Context context, Access access) throws StorjException {
        setAccess(context, access.serialize());
    }

    static synchronized void setAccess(Context context, String satelliteAddress, String serializedApiKey, String passphrase) throws StorjException {
        Access access = new Uplink().requestAccessWithPassphrase(satelliteAddress, serializedApiKey, passphrase);
        setAccess(context, access);
    }
}
