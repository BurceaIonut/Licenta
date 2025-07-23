package atm.licenta.crypto_engine.Utils;

import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.ecc.Curve;
import org.whispersystems.libsignal.ecc.ECKeyPair;
import org.whispersystems.libsignal.ecc.ECPrivateKey;
import org.whispersystems.libsignal.ecc.ECPublicKey;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class KeyRebuilder {
    public static IdentityKeyPair rebuildIdentityKeyPair(byte[] publicKeyBytes, byte[] privateKeyBytes) throws InvalidKeyException {
        IdentityKey publicKey = new IdentityKey(publicKeyBytes, 0);
        ECPrivateKey privateKey = Curve.decodePrivatePoint(privateKeyBytes);
        return new IdentityKeyPair(publicKey, privateKey);
    }

    public static ECKeyPair rebuildECKeyPair(byte[] publicKeyBytes, byte[] privateKeyBytes) throws InvalidKeyException {
        ECPublicKey publicKey = Curve.decodePoint(publicKeyBytes, 0);
        ECPrivateKey privateKey = Curve.decodePrivatePoint(privateKeyBytes);
        return new ECKeyPair(publicKey, privateKey);
    }
}
