package com.mparticle.internal.database.services.mp;


import com.mparticle.internal.Constants;
import com.mparticle.internal.JsonReportingMessage;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class ReportingServiceTest extends BaseMPServiceTest {


    @Test
    public void testInsertReportingMessage() throws JSONException {
         List<JsonReportingMessage> reportingMessages = getNReportingMessages(20, "123");
         for (JsonReportingMessage reportingMessage: reportingMessages) {
             ReportingService.insertReportingMessage(database, reportingMessage, 2L);
         }
         assertEquals(ReportingService.getReportingMessagesForUpload(database, true, 2L).size(), 20);
         assertEquals(ReportingService.getReportingMessagesForUpload(database).size(), 20);
     }

     @Test
     public void testInsertReportingMessageMpIdSpecific() throws JSONException {
         for (JsonReportingMessage reportingMessage: getNReportingMessages(20, "123")) {
             ReportingService.insertReportingMessage(database, reportingMessage, 2L);
         }
         assertEquals(ReportingService.getReportingMessagesForUpload(database, true, 2L).size(), 20);
         assertEquals(ReportingService.getReportingMessagesForUpload(database, true, 3L).size(), 0);
         assertEquals(ReportingService.getReportingMessagesForUpload(database, true, 4L).size(), 0);
         assertEquals(ReportingService.getReportingMessagesForUpload(database).size(), 20);
         for (JsonReportingMessage reportingMessage: getNReportingMessages(30, "123")) {
             ReportingService.insertReportingMessage(database, reportingMessage, 3L);
         }
         assertEquals(ReportingService.getReportingMessagesForUpload(database, true, 2L).size(), 20);
         assertEquals(ReportingService.getReportingMessagesForUpload(database, true, 3L).size(), 30);
         assertEquals(ReportingService.getReportingMessagesForUpload(database, false, 4L).size(), 50);
         assertEquals(ReportingService.getReportingMessagesForUpload(database, true, 4L).size(), 0);
         assertEquals(ReportingService.getReportingMessagesForUpload(database).size(), 50);
         for (JsonReportingMessage reportingMessage: getNReportingMessages(30, "123")) {
             ReportingService.insertReportingMessage(database, reportingMessage, Constants.TEMPORARY_MPID);
         }
         assertEquals(ReportingService.getReportingMessagesForUpload(database, true, 2L).size(), 20);
         assertEquals(ReportingService.getReportingMessagesForUpload(database, true, 3L).size(), 30);
         assertEquals(ReportingService.getReportingMessagesForUpload(database, true, Constants.TEMPORARY_MPID).size(), 30);
         assertEquals(ReportingService.getReportingMessagesForUpload(database, false, 4L).size(), 80);
         assertEquals(ReportingService.getReportingMessagesForUpload(database).size(), 50);
     }

     @Test
     public void testDeleteReportingMessages() throws JSONException {
         for (JsonReportingMessage reportingMessage: getNReportingMessages(20)) {
             ReportingService.insertReportingMessage(database, reportingMessage, 2L);
         }
         for (JsonReportingMessage reportingMessage: getNReportingMessages(10)) {
             ReportingService.insertReportingMessage(database, reportingMessage, 3L);
         }
         List<ReportingService.ReportingMessage> messagesFor2 = ReportingService.getReportingMessagesForUpload(database, true, 2L);
         List<ReportingService.ReportingMessage> messagesFor3 = ReportingService.getReportingMessagesForUpload(database, true, 3L);
         assertEquals(messagesFor2.size(), 20);
         assertEquals(messagesFor3.size(), 10);

         for (int i = 0; i < 5; i++) {
             ReportingService.deleteReportingMessage(database, messagesFor2.get(i).getReportingMessageId());
         }

         messagesFor2 = ReportingService.getReportingMessagesForUpload(database, true, 2L);
         messagesFor3 = ReportingService.getReportingMessagesForUpload(database, true, 3L);
         assertEquals(messagesFor2.size(), 15);
         assertEquals(messagesFor3.size(), 10);

         for (int i = 0; i < 10; i++) {
             ReportingService.deleteReportingMessage(database, messagesFor3.get(i).getReportingMessageId());
         }

         messagesFor2 = ReportingService.getReportingMessagesForUpload(database, true, 2L);
         messagesFor3 = ReportingService.getReportingMessagesForUpload(database, true, 3L);
         assertEquals(messagesFor2.size(), 15);
         assertEquals(messagesFor3.size(), 0);
     }


     @Test
     public void testEntryIntegrity() throws JSONException {
         List<JsonReportingMessage> jsonReportingMessages = getNReportingMessages(10);
         for (JsonReportingMessage reportingMessage: jsonReportingMessages) {
             ReportingService.insertReportingMessage(database, reportingMessage, 1L);
         }
         List<ReportingService.ReportingMessage> reportingMessages = ReportingService.getReportingMessagesForUpload(database, true, 1L);
         Collections.sort(reportingMessages, new Comparator<ReportingService.ReportingMessage>() {
             @Override
             public int compare(ReportingService.ReportingMessage o1, ReportingService.ReportingMessage o2) {
                 try {
                     return ((Integer)o1.getMsgObject().getInt("a random Number")).compareTo(o2.getMsgObject().getInt("a random Number")) ;
                 } catch (JSONException e) {
                     e.printStackTrace();
                 }
                 return -1;
             }
         });

         Collections.sort(jsonReportingMessages, new Comparator<JsonReportingMessage>() {
             @Override
             public int compare(JsonReportingMessage o1, JsonReportingMessage o2) {
                 try {
                     return ((Integer)o1.toJson().getInt("a random Number")).compareTo(o2.toJson().getInt("a random Number"));
                 } catch (JSONException e) {
                     e.printStackTrace();
                 }
                 return -1;
             }
         });

         assertEquals(jsonReportingMessages.size(), reportingMessages.size());

         for (int i = 0; i < jsonReportingMessages.size() && i < reportingMessages.size(); i++) {
             assertTrue(equals(jsonReportingMessages.get(i), reportingMessages.get(i)));
         }
     }



     private List<JsonReportingMessage> getNReportingMessages(int n) {
         return getNReportingMessages(n, null);
     }

     private List<JsonReportingMessage> getNReportingMessages(int n, String sessionId) {
         List<JsonReportingMessage> reportingMessages = new ArrayList<JsonReportingMessage>();
         for (int i = 0; i < n; i++) {
             reportingMessages.add(getRandomReportingMessage(sessionId != null ? sessionId : String.valueOf(new Random().nextInt())));
         }
         return reportingMessages;
     }

     private JsonReportingMessage getRandomReportingMessage(final String sessionId) {
         return new JsonReportingMessage() {
             int randomNumber;

             @Override
             public void setDevMode(boolean development) {
                 //do nothing
             }

             @Override
             public long getTimestamp() {
                 return System.currentTimeMillis() - 100;
             }

             @Override
             public int getModuleId() {
                 return 1;//MParticle.ServiceProviders.APPBOY;
             }

             @Override
             public JSONObject toJson() {
                 JSONObject jsonObject = new JSONObject();
                 try {
                     jsonObject.put("fieldOne", "a value");
                     jsonObject.put("fieldTwo", "another value");
                     jsonObject.put("a random Number", randomNumber == -1 ? randomNumber = new Random().nextInt() : randomNumber);
                 }
                 catch (JSONException ignore) {}
                 return jsonObject;
             }

             @Override
             public String getSessionId() {
                 return sessionId;
             }

             @Override
             public void setSessionId(String sessionId) {

             }
         };
     }

     private boolean equals(JsonReportingMessage jsonReportingMessage, ReportingService.ReportingMessage reportingMessage) {
         String reportingString = reportingMessage.getMsgObject().toString();
         String origStirng = jsonReportingMessage.toJson().toString();

         return reportingMessage.getSessionId().equals(jsonReportingMessage.getSessionId()) &&
                 reportingString.equals(origStirng);
     }
}
