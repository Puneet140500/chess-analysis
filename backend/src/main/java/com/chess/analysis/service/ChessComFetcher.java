package com.chess.analysis.service;

import com.chess.analysis.model.Game;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

// Fetches games from chess.com public API (no auth required for public profiles)
// API docs: https://www.chess.com/news/view/published-data-api
@Slf4j
@Service
public class ChessComFetcher implements GameFetcher {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public ChessComFetcher(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${chesscom.api.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
    }

    @Override
    public List<Game> getRecentGames(String username, int limit) {
        try {
            // Step 1: Get the current month's archive URL
            // chess.com organizes games by month: /pub/player/{user}/games/{year}/{month}
            String archivesUrl = baseUrl + "/player/" + username + "/games/archives";
            String archivesJson = restTemplate.getForObject(archivesUrl, String.class);

            JsonNode archivesNode = objectMapper.readTree(archivesJson);
            JsonNode archives = archivesNode.get("archives");

            if (archives == null || archives.isEmpty()) {
                return List.of();
            }

            // Get the most recent archive (last in array)
            String latestArchiveUrl = archives.get(archives.size() - 1).asText();

            // Step 2: Fetch games from that month
            String gamesJson = restTemplate.getForObject(latestArchiveUrl, String.class);
            JsonNode gamesNode = objectMapper.readTree(gamesJson);
            JsonNode games = gamesNode.get("games");

            if (games == null || games.isEmpty()) {
                return List.of();
            }

            List<Game> result = new ArrayList<>();

            // Iterate from the end (most recent first), take up to limit
            for (int i = games.size() - 1; i >= 0 && result.size() < limit; i--) {
                JsonNode gameNode = games.get(i);
                try {
                    result.add(parseGame(gameNode));
                } catch (Exception e) {
                    log.warn("Skipping unparseable game: {}", e.getMessage());
                }
            }

            return result;

        } catch (Exception e) {
            log.error("Failed to fetch games for user: {}", username, e);
            throw new RuntimeException("Could not fetch games from chess.com for: " + username, e);
        }
    }

    private Game parseGame(JsonNode node) {
        String url = node.path("url").asText("");
        // chess.com URLs end with the game ID: /game/live/12345678
        String gameId = url.substring(url.lastIndexOf('/') + 1);

        return Game.builder()
                .gameId(gameId)
                .url(url)
                .pgn(node.path("pgn").asText(""))
                .result(parseResult(node))
                .timeControl(node.path("time_control").asText(""))
                .whitePlayer(node.path("white").path("username").asText(""))
                .blackPlayer(node.path("black").path("username").asText(""))
                .endTime(node.path("end_time").asLong(0))
                .build();
    }

    private String parseResult(JsonNode node) {
        String whiteResult = node.path("white").path("result").asText("");
        return switch (whiteResult) {
            case "win" -> "1-0";
            case "checkmated", "resigned", "timeout", "abandoned" -> "0-1";
            default -> "1/2-1/2";
        };
    }
}
