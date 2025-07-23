package atm.licenta.cy.Models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatMessageModel {
    private Integer conversationId;
    private String from;
    private String message_in_plaintext;
    private String timestamp;
    private boolean sentByMe;
    private boolean groupMessage;
    private boolean isSent = true;
    private boolean isDelivered = false;
    private boolean isRedirected = false;

    public ChatMessageModel(String from, String message_in_plaintext, String timestamp, boolean sentByMe, boolean groupMessage) {
        this.from = from;
        this.message_in_plaintext = message_in_plaintext;
        this.timestamp = timestamp;
        this.sentByMe = sentByMe;
        this.groupMessage = groupMessage;
    }

    public String getFrom() {
        return from;
    }

    public String getMessage_in_plaintext() {
        return message_in_plaintext;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getTimeFromTimestamp() {
        try {
            SimpleDateFormat fullFormat = new SimpleDateFormat("HH:mm dd.MM.yyyy", Locale.getDefault());
            Date date = fullFormat.parse(timestamp);
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return timeFormat.format(date);
        } catch (Exception e) {
            return timestamp; // fallback
        }
    }

    public String getDateOnlyFromTimestamp() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd.MM.yyyy", Locale.getDefault());
            Date date = sdf.parse(timestamp);
            return new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(date);
        } catch (Exception e) {
            return timestamp;
        }
    }



    public boolean isSentByMe() {
        return sentByMe;
    }

    public boolean isGroupMessage() {
        return groupMessage;
    }

    public boolean isSent() {
        return isSent;
    }

    public void setSent(boolean sent) {
        isSent = sent;
    }

    public boolean isDelivered() {
        return isDelivered;
    }

    public void setDelivered(boolean delivered) {
        isDelivered = delivered;
    }

    public boolean isRedirected() {
        return isRedirected;
    }

    public void setRedirected(boolean redirected) {
        isRedirected = redirected;
    }
}
