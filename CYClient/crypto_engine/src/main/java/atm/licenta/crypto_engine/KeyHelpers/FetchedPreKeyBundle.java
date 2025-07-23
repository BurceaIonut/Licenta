package atm.licenta.crypto_engine.KeyHelpers;

import android.util.Base64;

import org.json.JSONObject;

import java.util.Arrays;

public class FetchedPreKeyBundle {
    public byte[] identityKeyPublic;
    public byte[] signedPreKeyPublic;
    public byte[] signedPreKeySignature;
    public byte[] oneTimePreKey;
    public String oneTimePreKeyID;
    public byte[] lastResortPQKey;
    public byte[] signedLastResortPQKeySignature;

    public static FetchedPreKeyBundle fromJSON(JSONObject obj) throws Exception {
        FetchedPreKeyBundle bundle = new FetchedPreKeyBundle();

        bundle.identityKeyPublic = Base64.decode(obj.getString("identityPublicKey"), Base64.DEFAULT);
        bundle.signedPreKeyPublic = Base64.decode(obj.getString("signedPublicKey"), Base64.DEFAULT);
        bundle.signedPreKeySignature = Base64.decode(obj.getString("signedPreKeySignature"), Base64.DEFAULT);

        if (obj.has("oneTimePreKey") && !obj.isNull("oneTimePreKey")) {
            bundle.oneTimePreKey = Base64.decode(obj.getString("oneTimePreKey"), Base64.DEFAULT);
            bundle.oneTimePreKeyID = obj.getString("oneTimePreKeyID");
        } else {
            bundle.oneTimePreKey = null;
            bundle.oneTimePreKeyID = null;
        }

        bundle.lastResortPQKey = Base64.decode(obj.getString("lastResortPQKey"), Base64.DEFAULT);
        bundle.signedLastResortPQKeySignature = Base64.decode(obj.getString("signedLastResortPQKeySignature"), Base64.DEFAULT);

        return bundle;
    }
}
