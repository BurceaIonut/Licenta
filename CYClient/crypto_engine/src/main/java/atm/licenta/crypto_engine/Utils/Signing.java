package atm.licenta.crypto_engine.Utils;

import android.content.Context;

import org.json.JSONObject;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.ecc.Curve;
import org.whispersystems.libsignal.ecc.ECPrivateKey;
import org.whispersystems.libsignal.ecc.ECPublicKey;

import atm.licenta.crypto_engine.KeyHelpers.KeyStoreHelper;

public class Signing {
    public static byte[] sign(byte[] privateKeyBytes, byte[] data) throws InvalidKeyException {
        ECPrivateKey privateKey = Curve.decodePrivatePoint(privateKeyBytes);
        return Curve.calculateSignature(privateKey, data);
    }

    public static boolean verify(byte[] publicKeyBytes, byte[] message, byte[] signature) throws InvalidKeyException {
        ECPublicKey publicKey = Curve.decodePoint(publicKeyBytes, 0);
        return Curve.verifySignature(publicKey, message, signature);
    }

    public static byte[] generateSignatureForRequest(Context context, String uid) throws Exception {
        KeyStore.LoadedKeys localKeys = KeyStore.loadEncryptedKeys(context, uid);
        byte[] identityKeyPrivateKey = KeyStoreHelper.decryptKeyFromKeystore(localKeys.identityKey, localKeys.identityIV);
        String cleanedUid = new JSONObject(uid).optString("uid");
        return Signing.sign(identityKeyPrivateKey, cleanedUid.getBytes());
    }

    public static byte[] generateSignatureForQRValidationRequest(Context context, String uid, String peerUID) throws Exception {
        KeyStore.LoadedKeys localKeys = KeyStore.loadEncryptedKeys(context, uid);
        byte[] identityKeyPrivateKey = KeyStoreHelper.decryptKeyFromKeystore(localKeys.identityKey, localKeys.identityIV);
        return Signing.sign(identityKeyPrivateKey, peerUID.getBytes());
    }
}