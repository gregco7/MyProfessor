package dev.gregco7.cli;

import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Where this process's web server actually ended up.
 *
 * <p>The shell asks for an ephemeral port so it can never collide with anything
 * already running, which means the port is only known once the server is up.
 * The event that carries it fires during context refresh, before any
 * ApplicationRunner starts, so by the time the shell needs a URL this is set.
 */
@Component
public class ServerAddress implements ApplicationListener<WebServerInitializedEvent> {

    private volatile int port = -1;

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        this.port = event.getWebServer().getPort();
    }

    public boolean isReady() {
        return port > 0;
    }

    public String baseUrl() {
        return "http://localhost:" + port;
    }

    public String probeUrl(java.util.UUID probeId) {
        return baseUrl() + "/probe/" + probeId;
    }
}
