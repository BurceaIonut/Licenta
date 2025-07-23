package atm.licenta.cy.Models;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CallItemModel {
    private String from;
    private String timestamp;
    private int imageResId;
    private boolean wasVideo;
    private boolean missed;

    public CallItemModel(String from, String timestamp, int imageResId, boolean wasVideo, boolean missed) {
        this.from = from;
        this.timestamp = formatTimestamp(timestamp);
        this.imageResId = imageResId;
        this.wasVideo = wasVideo;
        this.missed = missed;
    }

    private String formatTimestamp(String rawTimestamp) {
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
                SimpleDateFormat dayMonthYear = new SimpleDateFormat("d MMMM yyyy", new Locale("en"));
                SimpleDateFormat hourMinute = new SimpleDateFormat("HH:mm", Locale.getDefault());
                return dayMonthYear.format(date) + ", " + hourMinute.format(date);
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
    public String getTimestamp() {
        return timestamp;
    }
    public int getImageResId() {
        return imageResId;
    }
    public boolean isWasVideo() {
        return wasVideo;
    }
    public boolean isMissed() {
        return missed;
    }
}
