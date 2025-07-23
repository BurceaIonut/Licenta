package atm.licenta.crypto_engine.KeyHelpers;

public class EncryptedKeyData {
    public byte[] encryptedKey;
    public byte[] iv;
    public String idHash;

    public EncryptedKeyData(byte[] encryptedKey, byte[] iv, String idHash) {
        this.encryptedKey = encryptedKey;
        this.iv = iv;
        this.idHash = idHash;
    }
}
