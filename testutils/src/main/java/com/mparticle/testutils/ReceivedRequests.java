package com.mparticle.testutils;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

import java.util.ArrayList;
import java.util.List;

public class ReceivedRequests {
    List<ServeEvent> mServeEvents;

    public ReceivedRequests(List<ServeEvent> serveEvents) {
        this.mServeEvents = serveEvents;
    }

    public Identity Identity() {
        return new Identity(mServeEvents);
    }

    public Events Events() {
        return new Events(mServeEvents);
    }

    public Config Config() {
        return new Config(mServeEvents);
    }

    static List<ServeEvent> filterByUrlContains(String url, List<ServeEvent> serveEvents) {
        List<ServeEvent> events = new ArrayList<ServeEvent>();
        for (ServeEvent event: serveEvents) {
            if (event.getRequest().getUrl().contains(url)) {
                events.add(event);
            }
        }
        return events;
    }

    public static class Identity {
        private List<ServeEvent> identityServeEvents;

        private Identity(List<ServeEvent> serveEvents) {
            identityServeEvents = filterByUrlContains("/v1/", serveEvents);
        }

        public List<ServeEvent> identify() {
            return filterByUrlContains("/identify", identityServeEvents);
        }

        public List<ServeEvent> login() {
            return filterByUrlContains("/login", identityServeEvents);
        }

        public List<ServeEvent> logout() {
            return filterByUrlContains("/logout", identityServeEvents);
        }

        public List<ServeEvent> modify() {
            return filterByUrlContains("/modify", identityServeEvents);
        }
    }

    public static class Events {
        private List<ServeEvent> eventServeEvents;

        private Events(List<ServeEvent> serveEvents) {
            eventServeEvents = filterByUrlContains("/events/", serveEvents);
        }

        public List<ServeEvent> get() {
            return eventServeEvents;
        }
    }

    public static class Config {
        private List<ServeEvent> configServeEvents;

        private Config(List<ServeEvent> serveEvents) {
            configServeEvents = filterByUrlContains("/config/", serveEvents);
        }

        public List<ServeEvent> get() {
            return configServeEvents;
        }
    }
}
