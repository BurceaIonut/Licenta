package atm.licenta.crypto_engine.KeyHelpers;

import com.google.gson.annotations.SerializedName;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public class PreKeyBundle {
    @SerializedName("idPubKey")
    public String identityKeyPublic;
    @SerializedName("signedPubKey")
    public String signedPreKeyPublic;
    @SerializedName("signedPreKeySignature")
    public String signedPreKeySignature;
    @SerializedName("oneTimePreKeysMap")
    public Map<String, String> oneTimePreKeysMap;
    @SerializedName("lastResortPQKey")
    public String lastResortPQKey;
    @SerializedName("signedLastResortPQKeySignature")
    public String signedLastResortPQKeySignature;

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("identityPublicKey", identityKeyPublic);
            json.put("signedPublicKey", signedPreKeyPublic);
            json.put("signedPreKeySignature", signedPreKeySignature);

            JSONArray otpkArray = new JSONArray();
            for (Map.Entry<String, String> entry : oneTimePreKeysMap.entrySet()) {
                JSONObject obj = new JSONObject();
                obj.put("pubKey", entry.getKey());
                obj.put("id", entry.getValue());
                otpkArray.put(obj);
            }
            json.put("oneTimePreKeys", otpkArray);
            json.put("lastResortPQKey", lastResortPQKey);
            json.put("signedLastResortPQKeySignature", signedLastResortPQKeySignature);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }
}
