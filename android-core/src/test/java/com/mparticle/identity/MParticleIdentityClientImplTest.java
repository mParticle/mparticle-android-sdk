package com.mparticle.identity;

import android.content.Context;

import com.mparticle.MParticle;
import com.mparticle.internal.ConfigManager;

import junit.framework.Assert;

import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;


public class MParticleIdentityClientImplTest {

    @Test
    public void testConvertIdentityTypeToString() throws Exception {
        for (MParticle.IdentityType type : MParticle.IdentityType.values()) {
            Assert.assertEquals(type,
                    MParticleIdentityClientImpl.getIdentityType(
                            MParticleIdentityClientImpl.getStringValue(type)));
        }
    }

    @Test
    public void testOperatingSystemToString() {
        //make sure that all the cases are covered, default is not getting returned
        //if this test fails, it might be becuase you added a new OperatingSystem enum, but forgot
        //to update this method

        Set<String> osStringValues = new HashSet<String>();
        for (MParticle.OperatingSystem operatingSystem: MParticle.OperatingSystem.values()) {
            String osString = new MParticleIdentityClientImpl(Mockito.mock(Context.class), Mockito.mock(ConfigManager.class), operatingSystem).getOperatingSystemString();
            assertFalse(osStringValues.contains(osString));
            osStringValues.add(osString);
        }
    }
}