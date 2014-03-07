package com.mparticle;

import android.util.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.*;

/**
 * Created by sdozor on 3/4/14.
 */
final class RequestManager {
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);
    ConcurrentHashMap<String, MeasuredRequest> requests = new ConcurrentHashMap<String, MeasuredRequest>();

    public void addRequest(MeasuredRequest request){
        MeasuredRequest current = requests.get(request.getKey());
        if (current == null || request.foundHeaderTiming()){
            requests.put(request.getKey(), request);
        }
    }

    public RequestManager() {
        scheduler.scheduleAtFixedRate(processPending, 10, 30, SECONDS);
    }

    final Runnable processPending = new Runnable() {
        @Override
        public void run() {
            if (MParticle.getInstance().getDebugMode()){
                Log.d(Constants.LOG_TAG, "Processing " + requests.size() + " measured network requests.");
            }
            for (Map.Entry<String, MeasuredRequest> entry : requests.entrySet()){
                if (entry.getValue().readyForLogging()){
                    MeasuredRequest request = entry.getValue();
                    MParticle.getInstance().logNetworkPerformance(request.getUri(),
                            request.getStartTime(),
                            request.getMethod(),
                            request.getTotalTime(),
                            request.getBytesSent(),
                            request.getBytesReceived());
                    if (MParticle.getInstance().getDebugMode()){
                        Log.d(Constants.LOG_TAG, "Logging network request: " + request.toString());
                    }
                    request.reset();
                    requests.remove(entry.getKey());
                }
            }
        }
    };
}

