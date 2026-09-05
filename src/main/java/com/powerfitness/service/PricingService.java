package com.powerfitness.service;

import org.springframework.stereotype.Service;

@Service
public class PricingService {

    public static final double CARDIO_FEE = 500.0;

    public double getBasePlanPrice(String plan) {
        if (plan == null) return 800.0;
        switch (plan.trim()) {
            case "1 Month": return 800.0;
            case "3 Months": return 1800.0;
            case "6 Months": return 3500.0;
            case "1 Year": return 6800.0;
            default: return 800.0;
        }
    }

    public double calculateTotalAmount(String plan, boolean hasCardio) {
        double base = getBasePlanPrice(plan);
        double cardio = hasCardio ? CARDIO_FEE : 0.0;
        return base + cardio;
    }

    public int getPlanDurationDays(String plan) {
        if (plan == null) return 30;
        switch (plan.trim()) {
            case "1 Month": return 30;
            case "3 Months": return 90;
            case "6 Months": return 180;
            case "1 Year": return 365;
            default: return 30;
        }
    }
}
