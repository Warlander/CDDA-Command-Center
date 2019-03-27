package pl.warlander.cdda.launcher.utils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class TimeUtils {
    
    public static String timestampToNowString(LocalDateTime timestamp) {
        LocalDateTime currentTime = LocalDateTime.now();
        long days = timestamp.until(currentTime, ChronoUnit.DAYS);
        long hours = timestamp.until(currentTime, ChronoUnit.HOURS);
        long minutes = timestamp.until(currentTime, ChronoUnit.MINUTES);
        
        if (days > 1) {
            return days + " days ago";
        }
        else if (days == 1) {
            return days + " day ago";
        }
        else if (hours > 1) {
            return hours + " hours ago";
        }
        else if (hours == 1) {
            return hours + " hour ago";
        }
        else if (minutes > 1) {
            return minutes + " minutes ago";
        }
        else if (minutes == 1) {
            return minutes + " minute ago";
        }
        else {
            return "moment ago";
        }
    }
    
}
