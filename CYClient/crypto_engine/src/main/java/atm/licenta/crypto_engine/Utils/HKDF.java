package atm.licenta.crypto_engine.Utils;

import static atm.licenta.crypto_engine.Utils.Constants.PROTOCOL_INFO;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class HKDF {
    public static byte[] deriveHKDF(byte[] km, String info, int length) throws Exception {
        byte[] f = new byte[32];
        Arrays.fill(f, (byte) 0xFF);

        byte[] ikm = new byte[f.length + km.length];
        System.arraycopy(f, 0, ikm, 0, f.length);
        System.arraycopy(km, 0, ikm, f.length, km.length);

        byte[] salt = new byte[32];

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(salt, "HmacSHA256"));
        byte[] prk = mac.doFinal(ikm);

        byte[] okm = new byte[length];
        byte[] previous = new byte[0];
        int pos = 0;
        int iterations = (int) Math.ceil((double) length / mac.getMacLength());

        for (int i = 1; i <= iterations; i++) {
            mac.init(new SecretKeySpec(prk, "HmacSHA256"));
            mac.update(previous);
            mac.update(info.getBytes(StandardCharsets.UTF_8));
            mac.update((byte) i);
            previous = mac.doFinal();
            System.arraycopy(previous, 0, okm, pos, Math.min(previous.length, length - pos));
            pos += previous.length;
        }

        return okm;
    }

    public static byte[] deriveHKDFWithSalt(byte[] ikm, byte[] salt, String info, int length) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");

        mac.init(new SecretKeySpec(salt, "HmacSHA256"));
        byte[] prk = mac.doFinal(ikm);

        byte[] okm = new byte[length];
        byte[] previous = new byte[0];
        int pos = 0;
        int iterations = (int) Math.ceil((double) length / mac.getMacLength());

        for (int i = 1; i <= iterations; i++) {
            mac.init(new SecretKeySpec(prk, "HmacSHA256"));
            mac.update(previous);
            mac.update(info.getBytes(StandardCharsets.UTF_8));
            mac.update((byte) i);
            previous = mac.doFinal();
            System.arraycopy(previous, 0, okm, pos, Math.min(previous.length, length - pos));
            pos += previous.length;
        }

        return okm;
    }


    public static byte[][] kdfCK(byte[] chainKey) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");

        mac.init(new SecretKeySpec(chainKey, "HmacSHA256"));
        byte[] messageKey = mac.doFinal(new byte[] { 0x01 });

        mac.init(new SecretKeySpec(chainKey, "HmacSHA256"));
        byte[] nextChainKey = mac.doFinal(new byte[] { 0x02 });

        return new byte[][] { nextChainKey, messageKey };
    }

    public static byte[][] kdfRK(byte[] rootKey, byte[] dhOutput) throws Exception {
        byte[] derived = deriveHKDFWithSalt(dhOutput, rootKey, PROTOCOL_INFO, 64);
        byte[] newRootKey = Arrays.copyOfRange(derived, 0, 32);
        byte[] newChainKey = Arrays.copyOfRange(derived, 32, 64);
        return new byte[][] { newRootKey, newChainKey };
    }
}
