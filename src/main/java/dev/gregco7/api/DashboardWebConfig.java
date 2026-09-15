package dev.gregco7.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * What the React dashboard needs from the server besides the API itself.
 *
 * <p>Two arrangements are supported and they need different things. In
 * development the dashboard runs on its own dev server on another port, which
 * makes every call cross-origin — hence the CORS rule, listing exactly the
 * origins in {@code myprofessor.dashboard-origins}. In production the built
 * dashboard is copied into {@code src/main/resources/static} and served from
 * this same origin, where CORS does not apply at all.
 */
@Configuration
class DashboardWebConfig implements WebMvcConfigurer {

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
     * Client-side routing means the browser can ask for /sessions/{id} directly.
     * There is no such file, so those requests are handed the dashboard's entry
     * point and the router sorts it out. Only paths with no dot are forwarded,
     * which keeps real asset requests falling through to a genuine 404 instead
     * of quietly returning HTML.
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/{path:[^.]*}").setViewName("forward:/index.html");
        registry.addViewController("/{path:^(?!api$)[^.]*}/**").setViewName("forward:/index.html");
    }
}
