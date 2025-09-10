package com.rk.inventory_management_system.ui.nventory_management_system_ui.utils;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FormatUtils {

    private final static DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");
    private final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");


    public static String formatCurrency(Double amount) {
        return amount != null ? "Rs."+CURRENCY_FORMAT.format(amount) : "Rs. 0.00";
    }

    private static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? DATE_TIME_FORMATTER.format(dateTime) : "";
    }

    private static String formatPercentage(Double percentage) {
        return percentage != null ? CURRENCY_FORMAT.format(percentage) + "%" : "0%";
    }

}