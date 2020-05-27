package com.mparticle;


import android.webkit.WebView;

import com.mparticle.identity.IdentityApi;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.identity.MParticleUser;
import com.mparticle.internal.Constants;
import com.mparticle.internal.InternalSession;
import com.mparticle.internal.MParticleJSInterface;
import com.mparticle.mock.MockContext;
import com.mparticle.testutils.AndroidUtils.Mutable;
import com.mparticle.testutils.RandomUtils;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;


public class MParticleTest {

    @Test
    public void testSetUserAttribute() throws Exception {
        MParticle mp = new MockMParticle();

        InternalSession mockSession = Mockito.mock(InternalSession.class);
        Mockito.when(mp.Internal().getAppStateManager().getSession()).thenReturn(mockSession);
        Mockito.when(mp.Internal().getConfigManager().isEnabled()).thenReturn(true);
        Mockito.when(mp.Internal().getConfigManager().getMpid()).thenReturn(1L);
        MParticle.setInstance(mp);
        MParticle.start(MParticleOptions.builder(new com.mparticle.mock.MockContext()).build());
        assertFalse(mp.Identity().getCurrentUser().setUserAttribute(null, "test"));
        assertFalse(mp.Identity().getCurrentUser().setUserAttribute(null, null));
        assertFalse(mp.Identity().getCurrentUser().setUserAttribute(new String(new char[257]), null));

        ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> longCaptor = ArgumentCaptor.forClass(Long.class);

        String legalString = new String(new char[Constants.LIMIT_ATTR_KEY]);
        assertTrue(mp.Identity().getCurrentUser().setUserAttribute(legalString, null));
        Mockito.verify(mp.mMessageManager, Mockito.times(1)).setUserAttribute(legalString, null, 1, false);

        List<Integer> integerList = new LinkedList<Integer>();
        integerList.add(203948);
        assertTrue(mp.Identity().getCurrentUser().setUserAttribute("test2", integerList));

        Mockito.verify(mp.mMessageManager, Mockito.times(2)).setUserAttribute(stringCaptor.capture(), listCaptor.capture(), longCaptor.capture(), eq(false));
        assertTrue(stringCaptor.getValue().equals("test2"));
        List capturedStringList = listCaptor.getValue();
        assertTrue(capturedStringList.size() == 1);
        assertTrue(capturedStringList.get(0).equals("203948"));
        assertTrue(longCaptor.getValue() == 1);
        List<String> longStringList = new ArrayList<String>();
        for (int i = 0; i < Constants.LIMIT_ATTR_VALUE; i++){
            longStringList.add("a");
        }
        assertTrue(mp.Identity().getCurrentUser().setUserAttribute("test3", longStringList));

        Mockito.verify(mp.mMessageManager, Mockito.times(3)).setUserAttribute(stringCaptor.capture(), listCaptor.capture(), longCaptor.capture(), eq(false));
        assertTrue(stringCaptor.getValue().equals("test3"));
        assertTrue(longCaptor.getValue() == 1);
        capturedStringList = listCaptor.getValue();
        assertTrue(capturedStringList.equals(longStringList));
        longStringList.add("too much!");
        assertFalse(mp.Identity().getCurrentUser().setUserAttribute("test", longStringList));

        List<String> stringList = new LinkedList<String>();
        stringList.add(new String(new char[Constants.LIMIT_ATTR_VALUE]));
        assertTrue(mp.Identity().getCurrentUser().setUserAttribute("test", stringList));
        stringList.add(new String(new char[Constants.LIMIT_ATTR_VALUE+1]));
        assertFalse(mp.Identity().getCurrentUser().setUserAttribute("test", stringList));


        assertTrue(mp.Identity().getCurrentUser().setUserAttribute("test", null));
        assertTrue(mp.Identity().getCurrentUser().setUserAttribute("test", new String(new char[4096])));
        assertFalse(mp.Identity().getCurrentUser().setUserAttribute("test", new String(new char[4097])));
        assertTrue(mp.Identity().getCurrentUser().setUserAttribute("test", 1212));
    }

    @Test
    public void testSetUserAttributeList() throws Exception {
        MParticle mp = new MockMParticle();
        InternalSession mockSession = Mockito.mock(InternalSession.class);
        Mockito.when(mp.mInternal.getAppStateManager().getSession()).thenReturn(mockSession);
        Mockito.when(mp.mInternal.getConfigManager().isEnabled()).thenReturn(true);
        Mockito.when(mp.mInternal.getConfigManager().getMpid()).thenReturn(2L);
        MParticle.setInstance(mp);
        MParticle.start(MParticleOptions.builder(new com.mparticle.mock.MockContext()).build());
        assertFalse(mp.Identity().getCurrentUser().setUserAttributeList(null, null));
        assertFalse(mp.Identity().getCurrentUser().setUserAttributeList(new String(new char[257]), null));

        ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> longCaptor = ArgumentCaptor.forClass(Long.class);

        String legalString = new String(new char[256]);
        assertFalse(mp.Identity().getCurrentUser().setUserAttributeList(legalString, null));

        List<Integer> integerList = new LinkedList<Integer>();
        integerList.add(203948);
        assertTrue(mp.Identity().getCurrentUser().setUserAttribute("test2", integerList));

        Mockito.verify(mp.mMessageManager, Mockito.times(1)).setUserAttribute(stringCaptor.capture(), listCaptor.capture(), longCaptor.capture(), eq(false));
        assertTrue(stringCaptor.getValue().equals("test2"));
        List capturedStringList = listCaptor.getValue();
        assertTrue(capturedStringList.size() == 1);
        assertTrue(capturedStringList.get(0).equals("203948"));
        assertTrue(longCaptor.getValue() == 2);
        List<String> longStringList = new ArrayList<String>();
        for (int i = 0; i < Constants.LIMIT_ATTR_VALUE; i++){
            longStringList.add("a");
        }
        assertTrue(mp.Identity().getCurrentUser().setUserAttributeList("test3", longStringList));

        Mockito.verify(mp.mMessageManager, Mockito.times(2)).setUserAttribute(stringCaptor.capture(), listCaptor.capture(), longCaptor.capture(), eq(false));
        assertTrue(stringCaptor.getValue().equals("test3"));
        capturedStringList = listCaptor.getValue();
        assertTrue(capturedStringList.equals(longStringList));
        longStringList.add("too much!");
        assertFalse(mp.Identity().getCurrentUser().setUserAttributeList("test", longStringList));
        assertTrue(longCaptor.getValue() == 2);


        List<String> stringList = new LinkedList<String>();
        stringList.add(new String(new char[Constants.LIMIT_ATTR_VALUE]));
        assertTrue(mp.Identity().getCurrentUser().setUserAttributeList("test", stringList));
        stringList.add(new String(new char[Constants.LIMIT_ATTR_VALUE+1]));
        assertFalse(mp.Identity().getCurrentUser().setUserAttributeList("test", stringList));
        assertFalse(mp.Identity().getCurrentUser().setUserAttributeList("test", null));
    }

    @Test
    public void testIncrementUserAttribute() throws Exception {
        MParticle.setInstance(new MockMParticle());
        MParticle.start(MParticleOptions.builder(new com.mparticle.mock.MockContext()).build());
        MParticle mp = MParticle.getInstance();
        Mockito.when(mp.Internal().getConfigManager().getMpid()).thenReturn(12L);
        assertFalse(mp.Identity().getCurrentUser().incrementUserAttribute(null, 3));

        assertTrue(mp.Identity().getCurrentUser().incrementUserAttribute("test", 3));
        Mockito.verify(mp.mMessageManager, Mockito.times(1)).incrementUserAttribute("test", 3, 12);
    }

    @Test
    public void testSetUserTag() throws Exception {
        MParticle mp = new MockMParticle();
        Mockito.when(mp.Internal().getConfigManager().getMpid()).thenReturn(1L);
        InternalSession mockSession = Mockito.mock(InternalSession.class);
        Mockito.when(mp.mInternal.getAppStateManager().getSession()).thenReturn(mockSession);
        Mockito.when(mp.Internal().getConfigManager().isEnabled()).thenReturn(true);
        MParticle.setInstance(mp);
        assertFalse(mp.Identity().getCurrentUser().setUserTag(null));
        assertFalse(mp.Identity().getCurrentUser().setUserTag(""));
        assertTrue(mp.Identity().getCurrentUser().setUserTag("blah"));
        Mockito.verify(mp.mMessageManager, Mockito.times(1)).setUserAttribute("blah", null, 1, false);
    }


    @Test
    public void testGetUserAttributes() throws Exception {
        MParticle.setInstance(new MockMParticle());
        MParticle.start(MParticleOptions.builder(new com.mparticle.mock.MockContext()).build());
        MParticle mp = MParticle.getInstance();
        Mockito.when(mp.Internal().getConfigManager().getMpid()).thenReturn(1L);
        mp.Identity().getCurrentUser().getUserAttributes();
        Mockito.verify(mp.mMessageManager, Mockito.times(1)).getUserAttributes(null, 1L);
    }

    @Test
    public void testGetUserAttributeLists() throws Exception {
        MParticle.setInstance(new MockMParticle());
        MParticle.start(MParticleOptions.builder(new com.mparticle.mock.MockContext()).build());
        MParticle mp = MParticle.getInstance();
        Mockito.when(mp.Internal().getConfigManager().getMpid()).thenReturn(1L);
        mp.Identity().getCurrentUser().getUserAttributes();
        Mockito.verify(mp.mMessageManager, Mockito.times(1)).getUserAttributes(null, 1L);
    }

    @Test
    public void testGetAllUserAttributes() throws Exception {
        MParticle.setInstance(new MockMParticle());
        MParticle.start(MParticleOptions.builder(new com.mparticle.mock.MockContext()).build());
        MParticle mp = MParticle.getInstance();
        Mockito.when(mp.Internal().getConfigManager().getMpid()).thenReturn(1L);

        mp.Identity().getCurrentUser().getUserAttributes();
        Mockito.verify(mp.mMessageManager, Mockito.times(1)).getUserAttributes(null, 1L);
    }

    @Test
    public void testAttributeListener() throws Exception {
        MParticle.setInstance(new MockMParticle());
    }

    @Test
    public void testSetGetImei() throws Exception {
        MParticle.setDeviceImei(null);
        Assert.assertNull(MParticle.getDeviceImei());
        MParticle.setDeviceImei("foo imei");
        Assert.assertEquals("foo imei", MParticle.getDeviceImei());
        MParticle.setDeviceImei(null);
        Assert.assertNull(MParticle.getDeviceImei());
    }

    @Test
    public void testAddWebView() {
        MParticle mp = new MockMParticle();
        MParticle.setInstance(mp);
        RandomUtils ran = new RandomUtils();
        String[] values = new String[]{"", "123", ran.getAlphaNumericString(5), ran.getAlphaNumericString(20), ran.getAlphaNumericString(100)};

        //test that we apply the token stored in the ConfigManager
        for (final String value: values) {
            Mockito.when(mp.Internal().getConfigManager().getWorkspaceToken()).thenReturn(value);
            final Mutable<Boolean> called = new Mutable<Boolean>(false);
            WebView webView = new WebView(new MockContext()) {
                @Override
                public void addJavascriptInterface(Object object, String name) {
                    assertEquals(MParticleJSInterface.INTERFACE_BASE_NAME + "_" + value + "_v2", name);
                    called.value = true;
                }
            };

            mp.registerWebView(webView);
            assertTrue(called.value);
        }

        //Test that we override the token stored in the ConfigManager, if the Client provides a token.
        for (final String value: values) {
            Mockito.when(mp.Internal().getConfigManager().getWorkspaceToken()).thenReturn(value);
            final Mutable<Boolean> called = new Mutable<Boolean>(false);
            WebView webView = new WebView(new MockContext()) {
                @Override
                public void addJavascriptInterface(Object object, String name) {
                    assertEquals(MParticleJSInterface.INTERFACE_BASE_NAME + "_" + "hardcode" + "_v2", name);
                    called.value = true;
                }
            };

            mp.registerWebView(webView, "hardcode");
            assertTrue(called.value);
        }
    }

    @Test
    public void testDeferPushRegistrationModifyRequest() {
        MParticle instance = new MockMParticle();
        instance.mIdentityApi = Mockito.mock(IdentityApi.class);
        Mockito.when(instance.Identity().getCurrentUser()).thenReturn(null);
        Mockito.when(instance.Identity().modify(Mockito.any(IdentityApiRequest.class))).thenThrow(new RuntimeException("Unexpected Modify Request"));
        MParticle.setInstance(instance);
        MParticle.getInstance().logPushRegistration("instanceId", "senderId");


        Mockito.when(instance.Identity().getCurrentUser()).thenReturn(Mockito.mock(MParticleUser.class));
        Exception ex = null;
        try {
            MParticle.getInstance().logPushRegistration("instanceId", "senderId");
        } catch (Exception e) {
            ex = e;
        }
        assertEquals("Unexpected Modify Request", ex.getMessage());
    }

    @Test
    public void testLogBaseEvent() {
        MParticle instance = new MockMParticle();

        Mockito.when(instance.mConfigManager.isEnabled()).thenReturn(true);
        instance.logEvent(Mockito.mock(BaseEvent.class));

        Mockito.verify(instance.mKitManager, Mockito.times(1)).logEvent(Mockito.any(BaseEvent.class));

        instance = new MockMParticle();
        Mockito.when(instance.mConfigManager.isEnabled()).thenReturn(false);
        instance.logEvent(Mockito.mock(BaseEvent.class));
        instance.logEvent(Mockito.mock(MPEvent.class));

        Mockito.verify(instance.mKitManager, Mockito.times(0)).logEvent(Mockito.any(BaseEvent.class));
    }

    @Test
    public void testIdentityTypeParsing() {
        for(MParticle.IdentityType identityType: MParticle.IdentityType.values()) {
            assertEquals(identityType, MParticle.IdentityType.parseInt(identityType.getValue()));
        }
    }
}