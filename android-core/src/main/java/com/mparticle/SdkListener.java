package com.mparticle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mparticle.identity.AliasRequest;
import com.mparticle.identity.AliasResponse;
import com.mparticle.identity.MParticleUser;
import com.mparticle.internal.InternalSession;

import org.json.JSONObject;

import java.util.List;

/**
 * Note: This is an Experimental feature. Adding an instance of SdkListener will slow down the SDK and
 * should be used only in development.
 * <p>
 * SdkListener is a new feature which enables updates on and visibility into internal Events occuring
 * inside the SDK.
 */
public class SdkListener {
    public static final String ERROR_MESSAGE = "message";

    /**
     * Indicates that an API method was called. This includes invocations both from external sources (your code)
     * and those which originated from within the SDK.
     *
     * @param apiName    the name of the Api method
     * @param objects    the arguments used in the invocation
     * @param isExternal true, if the call originated from outside of the SDK
     */
    public void onApiCalled(@NonNull String apiName, @NonNull List<Object> objects, boolean isExternal) {

    }

    /**
     * Indicates that a new Database entry has been created.
     *
     * @param tableName  the name of the table, see {@link DatabaseTable}
     * @param primaryKey a unique identifier for the database row
     * @param message    the database entry in JSON form
     */
    public void onEntityStored(@NonNull DatabaseTable tableName, @NonNull long primaryKey, @NonNull JSONObject message) {

    }

    /**
     * Indicates that a Network Request has been started. Network Requests for a given {@link Endpoint} are performed.
     * synchronously, so the next invocation of {@link #onNetworkRequestStarted(Endpoint, String, JSONObject)}
     * of the same {@link Endpoint}, will refer to the same request
     *
     * @param type the type of network request, see {@link Endpoint}
     * @param url  the URL of the request
     * @param body the response body in JSON form
     */
    public void onNetworkRequestStarted(@NonNull Endpoint type, @NonNull String url, @NonNull JSONObject body) {

    }

    /**
     * Indicates that a Network Request has completed. Network Requests for a given {@link Endpoint} are performed
     * synchronously, so any invocation will refer to the same request as the most recent.
     * {@link #onNetworkRequestStarted(Endpoint, String, JSONObject)} invocation of the same {@link Endpoint}
     *
     * @param type         the type of network request, see {@link Endpoint}
     * @param url          the URL of the request
     * @param response     the response body in JSON form
     * @param responseCode the HTTP response code
     */
    public void onNetworkRequestFinished(@NonNull Endpoint type, @NonNull String url, @Nullable JSONObject response, int responseCode) {

    }

    /**
     * Indicates that a Kit method was invoked.
     *
     * @param kitId                the id of the kit, corresponds with a {@link com.mparticle.MParticle.ServiceProviders}
     * @param apiName              the method name which was invoked
     * @param invokingMethodName   the SDK Api call which triggered the invocation, if there was one
     * @param kitManagerMethodName the KitManager call which serverd as the intermediate trigger of the invocation, if there was one
     * @param objects              the arguments passed
     * @param used                 whether a {@link com.mparticle.internal.database.services.ReportingService.ReportingMessage} was generated as a result of the invocation. {@link com.mparticle.internal.database.services.ReportingService.ReportingMessage} indicate that an argument was consumed by the Kit
     */
    public void onKitApiCalled(int kitId, @NonNull String apiName, @Nullable String invokingMethodName, @Nullable String kitManagerMethodName, @NonNull List<Object> objects, boolean used) {

    }

    /**
     * Indicates that a Kit module, with kitId, is present in the classpath.
     *
     * @param kitId the id of the kit, corresponse with a {@link com.mparticle.MParticle.ServiceProviders}
     */
    public void onKitDetected(int kitId) {

    }

    /**
     * Indicates that a Configuration for a kit with kitId is being applied.
     *
     * @param kitId         the id of the kit, corresponse with a {@link com.mparticle.MParticle.ServiceProviders}
     * @param configuration the configuration in JSON form
     */
    public void onKitConfigReceived(int kitId, @NonNull JSONObject configuration) {

    }

    /**
     * Indicates that a kit with kitId was successfully started.
     *
     * @param kitId the id of the kit, corresponse with a {@link com.mparticle.MParticle.ServiceProviders}
     */
    public void onKitStarted(int kitId) {

    }

    /**
     * Indicates that either an attempt to start a kit was unsuccessful, or a started kit was stopped.
     * Possibilities for why this may happen include: {@link MParticleUser}'s loggedIn status or
     * {@link com.mparticle.consent.ConsentState} required it to be stopped, the Kit crashed, or a
     * configuration was received with excluded the kit.
     *
     * @param kitId  the id of the kit, corresponse with a {@link com.mparticle.MParticle.ServiceProviders}
     * @param reason a message containing the reason a kit was stopped
     */
    public void onKitExcluded(int kitId, @Nullable String reason) {

    }

    /**
     * Indicates that state of a Session may have changed.
     *
     * @param session the current {@link InternalSession} instance
     */
    public void onSessionUpdated(@Nullable InternalSession session) {

    }

    /**
     * Callback for {@link com.mparticle.identity.IdentityApi#aliasUsers(AliasRequest)} results.
     *
     * @param aliasResponse
     */
    public void onAliasRequestFinished(AliasResponse aliasResponse) {
    }

    public enum Endpoint {
        IDENTITY_LOGIN,
        IDENTITY_LOGOUT,
        IDENTITY_IDENTIFY,
        IDENTITY_MODIFY,
        IDENTITY_ALIAS,
        EVENTS,
        CONFIG
    }

    public enum DatabaseTable {
        ATTRIBUTES,
        BREADCRUMBS,
        MESSAGES,
        REPORTING,
        SESSIONS,
        UPLOADS,
        UNKNOWN
    }
}
