package atm.licenta.cy.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import atm.licenta.cy.R;

public class WelcomeActivity extends AppCompatActivity {
    private ImageButton exitButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        //splashScreen.setKeepOnScreenCondition(() -> true);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_welcome);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
        boolean hasAccount = prefs.getBoolean("hasAccount", false);

        if (hasAccount) {
            startActivity(new Intent(this, ConversationsActivity.class));
            finish();
        }

        splashScreen.setOnExitAnimationListener(splashScreenView -> {
            View iconView = splashScreenView.getIconView();

            if (iconView != null) {
                iconView.animate()
                        .scaleX(1.2f)
                        .scaleY(1.2f)
                        .alpha(0f)
                        .setDuration(500L)
                        .withEndAction(splashScreenView::remove)
                        .start();
            } else {
                splashScreenView.remove();
            }
        });

        exitButton = findViewById(R.id.exitButton);
        exitButton.setOnClickListener(v -> {
            finishAffinity();
            System.exit(0);
        });

        LinearLayout restoreButton = findViewById(R.id.already_registered);
        LinearLayout newAccountButton = findViewById(R.id.new_account);

        restoreButton.setOnClickListener(v -> {

        });

        newAccountButton.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

    }
}