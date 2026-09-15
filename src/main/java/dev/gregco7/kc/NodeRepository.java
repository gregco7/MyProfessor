package dev.gregco7.kc;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface NodeRepository extends JpaRepository<Node, UUID> {

}
