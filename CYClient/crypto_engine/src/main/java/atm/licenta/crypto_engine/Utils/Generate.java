package atm.licenta.crypto_engine.Utils;

import static atm.licenta.crypto_engine.Utils.Encryption.encryptAES;
import static atm.licenta.crypto_engine.Utils.PQ.generateKemKeypair;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.ecc.Curve;
import org.whispersystems.libsignal.ecc.ECKeyPair;
import org.whispersystems.libsignal.util.KeyHelper;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;

import atm.licenta.crypto_engine.KeyHelpers.EncryptedKeyData;
import atm.licenta.crypto_engine.KeyHelpers.KeyStoreHelper;
import atm.licenta.crypto_engine.KeyHelpers.PreKeyBundle;

public class Generate {
    public static byte[] generateIV() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
    public static IdentityKeyPair generateIdentityKeyPair() { return KeyHelper.generateIdentityKeyPair();}
    public static ECKeyPair generateSignedPreKey() {
        return Curve.generateKeyPair();
    }
    public static ECKeyPair generateOneTimePreKey() {
        return Curve.generateKeyPair();
    }
    public static ECKeyPair generateEphemeralKey(){return Curve.generateKeyPair();}
    public static PreKeyBundle generateKeysAndBundle() {
        try {
            PreKeyBundle bundle = new PreKeyBundle();

            IdentityKeyPair identityKey = generateIdentityKeyPair();
            bundle.identityKeyPublic = Base64.encodeToString(identityKey.getPublicKey().serialize(), Base64.NO_WRAP);

            ECKeyPair signedPreKey = generateSignedPreKey();
            bundle.signedPreKeyPublic = Base64.encodeToString(signedPreKey.getPublicKey().serialize(), Base64.NO_WRAP);

            byte[] signature = Signing.sign(identityKey.getPrivateKey().serialize(), signedPreKey.getPublicKey().serialize());
            bundle.signedPreKeySignature = Base64.encodeToString(signature, Base64.NO_WRAP);

            KeyStoreHelper.generateAESKeyIfNeeded();
            SecretKey aesKey = KeyStoreHelper.getAESKey();

            Map<String, EncryptedKeyData> otpkMap = new HashMap<>();
            bundle.oneTimePreKeysMap = new HashMap<>();

            for (int i = 0; i < 10; i++) {
                ECKeyPair otp = generateOneTimePreKey();

                byte[] pubSerialized = otp.getPublicKey().serialize();
                String pubKey = Base64.encodeToString(pubSerialized, Base64.NO_WRAP);

                byte[] hash = MessageDigest.getInstance("SHA-256").digest(pubSerialized);
                String id = Base64.encodeToString(hash, Base64.NO_WRAP);

                byte[] iv = generateIV();
                byte[] encrypted = encryptAES(otp.getPrivateKey().serialize(), aesKey, iv);

                otpkMap.put(id, new EncryptedKeyData(encrypted, iv, id));

                bundle.oneTimePreKeysMap.put(pubKey, id);
            }

            byte[][] KEMKeyPair = generateKemKeypair();
            bundle.lastResortPQKey = Base64.encodeToString(KEMKeyPair[0], Base64.NO_WRAP);

            byte[] ivPQ = generateIV();
            byte[] encryptedLastResortPQKey = encryptAES(KEMKeyPair[1], aesKey, ivPQ);

            byte[] PQSignature = Signing.sign(identityKey.getPrivateKey().serialize(), KEMKeyPair[0]);
            bundle.signedLastResortPQKeySignature = Base64.encodeToString(PQSignature, Base64.NO_WRAP);

            byte[] iv1 = generateIV();
            byte[] iv2 = generateIV();


            byte[] encryptedIdentityPriv = encryptAES(identityKey.getPrivateKey().serialize(), aesKey, iv1);
            byte[] encryptedSignedPriv = encryptAES(signedPreKey.getPrivateKey().serialize(), aesKey, iv2);

            TempKeyStore.set("PENDING_UID", encryptedIdentityPriv, iv1, encryptedSignedPriv, iv2, otpkMap, encryptedLastResortPQKey, ivPQ);

            return bundle;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
