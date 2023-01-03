import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Dates {
    public void getDate() {
        long time = 1669708323693L;
        Date date = new Date(time);
        DateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
        DateFormat format2 = new SimpleDateFormat("HH:mm:ss.SSS");
        format1.setTimeZone(TimeZone.getTimeZone("Australia/Sydney"));
        format2.setTimeZone(TimeZone.getTimeZone("Australia/Sydney"));
        String formatted = format1.format(date) + "T" + format2.format(date) + "Z";
        System.out.println(formatted);
    }

    public static void main(String[] args) {
        Dates d = new Dates();
        d.getDate();
    }
}
