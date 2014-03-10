package com.mparticle;

import android.util.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Created by sdozor on 3/4/14.
 */
final class MeasuredRequestManager {
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);
    ConcurrentHashMap<String, MeasuredRequest> requests = new ConcurrentHashMap<String, MeasuredRequest>();
    private CopyOnWriteArrayList<String> excludedUrlFilters = new CopyOnWriteArrayList<String>();
    private CopyOnWriteArrayList<String> queryStringFilters = new CopyOnWriteArrayList<String>();

    public void addRequest(MeasuredRequest request){
        MeasuredRequest current = requests.get(request.getKey());
        if (current == null || request.foundHeaderTiming()){
            requests.put(request.getKey(), request);
        }
    }

    public MeasuredRequestManager() {
        scheduler.scheduleAtFixedRate(processPending, 10, 30, SECONDS);
    }

    final Runnable processPending = new Runnable() {
        @Override
        public void run() {
            boolean debugLog = MParticle.getInstance().getDebugMode();
            if (debugLog){
                Log.d(Constants.LOG_TAG, "Processing " + requests.size() + " measured network requests.");
            }
            for (Map.Entry<String, MeasuredRequest> entry : requests.entrySet()){
                if (entry.getValue().readyForLogging()){
                    MeasuredRequest request = entry.getValue();
                    String uri = request.getUri();
                    if (isUriAllowed(uri)) {
                        if (!isUriQueryAllowed(uri)){
                            uri = redactQuery(uri);
                        }
                        MParticle.getInstance().logNetworkPerformance(uri,
                                request.getStartTime(),
                                request.getMethod(),
                                request.getTotalTime(),
                                request.getBytesSent(),
                                request.getBytesReceived());
                        if (debugLog) {
                            Log.d(Constants.LOG_TAG, "Logging network request: " + request.toString());
                        }
                    }
                    request.reset();
                    requests.remove(entry.getKey());
                }
            }
        }

        private String redactQuery(String uri) {
            int queryIndex = uri.indexOf("?");
            if (queryIndex > 0){
                return uri.substring(0, queryIndex);
            }else if ((queryIndex = uri.indexOf(("%3F"))) > 0){
                return  uri.substring(0, queryIndex);
            }
            return uri;
        }

        private boolean isUriQueryAllowed(String uri) {
            for (String queryFilter : queryStringFilters){
                if (uri.contains(queryFilter)){
                    return true;
                }
            }
            return false;
        }

        private boolean isUriAllowed(String uri){
            for (String filter : excludedUrlFilters){
                if (uri.contains(filter)){
                    return false;
                }
            }
            return true;
        }
    };

    public void addExcludedUrl(String url) {
        excludedUrlFilters.add(url);
    }

    public void addQueryStringFilter(String filter) {
        queryStringFilters.add(filter);
    }

    public void resetFilters() {
        excludedUrlFilters.clear();
        queryStringFilters.clear();
    }
}

