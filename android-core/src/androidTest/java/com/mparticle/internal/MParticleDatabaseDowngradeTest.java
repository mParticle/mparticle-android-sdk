package com.mparticle.internal;

import android.content.Context;

import com.mparticle.*;
import com.mparticle.AccessUtils;

import org.junit.Test;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertTrue;

public class MParticleDatabaseDowngradeTest extends BaseCleanInstallEachTest {
    private final int originalDBVersion = MParticleDatabase.DB_VERSION;

    @Override
    protected void beforeClass() throws Exception {

    }

    @Override
    public void before() throws Exception {
        MParticleDatabase.DB_VERSION = originalDBVersion + 1;
        MParticle.start(mContext, "apikey", "secret");
        AccessUtils.getMessageManager().mUploadHandler.setApiClient(new MParticleApiClientImpl(MParticle.getInstance().getConfigManager(), mContext.getSharedPreferences("", Context.MODE_PRIVATE), mContext) {
            @Override
            public void fetchConfig() throws IOException, MPConfigException {
                //
            }

            @Override
            public int sendMessageBatch(String message) throws IOException, MPThrottleException, MPRampException {
                return 429;
            }
        });
    }

    @Test
    public void testDowngrade() throws Exception {
        MParticle.getInstance().setUserAttribute("key", "value");
        MParticle.getInstance().leaveBreadcrumb("I've been here");

        writeRandomDataToDatabase();
        assertDatabaseWrittenTo();
        assertTrue(MParticle.getInstance().getConfigManager().mPreferences.getAll().size() > 0);

        //now downgrade
        MParticle.setInstance(null);
        MParticleDatabase.DB_VERSION = originalDBVersion;
        MParticle.start(mContext, "key", "value");
        com.mparticle.AccessUtils.getMessageManager().mUploadHandler.setApiClient(new MParticleApiClientImpl(MParticle.getInstance().getConfigManager(), mContext.getSharedPreferences("", Context.MODE_PRIVATE), mContext) {
            @Override
            public void fetchConfig() throws IOException, MPConfigException {
                //
            }

            @Override
            public int sendMessageBatch(String message) throws IOException, MPThrottleException, MPRampException {
                return 429;
            }
        });

        //make sure the database is empty
        assertEquals(Utils.getTableEntries(MParticleDatabase.BreadcrumbTable.TABLE_NAME).size(), 0);
        assertEquals(Utils.getTableEntries(MParticleDatabase.SessionTable.TABLE_NAME).size(), 0);
        assertEquals(Utils.getTableEntries(MParticleDatabase.MessageTable.TABLE_NAME).size(), 0);
        assertEquals(Utils.getTableEntries(MParticleDatabase.UploadTable.TABLE_NAME).size(), 0);
        assertEquals(Utils.getTableEntries(MParticleDatabase.UserAttributesTable.TABLE_NAME).size(), 0);
        assertEquals(MParticle.getInstance().getConfigManager().mPreferences.getAll().size(), 0);

        //make sure new database can be written to
        writeRandomDataToDatabase();
        assertDatabaseWrittenTo();


    }

    private void writeRandomDataToDatabase() throws Exception {
        MessageManager messageManager = AccessUtils.getMessageManager();
        MParticle.getInstance().setUserAttribute("key", "value");
        MParticle.getInstance().leaveBreadcrumb("I've been here");

        for (int i = 0; i < 5; i++) {
            for (int j =0; j < Utils.getInstance().randomInt(1,5); j++) {
                MParticle.getInstance().logEvent(Utils.getInstance().getRandomMPEventRich());
            }
            Utils.awaitStoreMessage();
            //hackto instantiate database in UploadHandler.. see LegacyUploadHandler
            messageManager.mUploadHandler.handleMessage(messageManager.mUploadHandler.obtainMessage(-1, -1000, -1000));
            messageManager.mUploadHandler.prepareMessageUploads(false);
        }

        Utils.awaitStoreMessage();
    }

    private void assertDatabaseWrittenTo() throws Exception {
        //assert that there is data is out database (not going to go crazy and do ALL the tables, this should suffice)
        assertNotSame(Utils.getTableEntries(MParticleDatabase.BreadcrumbTable.TABLE_NAME).size(), 0);
        assertNotSame(Utils.getTableEntries(MParticleDatabase.SessionTable.TABLE_NAME).size(), 0);
        assertNotSame(Utils.getTableEntries(MParticleDatabase.MessageTable.TABLE_NAME).size(), 0);
        assertNotSame(Utils.getTableEntries(MParticleDatabase.UploadTable.TABLE_NAME).size(), 0);
        assertNotSame(Utils.getTableEntries(MParticleDatabase.UserAttributesTable.TABLE_NAME).size(), 0);
    }
}
