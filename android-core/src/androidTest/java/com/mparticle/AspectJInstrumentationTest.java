package com.mparticle;

import android.content.Context;
import android.support.annotation.NonNull;

import com.mparticle.commerce.Cart;
import com.mparticle.commerce.CommerceApi;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.consent.ConsentState;
import com.mparticle.identity.IdentityApi;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.identity.MParticleUserImpl;
import com.mparticle.internal.listeners.ApiClass;
import com.mparticle.internal.listeners.InternalListenerManager;
import com.mparticle.testutils.AndroidUtils.Mutable;
import com.mparticle.testutils.BaseCleanStartedEachTest;

import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dalvik.system.DexFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * This tests the effectiveness of the AspectJ {@link com.mparticle.internal.listeners.ListenerHook}.
 *
 * the {@link AspectJInstrumentationTest#NO_INSTRUMENTATION} is a white list, which contains methods
 * that SHOULD NOT be instrumented with an onApiCalled() hook.
 *
 * If you have added a method to a class annotated with {@link ApiClass}, that SHOULD NOT be instrumented
 * and this test fails, you may need to add the method to the whitelist.
 *
 * If you have added a method to a class annotated with {@link ApiClass}, that SHOULD be instrumented
 * and this test fails, you may have to add a manual inclusion in {@link com.mparticle.internal.listeners.ListenerHook}
 * in accordance with the example. You can also manually call {@link InternalListenerManager#onApiCalled(Object...)} from
 * within the method.
 *
 */
public class AspectJInstrumentationTest extends BaseCleanStartedEachTest {

    private static final String[] NO_INSTRUMENTATION = new String[]{
            "Cart.getProduct",
            "Cart.products",
            "Cart.toString",
            "IdentityApi.Internal",
            "IdentityApi.getCurrentUser",
            "IdentityApi.getDeviceApplicationStamp",
            "IdentityApi.getUser",
            "IdentityApi.getUsers",
            "MParticle.Commerce",
            "MParticle.Identity",
            "MParticle.Internal",
            "MParticle.Media",
            "MParticle.Messaging",
            "MParticle.getAttributionListener",
            "MParticle.getAttributionResults",
            "MParticle.getCurrentSession",
            "MParticle.getDeviceImei",
            "MParticle.getEnvironment",
            "MParticle.getInstallReferrer",
            "MParticle.getInstance",
            "MParticle.getIntegrationAttributes",
            "MParticle.getKitInstance",
            "MParticle.getOptOut",
            "MParticle.getSessionTimeout",
            "MParticle.getSurveyUrl",
            "MParticle.getUserSegments",
            "MParticle.isAndroidIdDisabled",
            "MParticle.isAutoTrackingEnabled",
            "MParticle.isDevicePerformanceMetricsDisabled",
            "MParticle.isLocationTrackingEnabled",
            "MParticleUserImpl.getCart",
            "MParticleUserImpl.getConsentState",
            "MParticleUserImpl.getFirstSeenTime",
            "MParticleUserImpl.getId",
            "MParticleUserImpl.getLastSeenTime",
            "MParticleUserImpl.getSegments",
            "MParticleUserImpl.getUserAttributes",
            "MParticleUserImpl.getUserAttributes",
            "MParticleUserImpl.getUserIdentities",
            "MParticleUserImpl.isLoggedIn"
    };

    final Mutable<String> apiNameResult = new Mutable<String>(null);

    @OrchestratorOnly
    @Test
    public void testApiInstrumentation() throws IllegalAccessException, InterruptedException, InvocationTargetException {
        SdkListener sdkListener = new SdkListener() {
            @Override
            public void onApiCalled(@NonNull String apiName, @NonNull List<Object> objects, boolean isExternal) {
                if (apiName.contains("$")) {
                    fail("inner class method, " + apiName + " should not be instrumented");
                }
                if (apiNameResult.value == null && isExternal) {
                    apiNameResult.value = apiName;
                }
            }
        };

        MParticle.addListener(mContext, sdkListener);


        Set<Class> mParticleApiClasses = getAllPublicClasses();
        Set<Class> annotatedClasses = new HashSet<Class>();
        for (Class clazz: mParticleApiClasses) {
            if (clazz.getAnnotation(ApiClass.class) != null) {
                annotatedClasses.add(clazz);
            }
        }
        for (Class annotatedClass: annotatedClasses) {
            Object instance = getInstance(annotatedClass);
            areAllMethodInstrumentedTest(instance, annotatedClass);
        }
    }

    Set<String> seen = new HashSet<String>();
    private void areAllMethodInstrumentedTest(Object instance, Class clazz) throws IllegalAccessException, InterruptedException, InvocationTargetException {
        if (!clazz.isInstance(instance)) {
            fail(instance.getClass().getName() + " is not instance of " + clazz.getName());
        }
        boolean apiClassAnnotation = clazz.isAnnotationPresent(ApiClass.class);

        for (Method method : clazz.getMethods()) {
            String methodName = method.toString();
            if (Modifier.isPublic(method.getModifiers()) &&
                    !seen.contains(methodName)) {
                seen.add(methodName);

                //if method was inhereted from outside of the SDK, ignore it
                if (!method.getDeclaringClass().getName().startsWith("com.mparticle")) {
                    continue;
                }

                if (apiClassAnnotation) {
                    isMethodInstrumentedTest(instance, method, clazz);
                }
            }
        }
    }

    //Invokes the method in question with either mocked or barebones instances and tests whether
    //the callback to {@link SdkListener#onApiCalled()} was invoked
    private void isMethodInstrumentedTest(Object instance, Method method, Class clazz) {
        Class[] parameters = method.getParameterTypes();
        Object[] arguments = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Class parameter = parameters[i];
            //check if we have added a mock instance for the parameter type. using Mockito for mocks
            //was really really slow in some cases, so we just maintain a map of common mock objects and
            //primitives, basically cases where we can't get away with passing 'null' as the parameter
            if (getMocks().containsKey(parameter)) {
                arguments[i] = getMocks().get(parameter);
            } else if (parameter.isEnum()) {
                String[] fields = parameter.getFields()[parameter.getFields().length - 1].getName().split("\\.");
                String field = fields[fields.length - 1];
                Enum e = Enum.valueOf(parameter, field);
                arguments[i] = e;
            } else {
                arguments[i] = null;
            }
        }
        apiNameResult.value = null;
        try {
            method.invoke(instance, arguments);
        } catch (Exception ignore) {
            //ignore, probably will crash, don't care
        }
        if (isInstrumented(clazz, method)) {
            assertNotNull("Instrumentation not found for " + getApiName(clazz, method) + " (" + arguments.length + ") params",
                    apiNameResult.value);
            assertEquals(getApiName(clazz, method), apiNameResult.value);
        } else {
            if (apiNameResult.value != null) {
                assertNotEquals("Instrumentation should not have been found for  " + getApiName(clazz, method),
                        getApiName(clazz, method), apiNameResult.value);
            }
        }
    }

    Set<String> noInstrumentationSet;

    /**
     * Whether or not the method is "whitelisted" aka should be exempt from instrumentation
     */
    private boolean isInstrumented(Class clazz, Method method) {
        if (noInstrumentationSet == null) {
            noInstrumentationSet = new HashSet<String>(Arrays.asList(NO_INSTRUMENTATION));
        }
        return !noInstrumentationSet.contains(clazz.getSimpleName() + "." + method.getName());
    }

    private Set<Class> getAllPublicClasses() {
        Set<String> classes = new HashSet<String>();
        try {
            DexFile df = new DexFile(mContext.getPackageCodePath());
            for (Enumeration<String> iter = df.entries(); iter.hasMoreElements();) {
                String string = iter.nextElement();
                if (string.startsWith("com.mparticle")) {
                    classes.add(string);
                }
            }
        } catch (IOException ex) { }
        Set<Class> publicClasses = new HashSet<Class>();
        for (String classString: classes) {
            String[] packages = classString.split("\\.");
            if (packages.length == 0) {
                continue;
            }

            Class clazz;
            if (isPublic(clazz = getClass(classString))) {
                publicClasses.add(clazz);
            }
        }
        return publicClasses;
    }

    /**
     * helper method to generate simple instance of classes. Nothing really "has to work" internally
     * for the instances, since the call we are testing for will always be the first command executed.
     * After that, if it crashes we will just catch the Exception and move on
     */
    private Object getInstance(Class clazz) {
        if (clazz == MParticle.class) {
            return new MockMParticle();
        }
        if (clazz == IdentityApi.class) {
            return new MockIdentityApi();
        }
        if (clazz == MParticleUserImpl.class) {
            return new MockMParticleUser();
        }
        if (clazz == CommerceApi.class) {
            return new MockCommerceApi(mContext);
        }
        if (clazz == Cart.class) {
            return new Cart(mContext, 1L);
        }
        fail("Class, \"" + clazz.getName() + "\" is not able to be instantiated. Please provide" +
                "method to instantiate instance here, or remove @ApiClass annotation");
        throw new RuntimeException();
    }


    private Class getClass(String classString) {
        try {
            return Class.forName(classString);
        } catch (ClassNotFoundException ex) { }
        catch (NoClassDefFoundError ex) { }
        return null;
    }

    private boolean isPublic(Class clazz) {
        if (clazz != null) {
            return Modifier.isPublic(clazz.getModifiers());
        }
        return false;
    }

    //we have to make mocks lazily loaded since "Product" requires the SDK to be started to build;
    private static Map<Class<?>, Object> mocks;
    private Map<Class<?>, Object> getMocks() {
        if (mocks == null) {
            mocks = new HashMap<Class<?>, Object>() {{
                put(boolean.class, new Boolean(false));
                put(Boolean.class, new Boolean(false));
                put(char.class, new Character('a'));
                put(double.class, new Double(1.0d));
                put(float.class, new Float(1.0f));
                put(int.class, new Integer(1));
                put(long.class, new Long(1L));
                put(Long.class, new Long(1L));
                put(String.class, "");
                put(ConsentState.class, ConsentState.builder().build());
                put(IdentityApiRequest.class, IdentityApiRequest.withEmptyUser().build());
                put(Product.class, new Product.Builder("a", "0", 0).build());
                put(CommerceEvent.class, new CommerceEvent.Builder("j", new Product.Builder("a", "0", 0).build()).build());
                put(Context.class, mContext);
                put(List.class, new ArrayList());
            }};
        }
        return mocks;
    }

    private String getApiName(Class clazz, Method method) {
        return InternalListenerManager.getApiFormattedName(clazz.getSimpleName(), method.getName());
    }

    class MockMParticle extends MParticle {
        MockMParticle() { }

        @Override
        public Environment getEnvironment() {
            return Environment.Development;
        }
    }

    class MockIdentityApi extends IdentityApi {

        MockIdentityApi() {
            super();
        }
    }

    class MockMParticleUser extends MParticleUserImpl {

        MockMParticleUser() {
            super();
        }
    }

    class MockCommerceApi extends CommerceApi {

        MockCommerceApi(@NonNull Context context) {
            super(context);
        }
    }
}
