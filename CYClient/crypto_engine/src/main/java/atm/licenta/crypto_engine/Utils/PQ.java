package atm.licenta.crypto_engine.Utils;

public class PQ {
    static {
        System.loadLibrary("native-lib");
    }
    public static native byte[][] generateKemKeypair();
    public static native byte[][] encapsulate(byte[] publicKey);
    public static native byte[] decapsulate(byte[] ciphertext, byte[] secretKey);
}
