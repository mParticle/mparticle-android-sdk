package com.mparticle.internal.listeners;

import android.content.ContentValues;
import android.content.Context;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mparticle.SdkListener;
import com.mparticle.identity.AliasResponse;
import com.mparticle.internal.InternalSession;
import com.mparticle.internal.KitFrameworkWrapper;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class InternalListenerManager implements InternalListener {
    private static InternalListenerManager instance = null;
    private static final String INTERNAL_LISTENER_PROP = "debug.mparticle.listener";
    private Context context;
    final List<WeakReference<SdkListener>> sdkListeners = new ArrayList<WeakReference<SdkListener>>();
    final List<WeakReference<GraphListener>> graphListeners = new ArrayList<WeakReference<GraphListener>>();
    private boolean thrown = false;

    private InternalListenerManager(Context context) {
        this.context = context;
    }

    @Nullable
    public static InternalListenerManager start(Context context) {
        boolean canRun = MPUtility.isAppDebuggable(context) || context.getPackageName().equals(MPUtility.getProp(INTERNAL_LISTENER_PROP));
        if (instance == null && context != null && canRun) {
            instance = new InternalListenerManager(context.getApplicationContext());
        }
        return instance;
    }

    public static boolean isEnabled() {
        return instance != null &&
                instance.hasListeners();
    }

    @NonNull
    public static InternalListener getListener() {
        if (instance != null && isEnabled()) {
            return instance;
        } else {
            return InternalListener.EMPTY;
        }
    }

    public void addListener(SdkListener sdkListener) {
        for (WeakReference<SdkListener> listener : sdkListeners) {
            if (listener.get() == sdkListener) {
                return;
            }
        }
        sdkListeners.add(new WeakReference<SdkListener>(sdkListener));
        if (sdkListener instanceof GraphListener) {
            graphListeners.add(new WeakReference<GraphListener>((GraphListener) sdkListener));
        }
    }

    public void removeListener(SdkListener sdkListener) {
        for (WeakReference<SdkListener> listener : new ArrayList<WeakReference<SdkListener>>(sdkListeners)) {
            if (listener.get() == sdkListener) {
                sdkListeners.remove(listener);
            }
        }
        for (WeakReference<GraphListener> listener : new ArrayList<WeakReference<GraphListener>>(graphListeners)) {
            if (listener.get() == sdkListener) {
                graphListeners.remove(listener);
            }
        }
    }

    @Override
    public void onApiCalled(final Object... objects) {
        try {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            int index = -1;
            for (int i = 0; i < stackTrace.length; i++) {
                StackTraceElement element = stackTrace[i];
                if (element.getClassName().equals(getClass().getName())) {
                    index = i;
                    break;
                }
            }
            //move to call after the last call in this class;
            index++;
            while (stackTrace[index].getClassName().equals(ListenerHook.class.getName())) {
                index++;
            }
            StackTraceElement apiCall = stackTrace[index];
            StackTraceElement calledFrom = stackTrace[index + 1];
            final boolean isExternalApiInvocation = isExternalApiInvocation(calledFrom);
            final String apiName = getApiName(apiCall);

            broadcast(new SdkListenerRunnable() {
                @Override
                public void run(SdkListener listener) {
                    listener.onApiCalled(apiName, Arrays.asList(objects), isExternalApiInvocation);
                }
            });
        } catch (Exception ex) {
            if (!thrown) {
                thrown = true;
                Logger.error("SdkListener failed onApiCalled!\n" + ex.getMessage());
            }
        }
    }

    @Override
    public void onKitApiCalled(int kitId, Boolean used, Object... objects) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String methodName = null;
        for (int i = 0; i < stackTrace.length; i++) {
            StackTraceElement element = stackTrace[i];
            if (element.getClassName().equals(KitFrameworkWrapper.class.getName())) {
                methodName = element.getMethodName() + "()";
            }
        }
        onKitApiCalled(stackTrace, methodName, kitId, used, objects);
    }

    @Override
    public void onKitApiCalled(String methodName, int kitId, Boolean used, Object... objects) {
        onKitApiCalled(Thread.currentThread().getStackTrace(), methodName, kitId, used, objects);
    }

    private void onKitApiCalled(StackTraceElement[] stackTrace, final String methodName, final int kitId, final boolean used, Object... objects) {
        String invokingApiMethodName = null;
        String kitManagerMethodName = null;
        boolean foundInternal = false;
        boolean foundExternal = false;
        final List<Object> objectList = new ArrayList<Object>();
        for (Object obj : objects) {
            objectList.add(obj);
        }
        for (int i = 0; i < stackTrace.length; i++) {
            if (!isExternalApiInvocation(stackTrace[i])) {
                foundInternal = true;
            }
            if (foundInternal && !foundExternal) {
                if (isExternalApiInvocation(stackTrace[i])) {
                    invokingApiMethodName = getApiName(stackTrace[i - 1]);
                    foundExternal = true;
                }
            }
            if (stackTrace[i].getClassName().equals("com.mparticle.kits.KitManagerImpl")) {
                kitManagerMethodName = getApiName(stackTrace[i]);
            }
        }
        final String finalInvokingApiMethodName = invokingApiMethodName;
        final String finalKitManagerMethodName = kitManagerMethodName;
        broadcast(new SdkListenerRunnable() {
            @Override
            public void run(SdkListener listener) {
                listener.onKitApiCalled(kitId, methodName, finalInvokingApiMethodName, finalKitManagerMethodName, objectList, used);
            }
        });
    }


    @Override
    public void onCompositeObjects(final Object child, final Object parent) {
        broadcast(new SdkGraphListenerRunnable() {
            @Override
            public void run(GraphListener listener) {
                listener.onCompositeObjects(child, parent);
            }
        });
    }

    @Override
    public void onThreadMessage(final String handlerName, final Message msg, final boolean onNewThread) {
        StackTraceElement[] stackTrace = null;
        if (!onNewThread) {
            stackTrace = Thread.currentThread().getStackTrace();
        }
        final StackTraceElement[] finalStackTrace = stackTrace;
        broadcast(new SdkGraphListenerRunnable() {
            @Override
            public void run(GraphListener listener) {
                listener.onThreadMessage(handlerName, msg, onNewThread, finalStackTrace);
            }
        });
    }

    @Override
    public void onEntityStored(final Long primaryKey, final String tableName, ContentValues contentValues) {
        onCompositeObjects(contentValues, tableName + primaryKey);
        final JSONObject jsonObject = new JSONObject();
        SdkListener.DatabaseTable table = null;
        try {
            table = SdkListener.DatabaseTable.valueOf(tableName.toUpperCase());
        } catch (IllegalArgumentException ex) {
            table = SdkListener.DatabaseTable.UNKNOWN;
        }
        for (Map.Entry<String, Object> entry : contentValues.valueSet()) {
            try {
                if (entry.getValue() == null) {
                    jsonObject.put(entry.getKey(), JSONObject.NULL);
                } else {
                    jsonObject.put(entry.getKey(), entry.getValue());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        final SdkListener.DatabaseTable finalTable = table;
        broadcast(new SdkListenerRunnable() {
            @Override
            public void run(SdkListener listener) {
                listener.onEntityStored(finalTable, primaryKey, jsonObject);
            }
        });
    }

    @Override
    public void onNetworkRequestStarted(final SdkListener.Endpoint type, final String url, final JSONObject body, Object... objects) {
        for (Object obj : objects) {
            onCompositeObjects(obj, body);
        }
        final List<Object> objectList = new ArrayList<Object>();
        for (Object obj : objects) {
            objectList.add(obj);
        }
        broadcast(new SdkListenerRunnable() {
            @Override
            public void run(SdkListener listener) {
                listener.onNetworkRequestStarted(type, url, body);
            }
        });
    }

    @Override
    public void onNetworkRequestFinished(final SdkListener.Endpoint type, final String url, final JSONObject response, final int responseCode) {
        broadcast(new SdkListenerRunnable() {
            @Override
            public void run(SdkListener listener) {
                listener.onNetworkRequestFinished(type, url, response, responseCode);
            }
        });
    }

    @Override
    public void onSessionUpdated(final InternalSession internalSession) {
        broadcast(new SdkListenerRunnable() {
            @Override
            public void run(SdkListener listener) {
                listener.onSessionUpdated(new InternalSession(internalSession));
            }
        });
    }

    @Override
    public void onKitDetected(final int kitId) {
        broadcast(new SdkListenerRunnable() {
            @Override
            public void run(SdkListener listener) {
                listener.onKitDetected(kitId);
            }
        });
    }

    @Override
    public void onKitConfigReceived(final int kitId, final String configuration) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject = new JSONObject(configuration);
        } catch (JSONException e) {
        }
        final JSONObject jsonConfig = jsonObject;
        broadcast(new SdkListenerRunnable() {
            @Override
            public void run(SdkListener listener) {
                listener.onKitConfigReceived(kitId, jsonConfig);
            }
        });
    }

    @Override
    public void onKitExcluded(final int kitId, final String reason) {
        broadcast(new SdkListenerRunnable() {
            @Override
            public void run(SdkListener listener) {
                listener.onKitExcluded(kitId, reason);
            }
        });
    }

    @Override
    public void onKitStarted(final int kitId) {
        broadcast(new SdkListenerRunnable() {
            @Override
            public void run(SdkListener listener) {
                listener.onKitStarted(kitId);
            }
        });
    }

    @Override
    public void onAliasRequestFinished(final AliasResponse aliasResponse) {
        broadcast(new SdkListenerRunnable() {
            @Override
            public void run(SdkListener listener) {
                listener.onAliasRequestFinished(aliasResponse);
            }
        });
    }

    private void broadcast(SdkListenerRunnable runnable) {
        for (WeakReference<SdkListener> listenerRef : new ArrayList<WeakReference<SdkListener>>(sdkListeners)) {
            SdkListener listener = listenerRef.get();
            if (listener == null) {
                sdkListeners.remove(listenerRef);
            } else {
                runnable.run(listener);
            }
        }
    }

    private void broadcast(SdkGraphListenerRunnable runnable) {
        for (WeakReference<GraphListener> listenerRef : new ArrayList<WeakReference<GraphListener>>(graphListeners)) {
            GraphListener listener = listenerRef.get();
            if (listener == null) {
                graphListeners.remove(listenerRef);
            } else {
                runnable.run(listener);
            }
        }
    }

    interface SdkListenerRunnable {
        void run(SdkListener listener);
    }

    interface SdkGraphListenerRunnable {
        void run(GraphListener listener);
    }


    private String getApiName(StackTraceElement element) {
        String classNameString = getClassName(element.getClassName(), element.getMethodName());
        return getApiFormattedName(classNameString, element.getMethodName());
    }

    public static String getApiFormattedName(String className, String methodName) {
        return new StringBuilder()
                .append(className)
                .append(".")
                .append(methodName)
                .append("()")
                .toString();
    }

    private String getClassName(String className, String methodName) {
        String[] packageNames = className.split("\\.");
        String simpleClassName = packageNames[packageNames.length - 1];
        if (isObfuscated(simpleClassName)) {
            try {
                List<Class> superClasses = new ArrayList<Class>();
                Class clazz = Class.forName(className);
                superClasses.add(clazz.getSuperclass());
                for (Class interfce : clazz.getInterfaces()) {
                    superClasses.add(interfce);
                }
                for (Class superClass : superClasses) {
                    for (Method method : superClass.getMethods()) {
                        if (method.getName().equals(methodName)) {
                            String superClassName = getClassName(superClass.getName(), methodName);
                            if (!isObfuscated(superClassName)) {
                                return superClassName;
                            }
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return simpleClassName;
    }

    private boolean isObfuscated(@NonNull String className) {
        return Character.isLowerCase(className.toCharArray()[0]) && className.length() <= 3;
    }

    private boolean isExternalApiInvocation(StackTraceElement element) {
        return !element.getClassName().startsWith("com.mparticle") ||
                (element.getClassName().startsWith(context.getApplicationContext().getPackageName()) &&
                        context.getApplicationContext().getPackageName().length() > 1);
    }

    private boolean hasListeners() {
        return instance.sdkListeners.size() > 0 || instance.graphListeners.size() > 0;
    }
}
