package atm.licenta.cy.Models;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MessageItemModel {
    private String uid;
    private String from;
    private String lastMessage;
    private String rawTimestamp;
    private String formattedTimestamp;
    private int imageResId;
    private int unreadCount; //TODO
    private String conversationId; //TODO

    public MessageItemModel(Context context, String uid, String from, String lastMessage, String timestamp, int imageResId) {
        this.uid = uid;
        this.from = from;
        this.lastMessage = lastMessage;
        this.rawTimestamp = timestamp;
        this.formattedTimestamp = formatTimestamp(context, timestamp);
        this.imageResId = imageResId;
        this.unreadCount = 0;
    }

    private String formatTimestamp(Context context, String rawTimestamp) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("HH:mm dd.MM.yyyy", Locale.getDefault());
            Date date = inputFormat.parse(rawTimestamp);

            Calendar messageCal = Calendar.getInstance();
            messageCal.setTime(date);
            resetTime(messageCal);

            Calendar todayCal = Calendar.getInstance();
            resetTime(todayCal);

            Calendar yesterdayCal = (Calendar) todayCal.clone();
            yesterdayCal.add(Calendar.DATE, -1);

            if (messageCal.equals(todayCal)) {
                return "TODAY";
            } else if (messageCal.equals(yesterdayCal)) {
                return "YESTERDAY";
            } else {
                return new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(date);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return rawTimestamp;
        }
    }


    private void resetTime(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    public String getFrom() {
        return from;
    }
    public void setFrom(String from) {
        this.from = from;
    }
    public String getUid() {
        return uid;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public String getTimestamp() {
        return formattedTimestamp;
    }

    public String getRawTimestamp() {
        return rawTimestamp;
    }

    public int getImageResId() {
        return imageResId;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public String getConversationId() {
        return conversationId;
    }
}
