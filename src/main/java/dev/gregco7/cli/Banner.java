package dev.gregco7.cli;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The header drawn on entering the shell: what this is, what it will call, and
 * where it will write.
 *
 * <p>The model and the database are shown because both are chargeable or
 * destructive to get wrong, and both come from configuration rather than from
 * anything visible at the prompt. The command list is generated from the
 * registered commands, so it cannot drift from what the shell will actually
 * accept.
 */
@Component
class Banner {

    private static final String[] ART = {
            "    ___________",
            "   /\\          \\",
            "  /  \\          \\",
            " /    \\__________\\",
            " \\    /          /",
            "  \\  /          /",
            "   \\/__________/",
    };

    private final String model;
    private final String datasourceUrl;
    private final String version;

    Banner(@Value("${myprofessor.model}") String model,
           @Value("${spring.datasource.url}") String datasourceUrl,
           ObjectProvider<BuildProperties> buildProperties) {
        this.model = model;
        this.datasourceUrl = datasourceUrl;
        // Absent when running straight from classes rather than the packaged jar.
        this.version = buildProperties.getIfAvailable() != null
                ? buildProperties.getObject().getVersion()
                : "dev";
    }

    void draw(Term term, List<ShellCommand> commands) {
        String[] right = {
                Ansi.BOLD + "MyProfessor" + Ansi.RESET + "  " + Ansi.DIM + "v" + version + Ansi.RESET,
                "",
                Ansi.DIM + "model" + Ansi.RESET + "   " + model,
                Ansi.DIM + "db" + Ansi.RESET + "      " + datasourceUrl,
                "",
        };

        term.blank();
        for (int line = 0; line < ART.length; line++) {
            String art = Ansi.CYAN + ART[line] + Ansi.RESET;
            String padding = " ".repeat(Math.max(1, 22 - ART[line].length()));
            String text = line < right.length ? right[line] : "";
            term.println(text.isEmpty() ? "  " + art : "  " + art + padding + text);
        }

        term.blank();
        term.heading("  COMMANDS");
        int width = commands.stream().mapToInt(command -> command.usage().length()).max().orElse(0);
        for (ShellCommand command : commands) {
            term.println("    " + Ansi.GREEN + command.usage() + Ansi.RESET
                    + " ".repeat(width - command.usage().length() + 3)
                    + Ansi.DIM + command.description() + Ansi.RESET);
        }
        term.blank();
    }
}
