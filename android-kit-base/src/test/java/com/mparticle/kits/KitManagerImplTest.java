package com.mparticle.kits;

import com.mparticle.MParticle;
import com.mparticle.mock.MockMParticle;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.ReportingManager;
import com.mparticle.internal.BackgroundTaskHandler;
import com.mparticle.mock.MockContext;
import com.mparticle.mock.MockKitConfiguration;

import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class KitManagerImplTest {
    BackgroundTaskHandler mockBackgroundTaskHandler = new BackgroundTaskHandler(){

        @Override
        public void executeNetworkRequest(Runnable runnable) {
            //do nothing
        }
    };

    @Test
    public void testOnUserAttributesReceived() throws Exception {
        MParticle.setInstance(new MockMParticle());
        KitManagerImpl manager  = new KitManagerImpl(
                new MockContext(),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
                mockBackgroundTaskHandler
                );
        KitIntegration integration = Mockito.mock(
                KitIntegration.class,
                Mockito.withSettings().extraInterfaces(KitIntegration.AttributeListener.class)
        );
        KitIntegration integration2 = Mockito.mock(
                KitIntegration.class,
                Mockito.withSettings().extraInterfaces(KitIntegration.AttributeListener.class)
        );
        Mockito.when(((KitIntegration.AttributeListener)integration).supportsAttributeLists()).thenReturn(true);
        Mockito.when(((KitIntegration.AttributeListener)integration2).supportsAttributeLists()).thenReturn(false);
        Mockito.when(integration.getConfiguration()).thenReturn(MockKitConfiguration.createKitConfiguration());
        Mockito.when(integration2.getConfiguration()).thenReturn(MockKitConfiguration.createKitConfiguration());
        manager.providers.put(5, integration);
        manager.providers.put(6, integration2);
        Map<String, String> userAttributeSingles = new HashMap<>();
        userAttributeSingles.put("test", "whatever");
        userAttributeSingles.put("test 2", "whatever 2");
        Map<String, List<String>> userAttributeLists = new HashMap<>();
        List<String> attributeList = new LinkedList<>();
        attributeList.add("1");
        attributeList.add("2");
        attributeList.add("3");
        userAttributeLists.put("test 3", attributeList);
        manager.onUserAttributesReceived(userAttributeSingles, userAttributeLists, 1L);
        Mockito.verify(((KitIntegration.AttributeListener)integration), Mockito.times(1)).setAllUserAttributes(userAttributeSingles, userAttributeLists);

        Map<String, String> userAttributesCombined = new HashMap<>();
        userAttributesCombined.put("test", "whatever");
        userAttributesCombined.put("test 2", "whatever 2");
        userAttributesCombined.put("test 3", "1,2,3");
        Map<String, List<String>> clearedOutList = new HashMap<>();

        Mockito.verify(((KitIntegration.AttributeListener)integration2), Mockito.times(1)).setAllUserAttributes(userAttributesCombined, clearedOutList);
    }

    @Test
    public void testSetUserAttributeList() throws Exception {
        KitManagerImpl manager  = new KitManagerImpl(
                new MockContext(),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
                mockBackgroundTaskHandler);
        KitIntegration integration = Mockito.mock(
                KitIntegration.class,
                Mockito.withSettings().extraInterfaces(KitIntegration.AttributeListener.class)
        );
        KitIntegration integration2 = Mockito.mock(
                KitIntegration.class,
                Mockito.withSettings().extraInterfaces(KitIntegration.AttributeListener.class)
        );
        Mockito.when(((KitIntegration.AttributeListener)integration).supportsAttributeLists()).thenReturn(true);
        Mockito.when(((KitIntegration.AttributeListener)integration2).supportsAttributeLists()).thenReturn(false);
        Mockito.when(integration.getConfiguration()).thenReturn(MockKitConfiguration.createKitConfiguration());
        Mockito.when(integration2.getConfiguration()).thenReturn(MockKitConfiguration.createKitConfiguration());
        manager.providers.put(5, integration);
        manager.providers.put(6, integration2);

        List<String> attributeList = new LinkedList<>();
        attributeList.add("1");
        attributeList.add("2");
        attributeList.add("3");
        manager.setUserAttributeList("test key", attributeList, 1);
        Mockito.verify(((KitIntegration.AttributeListener)integration), Mockito.times(1)).setUserAttributeList("test key", attributeList);
        Mockito.verify(((KitIntegration.AttributeListener)integration2), Mockito.times(1)).setUserAttribute("test key", "1,2,3");
    }
}