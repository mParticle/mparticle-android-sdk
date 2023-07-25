package com.mparticle.identity;

import androidx.annotation.NonNull;

import com.mparticle.MParticle;
import com.mparticle.internal.Logger;

import java.util.concurrent.TimeUnit;

/**
 * This class represents a request to indicate that a provided mpid should be a proxy for another,
 * over a given timespan. This class must be initialzed with one of the overloaded {@link AliasRequest#builder()} methods.
 * The variant {@link AliasRequest#builder(MParticleUser, MParticleUser)} creates a default request based
 * on the 2 {@link MParticleUser} instances provided, and requires no additional input other than calling {@link Builder#build()},
 * while the no-arg method relies entirely on your input in the Builder to populate the request.
 */
public class AliasRequest {

    private Long sourceMpid;
    private Long destinationMpid;

    private Long startTime;
    private Long endTime;

    private AliasRequest(AliasRequest.Builder builder) {
        this.sourceMpid = builder.sourceMpid;
        this.destinationMpid = builder.destinationMpid;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
    }

    public long getSourceMpid() {
        return sourceMpid;
    }

    public long getDestinationMpid() {
        return destinationMpid;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    /**
     * Create an empty {@link AliasRequest.Builder} instance
     *
     * @return
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a default {@link AliasRequest.Builder} for 2 MParticleUsers. This will construct the request
     * using the sourceUser's firstSeenTime as the startTime, and its lastSeenTime as the endTime.
     *
     * There is a limit to how old the startTime can be, represented by the config field 'aliasMaxWindow', in days.
     * if the startTime falls before the limit, it will be adjusted to the oldest allowed startTime.
     * In rare cases, where the sourceUser's lastSeenTime also falls outside of the aliasMaxWindow limit,
     * after applying this adjustment it will be impossible to create an aliasRequest passes the aliasUsers()
     * validation that the startTime must be less than the endTime
     *
     * @param sourceUser      the user which is to be "copied" over
     * @param destinationUser the user which the sourceUser will be "copied" onto
     */
    public static Builder builder(@NonNull MParticleUser sourceUser, @NonNull MParticleUser destinationUser) {
        return new Builder()
                .destinationMpid(destinationUser.getId())
                .sourceMpid(sourceUser.getId())
                .startTime(sourceUser.getFirstSeenTime())
                .endTime(sourceUser.getLastSeenTime())
                .implicitStartTime(true);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AliasRequest) {
            AliasRequest request = ((AliasRequest) obj);
            return request.getEndTime() == getEndTime() &&
                    request.getStartTime() == getStartTime() &&
                    request.getSourceMpid() == getSourceMpid() &&
                    request.getDestinationMpid() == getDestinationMpid();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (getEndTime() + ":" + getStartTime() + ":" + getSourceMpid() + ":" + getDestinationMpid()).hashCode();
    }

    public static class Builder {
        private long sourceMpid;
        private long destinationMpid;

        private long startTime;
        private long endTime;

        private boolean implicitStartTime = false;

        /**
         * set the user which the sourceUser will be "copied" onto
         *
         * @param mpid the destination user's mpid
         * @return
         */
        public Builder destinationMpid(long mpid) {
            this.destinationMpid = mpid;
            return this;
        }

        /**
         * set the mpid of the user which is to be "copied" over
         *
         * @return the source user's mpid
         */
        public Builder sourceMpid(long mpid) {
            this.sourceMpid = mpid;
            return this;
        }

        /**
         * set a time indicating the beginning of the window of activity which should be aliased
         *
         * @param startTime the time, in milliseconds
         * @return
         */
        public Builder startTime(long startTime) {
            this.startTime = startTime;
            this.implicitStartTime = false;
            return this;
        }

        /**
         * set a time indicating the end of the window of activity which should be aliased
         *
         * @param endTime the time, in milliseconds
         */
        public Builder endTime(long endTime) {
            this.endTime = endTime;
            return this;
        }

        private Builder implicitStartTime(boolean implicitStartTime) {
            this.implicitStartTime = implicitStartTime;
            return this;
        }

        /**
         * build the {@link AliasRequest.Builder} into an immutable {@link AliasRequest}
         *
         * @return
         */
        public AliasRequest build() {
            if (implicitStartTime) {
                int aliasMaxWindow = 90;
                try {
                    MParticle instance = MParticle.getInstance();
                    if (instance != null) {
                        aliasMaxWindow = instance.Internal().getConfigManager().getAliasMaxWindow();
                    }
                } catch (Exception e) {
                    Logger.error(e.getMessage());
                }
                long earliestLegalStartTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(aliasMaxWindow);
                if (startTime < earliestLegalStartTime) {
                    startTime = earliestLegalStartTime;
                    if (startTime > endTime) {
                        Logger.warning("Source User has not been seen in the last %s days. Alias Request will likely fail");
                    }
                }
            }

            return new AliasRequest(this);
        }
    }
}
