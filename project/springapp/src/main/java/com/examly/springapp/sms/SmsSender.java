package com.examly.springapp.sms;

/** Sends one text message. Implementations: ConsoleSmsSender (development) and TwilioSmsSender. */
public interface SmsSender {
    /** @param toE164 phone number with country code, for example +919876543210 */
    void send(String toE164, String message);
}
