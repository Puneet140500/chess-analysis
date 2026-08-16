package com.chess.analysis.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EcoService {

    public record Opening(String eco, String name, String moves) {}

    private List<Opening> all = new ArrayList<>();

    @PostConstruct
    public void load() {
        try (var is = getClass().getResourceAsStream("/openings/eco.tsv");
             var reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t", 3);
                if (parts.length == 3) {
                    all.add(new Opening(parts[0].trim(), parts[1].trim(), parts[2].trim()));
                }
            }
            log.info("Loaded {} ECO openings", all.size());
        } catch (Exception e) {
            log.error("Failed to load ECO openings", e);
        }
    }

    public List<Opening> search(String query, String eco, int limit) {
        return all.stream()
                .filter(o -> eco == null || eco.isBlank() || o.eco().startsWith(eco.toUpperCase()))
                .filter(o -> {
                    if (query == null || query.isBlank()) return true;
                    String q = query.toLowerCase();
                    return o.name().toLowerCase().contains(q) || o.eco().toLowerCase().contains(q);
                })
                .limit(limit)
                .collect(Collectors.toList());
    }
}
