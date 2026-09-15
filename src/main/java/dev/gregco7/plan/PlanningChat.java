package dev.gregco7.plan;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * The one place planning talks to Claude. Both the probe and the session are
 * built from single-shot requests that must come back as a record, so they share
 * the model settings and the failure handling rather than each keeping a copy.
 */
@Service
class PlanningChat {

    private static final String MODEL = "claude-opus-5";

    private static final Log logger = LogFactory.getLog(PlanningChat.class);

    private final ChatClient chat;

    PlanningChat(ChatClient.Builder chatClientBuilder) {
        this.chat = chatClientBuilder.build();
    }

    /**
     * @param maxTokens room for the response; the material for one lesson needs
     *                  far more than an outline or a diagnostic does
     * @param what      what the request was for, in words that read as the middle
     *                  of "Claude could not ..." when it fails
     */
    <T> T ask(String system, String user, int maxTokens, Class<T> type, String what) {
        T answer;
        try {
            answer = chat.prompt()
                    .options(options(maxTokens))
                    .system(system)
                    .user(user)
                    .call()
                    .entity(type);
        }
        catch (RuntimeException ex) {
            // The exception below carries a @ResponseStatus, so Spring handles it
            // quietly and the upstream reason — a refusal, a rate limit, an empty
            // credit balance — would otherwise never reach the log.
            logger.error("Claude could not " + what, ex);
            throw new PlanGenerationException("Claude could not " + what, ex);
        }

        if (answer == null) {
            logger.error("Claude returned nothing usable when asked to " + what);
            throw new PlanGenerationException("Claude returned nothing usable when asked to " + what);
        }
        return answer;
    }

    private static AnthropicChatOptions.Builder options(int maxTokens) {
        AnthropicChatOptions.Builder options = AnthropicChatOptions.builder();
        options.model(MODEL);
        options.maxTokens(maxTokens);
        // Deciding what a learner already knows, and writing questions that
        // discriminate, are both reasoning work. Adaptive thinking lets Claude
        // spend where the topic is hard and skip where it is not.
        options.thinkingAdaptive();
        return options;
    }
}
