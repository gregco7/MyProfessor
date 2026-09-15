package dev.gregco7.probe;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ProbeRepository extends JpaRepository<Probe, UUID> {

}
