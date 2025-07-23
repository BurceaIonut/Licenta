package atm.licenta.cy.WebSockets;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class SignalingWebSocketManager {
    private static SignalingWebSocketManager instance;
    private WebSocket webSocket;
    private Context context;
    private boolean isConnected = false;
    private String roomId;
    private static final String TAG = "SignalingWS";

    private SignalingWebSocketManager(Context ctx) {
        this.context = ctx.getApplicationContext();
    }

    public static SignalingWebSocketManager getInstance(Context ctx) {
        if (instance == null) {
            instance = new SignalingWebSocketManager(ctx);
        }
        return instance;
    }

    public void connect(String roomId) {
        this.roomId = roomId;
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();

        Request request = new Request.Builder()
                .url("wss://172.29.23.176:8090/join?roomID=" + roomId)
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                isConnected = true;
                Log.d(TAG, "WebSocket opened");
                send("create or join");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d(TAG, "Received: " + text);
                try {
                    JSONObject json = new JSONObject(text);
                    String type = json.optString("type");

                    if ("ringing".equals(type)) {
                        Intent callIntent = new Intent("CALL_EVENT");
                        callIntent.putExtra("contactName", json.optString("fromName"));
                        callIntent.putExtra("contactUid", json.optString("fromUID"));
                        callIntent.putExtra("roomId", json.optString("roomId"));
                        context.sendBroadcast(callIntent);
                    } else {
                        Intent signalIntent = new Intent("SIGNALING_MESSAGE");
                        signalIntent.putExtra("message", text);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(signalIntent);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "Closing: " + reason);
                isConnected = false;
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "Error: " + t.getMessage());
                isConnected = false;
            }
        });
    }

    public void send(String message) {
        if (webSocket != null && isConnected) {
            JSONObject msg = new JSONObject();
            try {
                msg.put("content", message);
                webSocket.send(msg.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void send(JSONObject jsonMessage) {
        if (webSocket != null && isConnected) {
            webSocket.send(jsonMessage.toString());
            Log.d(TAG, "Sent JSON: " + jsonMessage.toString());
        }
    }

    public void close() {
        if (webSocket != null) {
            webSocket.close(1000, "Closing by user");
            webSocket = null;
            isConnected = false;
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void sendRinging(String fromUID, String fromName, String roomId) {
        if (webSocket != null && isConnected) {
            try {
                JSONObject json = new JSONObject();
                json.put("type", "ringing");
                json.put("fromUID", fromUID);
                json.put("fromName", fromName);
                json.put("roomId", roomId);
                webSocket.send(json.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
