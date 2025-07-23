package atm.licenta.crypto_engine.KeyExchange;

import static atm.licenta.crypto_engine.Utils.Constants.PROXY_GATEWAY_ADDRESS;

import android.annotation.SuppressLint;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import atm.licenta.crypto_engine.KeyHelpers.FetchedPreKeyBundle;

public class Fetch {
    public static FetchedPreKeyBundle fetchPKB(String UID) throws Exception {
        trustAllCertificates();

        if (UID.trim().startsWith("{")) {
            JSONObject jsonUID = new JSONObject(UID);
            UID = jsonUID.getString("uid");
        }

        URL url = new URL("https://" + PROXY_GATEWAY_ADDRESS + "/account/fetch/prekeybundle/" + UID);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            return null;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder responseBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            responseBuilder.append(line);
        }
        reader.close();
        conn.disconnect();

        JSONObject responseJson = new JSONObject(responseBuilder.toString());

        JSONObject renamedJson = new JSONObject();
        renamedJson.put("identityPublicKey", responseJson.getString("identityPublicKey"));
        renamedJson.put("signedPublicKey", responseJson.getString("signedPublicKey"));
        renamedJson.put("signedPreKeySignature", responseJson.getString("signedPreKeySignature"));

        if (responseJson.has("oneTimePreKey")) {
            renamedJson.put("oneTimePreKey", responseJson.getString("oneTimePreKey"));
            renamedJson.put("oneTimePreKeyID", responseJson.getString("oneTimePreKeyID"));
        }

        renamedJson.put("lastResortPQKey", responseJson.getString("lastResortPQKey"));
        renamedJson.put("signedLastResortPQKeySignature", responseJson.getString("signedLastResortPQKeySignature"));

        return FetchedPreKeyBundle.fromJSON(renamedJson);
    }

    private static void trustAllCertificates() {
        try {
            @SuppressLint("CustomX509TrustManager") TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @SuppressLint("TrustAllX509TrustManager")
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                        @SuppressLint("TrustAllX509TrustManager")
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    }
            };

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
