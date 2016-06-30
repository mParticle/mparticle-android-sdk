package com.mparticle.kits;

import com.mparticle.MParticle;

import org.junit.BeforeClass;
import org.mockito.Mockito;

public class KitIntegrationTest {

    @BeforeClass
    public static void setupAll() {
        MParticle mockMp = Mockito.mock(MParticle.class);
        Mockito.when(mockMp.getEnvironment()).thenReturn(MParticle.Environment.Development);
        MParticle.setInstance(mockMp);
    }

}