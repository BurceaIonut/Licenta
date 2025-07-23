package atm.licenta.cy.Activities;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.FileInputStream;

import atm.licenta.cy.R;

public class ShowQRActivity extends AppCompatActivity {

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_show_qractivity);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.option_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Show your QR Code to others");
        }

        try {
            SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
            String myUID =  prefs.getString("UID", null);
            FileInputStream fis = openFileInput(myUID + "_qr.png");
            Bitmap qrBitmap = BitmapFactory.decodeStream(fis);
            fis.close();

            ImageView qrImage = findViewById(R.id.qr_image);
            qrImage.setImageBitmap(qrBitmap);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}