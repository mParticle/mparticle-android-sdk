package com.mparticle.internal;

import android.os.Message;

import com.mparticle.MParticle;
import com.mparticle.MockMParticle;
import com.mparticle.identity.AliasRequest;
import com.mparticle.internal.database.MPDatabase;
import com.mparticle.internal.database.services.MParticleDBManager;
import com.mparticle.internal.messages.MPAliasMessage;
import com.mparticle.mock.MockContext;
import com.mparticle.testutils.AndroidUtils;
import com.mparticle.testutils.TestingUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import static com.mparticle.internal.Constants.MessageKey.REQUEST_ID;
import static com.mparticle.testutils.TestingUtils.assertJsonEqual;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertNotNull;

@RunWith(PowerMockRunner.class)
public class MessageHandlerTest {
    ConfigManager mConfigManager;
    MessageHandler handler;
    MessageManager mMessageManager;
    MParticleDBManager mParticleDatabaseManager;

    @Before
    public void setUp() throws Exception {
        MParticle.setInstance(new MockMParticle());
        AppStateManager stateManager = Mockito.mock(AppStateManager.class);
        mConfigManager = MParticle.getInstance().Internal().getConfigManager();
        mMessageManager = Mockito.mock(MessageManager.class);
        Mockito.when(mMessageManager.getApiKey()).thenReturn("apiKey");
        mParticleDatabaseManager = Mockito.mock(MParticleDBManager.class);
        handler = new MessageHandler(mMessageManager, new MockContext(), mParticleDatabaseManager, "dataplan1", 1) {
            @Override
            boolean databaseAvailable() {
                return true;
            }
        };
    }


    @Test
    public void testInsertAliasRequest() throws JSONException {
        final AndroidUtils.Mutable<JSONObject> insertedAliasRequest = new AndroidUtils.Mutable<JSONObject>(null);
        Mockito.when(mConfigManager.getDeviceApplicationStamp()).thenReturn("das");

        MParticleDBManager database = new MParticleDBManager(new MockContext()) {
            @Override
            public void insertAliasRequest(String apiKey, JSONObject request) {
                insertedAliasRequest.value = request;
            }

            @Override
            public MPDatabase getDatabase() {
                return null;
            }
        };

        handler.mMParticleDBManager = database;

        assertNull(insertedAliasRequest.value);

        AliasRequest aliasRequest = TestingUtils.getInstance().getRandomAliasRequest();
        MPAliasMessage aliasMessage = new MPAliasMessage(aliasRequest, "das","apiKey");;

        Message mockMessage = Mockito.mock(Message.class);
        mockMessage.what = MessageHandler.STORE_ALIAS_MESSAGE;
        mockMessage.obj = aliasMessage;
        handler.handleMessageImpl(mockMessage);


        assertNotNull(insertedAliasRequest.value);

        aliasMessage.remove(REQUEST_ID);
        insertedAliasRequest.value.remove(REQUEST_ID);
        assertJsonEqual(aliasMessage, insertedAliasRequest.value);
    }
}
