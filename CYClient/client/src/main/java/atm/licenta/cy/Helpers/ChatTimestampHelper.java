package atm.licenta.cy.Helpers;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import atm.licenta.cy.Models.ChatMessageModel;

public class ChatTimestampHelper {
    public static List<Object> prepareDisplayList(List<ChatMessageModel> originalMessages) {
        Map<String, List<ChatMessageModel>> groupedByDate = new TreeMap<>((d1, d2) -> {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                Date date1 = sdf.parse(d1);
                Date date2 = sdf.parse(d2);
                return date1.compareTo(date2);
            } catch (Exception e) {
                return d1.compareTo(d2);
            }
        });

        for (ChatMessageModel msg : originalMessages) {
            String date = msg.getDateOnlyFromTimestamp();
            groupedByDate.putIfAbsent(date, new ArrayList<>());
            groupedByDate.get(date).add(msg);
        }

        List<Object> displayList = new ArrayList<>();

        for (String date : groupedByDate.keySet()) {
            String label;
            if (isToday(date)) {
                label = "TODAY";
            } else if (isYesterday(date)) {
                label = "YESTERDAY";
            } else {
                label = date;
            }

            displayList.add(label);
            displayList.addAll(groupedByDate.get(date));
        }

        return displayList;
    }


    private static boolean isToday(String date) {
        return formatDate(new Date()).equals(date);
    }

    private static boolean isYesterday(String date) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return formatDate(cal.getTime()).equals(date);
    }

    private static String formatDate(Date date) {
        return new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(date);
    }
}