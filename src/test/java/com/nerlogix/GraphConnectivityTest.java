package com.nerlogix;

import com.nerlogix.entity.RoadNode;
import com.nerlogix.entity.RoadSegment;
import com.nerlogix.repository.RoadNodeRepository;
import com.nerlogix.repository.RoadSegmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class GraphConnectivityTest {

    @Autowired
    private RoadNodeRepository roadNodeRepository;

    @Autowired
    private RoadSegmentRepository roadSegmentRepository;

    @Test
    void testGraphIsConnected() {
        List<RoadNode> allNodes = roadNodeRepository.findAll();
        List<RoadSegment> allSegments = roadSegmentRepository.findAll();

        assertThat(allNodes.size()).isGreaterThanOrEqualTo(35);
        assertThat(allSegments.size()).isGreaterThanOrEqualTo(60);

        Map<Long, List<Long>> adj = new HashMap<>();
        for (RoadNode n : allNodes) {
            adj.put(n.getId(), new ArrayList<>());
        }

        for (RoadSegment s : allSegments) {
            adj.get(s.getFromNode().getId()).add(s.getToNode().getId());
            adj.get(s.getToNode().getId()).add(s.getFromNode().getId());
        }

        // BFS from Guwahati
        RoadNode guwahati = roadNodeRepository.findByName("Guwahati").orElseThrow();
        Set<Long> visited = new HashSet<>();
        Queue<Long> queue = new LinkedList<>();

        queue.add(guwahati.getId());
        visited.add(guwahati.getId());

        while (!queue.isEmpty()) {
            Long current = queue.poll();
            for (Long neighbor : adj.getOrDefault(current, Collections.emptyList())) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        assertThat(visited.size()).isEqualTo(allNodes.size());
    }

    @Test
    void testTwoDistinctGuwahatiToImphalPaths() {
        List<RoadSegment> allSegments = roadSegmentRepository.findAll();
        RoadNode guwahati = roadNodeRepository.findByName("Guwahati").orElseThrow();
        RoadNode imphal = roadNodeRepository.findByName("Imphal").orElseThrow();

        Map<Long, List<Long>> adj = new HashMap<>();
        for (RoadSegment s : allSegments) {
            adj.computeIfAbsent(s.getFromNode().getId(), k -> new ArrayList<>()).add(s.getToNode().getId());
            adj.computeIfAbsent(s.getToNode().getId(), k -> new ArrayList<>()).add(s.getFromNode().getId());
        }

        // Find all simple paths from Guwahati to Imphal using DFS (up to max depth 10)
        List<List<Long>> paths = new ArrayList<>();
        findPaths(guwahati.getId(), imphal.getId(), new HashSet<>(), new ArrayList<>(List.of(guwahati.getId())), adj, paths, 10);

        assertThat(paths.size()).isGreaterThanOrEqualTo(2);

        // Verify we have at least two distinct paths (sharing at most origin and destination or disjoint intermediate nodes)
        boolean hasTwoDistinct = false;
        for (int i = 0; i < paths.size(); i++) {
            for (int j = i + 1; j < paths.size(); j++) {
                Set<Long> p1Inter = new HashSet<>(paths.get(i));
                p1Inter.remove(guwahati.getId());
                p1Inter.remove(imphal.getId());

                Set<Long> p2Inter = new HashSet<>(paths.get(j));
                p2Inter.remove(guwahati.getId());
                p2Inter.remove(imphal.getId());

                // Check if they don't share all intermediate nodes
                if (!p1Inter.equals(p2Inter)) {
                    hasTwoDistinct = true;
                    break;
                }
            }
            if (hasTwoDistinct) break;
        }

        assertThat(hasTwoDistinct).isTrue();
    }

    private void findPaths(Long current, Long target, Set<Long> visited, List<Long> currentPath, Map<Long, List<Long>> adj, List<List<Long>> result, int maxDepth) {
        if (currentPath.size() > maxDepth) return;
        if (current.equals(target)) {
            result.add(new ArrayList<>(currentPath));
            return;
        }

        visited.add(current);
        for (Long neighbor : adj.getOrDefault(current, Collections.emptyList())) {
            if (!visited.contains(neighbor)) {
                currentPath.add(neighbor);
                findPaths(neighbor, target, visited, currentPath, adj, result, maxDepth);
                currentPath.remove(currentPath.size() - 1);
            }
        }
        visited.remove(current);
    }
}
