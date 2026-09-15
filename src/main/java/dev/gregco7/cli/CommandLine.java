package dev.gregco7.cli;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a typed line into a command and its arguments.
 *
 * <p>Quoting is honoured because the first argument is a concept — "Recursion in
 * programming" is one argument, not three — and the prompt shows it quoted, so
 * it has to accept it back that way.
 */
record CommandLine(String name, List<String> args) {

    /** @return the parsed line, or null if nothing but whitespace was typed */
    static CommandLine parse(String line) {
        List<String> tokens = tokenize(line);
        if (tokens.isEmpty()) {
            return null;
        }
        return new CommandLine(tokens.getFirst(), tokens.subList(1, tokens.size()));
    }

    private static List<String> tokenize(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        char quote = 0;
        boolean quoted = false;

        for (char ch : line.toCharArray()) {
            if (quote != 0) {
                if (ch == quote) {
                    quote = 0;
                }
                else {
                    token.append(ch);
                }
            }
            else if (ch == '\'' || ch == '"') {
                quote = ch;
                // An empty pair of quotes is still an argument the user meant to pass.
                quoted = true;
            }
            else if (Character.isWhitespace(ch)) {
                if (!token.isEmpty() || quoted) {
                    tokens.add(token.toString());
                    token.setLength(0);
                    quoted = false;
                }
            }
            else {
                token.append(ch);
            }
        }
        if (!token.isEmpty() || quoted) {
            tokens.add(token.toString());
        }
        return tokens;
    }
}
