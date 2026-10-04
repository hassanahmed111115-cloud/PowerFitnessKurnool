package com.powerfitness.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    @Value("${FAST2SMS_API_KEY:#{null}}")
    private String fast2SmsApiKey;

    @Value("${TWILIO_ACCOUNT_SID:#{null}}")
    private String twilioAccountSid;

    @Value("${TWILIO_AUTH_TOKEN:#{null}}")
    private String twilioAuthToken;

    @Value("${TWILIO_FROM_NUMBER:#{null}}")
    private String twilioFromNumber;

    @Value("${SMS_API_URL:#{null}}")
    private String genericSmsUrl;

    @Value("${SMS_API_KEY:#{null}}")
    private String genericSmsKey;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public boolean isConfigured() {
        return (fast2SmsApiKey != null && !fast2SmsApiKey.trim().isEmpty())
                || (twilioAccountSid != null && !twilioAccountSid.trim().isEmpty()
                    && twilioAuthToken != null && !twilioAuthToken.trim().isEmpty()
                    && twilioFromNumber != null && !twilioFromNumber.trim().isEmpty())
                || (genericSmsUrl != null && !genericSmsUrl.trim().isEmpty());
    }

    public void sendOtp(String mobileNumber, String otp) {
        if (mobileNumber == null || otp == null) {
            throw new IllegalArgumentException("Mobile number and OTP are required");
        }

        String cleanPhone = mobileNumber.replaceAll("[^0-9]", "");
        String maskedPhone = cleanPhone.length() >= 4 
                ? "******" + cleanPhone.substring(cleanPhone.length() - 4) 
                : "******";

        if (fast2SmsApiKey != null && !fast2SmsApiKey.trim().isEmpty()) {
            sendViaFast2Sms(cleanPhone, otp, maskedPhone);
        } else if (twilioAccountSid != null && !twilioAccountSid.trim().isEmpty()
                && twilioAuthToken != null && !twilioAuthToken.trim().isEmpty()
                && twilioFromNumber != null && !twilioFromNumber.trim().isEmpty()) {
            sendViaTwilio(cleanPhone, otp, maskedPhone);
        } else if (genericSmsUrl != null && !genericSmsUrl.trim().isEmpty()) {
            sendViaGenericHttp(cleanPhone, otp, maskedPhone);
        } else {
            log.error("SMS dispatch failed: No SMS provider configured in environment variables for destination {}", maskedPhone);
            throw new IllegalStateException("SMS service is not configured. Please configure FAST2SMS_API_KEY or TWILIO credentials in environment variables.");
        }
    }

    private void sendViaFast2Sms(String phone, String otp, String maskedPhone) {
        try {
            String targetNumber = phone.length() > 10 ? phone.substring(phone.length() - 10) : phone;
            String jsonPayload = String.format("{\"route\":\"otp\",\"variables_values\":\"%s\",\"numbers\":\"%s\"}", otp, targetNumber);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.fast2sms.com/dev/bulkV2"))
                    .header("authorization", fast2SmsApiKey.trim())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("OTP SMS dispatched successfully via Fast2SMS to {}", maskedPhone);
            } else {
                log.error("Fast2SMS API returned HTTP status {} for destination {}", response.statusCode(), maskedPhone);
                throw new RuntimeException("Fast2SMS gateway returned status: " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("Failed to send OTP SMS via Fast2SMS to {}: {}", maskedPhone, e.getMessage());
            throw new RuntimeException("SMS dispatch failed via Fast2SMS: " + e.getMessage(), e);
        }
    }

    private void sendViaTwilio(String phone, String otp, String maskedPhone) {
        try {
            String toFormatted = phone.startsWith("+") ? phone : (phone.length() == 10 ? "+91" + phone : "+" + phone);
            String messageBody = "Your Power Fitness Kurnool admin password recovery OTP is " + otp + ". Valid for 5 minutes. Do not share.";

            String formData = "To=" + URLEncoder.encode(toFormatted, StandardCharsets.UTF_8)
                    + "&From=" + URLEncoder.encode(twilioFromNumber.trim(), StandardCharsets.UTF_8)
                    + "&Body=" + URLEncoder.encode(messageBody, StandardCharsets.UTF_8);

            String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                    (twilioAccountSid.trim() + ":" + twilioAuthToken.trim()).getBytes(StandardCharsets.UTF_8)
            );
            String endpoint = "https://api.twilio.com/2010-04-01/Accounts/" + twilioAccountSid.trim() + "/Messages.json";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Authorization", authHeader)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formData, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("OTP SMS dispatched successfully via Twilio to {}", maskedPhone);
            } else {
                log.error("Twilio API returned HTTP status {} for destination {}", response.statusCode(), maskedPhone);
                throw new RuntimeException("Twilio gateway returned status: " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("Failed to send OTP SMS via Twilio to {}: {}", maskedPhone, e.getMessage());
            throw new RuntimeException("SMS dispatch failed via Twilio: " + e.getMessage(), e);
        }
    }

    private void sendViaGenericHttp(String phone, String otp, String maskedPhone) {
        try {
            String messageBody = "Your Power Fitness Kurnool admin password recovery OTP is " + otp + ". Valid for 5 minutes. Do not share.";
            HttpRequest.Builder builder = HttpRequest.newBuilder().timeout(Duration.ofSeconds(15));

            if (genericSmsKey != null && !genericSmsKey.trim().isEmpty()) {
                builder.header("Authorization", "Bearer " + genericSmsKey.trim());
                builder.header("X-API-KEY", genericSmsKey.trim());
            }

            if (genericSmsUrl.contains("{otp}") || genericSmsUrl.contains("{phone}")) {
                String targetUrl = genericSmsUrl.replace("{phone}", URLEncoder.encode(phone, StandardCharsets.UTF_8))
                                               .replace("{otp}", URLEncoder.encode(otp, StandardCharsets.UTF_8));
                builder.uri(URI.create(targetUrl)).GET();
            } else {
                String jsonPayload = String.format("{\"phone\":\"%s\",\"otp\":\"%s\",\"message\":\"%s\"}", phone, otp, messageBody);
                builder.uri(URI.create(genericSmsUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8));
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("OTP SMS dispatched successfully via Generic Gateway to {}", maskedPhone);
            } else {
                log.error("Generic SMS Gateway returned HTTP status {} for destination {}", response.statusCode(), maskedPhone);
                throw new RuntimeException("Generic SMS gateway returned status: " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("Failed to send OTP SMS via Generic Gateway to {}: {}", maskedPhone, e.getMessage());
            throw new RuntimeException("SMS dispatch failed via Generic Gateway: " + e.getMessage(), e);
        }
    }
}
