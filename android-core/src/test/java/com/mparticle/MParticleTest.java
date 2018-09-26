package com.mparticle;


import com.mparticle.internal.InternalSession;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;


public class MParticleTest {

    ExecutorService executorService = Executors.newFixedThreadPool(2);

    @Test
    public void testSetUserAttribute() throws Exception {
        MParticle mp = new MockMParticle();
        //mp.mConfigManager = Mockito.mock(ConfigManager.class);
//        mp.mAppStateManager = Mockito.mock(AppStateManager.class);
        //mp.mIdentity = new IdentityApi();
        InternalSession mockSession = Mockito.mock(InternalSession.class);
        Mockito.when(mockSession.checkEventLimit()).thenReturn(true);
        Mockito.when(mp.mAppStateManager.getSession()).thenReturn(mockSession);
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

        String legalString = new String(new char[256]);
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
        for (int i = 0; i < 1000; i++){
            longStringList.add("whatever");
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
        stringList.add(new String(new char[512]));
        assertTrue(mp.Identity().getCurrentUser().setUserAttribute("test", stringList));
        stringList.add(new String(new char[513]));
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
        Mockito.when(mockSession.checkEventLimit()).thenReturn(true);
        Mockito.when(mp.mAppStateManager.getSession()).thenReturn(mockSession);
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
        for (int i = 0; i < 1000; i++){
            longStringList.add("whatever");
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
        stringList.add(new String(new char[512]));
        assertTrue(mp.Identity().getCurrentUser().setUserAttributeList("test", stringList));
        stringList.add(new String(new char[513]));
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
        Mockito.when(mockSession.checkEventLimit()).thenReturn(true);
        Mockito.when(mp.mAppStateManager.getSession()).thenReturn(mockSession);
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

}