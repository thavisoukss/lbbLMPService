package com.lbb.lmps.utils;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class CommonInfo {

    /** Returns current datetime formatted as {@code yyyyMMddHHmmss}. */
    public String genDt() {
        return LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    /**
     * Generates a 12-digit RRN using the Luhn check digit algorithm.
     * Format: yDDDHHMMSSSx (last digit of year, day-of-year, hour, minute, millisecond, check digit).
     */
    public String genRRNO() {
        LocalDateTime now = LocalDateTime.now();
        long epochMilli = System.currentTimeMillis();

        String rrn = String.format("%02d%03d%02d%02d%03d",
                now.getYear() % 100,
                now.getDayOfYear(),
                now.getHour(),
                now.getMinute(),
                epochMilli % 1000);

        int sum = 0;
        for (int i = 0; i < rrn.length(); i++) {
            int digit = Character.getNumericValue(rrn.charAt(i));
            if (i % 2 == 0) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
        }
        int checkDigit = (10 - (sum % 10)) % 10;
        return (rrn + checkDigit).substring(1, 13);
    }

    public String genTransactionId(String pre) {
        LocalDateTime now = LocalDateTime.now();
        long epochMilli = System.currentTimeMillis();

        String year = String.format("%02d", now.getYear() % 100);
        String day = String.format("%03d", now.getDayOfYear());
        String timeSeconds = String.format("%05d", LocalTime.now().toSecondOfDay());
        String millis = String.format("%03d", epochMilli % 1000);
        String random = String.format("%03d", ThreadLocalRandom.current().nextInt(1000));

        return pre + year + day + timeSeconds + millis + random;
    }
}
