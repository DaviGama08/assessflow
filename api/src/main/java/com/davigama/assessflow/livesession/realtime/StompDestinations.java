package com.davigama.assessflow.livesession.realtime;

/**
 * RabbitMQ STOMP topic destinations cannot contain extra slashes
 * ({@code /topic/sessions/{id}} is rejected). Relay mode maps the public
 * contract onto {@code /exchange/amq.topic} routing keys. Simple broker
 * keeps the public destinations unchanged.
 */
public final class StompDestinations {
    private static final String PARTICIPANT = "/topic/sessions/";
    private static final String HOST = "/topic/host/sessions/";

    private StompDestinations() {}

    public static String participants(java.util.UUID sessionId) {
        return PARTICIPANT + sessionId;
    }

    public static String host(java.util.UUID sessionId) {
        return HOST + sessionId;
    }

    public static String forBroker(String destination) {
        if (destination == null) {
            return null;
        }
        if (destination.startsWith(HOST)) {
            return "/exchange/amq.topic/live.host.sessions." + destination.substring(HOST.length());
        }
        if (destination.startsWith(PARTICIPANT)) {
            return "/exchange/amq.topic/live.sessions." + destination.substring(PARTICIPANT.length());
        }
        return destination;
    }
}
