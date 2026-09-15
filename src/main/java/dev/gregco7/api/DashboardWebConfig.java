package dev.gregco7.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * What the React dashboard needs from the server besides the API itself.
 *
 * <p>Two arrangements are supported and they need different things. In
 * development the dashboard runs on its own dev server, which proxies /api here
 * — same-origin, so CORS does not come into it. The CORS rule exists for the
 * case where someone points a dev server straight at this port instead. In
 * production the built bundle is written into {@code src/main/resources/static}
 * by {@code npm run build} and served from this origin.
 */
@Configuration
class DashboardWebConfig implements WebMvcConfigurer {

    private static final String STATIC_ROOT = "classpath:/static/";
    private static final String ENTRY_POINT = "/static/index.html";

    private final String[] origins;

    DashboardWebConfig(@Value("${myprofessor.dashboard-origins}") String[] origins) {
        this.origins = origins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST")
                .allowedHeaders("*");
    }

    /**
     * Serves the bundle, and hands client-side routes to its entry point.
     *
     * <p>The fallback is decided by whether the file is actually there, not by
     * the shape of the path. An earlier version matched routes by pattern and
     * forwarded anything that looked like {@code /segment/rest}, which swallowed
     * {@code /assets/index-abc123.js} and answered it with HTML — a 200 carrying
     * the wrong content type, which fails only later and in the browser.
     *
     * <p>A request that misses and carries a file extension is left to 404
     * honestly, so a genuinely absent asset does not come back as the app.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations(STATIC_ROOT)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location)
                            throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        if (resourcePath.startsWith("api/") || hasExtension(resourcePath)) {
                            return null;
                        }
                        Resource entry = new ClassPathResource(ENTRY_POINT);
                        return entry.exists() ? entry : null;
                    }
                });
    }

    /** True when the last segment looks like a file rather than a route. */
    private static boolean hasExtension(String resourcePath) {
        int slash = resourcePath.lastIndexOf('/');
        return resourcePath.indexOf('.', slash + 1) >= 0;
    }
}
