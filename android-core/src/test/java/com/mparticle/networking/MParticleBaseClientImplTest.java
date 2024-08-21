package com.mparticle.networking;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import com.mparticle.internal.ConfigManager;
import com.mparticle.mock.MockSharedPreferences;

import org.junit.Test;
import org.mockito.Mockito;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.SSLSocketFactory;


public class MParticleBaseClientImplTest {

    @Test
    public void testPinningCertificates() throws Exception {
        ConfigManager mockConfigManager = Mockito.mock(ConfigManager.class);
        Mockito.when(mockConfigManager.getApiKey()).thenReturn("foo");
        Mockito.when(mockConfigManager.getApiSecret()).thenReturn("bar");
        NetworkOptions mockNetworkOptions = Mockito.mock(NetworkOptions.class);
        Mockito.when(mockNetworkOptions.isPinningDisabledInDevelopment()).thenReturn(false);
        Mockito.when(mockConfigManager.getNetworkOptions()).thenReturn(mockNetworkOptions);
        final boolean[] writeCalled = {false};
        final boolean[] getSocketFactoryCalled = {false};
        final BaseNetworkConnection client = new NetworkConnection(mockConfigManager, new MockSharedPreferences()) {
            @Override
            protected SSLSocketFactory getSocketFactory() throws Exception {
                getSocketFactoryCalled[0] = true;
                assertFalse(writeCalled[0]);
                return Mockito.mock(SSLSocketFactory.class);
            }

            @Override
            boolean isPostGingerBread() {
                return true;
            }

            @Override
            protected GZIPOutputStream getOutputStream(MPConnection connection) throws IOException {
                return new GZIPOutputStream(Mockito.mock(BufferedOutputStream.class)) {
                    @Override
                    public void close() throws IOException {
                        //do nothing
                    }

                    @Override
                    public void write(@NonNull byte[] b) throws IOException {
                        writeCalled[0] = true;
                        assertTrue(getSocketFactoryCalled[0]);

                    }
                };
            }
        };

        MPConnection mockConnection = Mockito.mock(MPConnection.class);
        Mockito.when(mockConnection.isHttps()).thenReturn(true);
        client.makeUrlRequest(null, mockConnection, "message", true);
        assertTrue(getSocketFactoryCalled[0]);
        assertTrue(writeCalled[0]);
    }
}