package com.mparticle.internal;

import android.support.annotation.NonNull;
import android.test.mock.MockContext;

import com.mparticle.mock.MockSharedPreferences;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(PowerMockRunner.class)
@PrepareForTest(GZIPOutputStream.class)
public class MParticleBaseClientImplTest {
    @Test
    public void makeUrlRequest() throws Exception {
        ConfigManager mockConfigManager = Mockito.mock(ConfigManager.class);
        Mockito.when(mockConfigManager.getApiKey()).thenReturn("foo");
        Mockito.when(mockConfigManager.getApiSecret()).thenReturn("bar");
        final boolean[] writeCalled = {false};
        final boolean[] getSocketFactoruCalled = {false};
        final MParticleBaseClientImpl client = new MParticleApiClientImpl(mockConfigManager, new MockSharedPreferences(), new MockContext()) {
            @Override
            protected SSLSocketFactory getSocketFactory() throws Exception {
                getSocketFactoruCalled[0] = true;
                assertFalse(writeCalled[0]);
                return Mockito.mock(SSLSocketFactory.class);
            }

            @Override
            boolean isDebug() {
                return false;
            }

            @Override
            boolean isPostGingerBread() {
                return true;
            }

            @Override
            protected GZIPOutputStream getOutputStream(HttpURLConnection connection) throws IOException {
                return new GZIPOutputStream(Mockito.mock(BufferedOutputStream.class)) {
                    @Override
                    public void close() throws IOException {
                        //do nothing
                    }

                    @Override
                    public void write(@NonNull byte[] b) throws IOException {
                        writeCalled[0] = true;
                        assertTrue(getSocketFactoruCalled[0]);

                    }
                };
            }
        };

        HttpURLConnection mockConnection = Mockito.mock(HttpsURLConnection.class);
        client.makeUrlRequest(mockConnection, "message" , true);
        assertTrue(getSocketFactoruCalled[0]);
        assertTrue(writeCalled[0]);
    }
}