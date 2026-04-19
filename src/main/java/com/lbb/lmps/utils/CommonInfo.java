package com.lbb.lmps.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

@Component
@Slf4j
public class CommonInfo {

    /**
     * gen request date time
     * yyyyMMddHHmmss
     *
     * @return 20221231101960
     */
    public String genDt() {
        return DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
    }

    /**
     * gen RRN (Luhn algorithm)
     * yDDDHHMMSSSx
     *
     * @return
     */
    public String genRRNO() {

        // get datetime
        Date today = new Date();
        // Get the current date and time.
        Calendar calendar = Calendar.getInstance();

        // Get the day of the year.
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);

        SimpleDateFormat yearFmt = new SimpleDateFormat("yy");

        // print year 2 digits
        String year = yearFmt.format(today);
        // print format 3 digits
        String numDayOfYear = String.format("%03d", dayOfYear);
        // print hour 24 h 2 digits
        String hour = String.format("%02d", Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
        //print minute 2 digits
        String minute = String.format("%02d", Calendar.getInstance().get(Calendar.MINUTE));

        // print millisecond of the day
        String milliSec = String.format("%03d", today.getTime() % 1000);

        String rrn = year + numDayOfYear + hour + minute + milliSec;

        // Calculate the check digit using the Luhn algorithm
        int sum = 0;
        for (int i = 0; i < rrn.length(); i++) {
            int digit = Integer.parseInt(rrn.substring(i, i + 1));
            if (i % 2 == 0) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
        }
        int checkDigit = (10 - (sum % 10)) % 10;

        return (rrn + checkDigit).substring(1, 13);

    }

    public String genTransactionId(String pre) {
        // get datetime
        Date today = new Date();

        // print year 2 digits
        String year = new SimpleDateFormat("yy").format(today);

        // Get the day of the year (ex: 365)
        int dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        String dayOfYearFmt = String.format("%03d", dayOfYear);

        //--- convert datetime from midnight to second
        String timeToSecond = String.format("%05d", LocalTime.now().toSecondOfDay());

        //--- get milliseconds last 3 digits
        long currentMilliseconds = System.currentTimeMillis();
        int last3digits = (int) (currentMilliseconds % 1000);

        return pre + year + dayOfYearFmt + timeToSecond + String.format("%03d", last3digits) + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }

}
