package com.mparticle.kits

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.mparticle.MParticle.IdentityType
import com.mparticle.identity.MParticleUser
import com.mparticle.kits.KitIntegration.ApplicationStateListener
import com.mparticle.kits.KitIntegration.IdentityListener
import io.radar.sdk.Radar
import io.radar.sdk.Radar.getMetadata
import io.radar.sdk.Radar.getUserId
import io.radar.sdk.Radar.initialize
import io.radar.sdk.Radar.setAdIdEnabled
import io.radar.sdk.Radar.setMetadata
import io.radar.sdk.Radar.setUserId
import io.radar.sdk.Radar.startTracking
import io.radar.sdk.Radar.stopTracking
import io.radar.sdk.Radar.trackOnce
import io.radar.sdk.RadarTrackingOptions
import org.json.JSONException
import org.json.JSONObject
import java.util.LinkedList

class RadarKit :
    KitIntegration(),
    ApplicationStateListener,
    IdentityListener {
    @JvmField
    var mRunAutomatically = true

    private fun tryStartTracking() {
        val hasGrantedPermissions =
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        if (hasGrantedPermissions) {
            startTracking(RadarTrackingOptions.EFFICIENT)
        }
    }

    private fun tryTrackOnce() {
        val hasGrantedPermissions =
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        if (hasGrantedPermissions) {
            trackOnce(null as Radar.RadarTrackCallback?)
        }
    }

    override fun onKitCreate(
        settings: Map<String, String>,
        context: Context,
    ): List<ReportingMessage> {
        val publishableKey = settings[KEY_PUBLISHABLE_KEY]
        mRunAutomatically =
            settings.containsKey(KEY_RUN_AUTOMATICALLY) &&
            settings[KEY_RUN_AUTOMATICALLY].toBoolean()
        initialize(context, publishableKey, null)
        setAdIdEnabled(true)
        val user = currentUser
        if (user != null) {
            val identities = user.userIdentities
            val customerId = identities[IdentityType.CustomerId]
            customerId?.let { setUserId(it) }
            val radarMetadata = JSONObject()
            try {
                radarMetadata.put(
                    "mParticleId",
                    user.id.toString(),
                )
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            setMetadata(radarMetadata)
        }
        if (mRunAutomatically) {
            tryStartTracking()
        } else {
            stopTracking()
        }
        val messageList = LinkedList<ReportingMessage>()
        messageList.add(
            ReportingMessage(
                this,
                ReportingMessage.MessageType.APP_STATE_TRANSITION,
                System.currentTimeMillis(),
                null,
            ),
        )
        return messageList
    }

    override fun getName(): String = KIT_NAME

    override fun onApplicationForeground() {
        if (mRunAutomatically) {
            tryTrackOnce()
        } else {
            stopTracking()
        }
    }

    override fun onApplicationBackground() {}

    fun setUserAndTrack(
        user: MParticleUser?,
        currentRadarId: String?,
        currentMetadata: JSONObject?,
    ): Boolean = setUserAndTrack(user, currentRadarId, currentMetadata, false)

    fun setUserAndTrack(
        user: MParticleUser?,
        currentRadarId: String?,
        currentMetadata: JSONObject?,
        unitTesting: Boolean,
    ): Boolean {
        if (user == null) {
            return false
        }
        val newCustomerId = user.userIdentities[IdentityType.CustomerId]
        var newMpId: String? = null
        if ((user.id).toInt() != 0) {
            newMpId = user.id.toString()
        }
        var currentMpId: String? = null
        if (currentMetadata != null) {
            if (currentMetadata.has(M_PARTICLE_ID)) {
                try {
                    currentMpId = currentMetadata.getString(M_PARTICLE_ID)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
        val updatedCustomerId =
            if (newCustomerId == null) currentRadarId != null else newCustomerId != currentRadarId
        val updatedMpId = if (newMpId == null) currentMpId != null else newMpId != currentMpId
        if (updatedCustomerId && !unitTesting) {
            setUserId(newCustomerId)
        }
        if (updatedMpId && !unitTesting) {
            try {
                currentMetadata?.put(M_PARTICLE_ID, newMpId)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            setMetadata(currentMetadata)
        }
        if ((updatedCustomerId || updatedMpId) && !unitTesting) {
            if (mRunAutomatically) {
                tryTrackOnce()
                tryStartTracking()
            }
        }
        return updatedCustomerId || updatedMpId
    }

    override fun onIdentifyCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest,
    ) {
        setUserAndTrack(mParticleUser, getUserId(), getMetadata())
    }

    override fun onLoginCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest,
    ) {
        setUserAndTrack(mParticleUser, getUserId(), getMetadata())
    }

    override fun onLogoutCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest,
    ) {
        setUserAndTrack(mParticleUser, getUserId(), getMetadata())
    }

    override fun onModifyCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest,
    ) {
        setUserAndTrack(mParticleUser, getUserId(), getMetadata())
    }

    override fun onUserIdentified(mParticleUser: MParticleUser) {}

    override fun setOptOut(optedOut: Boolean): List<ReportingMessage> {
        if (mRunAutomatically) {
            stopTracking()
        }
        val messageList: MutableList<ReportingMessage> = LinkedList()
        messageList.add(
            ReportingMessage(
                this,
                ReportingMessage.MessageType.OPT_OUT,
                System.currentTimeMillis(),
                null,
            ),
        )
        return messageList
    }

    companion object {
        private const val KEY_PUBLISHABLE_KEY = "publishableKey"
        private const val KEY_RUN_AUTOMATICALLY = "runAutomatically"
        private const val M_PARTICLE_ID = "mParticleId"
        const val KIT_NAME = "RadarKit"
    }
}
