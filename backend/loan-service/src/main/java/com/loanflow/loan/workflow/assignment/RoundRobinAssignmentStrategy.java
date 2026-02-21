package com.loanflow.loan.workflow.assignment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Round-robin assignment strategy.
 * Cycles through configured officers for each candidate group.
 *
 * This is the default strategy (active when strategy=ROUND_ROBIN or not specified).
 * State is kept in-memory and resets on application restart.
 */
@Component
@ConditionalOnProperty(name = "loanflow.assignment.strategy", havingValue = "ROUND_ROBIN", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class RoundRobinAssignmentStrategy implements AssignmentStrategy {

    private final AssignmentProperties properties;

    /**
     * Per-group counter for round-robin cycling.
     * Uses ConcurrentHashMap + AtomicInteger for thread safety.
     */
    private final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    @Override
    public Optional<String> selectAssignee(String candidateGroup) {
        List<String> officers = properties.getOfficers().get(candidateGroup);
        if (officers == null || officers.isEmpty()) {
            log.warn("No officers configured for candidate group: {}", candidateGroup);
            return Optional.empty();
        }

        AtomicInteger counter = counters.computeIfAbsent(candidateGroup, k -> new AtomicInteger(0));
        int index = Math.floorMod(counter.getAndIncrement(), officers.size());
        String selectedOfficer = officers.get(index);

        log.info("Round-robin assignment: group={}, selected={} (index={})", candidateGroup, selectedOfficer, index);
        return Optional.of(selectedOfficer);
    }
}
