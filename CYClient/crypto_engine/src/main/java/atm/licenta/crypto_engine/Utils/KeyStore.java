package atm.licenta.crypto_engine.Utils;

import android.content.Context;
import android.util.Base64;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import atm.licenta.crypto_engine.KeyHelpers.EncryptedKeyData;
import atm.licenta.crypto_engine.KeyHelpers.KeyStoreHelper;

public class KeyStore {

    public static class LoadedKeys {
        public byte[] identityKey, identityIV;
        public byte[] signedKey, signedIV;
        public Map<String, EncryptedKeyData> otpKeyMap;
        public LoadedKeys() {
            otpKeyMap = new HashMap<>();
        }
        public byte[] lastResortPQKey, lastResortPQKeyIV;
    }

    public static void saveEncryptedKeys(Context context, String uid,
                                         byte[] identityKey, byte[] identityIV,
                                         byte[] signedKey, byte[] signedIV,
                                         Map<String, EncryptedKeyData> otpKeyMap, byte[] lastResortPQKey, byte[] lastResortPQKeyIV) {
        File dir = new File(context.getFilesDir(), "keystore_data");
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, uid + ".bin");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            writeField(fos, identityIV);
            writeField(fos, identityKey);
            writeField(fos, signedIV);
            writeField(fos, signedKey);

            fos.write(otpKeyMap.size());

            for (Map.Entry<String, EncryptedKeyData> entry : otpKeyMap.entrySet()) {
                byte[] idBytes = entry.getKey().getBytes(StandardCharsets.UTF_8);
                byte[] iv = entry.getValue().iv;
                byte[] key = entry.getValue().encryptedKey;
                byte[] idHashBytes = entry.getValue().idHash.getBytes(StandardCharsets.UTF_8);

                writeField(fos, idBytes);
                writeField(fos, iv);
                writeField(fos, key);
                writeField(fos, idHashBytes);
            }

            writeField(fos, lastResortPQKeyIV);
            writeField(fos, lastResortPQKey);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static LoadedKeys loadEncryptedKeys(Context context, String uid) {
        File file = new File(new File(context.getFilesDir(), "keystore_data"), uid + ".bin");

        LoadedKeys result = new LoadedKeys();
        try (FileInputStream fis = new FileInputStream(file)) {
            result.identityIV = readNext(fis);
            result.identityKey = readNext(fis);
            result.signedIV = readNext(fis);
            result.signedKey = readNext(fis);

            int count = fis.read();
            for (int i = 0; i < count; i++) {
                byte[] idHashBytes = readNext(fis);
                byte[] iv = readNext(fis);
                byte[] encrypted = readNext(fis);
                String idHash = new String(readNext(fis), StandardCharsets.UTF_8);
                result.otpKeyMap.put(idHash, new EncryptedKeyData(encrypted, iv, idHash));
            }

            result.lastResortPQKeyIV = readNext(fis);
            result.lastResortPQKey = readNext(fis);

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] loadOneTimePreKeyByID(Context context, String uid, String idOriginal) {
        try {
            LoadedKeys keys = loadEncryptedKeys(context, uid);
            if (keys == null) return null;

            //String idHash = idOriginal;

            EncryptedKeyData ekd = keys.otpKeyMap.get(idOriginal);
            if (ekd == null) return null;

            return KeyStoreHelper.decryptKeyFromKeystore(ekd.encryptedKey, ekd.iv);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void removeOneTimePreKey(Context context, String uid, String idHashToRemove) {
        LoadedKeys keys = loadEncryptedKeys(context, uid);
        if (keys == null) return;

        keys.otpKeyMap.remove(idHashToRemove);

        saveEncryptedKeys(context, uid,
                keys.identityKey, keys.identityIV,
                keys.signedKey, keys.signedIV,
                keys.otpKeyMap, keys.lastResortPQKey, keys.lastResortPQKeyIV);
    }

    private static void writeField(FileOutputStream fos, byte[] data) throws IOException {
        DataOutputStream dos = new DataOutputStream(fos);
        dos.writeInt(data.length);
        dos.write(data);
    }

    private static byte[] readNext(FileInputStream fis) throws IOException {
        DataInputStream dis = new DataInputStream(fis);
        int len = dis.readInt();
        if (len <= 0 || len > 100_000) throw new IOException("Invalid field length: " + len);
        byte[] data = new byte[len];
        int read = dis.read(data);
        if (read != len) throw new IOException("Could not read full data, expected " + len + ", got " + read);
        return data;
    }
}