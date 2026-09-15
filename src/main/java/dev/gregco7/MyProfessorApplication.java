package dev.gregco7;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * One jar, two front ends.
 *
 * <p>Default is the HTTP API the React dashboard talks to. With the {@code cli}
 * profile the shell takes the terminal instead and no server starts.
 */
@SpringBootApplication
public class MyProfessorApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context =
                SpringApplication.run(MyProfessorApplication.class, args);

        // ApplicationRunners finish inside run(), so by the time this line is
        // reached in CLI mode the learner has already left the shell and the
        // process should end. Under the API this must not fire: the server is
        // the thing that is supposed to keep running.
        if (context.getEnvironment().matchesProfiles("cli")) {
            System.exit(SpringApplication.exit(context));
        }
    }

}
