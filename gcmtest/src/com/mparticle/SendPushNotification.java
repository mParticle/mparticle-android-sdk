package com.mparticle;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Sender;

public class SendPushNotification {

    public static void main(String[] args) {

        if (args.length < 2) {
            System.out.println("Usage: java SendPushNotification <gcm_api_key> <device1_id> [<device2_id> ...]");
            System.out.println("You can send to multiple devices");
            System.out.println("Make sure that the GCM API key is configured to accept messages from your IP address");
            return;
        }

        String gcm_api_key = args[0];
        ArrayList<String> devices = new ArrayList<String>();
        for (int i=1; i< args.length; i++) {
            devices.add(args[i]);
        }

        String title = "mParticle Message Title";
        String text = "Message sent at " + DateFormat.getDateTimeInstance().format(new Date());

        Message message = new Message.Builder()
                .addData("mp::notification::title", title)
                .addData("mp::notification::text", text)
                .build();

        try {
            Sender sender = new Sender(gcm_api_key);
            MulticastResult multicastResult = sender.send(message, devices, 5);

            System.out.println("Sent message to " + multicastResult.getSuccess() + " devices");
            System.out.println("Failed to send to " + multicastResult.getFailure() + " devices");

        } catch (Throwable t) {
            System.out.println("Error" + t.getMessage());
        }

    }

}
