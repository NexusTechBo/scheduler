package com.example.scheduler.service.impl;

import com.example.scheduler.service.TwilioWhatsappService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TwilioWhatsappServiceImpl implements TwilioWhatsappService {
    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.whatsapp-number}")
    private String fromWhatsAppNumber;

    public void init() {
        Twilio.init(accountSid, authToken);
    }

    @Override
    public String sendWhatsAppMessage(String toPhoneNumber, String messageBody) {
        var to = new PhoneNumber("whatsapp:" + toPhoneNumber);
        var from = new PhoneNumber(fromWhatsAppNumber);
        var message = Message.creator(to, from, messageBody).create();
        return message.getSid();
    }
}
