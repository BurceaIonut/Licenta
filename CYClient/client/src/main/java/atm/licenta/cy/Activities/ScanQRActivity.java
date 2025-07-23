package atm.licenta.cy.Activities;

import static atm.licenta.cy.Helpers.Constants.PROXY_GATEWAY_ADDRESS;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import atm.licenta.crypto_engine.KeyHelpers.KeyStoreHelper;
import atm.licenta.crypto_engine.Utils.KeyStore;
import atm.licenta.crypto_engine.Utils.Signing;
import atm.licenta.cy.Database.DBClient;
import atm.licenta.cy.Database.Entities.ContactEntity;
import atm.licenta.cy.R;
import atm.licenta.cy.WebSockets.WebSocketClientManager;
import okhttp3.WebSocket;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ScanQRActivity extends AppCompatActivity {

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 123) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startQRCodeScanner();
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_scan_qractivity);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.option_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Scan your friend's QR Code");
        }

        Button startScan = findViewById(R.id.startScanButton);
        startScan.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 123);
            } else {
                startQRCodeScanner();
            }
        });

    }

    private void startQRCodeScanner() {
        IntentIntegrator integrator = new IntentIntegrator(ScanQRActivity.this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan a QR code");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    JSONObject json = new JSONObject(result.getContents());
                    handleScannedContact(json);
                } catch (Exception e) {
                    Toast.makeText(this, "Invalid QR content", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleScannedContact(JSONObject json) {
        try {
            String uid = json.getString("uid");
            String cleanedUid = new JSONObject(uid).optString("uid");
            String firstName = json.getString("first_name");
            String lastName = json.getString("last_name");
            String status = json.optString("status", "");
            String identityPublicKeyBase64 = json.getString("identityPublicKey");
            new Thread(() -> {
                DBClient.getInstance(getApplicationContext())
                        .getAppDatabase()
                        .contactDao()
                        .deleteByUid(cleanedUid);
                ContactEntity existing = DBClient.getInstance(getApplicationContext())
                        .getAppDatabase()
                        .contactDao()
                        .getContactByUid(cleanedUid);

                if (existing != null) {
                    Looper.prepare();
                    Toast.makeText(this, "This person already exists in your contact list", Toast.LENGTH_SHORT).show();
                } else {
                    ContactEntity contact = new ContactEntity(cleanedUid, firstName, lastName, status, identityPublicKeyBase64);
                    DBClient.getInstance(getApplicationContext())
                            .getAppDatabase()
                            .contactDao()
                            .insert(contact);
                    sendValidationRequestAndShowQR(cleanedUid);
                }
            }).start();
        } catch (JSONException e) {
            runOnUiThread(() -> Toast.makeText(this, "Invalid QR content", Toast.LENGTH_SHORT).show());
            e.printStackTrace();
        }
    }

    private void sendValidationRequestAndShowQR(String scannedUID) {
            try {
                SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
                String myUID = prefs.getString("UID", null);

                String signature = Base64.encodeToString(Signing.generateSignatureForQRValidationRequest(getApplicationContext(), myUID, scannedUID), Base64.NO_WRAP);

                String cleanedUid = new JSONObject(myUID).optString("uid");
                JSONObject requestJson = new JSONObject();
                requestJson.put("uid1", cleanedUid);
                requestJson.put("uid2", scannedUID);
                requestJson.put("signature", signature);

                WebSocket socket = WebSocketClientManager.getInstance().getWebSocket();
                if (socket == null) {
                    Log.e("WebSocket", "WebSocket not connected");
                    return;
                }
                socket.send(requestJson.toString());
            } catch (Exception e) {
                Log.e("ERROR SENDING QR CODE VALIDATION REQUEST: ", e.getMessage());
            }
    }
}