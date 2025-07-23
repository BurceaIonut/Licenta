package atm.licenta.cy.Services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.core.content.ContextCompat;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences prefs = context.getSharedPreferences("userPrefs", Context.MODE_PRIVATE);
            String uid = prefs.getString("UID", null);

            if (uid != null) {
                Intent serviceIntent = new Intent(context, ForegroundWebSocketService.class);
                serviceIntent.putExtra("uid", uid);
                ContextCompat.startForegroundService(context, serviceIntent);
            }
        }
    }
}