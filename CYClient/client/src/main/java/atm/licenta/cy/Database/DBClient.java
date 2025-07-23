package atm.licenta.cy.Database;

import android.content.Context;

import androidx.room.Room;

import net.sqlcipher.database.SupportFactory;

import javax.crypto.SecretKey;

import atm.licenta.crypto_engine.KeyHelpers.KeyStoreHelper;

public class DBClient {
    private static DBClient instance;
    private final CYDB appDatabase;

    private DBClient(Context context) {
        try {
            KeyStoreHelper.generateAESDBKeyIfNeeded();
            SecretKey secretKey = KeyStoreHelper.getAESDBKey();
            byte[] keyBytes = secretKey.getEncoded();
            SupportFactory factory = new SupportFactory(keyBytes);

            appDatabase = Room.databaseBuilder(context, CYDB.class, "cy-db-encrypted")
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration()
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize encrypted DB", e);
        }
    }

    public static synchronized DBClient getInstance(Context context) {
        if (instance == null)
            instance = new DBClient(context.getApplicationContext());
        return instance;
    }

    public CYDB getAppDatabase() {
        return appDatabase;
    }
}
