package org.example.main.service.table;

import org.example.main.model.table.RestaurantTable;
import org.example.main.repository.table.RestaurantTableRepository;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.concurrent.ThreadLocalRandom;

public final class RestaurantTableUtils {

    private RestaurantTableUtils() {}

    public static int ensureTableNumber(RestaurantTable table, RestaurantTableRepository repository) {
        if (table.getTableNumber() != null) return table.getTableNumber();

        int candidate = 1;
        while (repository.findByTableNumber(candidate).isPresent()) {
            candidate++;
            if (candidate > 99999) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "No available table numbers");
            }
        }
        table.setTableNumber(candidate);
        return candidate;
    }

    public static String formatCode(int tableNumber) {
        return "T" + tableNumber;
    }


    public static String generatePinCode() {
        int n = ThreadLocalRandom.current().nextInt(0, 10000);
        return String.format("%04d", n);
    }

    public static String sanitizePin(String candidate) {
        if (candidate == null) {
            return generatePinCode();
        }
        String digits = candidate.replaceAll("\\D+", "");
        if (digits.isEmpty()) {
            return generatePinCode();
        }
        if (digits.length() > 4) {
            digits = digits.substring(digits.length() - 4);
        }
        try {
            int val = Integer.parseInt(digits);
            return String.format("%04d", val);
        } catch (NumberFormatException ex) {
            return generatePinCode();
        }
    }

    public static boolean isValidPin(String pin) {
        return pin != null && pin.matches("^\\d{1,4}$");
    }
}