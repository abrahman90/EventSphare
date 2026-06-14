package com.example.eventsphere.util;

import com.example.eventsphere.enums.EventCategory;

public class PricingUtil {

    public static double getCategoryPrice(EventCategory category) {
        return switch (category) {
            case SEMINAR -> 500.0;
            case WORKSHOP -> 700.0;
            case CONFERENCE -> 1000.0;
            case WEDDING -> 1500.0;
            case CORPORATE_EVENT -> 1200.0;
        };
    }

    public static double getCapacityCharge(int capacity) {
        if (capacity > 500) return 1200.0;
        if (capacity > 100) return 500.0;
        return 0.0;
    }

    public static double calculateTotal(EventCategory category, int capacity) {
        return getCategoryPrice(category) + getCapacityCharge(capacity);
    }
}
