package atm.licenta.cy.Activities;

import static atm.licenta.cy.Helpers.CertificateHelper.trustAllCertificates;
import static atm.licenta.cy.Helpers.Constants.PROXY_GATEWAY_ADDRESS;
import static atm.licenta.crypto_engine.Utils.Generate.generateKeysAndBundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import atm.licenta.cy.Database.DBClient;
import atm.licenta.cy.Database.Entities.ProfileEntity;
import atm.licenta.cy.R;
import atm.licenta.crypto_engine.KeyHelpers.PreKeyBundle;
import atm.licenta.crypto_engine.Utils.KeyStore;
import atm.licenta.crypto_engine.Utils.TempKeyStore;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import android.graphics.Bitmap;
import java.io.FileOutputStream;

public class RegisterActivity extends AppCompatActivity {
    private TextInputEditText firstNameInput, lastNameInput;
    private Button createProfileButton;
    private ImageButton exitButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.register_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.register), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.option_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        exitButton = findViewById(R.id.exitButton);
        exitButton.setOnClickListener(v -> {
            finishAffinity();
            System.exit(0);
        });

        firstNameInput = findViewById(R.id.firstNameInput);
        lastNameInput = findViewById(R.id.lastNameInput);
        createProfileButton = findViewById(R.id.createProfileButton);

        createProfileButton.setOnClickListener(v -> {
            String firstName = firstNameInput.getText() != null ? firstNameInput.getText().toString() : "";
            String lastName = lastNameInput.getText() != null ? lastNameInput.getText().toString() : "";

            if (firstName.isEmpty() || lastName.isEmpty()) {
                Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show();
                return;
            }

            new Thread(() -> {
                try {
                    trustAllCertificates();

                    PreKeyBundle PKB = generateKeysAndBundle();

                    URL url = new URL("https://" + PROXY_GATEWAY_ADDRESS + "/account/register");
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONObject json = PKB.toJSON();
                    json.put("first_name", firstName);
                    json.put("last_name", lastName);

                    OutputStream os = conn.getOutputStream();
                    os.write(json.toString().getBytes());
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        String uid = new java.util.Scanner(conn.getInputStream()).useDelimiter("\\A").next();

                        ProfileEntity profile = new ProfileEntity(uid, firstName, lastName);
                        DBClient.getInstance(getApplicationContext())
                                .getAppDatabase()
                                .profileDao()
                                .insertProfile(profile);

                        KeyStore.saveEncryptedKeys(
                                getApplicationContext(), uid,
                                TempKeyStore.identityKey, TempKeyStore.identityIV,
                                TempKeyStore.signedKey, TempKeyStore.signedIV,
                                TempKeyStore.otpKeyMap, TempKeyStore.lastResortPQKey, TempKeyStore.lastResortPQKeyIV
                        );

                        TempKeyStore.clear();

                        runOnUiThread(() -> {
                            SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean("hasAccount", true);
                            editor.putString("UID", uid);
                            editor.putString("firstName", firstName);
                            editor.putString("lastName", lastName);
                            editor.putString("identityPublicKey", PKB.identityKeyPublic);
                            editor.apply();

                            generateAndSaveQR(uid, firstName, lastName, PKB.identityKeyPublic);

                            Intent intent = new Intent(RegisterActivity.this, ConversationsActivity.class);
                            startActivity(intent);
                            finish();
                        });

                    } else {
                        runOnUiThread(() -> Toast.makeText(this, "Register error!", Toast.LENGTH_SHORT).show());
                    }

                    conn.disconnect();

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            }).start();
        });
    }

    private void generateAndSaveQR(String uid, String firstName, String lastName, String identityPublicKey) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("uid", uid);
            jsonObject.put("first_name", firstName);
            jsonObject.put("last_name", lastName);
            jsonObject.put("status", "");
            jsonObject.put("identityPublicKey", identityPublicKey);

            String qrData = jsonObject.toString();

            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(qrData, BarcodeFormat.QR_CODE, 600, 600);

            FileOutputStream fos = openFileOutput(uid + "_qr.png", MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            Log.d("QR", "QR code saved in internal storage");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}