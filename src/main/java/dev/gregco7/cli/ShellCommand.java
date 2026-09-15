package dev.gregco7.cli;

import java.util.List;

/**
 * One thing that can be typed at the prompt.
 *
 * <p>Implementations are picked up as beans, so the shell's dispatch table and
 * the banner's command list are both derived from what exists rather than kept
 * in step by hand.
 */
public interface ShellCommand {

    /** What is typed to invoke it, leading slash included. */
    String name();

    /** The invocation with its arguments, shown in the command list. */
    String usage();

    /** One line, shown beside the usage. */
    String description();

    /**
     * @param args the arguments as typed, quotes already removed
     * @throws IllegalArgumentException if they are wrong; the shell prints the
     *                                  message and the usage rather than a trace
     */
    void run(List<String> args, Term term);
}
