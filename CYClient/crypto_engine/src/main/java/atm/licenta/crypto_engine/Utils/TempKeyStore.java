package atm.licenta.crypto_engine.Utils;

import java.util.List;
import java.util.Map;

import atm.licenta.crypto_engine.KeyHelpers.EncryptedKeyData;

public class TempKeyStore {
    public static byte[] identityKey, identityIV;
    public static byte[] signedKey, signedIV;
    public static Map<String, EncryptedKeyData> otpKeyMap;
    public static byte[] lastResortPQKey, lastResortPQKeyIV;

    public static void set(String placeholderUID,
                           byte[] idKey, byte[] idIV,
                           byte[] signedKey_, byte[] signedIV_,
                           Map<String, EncryptedKeyData> otpMap, byte[] lastResortPQKeyBytes, byte[] lastResortPQKeyIVBytes) {
        identityKey = idKey;
        identityIV = idIV;
        signedKey = signedKey_;
        signedIV = signedIV_;
        otpKeyMap = otpMap;
        lastResortPQKey = lastResortPQKeyBytes;
        lastResortPQKeyIV = lastResortPQKeyIVBytes;
    }

    public static void clear() {
        identityKey = null;
        identityIV = null;
        signedKey = null;
        signedIV = null;
        otpKeyMap = null;
        lastResortPQKey = null;
        lastResortPQKeyIV = null;
    }
}
