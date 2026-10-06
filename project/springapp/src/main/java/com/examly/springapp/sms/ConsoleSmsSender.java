package com.examly.springapp.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Development sender: no SMS leaves the machine, the message is printed in the backend console instead.
 * Active when otp.provider is "dev" (the default).
 */
@Component
@ConditionalOnProperty(name = "otp.provider", havingValue = "dev", matchIfMissing = true)
public class ConsoleSmsSender implements SmsSender {
    private static final Logger log = LoggerFactory.getLogger(ConsoleSmsSender.class);

    @Override
    public void send(String toE164, String message) {
        String masked = toE164.length() > 4 ? "******" + toE164.substring(toE164.length() - 4) : "****";
        log.info("DEV SMS (not really sent) to {}: {}", masked, message);
    }
}
