package atm.licenta.cy.WebSockets;

import static atm.licenta.cy.Helpers.Constants.AEAD_INFO;
import static atm.licenta.cy.Helpers.Constants.CHAT_SERVICE_ADDRESS;
import static atm.licenta.cy.Helpers.Constants.PROTOCOL_INFO;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONObject;
import org.whispersystems.libsignal.ecc.Curve;
import org.whispersystems.libsignal.ecc.ECKeyPair;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import atm.licenta.cy.R;

import atm.licenta.cy.Activities.ChatActivity;
import atm.licenta.cy.Database.DBClient;
import atm.licenta.cy.Database.Entities.ContactEntity;
import atm.licenta.cy.Database.Entities.ConversationEntity;
import atm.licenta.cy.Database.Entities.DHRatchetStateEntity;
import atm.licenta.cy.Database.Entities.KeyStateEntity;
import atm.licenta.cy.Database.Entities.MessageEntity;
import atm.licenta.cy.Database.Entities.SkippedMessageKeyEntity;
import atm.licenta.cy.Helpers.IntentKeys;
import atm.licenta.crypto_engine.KeyHelpers.KeyStoreHelper;
import atm.licenta.crypto_engine.Utils.Encryption;
import atm.licenta.crypto_engine.Utils.HKDF;
import atm.licenta.crypto_engine.Utils.KeyExchange;
import atm.licenta.crypto_engine.Utils.KeyStore;
import atm.licenta.crypto_engine.Utils.PQ;
import atm.licenta.crypto_engine.Utils.Signing;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebSocketClientManager {
    private static WebSocketClientManager instance;
    private WebSocket webSocket;
    private Context context;

    private WebSocketClientManager() {}

    public static synchronized WebSocketClientManager getInstance() {
        if (instance == null) {
            instance = new WebSocketClientManager();
        }
        return instance;
    }

    public void init(Context ctx) {
        this.context = ctx.getApplicationContext();
    }

    public void connect(String uid) {
        if (webSocket != null) return;

        OkHttpClient client = new OkHttpClient.Builder()
                .pingInterval(1000, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url("ws://"+ CHAT_SERVICE_ADDRESS +"/ws")
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                JSONObject setUID = new JSONObject();
                try {
                    String cleanedUid = new JSONObject(uid).optString("uid");
                    setUID.put("setAccountUID", cleanedUid);
                    byte[] signature = Signing.generateSignatureForRequest(context, uid);
                    setUID.put("signature", Base64.encodeToString(signature, Base64.NO_WRAP));
                    webSocket.send(setUID.toString());
                    Log.e("WebSocket", "Oppened connection!");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    Log.e("ON MESSAGE:", text);
                    JSONObject json = new JSONObject(text);

                    if (json.has("initialMessage")) {
                        handleInitialMessage(json);
                    }else if (json.has("normalMessage")) {
                        handleNormalMessage(json);
                    } else if (json.has("uid1") && json.has("uid2") && json.has("signature1") && json.has("signature2")) {
                        String uid1 = json.getString("uid1");
                        String uid2 = json.getString("uid2");
                        String sig1 = json.getString("signature1");
                        String sig2 = json.getString("signature2");

                        DBClient db = DBClient.getInstance(context);
                        ContactEntity contact;

                        String cleanedUid = new JSONObject(uid).optString("uid");
                        boolean valid;
                        if(cleanedUid.equals(uid1)){
                            contact = db.getAppDatabase().contactDao().getContactByUid(uid2);
                            byte[] localContactIdentityPublicKey = Base64.decode(contact.identityPublicKey, Base64.NO_WRAP);
                            valid = Signing.verify(localContactIdentityPublicKey, uid1.getBytes(StandardCharsets.UTF_8), Base64.decode(sig2, Base64.NO_WRAP));
                            if(valid){
                                db.getAppDatabase().contactDao().updatePendingStatusForContact(uid2);
                            }
                        }else if(cleanedUid.equals(uid2)){
                            contact = db.getAppDatabase().contactDao().getContactByUid(uid1);
                            byte[] localContactIdentityPublicKey = Base64.decode(contact.identityPublicKey, Base64.NO_WRAP);
                            valid = Signing.verify(localContactIdentityPublicKey, uid2.getBytes(StandardCharsets.UTF_8), Base64.decode(sig1, Base64.NO_WRAP));
                            if(valid){
                                db.getAppDatabase().contactDao().updatePendingStatusForContact(uid1);
                            }
                        }
                    }

                } catch (Exception e) {
                    Log.e("WS_RECEIVE_ERROR", "Invalid JSON: " + text, e);
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
                Log.e("WebSocket", "Error", t);
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                webSocket.close(1000, null);
                Log.e("WebSocket", "Closing: " + reason);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.e("WebSocket", "Closed: " + reason);
            }
        });
    }

    private void handleInitialMessage(JSONObject json) {
        try {
            JSONObject initMsg = json.getJSONObject("initialMessage");

            String senderUID = json.getString("senderUID");
            String fullName = json.getString("fullName");
            String timestamp = json.optString("timestamp", "");

            String IKA_b64 = initMsg.getString("IKA");
            String EKA_b64 = initMsg.getString("EKA");
            String kemCT_b64 = initMsg.getString("pqkemCiphertext");
            String ciphertext_b64 = initMsg.getString("ciphertext");
            String iv_b64 = initMsg.getString("iv");
            String OPK_ID = null;
            if(initMsg.has("OPK_ID")) {
                OPK_ID = initMsg.getString("OPK_ID");
            }

            byte[] IKA = Base64.decode(IKA_b64, Base64.NO_WRAP);
            byte[] EKA = Base64.decode(EKA_b64, Base64.NO_WRAP);
            byte[] kemCT = Base64.decode(kemCT_b64, Base64.NO_WRAP);
            byte[] ciphertext = Base64.decode(ciphertext_b64, Base64.NO_WRAP);
            byte[] iv = Base64.decode(iv_b64, Base64.NO_WRAP);

            DBClient db = DBClient.getInstance(context);
            SharedPreferences prefs = context.getSharedPreferences("userPrefs", Context.MODE_PRIVATE);
            String myUID = prefs.getString("UID", null);

            KeyStore.LoadedKeys localKeys = KeyStore.loadEncryptedKeys(context, myUID);
            byte[] IK_B_priv = KeyStoreHelper.decryptKeyFromKeystore(localKeys.identityKey, localKeys.identityIV);
            byte[] SPK_B_priv = KeyStoreHelper.decryptKeyFromKeystore(localKeys.signedKey, localKeys.signedIV);
            byte[] OPK_B_priv = null;
            if(OPK_ID != null){
                OPK_B_priv = KeyStore.loadOneTimePreKeyByID(context, myUID, OPK_ID);
            }

            List<byte[]> secrets = new ArrayList<>();
            secrets.add(KeyExchange.calculateAgreement(IKA, SPK_B_priv));
            secrets.add(KeyExchange.calculateAgreement(EKA, IK_B_priv));
            secrets.add(KeyExchange.calculateAgreement(EKA, SPK_B_priv));
            if (OPK_B_priv != null) {
                secrets.add(KeyExchange.calculateAgreement(EKA, OPK_B_priv));
                KeyStore.removeOneTimePreKey(context, myUID, OPK_ID);
            }

            byte[] lastResortPQKeyB = KeyStoreHelper.decryptKeyFromKeystore(localKeys.lastResortPQKey, localKeys.lastResortPQKeyIV);
            byte[] pqSharedSecret = PQ.decapsulate(kemCT, lastResortPQKeyB);
            secrets.add(pqSharedSecret);

            byte[] SK = HKDF.deriveHKDF(KeyExchange.concatSecrets(secrets), PROTOCOL_INFO, 32);

            byte[] IK_B_PUB = Base64.decode(prefs.getString("identityPublicKey", null), Base64.NO_WRAP);
            byte[] AD = KeyExchange.concat(IKA, IK_B_PUB);

            byte[] aeadKeyBytes = HKDF.deriveHKDF(SK, AEAD_INFO, 32);
            SecretKey aesGCMKey = new SecretKeySpec(aeadKeyBytes, "AES");
            String decryptedMessage = new String(Encryption.decrypt(ciphertext, aesGCMKey, iv, AD), StandardCharsets.UTF_8);

            KeyExchange.InitialRatchetKeys ratchetKeys = KeyExchange.deriveInitialRatchetKeys(SK, false);

            KeyStateEntity ks = new KeyStateEntity(senderUID, ratchetKeys.rootKey, ratchetKeys.sendingChainKey, ratchetKeys.receivingChainKey);
            db.getAppDatabase().keyStateDao().insertOrUpdateKeyState(ks);

            ECKeyPair localRatchetKeyPair = Curve.generateKeyPair();
            DHRatchetStateEntity dhState = new DHRatchetStateEntity(senderUID, localRatchetKeyPair.getPrivateKey().serialize(),
                    localRatchetKeyPair.getPublicKey().serialize()
                    ,EKA, 0, 0, 0);
            db.getAppDatabase().dhRatchetStateDao().insertOrUpdate(dhState);

            ContactEntity contact = db.getAppDatabase().contactDao().getContactByUid(senderUID);
            if (contact == null) {
                contact = new ContactEntity();
                contact.uid = senderUID;
                String[] parts = fullName.split(" ", 2);
                contact.firstName = parts[0];
                contact.lastName = parts.length > 1 ? parts[1] : "";
                contact.identityPublicKey = Base64.encodeToString(IKA, Base64.NO_WRAP);
                contact.status = "";
                db.getAppDatabase().contactDao().insert(contact);
            }

            String contactName = contact.firstName + " " + contact.lastName;
            db.getAppDatabase().conversationDao().insertOrUpdate(new ConversationEntity(senderUID, contactName, decryptedMessage, timestamp));
            db.getAppDatabase().messageDao().insert(new MessageEntity(senderUID, false, decryptedMessage, timestamp, 0));

            Intent intent = new Intent("INCOMING_CHAT_MESSAGE");
            intent.putExtra("senderUID", senderUID);
            intent.putExtra("data", decryptedMessage);
            intent.putExtra("timestamp", timestamp);
            intent.putExtra("messageType", "text");
            intent.putExtra("fullName", fullName);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

            String activeChatUid = prefs.getString("activeChatUid", null);
            if (!senderUID.equals(activeChatUid)) {
                showIncomingMessageNotification(contactName, decryptedMessage, senderUID);
            }

        } catch (Exception e) {
            Log.e("INITIAL_MSG_ERROR", "Error at processing initial PQXDH message", e);
        }
    }

    private void handleNormalMessage(JSONObject json) {
        try {
            JSONObject dataObj = json.getJSONObject("normalMessage");

            String fullName = json.getString("fullName");
            String senderUID = json.getString("senderUID");
            String timestamp = json.optString("timestamp", "");
            //String messageType = json.optString("messageType", "text");

            String ciphertext_b64 = dataObj.getString("ciphertext");
            String iv_b64 = dataObj.getString("iv");
            String dhs_b64 = dataObj.getString("DHs");
            String ika_b64 = dataObj.getString("IKA");
            int messageIndex = dataObj.optInt("messageIndex", -1);
            int pn = dataObj.optInt("PN", -1);

            byte[] ciphertext = Base64.decode(ciphertext_b64, Base64.NO_WRAP);
            byte[] iv = Base64.decode(iv_b64, Base64.NO_WRAP);
            byte[] DHs = Base64.decode(dhs_b64, Base64.NO_WRAP);
            byte[] ika = Base64.decode(ika_b64, Base64.NO_WRAP);

            DBClient db = DBClient.getInstance(context);
            DHRatchetStateEntity ratchetState = db.getAppDatabase().dhRatchetStateDao().getState(senderUID);
            KeyStateEntity keyState = db.getAppDatabase().keyStateDao().getKeyStateForContact(senderUID);
            ContactEntity contact = db.getAppDatabase().contactDao().getContactByUid(senderUID);
            String contactName = contact.firstName + " " + contact.lastName;

            byte[] messageKey;
            SkippedMessageKeyEntity skippedKey = db.getAppDatabase().skippedMessageKeyDao().getKey(senderUID, messageIndex);
            if (skippedKey != null) {
                messageKey = skippedKey.messageKey;
                db.getAppDatabase().skippedMessageKeyDao().deleteKey(senderUID, messageIndex);
            } else {
                if (!Arrays.equals(ratchetState.DHrPublicKey, DHs)) {
                    int skippedInOldChain = pn - ratchetState.Nr;

                    for (int i = 0; i < skippedInOldChain; i++) {
                        byte[][] skipped = HKDF.kdfCK(keyState.receivingChainKey);
                        keyState.receivingChainKey = skipped[0];
                        byte[] sk = skipped[1];

                        SkippedMessageKeyEntity entity = new SkippedMessageKeyEntity(senderUID, ratchetState.Nr, ratchetState.DHrPublicKey, sk);
                        db.getAppDatabase().skippedMessageKeyDao().insert(entity);
                        ratchetState.Nr += 1;
                    }

                    ratchetState.PN = ratchetState.Ns;
                    ratchetState.Ns = 0;
                    ratchetState.Nr = 0;

                    byte[] DH_out = KeyExchange.calculateAgreement(DHs, ratchetState.DHsPrivateKey);
                    byte[][] rk_ck = HKDF.kdfRK(keyState.rootKey, DH_out);

                    keyState.rootKey = rk_ck[0];
                    keyState.receivingChainKey = rk_ck[1];

                    ratchetState.DHrPublicKey = DHs;
                }

                while (ratchetState.Nr < messageIndex) {
                    byte[][] skipped = HKDF.kdfCK(keyState.receivingChainKey);
                    keyState.receivingChainKey = skipped[0];
                    byte[] sk = skipped[1];

                    SkippedMessageKeyEntity entity = new SkippedMessageKeyEntity(senderUID, ratchetState.Nr, ratchetState.DHrPublicKey, sk);
                    db.getAppDatabase().skippedMessageKeyDao().insert(entity);
                    ratchetState.Nr += 1;
                }

                byte[][] updated = HKDF.kdfCK(keyState.receivingChainKey);
                keyState.receivingChainKey = updated[0];
                messageKey = updated[1];
                ratchetState.Nr += 1;
            }

            SharedPreferences prefs = context.getSharedPreferences("userPrefs", Context.MODE_PRIVATE);
            byte[] ikb = Base64.decode(prefs.getString("identityPublicKey", null), Base64.NO_WRAP);
            byte[] AD = KeyExchange.concat(ika, ikb);
            SecretKey aesKey = new SecretKeySpec(messageKey, "AES");
            String plaintext;
            try {
                byte[] plainBytes = Encryption.decrypt(ciphertext, aesKey, iv, AD);
                plaintext = new String(plainBytes, StandardCharsets.UTF_8);
            } catch (Exception e) {
                Log.e("DECRYPT", "Failed to decrypt", e);
                return;
            }

            db.getAppDatabase().keyStateDao().insertOrUpdateKeyState(keyState);
            db.getAppDatabase().dhRatchetStateDao().insertOrUpdate(ratchetState);

            MessageEntity msg = new MessageEntity(senderUID, false, plaintext, timestamp, messageIndex);
            db.getAppDatabase().messageDao().insert(msg);
            db.getAppDatabase().conversationDao().updateLastMessage(senderUID, plaintext, timestamp);

            Intent intent = new Intent("INCOMING_CHAT_MESSAGE");
            intent.putExtra("senderUID", senderUID);
            intent.putExtra("data", plaintext);
            intent.putExtra("timestamp", timestamp);
            intent.putExtra("messageType", "text");
            intent.putExtra("fullName", fullName);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

            String activeChatUid = prefs.getString("activeChatUid", null);
            if (!senderUID.equals(activeChatUid)) {
                showIncomingMessageNotification(contactName, plaintext, senderUID);
            }
        } catch (Exception e) {
            Log.e("NORMAL_MSG_ERROR", "Error at processing normal PQXDH message: ", e);
        }
    }

    private void showIncomingMessageNotification(String senderName, String message, String senderUID) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent chatIntent = new Intent(context, ChatActivity.class);
        chatIntent.putExtra(IntentKeys.CONTACT_UID, senderUID);
        chatIntent.putExtra(IntentKeys.CONTACT_NAME, senderName);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntentWithParentStack(chatIntent);

        PendingIntent pendingIntent = stackBuilder.getPendingIntent(
                senderUID.hashCode(),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "chat_channel")
                .setSmallIcon(R.drawable.ic_chat_notification)
                .setContentTitle(senderName)
                .setContentText(message.length() > 40 ? message.substring(0, 40) + "..." : message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        int notificationId = senderUID.hashCode();
        notificationManager.notify(notificationId, builder.build());
    }

    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "App closed");
            webSocket = null;
        }
    }

    public WebSocket getWebSocket() {
        return webSocket;
    }
}