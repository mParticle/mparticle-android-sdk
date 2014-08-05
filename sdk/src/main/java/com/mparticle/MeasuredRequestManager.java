package com.mparticle;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Created by sdozor on 3/4/14.
 */
final class MeasuredRequestManager {
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);
    final HashSet<MeasuredRequest> requests = new HashSet<MeasuredRequest>();
    private CopyOnWriteArrayList<String> excludedUrlFilters = new CopyOnWriteArrayList<String>();
    private CopyOnWriteArrayList<String> queryStringFilters = new CopyOnWriteArrayList<String>();
    private static final String MPARTICLEHOST = ".mparticle.com";
    private ScheduledFuture<?> runner;
    private boolean enabled;

    public void addRequest(MeasuredRequest request){
        synchronized (requests) {
            requests.add(request);
        }
    }

    public MeasuredRequestManager() {

    }

    final Runnable processPending = new Runnable() {
        @Override
        public void run() {
            synchronized (requests) {

                if (requests.size() > 0) {
                    MParticle.getInstance().mConfigManager.debugLog("Processing ", Integer.toString(requests.size()), " measured network requests.");
                }
                Iterator<MeasuredRequest> iter = requests.iterator();
                ArrayList<String> loggedUris = new ArrayList<String>();
                ArrayList<Integer> loggedBodys = new ArrayList<Integer>();
                while(iter.hasNext()) {
                    MeasuredRequest request = iter.next();
                    try {
                        String uri = request.getUri();
                        String requestString = request.getRequestString();
                        boolean allowed = isUriAllowed(uri);
                        if (request.readyForLogging() && !loggedUris.contains(uri)) {
                            if (allowed) {
                        /* disabling this for the server-side extractors...for now
                         if (!isUriQueryAllowed(uri)){
                            uri = redactQuery(uri);
                        }*/

                                MParticle.getInstance().logNetworkPerformance(uri,
                                        request.getStartTime(),
                                        request.getMethod(),
                                        request.getTotalTime(),
                                        request.getBytesSent(),
                                        request.getBytesReceived(),
                                        requestString);
                                if ("POST".equalsIgnoreCase(request.getMethod())){
                                    loggedBodys.add(requestString.hashCode());
                                }else{
                                    loggedUris.add(uri);
                                }

                                MParticle.getInstance().mConfigManager.debugLog("Logging network request: " + request.toString());

                            }
                            request.reset();
                            iter.remove();
                        } else if (!allowed || (System.currentTimeMillis() - request.getStartTime()) > (60 * 1000) || loggedUris.contains(uri) || loggedBodys.contains(requestString.hashCode())) {
                            iter.remove();
                        }
                    }catch (Exception e){
                        iter.remove();
                    }
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
    public boolean isUriAllowed(String uri){
        if (uri == null){
            return true;
        }
        if (uri.contains(MPARTICLEHOST)){
            return false;
        }
        for (String filter : excludedUrlFilters){
            if (uri.contains(filter)){
                return false;
            }
        }
        return true;
    }

    public void setEnabled(boolean enabled){
        this.enabled = enabled;
        if (this.enabled){
            runner = scheduler.scheduleAtFixedRate(processPending, 10, 15, SECONDS);
        }else{
            if (runner != null){
                runner.cancel(true);
            }
        }
    }

    public boolean getEnabled() {
        return enabled;
    }
}

