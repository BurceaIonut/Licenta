package atm.licenta.cy.Services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import atm.licenta.cy.Activities.CallingActivity;

public class CallEventReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        String contactName = intent.getStringExtra("contactName");
        String contactUid = intent.getStringExtra("contactUid");
        String roomId = intent.getStringExtra("roomId");

        Intent callIntent = new Intent(context, CallingActivity.class);
        callIntent.putExtra("isIncoming", true);
        callIntent.putExtra("contactName", contactName);
        callIntent.putExtra("contactUid", contactUid);
        callIntent.putExtra("roomId", roomId);
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(callIntent);
    }
}
