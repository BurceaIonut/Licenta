package atm.licenta.cy.Activities;

import static atm.licenta.cy.Helpers.CertificateHelper.trustAllCertificates;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONException;
import org.json.JSONObject;

import atm.licenta.cy.R;
import atm.licenta.cy.WebSockets.SignalingWebSocketManager;

public class CallingActivity extends AppCompatActivity {
    private LinearLayout layoutCalling, layoutIncoming;
    private WebView webviewCall;
    private Button btnAccept, btnDecline;
    private ImageView imageCalling, imageIncoming;
    private TextView nameCalling, nameIncoming, statusIncoming, statusCalling;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1000) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    finish();
                    return;
                }
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webviewCall != null) {
            webviewCall.destroy();
        }
        //SignalingWebSocketManager.getInstance(this).send("bye");
        //SignalingWebSocketManager.getInstance(this).close();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_calling);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //trustAllCertificates();

        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.RECORD_AUDIO
            }, 1000);
        }


        //SignalingWebSocketManager signaling = SignalingWebSocketManager.getInstance(this);

        boolean isIncoming = getIntent().getBooleanExtra("isIncoming", false);
        String contactName = getIntent().getStringExtra("contactName");
        String contactUid = getIntent().getStringExtra("contactUid");
        String roomId = getIntent().getStringExtra("roomId");

        //signaling.connect(roomId);

        layoutCalling = findViewById(R.id.layout_calling);
        layoutIncoming = findViewById(R.id.layout_incoming);
        webviewCall = findViewById(R.id.webview_call);

        imageCalling = findViewById(R.id.image_profile_calling);
        imageIncoming = findViewById(R.id.image_profile_incoming);
        nameCalling = findViewById(R.id.text_name_calling);
        nameIncoming = findViewById(R.id.text_name_incoming);
        statusCalling = findViewById(R.id.text_status_calling);
        statusIncoming = findViewById(R.id.text_status_incoming);
        btnAccept = findViewById(R.id.btn_accept);
        btnDecline = findViewById(R.id.btn_decline);

        nameCalling.setText(contactName);
        nameIncoming.setText(contactName);
        imageCalling.setImageResource(R.drawable.profile_picture_placeholder);
        imageIncoming.setImageResource(R.drawable.profile_picture_placeholder);

        if (!isIncoming) {
            //showCallingUI();
            String url = "https://172.29.23.176:8090/room?roomID=" + roomId + "&audio=true&video=true&platform=android";
            showWebViewUI(url);

//            SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
//            String myUid = prefs.getString("UID", null);
//            String myUIDCleaned;
//            try {
//                myUIDCleaned = new JSONObject(myUid).optString("uid");
//            } catch (JSONException e) {
//                throw new RuntimeException(e);
//            }
//
//            SignalingWebSocketManager.getInstance(this)
//                    .sendRinging(myUIDCleaned, contactName, roomId);
        } else {
            showIncomingUI();
        }

//        btnAccept.setOnClickListener(v -> {
//            //signaling.send("got user media");
//            String url = "https://172.29.23.176:8090/room?roomID=" + roomId + "&audio=true&video=true&platform=android";
//            showWebViewUI(url);
//        });
//
//        btnDecline.setOnClickListener(v -> {
//            SignalingWebSocketManager.getInstance(this).send("bye");
//            SignalingWebSocketManager.getInstance(this).close();
//            finish();
//        });
    }

    private void showCallingUI() {
        layoutCalling.setVisibility(View.VISIBLE);
        layoutIncoming.setVisibility(View.GONE);
        webviewCall.setVisibility(View.GONE);
    }

    private void showIncomingUI() {
        layoutCalling.setVisibility(View.GONE);
        layoutIncoming.setVisibility(View.VISIBLE);
        webviewCall.setVisibility(View.GONE);
    }

    private void showWebViewUI(String url) {
        layoutCalling.setVisibility(View.GONE);
        layoutIncoming.setVisibility(View.GONE);
        webviewCall.setVisibility(View.VISIBLE);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);

        finish();

//        WebSettings webSettings = webviewCall.getSettings();
//        webSettings.setJavaScriptEnabled(true);
//        webSettings.setMediaPlaybackRequiresUserGesture(false);
//
//        webviewCall.setWebViewClient(new WebViewClient() {
//            @Override
//            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
//                handler.proceed();
//            }
//        });
//
//        webviewCall.setWebChromeClient(new WebChromeClient() {
//            @Override
//            public void onPermissionRequest(final PermissionRequest request) {
//                runOnUiThread(() -> request.grant(request.getResources()));
//            }
//        });
//        webviewCall.loadUrl(url);
    }
}