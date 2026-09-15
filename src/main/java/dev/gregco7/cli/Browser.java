package dev.gregco7.cli;

import java.io.IOException;
import java.util.List;

/**
 * Opens a URL in whatever the desktop considers its browser.
 *
 * <p>Deliberately not {@code java.awt.Desktop}: it drags in AWT, and on a
 * headless or oddly-configured JVM it throws rather than degrading. Handing the
 * job to the platform's own opener is smaller and fails in a way the caller can
 * recover from by simply printing the link.
 */
final class Browser {

    private Browser() {}

    /** @return true if something was launched; false leaves the caller to print the URL */
    static boolean open(String url) {
        String os = System.getProperty("os.name", "").toLowerCase();
        List<String> command = os.contains("mac")
                ? List.of("open", url)
                : os.contains("win")
                        ? List.of("rundll32", "url.dll,FileProtocolHandler", url)
                        : List.of("xdg-open", url);
        try {
            return new ProcessBuilder(command).start().waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
        catch (IOException | RuntimeException ex) {
            return false;
        }
    }
}
