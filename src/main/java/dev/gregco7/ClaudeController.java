package dev.gregco7;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClaudeController {

    @Value("${CLAUDE_KEY}")
    private String apiKey;

    public String chat() {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
