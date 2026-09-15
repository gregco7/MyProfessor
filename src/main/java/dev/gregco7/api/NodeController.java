package dev.gregco7.api;

import dev.gregco7.kc.AttemptResult;
import dev.gregco7.kc.Node;
import dev.gregco7.kc.NodeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/nodes")
class NodeController {

    private final NodeService nodes;

    NodeController(NodeService nodes) {
        this.nodes = nodes;
    }

    /** The lesson: material to read, then the questions, hints included, key withheld. */
    @GetMapping("/{nodeId}")
    LessonView get(@PathVariable UUID nodeId) {
        return LessonView.of(nodes.get(nodeId));
    }

    /**
     * Records a sitting of the lesson's questions.
     *
     * <p>Stamps lastAttemptedTime whether it passed or not — that is what the
     * column is for. Fast: no Claude call, just marking and a write.
     */
    @PostMapping("/{nodeId}/attempt")
    AttemptResult attempt(@PathVariable UUID nodeId,
                          @Valid @RequestBody ApiRequests.Attempt request) {
        return nodes.attempt(nodeId, ApiRequests.toSubmitted(request.answers()));
    }

    /**
     * The answer key, with what the learner gave beside it.
     *
     * <p>409 until the lesson has been attempted. Handing the key over on demand
     * would make thinking about the questions optional.
     */
    @GetMapping("/{nodeId}/review")
    ReviewView review(@PathVariable UUID nodeId) {
        Node node = nodes.get(nodeId);
        if (node.getLastAttemptedTime() == null) {
            throw new NotAttemptedException(nodeId);
        }
        return ReviewView.of(node);
    }
}
