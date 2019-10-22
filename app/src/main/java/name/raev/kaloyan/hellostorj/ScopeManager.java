package name.raev.kaloyan.hellostorj;

import android.content.Context;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import io.storj.ApiKey;
import io.storj.EncryptionAccess;
import io.storj.Key;
import io.storj.Project;
import io.storj.Scope;
import io.storj.StorjException;
import io.storj.Uplink;

class ScopeManager {

    private static String AUTH_FILENAME = "auth";
    private static String SCOPE_KEY = "scope";

    private static Scope _scope;

    static synchronized Scope getScope(Context context) throws StorjException {
        if (_scope != null) {
            return _scope;
        }

        Properties props = new Properties();
        try {
            props.load(new FileReader(new File(context.getFilesDir(), AUTH_FILENAME)));
        } catch (IOException e) {
            throw new ScopeNotFoundException();
        }

        _scope = Scope.parse(props.getProperty(SCOPE_KEY));

        return _scope;
    }

    static synchronized void setScope(Context context, String serializedScope) throws StorjException {
        Scope scope = Scope.parse(serializedScope);

        Properties props = new Properties();
        props.setProperty(SCOPE_KEY, serializedScope);
        try {
            props.store(new PrintWriter(new File(context.getFilesDir(), AUTH_FILENAME)), null);
        } catch (IOException e) {
            throw new StorjException(e);
        }

        _scope = scope;
    }

    static synchronized void setScope(Context context, Scope scope) throws StorjException {
        setScope(context, scope.serialize());
    }

    static synchronized void setScope(Context context, String satelliteAddress, String serializedApiKey, String passphrase) throws StorjException {
        ApiKey apiKey = ApiKey.parse(serializedApiKey);
        try (Uplink uplink = new Uplink();
             Project project = uplink.openProject(satelliteAddress, apiKey)) {
            Key saltedKey = Key.getSaltedKeyFromPassphrase(project, passphrase);
            EncryptionAccess access = new EncryptionAccess(saltedKey);
            Scope scope = new Scope(satelliteAddress, apiKey, access);
            setScope(context, scope);
        }
    }
}
