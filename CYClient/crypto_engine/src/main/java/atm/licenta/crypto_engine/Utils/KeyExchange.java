package atm.licenta.crypto_engine.Utils;

import static atm.licenta.crypto_engine.Utils.Constants.DOUBLE_RATCHET_INFO;

import org.whispersystems.libsignal.ecc.Curve;
import org.whispersystems.libsignal.ecc.ECPrivateKey;
import org.whispersystems.libsignal.ecc.ECPublicKey;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class KeyExchange {
    public static class InitialRatchetKeys {
        public final byte[] rootKey;
        public final byte[] sendingChainKey;
        public final byte[] receivingChainKey;

        public InitialRatchetKeys(byte[] rk, byte[] cks, byte[] ckr) {
            this.rootKey = rk;
            this.sendingChainKey = cks;
            this.receivingChainKey = ckr;
        }
    }
    public static InitialRatchetKeys deriveInitialRatchetKeys(byte[] sharedSecret, boolean isInitiator) throws Exception {
        byte[] hkdfOutput = HKDF.deriveHKDF(sharedSecret, DOUBLE_RATCHET_INFO, 96);

        byte[] rootKey = Arrays.copyOfRange(hkdfOutput, 0, 32);
        byte[] cks = Arrays.copyOfRange(hkdfOutput, 32, 64);
        byte[] ckr = Arrays.copyOfRange(hkdfOutput, 64, 96);

        return isInitiator ? new InitialRatchetKeys(rootKey, cks, ckr) : new InitialRatchetKeys(rootKey, ckr, cks);
    }
    public static byte[] concatSecrets(List<byte[]> list) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : list) out.write(part);
        return out.toByteArray();
    }

    public static byte[] calculateAgreement(byte[] publicKeyBytes, byte[] privateKeyBytes) throws Exception {
        ECPublicKey pubKey = Curve.decodePoint(publicKeyBytes, 0);
        ECPrivateKey privKey = Curve.decodePrivatePoint(privateKeyBytes);
        return Curve.calculateAgreement(pubKey, privKey);
    }

    public static byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

}
