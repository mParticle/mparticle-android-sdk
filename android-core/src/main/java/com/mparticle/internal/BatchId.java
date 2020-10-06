package com.mparticle.internal;

import androidx.annotation.Nullable;

import com.mparticle.internal.database.services.MessageService;

import java.util.Arrays;

public class BatchId {
    private long mpid;
    private String sessionId;
    private String dataplanId;
    private Integer dataplanVersion;

    public BatchId(long mpid, String sessionId, String dataplanId, Integer dataplanVersion) {
        this.mpid = mpid;
        this.sessionId = sessionId;
        this.dataplanId = dataplanId;
        this.dataplanVersion = dataplanVersion;
    }

    public BatchId(MessageService.ReadyMessage readyMessage) {
        mpid = readyMessage.getMpid();
        sessionId = readyMessage.getSessionId();
        dataplanId = readyMessage.getDataplanId();
        dataplanVersion = readyMessage.getDataplanVersion();
    }

    public long getMpid() {
        return mpid;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getDataplanId() {
        return dataplanId;
    }

    public Integer getDataplanVersion() {
        return dataplanVersion;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof BatchId)) {
            return false;
        }
        BatchId batchId = (BatchId)obj;
        for (int i = 0; i < fields().length; i++) {
            if (!MPUtility.isEqual(fields()[i], batchId.fields()[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(fields());
    }

    private Object[] fields() {
        return new Object[]{mpid, sessionId, dataplanId, dataplanVersion};
    }
}