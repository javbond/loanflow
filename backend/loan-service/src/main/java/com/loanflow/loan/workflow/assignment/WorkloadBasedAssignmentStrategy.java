package com.loanflow.loan.workflow.assignment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.TaskService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Workload-based assignment strategy.
 * Assigns tasks to the officer with the fewest active Flowable tasks.
 *
 * Activated when loanflow.assignment.strategy=WORKLOAD_BASED in application.yml.
 */
@Component
@ConditionalOnProperty(name = "loanflow.assignment.strategy", havingValue = "WORKLOAD_BASED")
@RequiredArgsConstructor
@Slf4j
public class WorkloadBasedAssignmentStrategy implements AssignmentStrategy {

    private final AssignmentProperties properties;
    private final TaskService taskService;

    @Override
    public Optional<String> selectAssignee(String candidateGroup) {
        List<String> officers = properties.getOfficers().get(candidateGroup);
        if (officers == null || officers.isEmpty()) {
            log.warn("No officers configured for candidate group: {}", candidateGroup);
            return Optional.empty();
        }

        // Collect workload counts for all officers, then pick the minimum
        Optional<String> selectedOfficer = officers.stream()
                .map(userId -> new AbstractMap.SimpleEntry<>(userId, countActiveTasks(userId)))
                .min(Comparator.comparingLong(Map.Entry::getValue))
                .map(Map.Entry::getKey);

        selectedOfficer.ifPresent(officer ->
                log.info("Workload-based assignment: group={}, selected={}", candidateGroup, officer));

        return selectedOfficer;
    }

    /**
     * Count active (assigned) Flowable tasks for a given user.
     */
    private long countActiveTasks(String userId) {
        return taskService.createTaskQuery()
                .taskAssignee(userId)
                .count();
    }
}
