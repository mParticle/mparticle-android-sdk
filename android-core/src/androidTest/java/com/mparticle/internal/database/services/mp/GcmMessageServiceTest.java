//package com.mparticle.internal.database.services.mp;
//
//import android.os.Bundle;
//import android.os.Parcel;
//
//import com.mparticle.internal.AppStateManager;
//import com.mparticle.internal.dto.GcmMessageDTO;
//import com.mparticle.messaging.AbstractCloudMessage;
//import com.mparticle.messaging.CloudAction;
//import com.mparticle.messaging.MPCloudNotificationMessage;
//
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
//import java.util.Random;
//
//import static junit.framework.Assert.assertEquals;
//import static junit.framework.Assert.assertNotNull;
//import static junit.framework.Assert.assertTrue;
//
//
//public class GcmMessageServiceTest extends BaseMPServiceTest {
//    private final static String CAMPAIGN_ID = "m_cid";
//    private final static String CONTENT_ID = "m_cntid";
//    private final static String EXPIRATION = "m_expy";
//
//    @Before
//    public void setUp() {
//        clearDatabase();
//    }
//
//    @Test
//    public void testNullValues() throws AbstractCloudMessage.InvalidGcmMessageException {
//        GcmMessageService.insertGcmMessage(database, getRandomCloudMessage(), AppStateManager.APP_STATE_FOREGROUND, 2L);
//        assertEquals(GcmMessageService.getGcmHistory(database, 2L).size(), 1);
//    }
//
//    @Test
//    public void testMpIdSpecificInsert() throws AbstractCloudMessage.InvalidGcmMessageException {
//        clearDatabase();
//        for (int i = 0; i < 10; i++) {
//            GcmMessageService.insertGcmMessage(database, getRandomCloudMessage(), AppStateManager.APP_STATE_FOREGROUND, 2L);
//        }
//        assertEquals(GcmMessageService.getGcmHistory(database, 2L).size(), 10);
//        assertEquals(GcmMessageService.getGcmHistory(database, 3L).size(), 0);
//
//        for (int i = 0; i < 10; i++) {
//            GcmMessageService.insertGcmMessage(database, getRandomCloudMessage(), AppStateManager.APP_STATE_FOREGROUND, 3L);
//        }
//        assertEquals(GcmMessageService.getGcmHistory(database, 2L).size(), 10);
//        assertEquals(GcmMessageService.getGcmHistory(database, 3L).size(), 10);
//
//    }
//
//    @Test
//    public void testMpIdSpecificClearProvider() throws AbstractCloudMessage.InvalidGcmMessageException {
//        clearDatabase();
//        GcmMessageService.insertGcmMessage(database, getProviderCloudMessage(), AppStateManager.APP_STATE_FOREGROUND, 3L);
//        assertEquals(GcmMessageService.getGcmHistory(database, 3L).size(), 1);
//        GcmMessageService.clearOldProviderGcm(database, 3L);
//        assertEquals(GcmMessageService.getGcmHistory(database, 3L).size(), 0);
//    }
//
//    private MPCloudNotificationMessage getProviderCloudMessage() throws AbstractCloudMessage.InvalidGcmMessageException {
//        return getCloudMessage(-1);
//    }
//
//    private MPCloudNotificationMessage getRandomCloudMessage() throws AbstractCloudMessage.InvalidGcmMessageException {
//        return getCloudMessage(new Random().nextInt());
//    }
//
//    private MPCloudNotificationMessage getCloudMessage(int contentId) throws AbstractCloudMessage.InvalidGcmMessageException {
//        Bundle bundle = new Bundle();
//        bundle.putString(CONTENT_ID, String.valueOf(contentId));
//        bundle.putString(CAMPAIGN_ID, String.valueOf(432));
//        bundle.putString(EXPIRATION, System.currentTimeMillis() + (1000 * 100) + "");
//        return new MPCloudNotificationMessage(bundle);
//    }
//}

//
//if (message instanceof MPCloudNotificationMessage) {
//        contentValues.put(GcmMessageTableColumns.CONTENT_ID, ((MPCloudNotificationMessage) message).getContentId());
//        contentValues.put(GcmMessageTableColumns.CAMPAIGN_ID, ((MPCloudNotificationMessage) message).getCampaignId());
//        contentValues.put(GcmMessageTableColumns.EXPIRATION, ((MPCloudNotificationMessage) message).getExpiration());
//        contentValues.put(GcmMessageTableColumns.DISPLAYED_AT, message.getActualDeliveryTime());
//        } else {
//        contentValues.put(GcmMessageTableColumns.CONTENT_ID, GcmMessageTableColumns.PROVIDER_CONTENT_ID);
//        contentValues.put(GcmMessageTableColumns.CAMPAIGN_ID, 0);
//        contentValues.put(GcmMessageTableColumns.EXPIRATION, System.currentTimeMillis() + (24 * 60 * 60 * 1000));
//        contentValues.put(GcmMessageTableColumns.DISPLAYED_AT, System.currentTimeMillis());
//        }
//        contentValues.put(GcmMessageTableColumns.PAYLOAD, message.getRedactedJsonPayload().toString());
//        contentValues.put(GcmMessageTableColumns.BEHAVIOR, 0);
//        contentValues.put(GcmMessageTableColumns.CREATED_AT, System.currentTimeMillis());
//        contentValues.put(GcmMessageTableColumns.MP_ID, String.valueOf(mpId));
//        contentValues.put(GcmMessageTableColumns.APPSTATE, appState);