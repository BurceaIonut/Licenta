package atm.licenta.cy.Activities;

import static atm.licenta.cy.Helpers.Constants.AEAD_INFO;
import static atm.licenta.cy.Helpers.Constants.PROTOCOL_INFO;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;
import org.whispersystems.libsignal.ecc.Curve;
import org.whispersystems.libsignal.ecc.ECKeyPair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.google.GoogleEmojiProvider;
import com.vanniktech.emoji.EmojiPopup;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import atm.licenta.cy.Database.DBClient;
import atm.licenta.cy.Database.Entities.ContactEntity;
import atm.licenta.cy.Database.Entities.ConversationEntity;
import atm.licenta.cy.Database.Entities.DHRatchetStateEntity;
import atm.licenta.cy.Database.Entities.KeyStateEntity;
import atm.licenta.cy.Database.Entities.MessageEntity;
import atm.licenta.cy.Helpers.ChatTimestampHelper;
import atm.licenta.cy.Adapters.ChatMessagesAdapter;
import atm.licenta.cy.Helpers.DateTimeHelper;
import atm.licenta.cy.Helpers.IntentKeys;
import atm.licenta.cy.Models.AttachmentMessageModel;
import atm.licenta.cy.Models.ChatMessageModel;
import atm.licenta.cy.Models.VoiceMessageModel;
import atm.licenta.cy.R;
import atm.licenta.cy.WebSockets.WebSocketClientManager;
import atm.licenta.crypto_engine.KeyExchange.Fetch;
import atm.licenta.crypto_engine.KeyHelpers.FetchedPreKeyBundle;
import atm.licenta.crypto_engine.KeyHelpers.KeyStoreHelper;
import atm.licenta.crypto_engine.Utils.Encryption;
import atm.licenta.crypto_engine.Utils.Generate;
import atm.licenta.crypto_engine.Utils.HKDF;
import atm.licenta.crypto_engine.Utils.KeyExchange;
import atm.licenta.crypto_engine.Utils.KeyStore;
import atm.licenta.crypto_engine.Utils.PQ;
import atm.licenta.crypto_engine.Utils.Signing;
import okhttp3.WebSocket;

public class ChatActivity extends AppCompatActivity {
    private ImageView sendButton;
    private ImageView voiceCallButton;
    private ImageView videoCallButton;
    private int currentIconResId = R.drawable.mic_icon;
    private long recordingStartTime = 0L;
    private Handler recordingHandler = new Handler();
    private Runnable updateRecordingTimer;
    private List<ChatMessageModel> messageList = new ArrayList<>();
    private ChatMessagesAdapter adapter;
    private RecyclerView recyclerView;
    private ImageView emojiButton;
    private MediaRecorder recorder;
    private File audioFile;
    private boolean isRecording = false;
    private ImageView attachButton;
    private int messagesLoadedCount = 0;
    private final int PAGE_SIZE = 50;
    private boolean isLoading = false;
    private BroadcastReceiver incomingMessageReceiver;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1001 && permissions.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startRecordingVisual();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) return;

            String timestamp = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date());

            String fileName = "Unknown";
            String fileSize = "0 MB";
            String fileExtension = "FILE";

            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                    if (sizeIndex != -1) {
                        long sizeBytes = cursor.getLong(sizeIndex);
                        fileSize = String.format(Locale.getDefault(), "%.1f MB", sizeBytes / (1024.0 * 1024.0));
                    }
                    int dotIndex = fileName.lastIndexOf(".");
                    if (dotIndex != -1) {
                        fileExtension = fileName.substring(dotIndex + 1).toUpperCase();
                    }
                }
            }

            String fileType = (requestCode == 2001) ? "image" : "document";

            String finalFileName = fileName;
            String finalFileExtension = fileExtension;
            String finalFileSize = fileSize;
            new Thread(() -> {
                String savedPath = copyUriToInternalStorage(ChatActivity.this, uri, finalFileName);
                if (savedPath == null) {
                    runOnUiThread(() ->
                            Toast.makeText(ChatActivity.this, "Error saving file", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                AttachmentMessageModel msg = new AttachmentMessageModel("Eu", timestamp, true, savedPath, finalFileExtension, fileType, finalFileName, finalFileSize);

                messageList.add(msg);
                runOnUiThread(this::sortAndDisplayMessages);
            }).start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(incomingMessageReceiver,
                new IntentFilter("INCOMING_CHAT_MESSAGE"));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(incomingMessageReceiver);

        SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
        prefs.edit().remove("activeChatUid").apply();

        super.onPause();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Toolbar toolbar = findViewById(R.id.chat_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setTitle("");
        }

        TextView contactNameView = findViewById(R.id.contact_name);
        ImageView contactImageView = findViewById(R.id.contact_image);

        SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
        String myUid = prefs.getString("UID", null);
        String contactUid = getIntent().getStringExtra(IntentKeys.CONTACT_UID);
        String contactName = getIntent().getStringExtra(IntentKeys.CONTACT_NAME);
        contactImageView.setImageResource(R.drawable.profile_picture_placeholder);
        contactNameView.setText(contactName);

        prefs.edit().putString("activeChatUid", contactUid).apply();

        recyclerView = findViewById(R.id.messages_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        recyclerView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (adapter != null && bottom < oldBottom) {
                recyclerView.postDelayed(() -> {
                    if (adapter.getItemCount() > 0) {
                        recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                    }
                }, 100);
            }
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && layoutManager.findFirstVisibleItemPosition() == 0 && !isLoading) {
                    loadMoreMessages(contactUid);
                }
            }
        });


        EditText messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_button);

        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().isEmpty()) {
                    if (currentIconResId != R.drawable.mic_icon) {
                        fadeChangeIcon(sendButton, R.drawable.mic_icon);
                        currentIconResId = R.drawable.mic_icon;
                    }
                } else {
                    if (currentIconResId != R.drawable.send_button) {
                        fadeChangeIcon(sendButton, R.drawable.send_button);
                        currentIconResId = R.drawable.send_button;
                    }
                }
            }


            @Override
            public void afterTextChanged(Editable s) {}
        });

        voiceCallButton = findViewById(R.id.call_voice);
        videoCallButton = findViewById(R.id.call_video);

        voiceCallButton.setOnClickListener(v->{
            Intent intent = new Intent(ChatActivity.this, CallingActivity.class);
            intent.putExtra("isIncoming", false);
            intent.putExtra("contactName", contactName);
            intent.putExtra("contactUid", contactUid);
            intent.putExtra("roomId", "123");
            startActivity(intent);
        });

        videoCallButton.setOnClickListener(v->{
            Intent intent = new Intent(ChatActivity.this, CallingActivity.class);
            intent.putExtra("isIncoming", false);
            intent.putExtra("contactName", contactName);
            intent.putExtra("contactUid", contactUid);
            intent.putExtra("roomId", "123");
            startActivity(intent);
        });

        sendButton.setOnTouchListener((v, event) -> {
            if (currentIconResId != R.drawable.mic_icon) {
                return false;
            }
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startRecordingVisual();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    stopRecordingVisual();
                    return true;
            }
            return false;
        });

        sendButton.setOnClickListener(v -> {
            if (currentIconResId != R.drawable.send_button) return;

            String messageText = messageEditText.getText().toString().trim();
            if (messageText.isEmpty()) return;

            messageEditText.setText("");

            String timestamp = DateTimeHelper.getCurrentTimestamp();

            saveMessageToDatabase(contactUid, contactName, messageText, true, timestamp);

            new Handler(Looper.getMainLooper()).postDelayed(() -> loadMoreMessages(contactUid), 100);
        });

        incomingMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                String senderUID = intent.getStringExtra("senderUID");
                if (!senderUID.equals(contactUid)) return;

                String data = intent.getStringExtra("data");
                String timestamp = intent.getStringExtra("timestamp");
                String newName = intent.getStringExtra("fullName");

                if (newName != null && !newName.isEmpty()) {
                    TextView contactNameView = findViewById(R.id.contact_name);
                    contactNameView.setText(newName);
                }

                ChatMessageModel message = new ChatMessageModel(newName != null ? newName : contactName, data, timestamp, false, false);

                for (ChatMessageModel existing : messageList) {
                    if (existing.getMessage_in_plaintext().equals(message.getMessage_in_plaintext()) &&
                            existing.getTimestamp().equals(message.getTimestamp()) &&
                            existing.getFrom() == message.getFrom()) {
                        return;
                    }
                }

                messageList.add(message);
                runOnUiThread(ChatActivity.this::sortAndDisplayMessages);
            }
        };


        EmojiManager.install(new GoogleEmojiProvider());
        emojiButton = findViewById(R.id.emoji_button);

        EmojiPopup emojiPopup = new EmojiPopup(findViewById(R.id.main), messageEditText);

        emojiButton.setOnClickListener(v -> {
            emojiPopup.toggle();

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (emojiPopup.isShowing() && adapter != null && adapter.getItemCount() > 0) {
                    recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                }
            }, 200);
        });

        messageEditText.setOnClickListener(v -> {
            if (emojiPopup.isShowing()) {
                emojiPopup.dismiss();
            }
        });

        attachButton = findViewById(R.id.attach_button);
        attachButton.setOnClickListener(v -> showAttachmentMenu());

        if (messagesLoadedCount == 0) {
            loadMoreMessages(contactUid);
        }
    }

    private void saveMessageToDatabase(String contactUid, String contactName, String messageText, boolean fromMe, String timestamp) {
        new Thread(() -> {
            DBClient db = DBClient.getInstance(getApplicationContext());
            MessageEntity message = new MessageEntity();
            message.conversationUid = contactUid;
            message.fromMe = fromMe;
            message.message = messageText;
            message.timestamp = timestamp;

            SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
            String myUID = prefs.getString("UID", null);
            String firstName = prefs.getString("firstName", "");
            String lastName = prefs.getString("lastName", "");
            String fullName = (firstName + " " + lastName).trim();

            ConversationEntity existing = db.getAppDatabase().conversationDao().getConversationByUid(contactUid);
            if (existing != null) {
                KeyStateEntity keys = db.getAppDatabase().keyStateDao().getKeyStateForContact(contactUid);
                DHRatchetStateEntity ratchetState = db.getAppDatabase().dhRatchetStateDao().getState(contactUid);
                byte[] currentDHsPub;
                if (!Arrays.equals(ratchetState.DHrPublicKey, ratchetState.lastDHrUsedForSendingRatchet)) {
                    ECKeyPair localNewRatchetKey = Curve.generateKeyPair();
                    ratchetState.DHsPrivateKey = localNewRatchetKey.getPrivateKey().serialize();
                    ratchetState.DHsPublicKey = localNewRatchetKey.getPublicKey().serialize();
                    currentDHsPub = ratchetState.DHsPublicKey;
                    ratchetState.PN = ratchetState.Ns;
                    byte[] DH_out;
                    try {
                        DH_out = KeyExchange.calculateAgreement(
                                ratchetState.DHrPublicKey,
                                ratchetState.DHsPrivateKey
                        );
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    ratchetState.lastDHrUsedForSendingRatchet = ratchetState.DHrPublicKey;

                    byte[][] rk_ck;
                    try {
                        rk_ck = HKDF.kdfRK(keys.rootKey, DH_out);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    keys.rootKey = rk_ck[0];
                    keys.sendingChainKey = rk_ck[1];

                    db.getAppDatabase().keyStateDao().insertOrUpdateKeyState(keys);
                    db.getAppDatabase().dhRatchetStateDao().insertOrUpdate(ratchetState);
                }else{
                    currentDHsPub = ratchetState.DHsPublicKey;
                }


                try {
                    int currentMessageIndex = ratchetState.Ns;
                    byte[][] updated = HKDF.kdfCK(keys.sendingChainKey);
                    keys.sendingChainKey = updated[0];
                    byte[] messageKey = updated[1];
                    ratchetState.Ns += 1;

                    db.getAppDatabase().keyStateDao().insertOrUpdateKeyState(keys);
                    db.getAppDatabase().dhRatchetStateDao().insertOrUpdate(ratchetState);

                    ContactEntity contact = db.getAppDatabase()
                            .contactDao()
                            .getContactByUid(contactUid);
                    byte[] localContactIdentityPublicKey = Base64.decode(contact.identityPublicKey, Base64.NO_WRAP);

                    byte[] IKA_pub = Base64.decode(prefs.getString("identityPublicKey", null), Base64.NO_WRAP);
                    byte[] AD = KeyExchange.concat(IKA_pub, localContactIdentityPublicKey);

                    SecretKey aesKey = new SecretKeySpec(messageKey, "AES");
                    Encryption.AESEncryptionResult encrypted = Encryption.encrypt(messageText.getBytes(StandardCharsets.UTF_8), aesKey, AD);

                    JSONObject payload = new JSONObject();
                    payload.put("IKA", Base64.encodeToString(IKA_pub, Base64.NO_WRAP));
                    payload.put("ciphertext", Base64.encodeToString(encrypted.ciphertext, Base64.NO_WRAP));
                    payload.put("iv", Base64.encodeToString(encrypted.iv, Base64.NO_WRAP));
                    payload.put("messageIndex", currentMessageIndex);
                    payload.put("DHs", Base64.encodeToString(currentDHsPub, Base64.NO_WRAP));
                    payload.put("PN", ratchetState.PN);

                    db.getAppDatabase().conversationDao().updateLastMessage(contactUid, messageText, timestamp);
                    sendMessageToWebSocket(myUID, contactUid, payload, timestamp, fullName);

                    message.messageIndex = ratchetState.Ns;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                try {
                    FetchedPreKeyBundle pkb = Fetch.fetchPKB(contactUid);

                    ContactEntity contact = db.getAppDatabase()
                            .contactDao()
                            .getContactByUid(contactUid);
                    byte[] localContactIdentityPublicKey = Base64.decode(contact.identityPublicKey, Base64.NO_WRAP);

                    boolean valid = Signing.verify(
                            localContactIdentityPublicKey,
                            pkb.signedPreKeyPublic,
                            pkb.signedPreKeySignature
                    );

                    if (!valid) {
                        Log.e("PKB", "Invalid signature on SPK");
                        return;
                    }

                    KeyStore.LoadedKeys localKeys = KeyStore.loadEncryptedKeys(getApplicationContext(), myUID);

                    valid = Signing.verify(
                            localContactIdentityPublicKey,
                            pkb.lastResortPQKey,
                            pkb.signedLastResortPQKeySignature
                    );

                    if (!valid) {
                        Log.e("PKB", "Invalid signature on PQK");
                        return;
                    }

                    byte[][] encapsResult = PQ.encapsulate(pkb.lastResortPQKey);

                    byte[] IK_A_priv = KeyStoreHelper.decryptKeyFromKeystore(localKeys.identityKey, localKeys.identityIV);

                    ECKeyPair ephemeralKey = Generate.generateEphemeralKey();
                    byte[] EK_A_priv = ephemeralKey.getPrivateKey().serialize();
                    byte[] EK_A_pub = ephemeralKey.getPublicKey().serialize();

                    List<byte[]> secrets = new ArrayList<>();
                    secrets.add(KeyExchange.calculateAgreement(
                            pkb.signedPreKeyPublic,
                            IK_A_priv
                    ));
                    secrets.add(KeyExchange.calculateAgreement(
                            pkb.identityKeyPublic,
                            EK_A_priv
                    ));
                    secrets.add(KeyExchange.calculateAgreement(
                            pkb.signedPreKeyPublic,
                            EK_A_priv
                    ));

                    if (pkb.oneTimePreKey != null && pkb.oneTimePreKeyID != null) {
                        secrets.add(KeyExchange.calculateAgreement(
                                pkb.oneTimePreKey,
                                EK_A_priv
                        ));
                    }

                    secrets.add(encapsResult[1]);

                    byte[] SK = HKDF.deriveHKDF(KeyExchange.concatSecrets(secrets), PROTOCOL_INFO, 32);

                    byte[] IK_A_pub = Base64.decode(prefs.getString("identityPublicKey", null), Base64.NO_WRAP);

                    byte[] AD = KeyExchange.concat(IK_A_pub, Curve.decodePoint(pkb.identityKeyPublic, 0).serialize());

                    KeyExchange.InitialRatchetKeys ratchetKeys = KeyExchange.deriveInitialRatchetKeys(SK, true);

                    KeyStateEntity keyState = new KeyStateEntity(contactUid, ratchetKeys.rootKey, ratchetKeys.sendingChainKey, ratchetKeys.receivingChainKey);

                    db.getAppDatabase().keyStateDao().insertOrUpdateKeyState(keyState);

                    DHRatchetStateEntity dhState = new DHRatchetStateEntity(contactUid, EK_A_priv, EK_A_pub, pkb.signedPreKeyPublic, 0, 0, 0);

                    db.getAppDatabase().dhRatchetStateDao().insertOrUpdate(dhState);

                    JSONObject initialMessage = new JSONObject();
                    initialMessage.put("IKA", Base64.encodeToString(IK_A_pub, Base64.NO_WRAP));
                    initialMessage.put("EKA", Base64.encodeToString(EK_A_pub, Base64.NO_WRAP));
                    initialMessage.put("pqkemCiphertext", Base64.encodeToString(encapsResult[0], Base64.NO_WRAP));
                    if(pkb.oneTimePreKeyID != null){
                        initialMessage.put("OPK_ID", pkb.oneTimePreKeyID);
                    }

                    byte[] aeadKeyBytes = HKDF.deriveHKDF(SK, AEAD_INFO, 32);
                    SecretKey aesGCMKey = new SecretKeySpec(aeadKeyBytes, "AES");
                    Encryption.AESEncryptionResult encrypted = Encryption.encrypt(messageText.getBytes(StandardCharsets.UTF_8), aesGCMKey, AD);

                    initialMessage.put("ciphertext", Base64.encodeToString(encrypted.ciphertext, Base64.NO_WRAP));
                    initialMessage.put("iv", Base64.encodeToString(encrypted.iv, Base64.NO_WRAP));

                    ConversationEntity conversation = new ConversationEntity(contactUid, contactName, messageText, timestamp);
                    db.getAppDatabase().conversationDao().insertOrUpdate(conversation);

                    message.messageIndex = 0;

                    sendInitialMessageToWebSocket(myUID, contactUid, initialMessage, timestamp, fullName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            db.getAppDatabase().messageDao().insert(message);
        }).start();
        ChatMessageModel chatMsg = new ChatMessageModel("Eu", messageText, timestamp, true, false);
        messageList.add(chatMsg);
        runOnUiThread(this::sortAndDisplayMessages);
    }

    private void loadMoreMessages(String conversationUid) {
        isLoading = true;

        new Thread(() -> {
            DBClient db = DBClient.getInstance(getApplicationContext());

            ConversationEntity conv = db.getAppDatabase().conversationDao().getConversationByUid(conversationUid);
            String contactName = (conv != null) ? conv.name : "Contact";

            List<MessageEntity> newMessages = db.getAppDatabase()
                    .messageDao()
                    .getMessagesByConversation(conversationUid, PAGE_SIZE, messagesLoadedCount);

            List<ChatMessageModel> converted = new ArrayList<>();
            for (MessageEntity entity : newMessages) {
                converted.add(new ChatMessageModel(
                        entity.fromMe ? "Eu" : contactName,
                        entity.message,
                        entity.timestamp,
                        entity.fromMe,
                        false
                ));
            }

            runOnUiThread(() -> {
                if (!converted.isEmpty()) {

                    for (ChatMessageModel msg : converted) {
                        boolean alreadyExists = false;
                        for (ChatMessageModel existing : messageList) {
                            if (existing.getMessage_in_plaintext().equals(msg.getMessage_in_plaintext()) &&
                                    existing.getTimestamp().equals(msg.getTimestamp()) &&
                                    Objects.equals(existing.getFrom(), msg.getFrom())) {
                                alreadyExists = true;
                                break;
                            }
                        }
                        if (!alreadyExists) {
                            messageList.add(msg);
                        }
                    }

                    messagesLoadedCount += converted.size();

                    List<Object> prepared = ChatTimestampHelper.prepareDisplayList(messageList);

                    if (adapter == null) {
                        adapter = new ChatMessagesAdapter(prepared);
                        recyclerView.setAdapter(adapter);
                        recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                    } else {
                        adapter.updateList(prepared);
                    }

                    recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                }

                isLoading = false;
            });
        }).start();
    }

    private void showAttachmentMenu() {
        String[] options = {"Image", "Document", "Camera"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose file type...");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    Intent pickImage = new Intent(Intent.ACTION_GET_CONTENT);
                    pickImage.setType("image/*");
                    startActivityForResult(pickImage, 2001);
                    break;
                case 1:
                    Intent pickDoc = new Intent(Intent.ACTION_GET_CONTENT);
                    pickDoc.setType("*/*");
                    startActivityForResult(pickDoc, 2002);
                    break;
                case 2:
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, 2003);
                    break;
            }
        });
        builder.show();
    }
    public String copyUriToInternalStorage(Context context, Uri uri, String desiredFileName) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            File outputDir = new File(context.getFilesDir(), "attachments");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            File outputFile = new File(outputDir, desiredFileName);
            OutputStream outputStream = new FileOutputStream(outputFile);

            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            return outputFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    private void fadeChangeIcon(ImageView imageView, int newIconResId) {
        imageView.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction(() -> {
                    imageView.setImageResource(newIconResId);
                    imageView.animate()
                            .alpha(1f)
                            .setDuration(150)
                            .start();
                })
                .start();
    }
    private void startRecordingVisual() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO}, 1001);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO}, 1001);
            }

            return;
        }

        if (isRecording) return;
        isRecording = true;

        sendButton.animate()
                .alpha(0f)
                .setDuration(100)
                .withEndAction(() -> {
                    sendButton.setBackgroundResource(R.drawable.bg_send_button_recording);
                    sendButton.animate().alpha(1f).setDuration(100).start();
                }).start();

        EditText messageEditText = findViewById(R.id.message_edit_text);
        TextView recordingTimer = findViewById(R.id.recording_timer);
        messageEditText.setVisibility(View.GONE);
        recordingTimer.setVisibility(View.VISIBLE);

        try {
            audioFile = File.createTempFile("voice_", ".m4a", getCacheDir());
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setOutputFile(audioFile.getAbsolutePath());
            recorder.prepare();
            recorder.start();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Recording error...", Toast.LENGTH_SHORT).show();
            return;
        }

        recordingStartTime = SystemClock.elapsedRealtime();
        updateRecordingTimer = new Runnable() {
            @SuppressLint("DefaultLocale")
            @Override
            public void run() {
                long elapsedMillis = SystemClock.elapsedRealtime() - recordingStartTime;
                int seconds = (int) (elapsedMillis / 1000);
                int minutes = seconds / 60;
                seconds %= 60;
                recordingTimer.setText(String.format("%02d:%02d", minutes, seconds));
                recordingHandler.postDelayed(this, 500);
            }
        };
        recordingHandler.post(updateRecordingTimer);
    }
    private void stopRecordingVisual() {
        if (!isRecording) return;
        isRecording = false;

        sendButton.animate()
                .alpha(0f)
                .setDuration(100)
                .withEndAction(() -> {
                    sendButton.setBackgroundResource(R.drawable.bg_send_button);
                    sendButton.animate().alpha(1f).setDuration(100).start();
                }).start();

        EditText messageEditText = findViewById(R.id.message_edit_text);
        TextView recordingTimer = findViewById(R.id.recording_timer);
        messageEditText.setVisibility(View.VISIBLE);
        recordingTimer.setVisibility(View.GONE);

        recordingHandler.removeCallbacks(updateRecordingTimer);

        if (recorder != null) {
            try {
                recorder.stop();
            } catch (RuntimeException e) {
                e.printStackTrace();
                audioFile.delete();
                Toast.makeText(this, "Hold more to record...", Toast.LENGTH_SHORT).show();
                return;
            }
            recorder.release();
            recorder = null;
        }

        long elapsedMillis = SystemClock.elapsedRealtime() - recordingStartTime;
        int durationSeconds = (int) (elapsedMillis / 1000);

        if (durationSeconds < 1) {
            if (audioFile != null && audioFile.exists()) {
                audioFile.delete();
            }
            Toast.makeText(this, "Hold longer to record...", Toast.LENGTH_SHORT).show();
            return;
        }

        String timestamp = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date());

        VoiceMessageModel voiceMsg = new VoiceMessageModel(
                "Eu", timestamp, true, audioFile.getAbsolutePath(), durationSeconds
        );

        messageList.add(voiceMsg);

        sortAndDisplayMessages();
    }
    private void sortAndDisplayMessages() {
        new Thread(() -> {
            messageList.sort((m1, m2) -> {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd.MM.yyyy", Locale.getDefault());
                    Date date1 = sdf.parse(m1.getTimestamp());
                    Date date2 = sdf.parse(m2.getTimestamp());
                    return date1.compareTo(date2);
                } catch (Exception e) {
                    e.printStackTrace();
                    return 0;
                }
            });

            List<Object> displayList = ChatTimestampHelper.prepareDisplayList(messageList);

            runOnUiThread(() -> {
                if (adapter == null) {
                    adapter = new ChatMessagesAdapter(displayList);
                    recyclerView.setAdapter(adapter);
                } else {
                    adapter.updateList(displayList);
                }
                recyclerView.scrollToPosition(adapter.getItemCount() - 1);
            });
        }).start();
    }

    private void sendInitialMessageToWebSocket(String senderUid, String receiverUid, JSONObject initialMessage, String timestamp, String fullName) {
        try {
            WebSocket socket = WebSocketClientManager.getInstance().getWebSocket();
            if (socket == null) {
                Log.e("WebSocket", "WebSocket not connected");
                return;
            }

            JSONObject msg = new JSONObject();
            String cleanedSenderUid = new JSONObject(senderUid).optString("uid");
            msg.put("UID", cleanedSenderUid);
            msg.put("toUID", receiverUid);
            JSONObject initialMessageJSON = new JSONObject();
            initialMessageJSON.put("initialMessage", initialMessage);
            msg.put("data", initialMessageJSON);
            msg.put("timestamp", timestamp);
            msg.put("messageType", "text");
            msg.put("fullName", fullName);

            socket.send(msg.toString());
        } catch (Exception e) {
            Log.e("WebSocket", "Error sending message", e);
        }
    }

    private void sendMessageToWebSocket(String senderUid, String receiverUid, JSONObject message, String timestamp, String fullName) {
        try {
            WebSocket socket = WebSocketClientManager.getInstance().getWebSocket();
            if (socket == null) {
                Log.e("WebSocket", "WebSocket not connected");
                return;
            }

            JSONObject msg = new JSONObject();
            msg.put("UID", senderUid);
            msg.put("toUID", receiverUid);
            JSONObject normalMessageJSON = new JSONObject();
            normalMessageJSON.put("normalMessage", message);
            msg.put("data", normalMessageJSON);
            msg.put("timestamp", timestamp);
            msg.put("messageType", "text");
            msg.put("fullName", fullName);

            Log.d("WebSocket", "msg = " + msg);
            socket.send(msg.toString());
        } catch (Exception e) {
            Log.e("WebSocket", "Error sending message", e);
        }
    }
}