package dev.gregco7.plan;

import com.anthropic.models.messages.OutputConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * The one place planning talks to Claude. Both the probe and the session are
 * built from single-shot requests that must come back as a record, so they share
 * the model settings, the failure handling, and the accounting.
 *
 * <p>The conversion to a record is done here rather than through the client's
 * {@code entity()} shortcut. That shortcut throws from inside itself when the
 * response will not parse, taking the token counts and the raw text down with
 * it — which is exactly the moment both are needed. Asking for the response
 * first and converting second means a failure can say what came back and what
 * it cost.
 */
@Service
class PlanningChat {

    private static final Log logger = LogFactory.getLog(PlanningChat.class);

    private final ChatClient chat;
    private final String model;

    // Running totals for the life of the process. Planning is the only thing here
    // that costs money, and it costs it in output tokens; without a number to look
    // at, spend is invisible until the bill arrives.
    private final AtomicLong inputTokens = new AtomicLong();
    private final AtomicLong outputTokens = new AtomicLong();

    PlanningChat(ChatClient.Builder chatClientBuilder, @Value("${myprofessor.model}") String model) {
        this.chat = chatClientBuilder.build();
        this.model = model;
    }

    /**
     * @param maxTokens ceiling for thinking and answer together, not just the answer
     * @param what      what the request was for, in words that read as the middle
     *                  of "Claude could not ..." when it fails
     */
    <T> T ask(String system, String user, int maxTokens, Class<T> type, String what) {
        BeanOutputConverter<T> converter = new BeanOutputConverter<>(type);

        ChatResponse response;
        try {
            response = chat.prompt()
                    .options(options(maxTokens))
                    .system(system)
                    // The schema instructions travel with the request; asking for the
                    // raw response means they are no longer appended for us.
                    .user(user + "\n\n" + converter.getFormat())
                    .call()
                    .chatResponse();
        }
        catch (RuntimeException ex) {
            // The shell shows a one-line message and the dashboard a ProblemDetail,
            // so the upstream reason — a refusal, a rate limit, an empty credit
            // balance — is logged here or it is lost.
            logger.error("Claude could not " + what, ex);
            throw new PlanGenerationException("Claude could not " + what, ex);
        }

        account(response, what);
        String text = textOf(response);
        String finishReason = finishReasonOf(response);

        if (text == null || text.isBlank()) {
            // The usual cause is the whole budget going on thinking. Say so, rather
            // than leaving a bare Jackson "end-of-input" to be deciphered later.
            logger.error("Empty response to '%s' (finishReason=%s, maxTokens=%d, blocks=%s)"
                    .formatted(what, finishReason, maxTokens, blockSizes(response)));
            throw new PlanGenerationException(
                    "Claude returned no answer when asked to " + what
                            + " (finished as '" + finishReason + "')");
        }

        try {
            T answer = converter.convert(text);
            if (answer == null) {
                throw new PlanGenerationException("Claude returned nothing usable when asked to " + what);
            }
            return answer;
        }
        catch (RuntimeException ex) {
            logger.error("Unparseable response to '%s' (finishReason=%s). First 500 chars: %s"
                    .formatted(what, finishReason, text.substring(0, Math.min(500, text.length()))), ex);
            throw new PlanGenerationException("Claude's answer could not be read when asked to " + what, ex);
        }
    }

    /**
     * Joins every generation in the response rather than taking the first.
     *
     * <p>A reply from a thinking model arrives as more than one block, and on
     * this model the thinking block's text is empty by default — the reasoning
     * happens and is billed, but is not sent back. Reading only the first block
     * therefore yields "" while the JSON sits in the next one, which looks
     * exactly like the model having said nothing at all.
     */
    private static String textOf(ChatResponse response) {
        if (response == null || response.getResults() == null) {
            return null;
        }
        return response.getResults().stream()
                .map(generation -> generation.getOutput() == null
                        ? null : generation.getOutput().getText())
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining());
    }

    /** Block-by-block text lengths, so an empty answer can be told from a misread one. */
    private static String blockSizes(ChatResponse response) {
        if (response == null || response.getResults() == null) {
            return "none";
        }
        return response.getResults().stream()
                .map(generation -> generation.getOutput() == null
                        || generation.getOutput().getText() == null
                        ? "null" : String.valueOf(generation.getOutput().getText().length()))
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static String finishReasonOf(ChatResponse response) {
        return response == null || response.getResult() == null
                || response.getResult().getMetadata() == null
                ? "unknown"
                : String.valueOf(response.getResult().getMetadata().getFinishReason());
    }

    private void account(ChatResponse response, String what) {
        if (response == null || response.getMetadata() == null) {
            return;
        }
        Usage usage = response.getMetadata().getUsage();
        if (usage == null) {
            return;
        }
        long in = usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
        long out = usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;

        logger.info("tokens in=%d out=%d (running in=%d out=%d) — %s".formatted(
                in, out, inputTokens.addAndGet(in), outputTokens.addAndGet(out), what));
    }

    private AnthropicChatOptions.Builder options(int maxTokens) {
        AnthropicChatOptions.Builder options = AnthropicChatOptions.builder();
        options.model(model);
        options.maxTokens(maxTokens);
        // Deciding what a learner already knows, and writing questions that
        // discriminate, are both reasoning work, so thinking stays on. Effort is
        // held at medium because it is the lever that decides how much of the
        // budget that reasoning consumes, and this work does not need the top of
        // the range to come out well.
        options.thinkingAdaptive();
        options.effort(OutputConfig.Effort.MEDIUM);
        return options;
    }
}
