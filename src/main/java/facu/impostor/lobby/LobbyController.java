package facu.impostor.lobby;

import facu.impostor.words.CategoryRepository;
import facu.impostor.words.Word;
import facu.impostor.words.WordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/lobby")
public class LobbyController {
    private final LobbyService lobbyService;
    private final WordRepository wordRepository;
    private final CategoryRepository categoryRepository;

    @Autowired
    public LobbyController(LobbyService lobbyService, WordRepository wordRepository, CategoryRepository categoryRepository) {
        this.lobbyService = lobbyService;
        this.wordRepository = wordRepository;
        this.categoryRepository = categoryRepository;
        lobbyService.setWordRepository(wordRepository);
    }

    // Crear lobby
    @PostMapping("/create")
    public Map<String, Object> createLobby(@RequestBody CreateLobbyRequest req) {
        LobbyService.Lobby lobby = lobbyService.createLobby(req.hostName, req.impostorCount, req.categories);
        LobbyService.Player host = lobby.join(req.hostName);
        Map<String, Object> resp = new HashMap<>();
        resp.put("lobbyId", lobby.id);
        resp.put("playerId", host.id);
        resp.put("hostToken", lobby.hostToken);
        return resp;
    }

    // Listar lobbys
    @GetMapping("/list")
    public List<LobbyService.LobbySummary> listLobbies() {
        return lobbyService.listLobbies();
    }

    // Unirse a lobby
    @PostMapping("/join")
    public Map<String, Object> joinLobby(@RequestBody JoinLobbyRequest req) {
        LobbyService.Player player = lobbyService.joinLobby(req.lobbyId, req.playerName);
        Map<String, Object> resp = new HashMap<>();
        resp.put("playerId", player.id);
        return resp;
    }

    // Estado de lobby
    @GetMapping("/state")
    public Map<String, Object> getLobbyState(@RequestParam String lobbyId) {
        LobbyService.Lobby lobby = lobbyService.getLobby(lobbyId);
        if (lobby == null) throw new RuntimeException("Lobby no encontrado");
        Map<String, Object> resp = new HashMap<>();
        resp.put("hostName", lobby.hostName);
        List<Map<String, Object>> players = new ArrayList<>();
        for (LobbyService.Player p : lobby.players.values()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.id);
            m.put("name", p.name);
            players.add(m);
        }
        resp.put("players", players);
        resp.put("currentRound", lobby.currentRound == null ? null : Map.of(
            "roundNumber", lobby.currentRound.number
        ));
        return resp;
    }

    // Iniciar ronda
    @PostMapping("/round/start")
    public Map<String, Object> startRound(@RequestHeader(value = "X-Host-Token", required = false) String hostToken, @RequestBody StartRoundRequest req) {
        LobbyService.Lobby lobby = lobbyService.getLobby(req.lobbyId);
        if (lobby == null) throw new RuntimeException("Lobby no encontrado");
        if (hostToken == null || !hostToken.equals(lobby.hostToken)) throw new RuntimeException("No autorizado");
        LobbyService.Round round = lobby.startNewRound();
        return Map.of("ok", true, "roundNumber", round.number);
    }

    // Obtener palabra/rol de jugador
    @GetMapping("/me/{playerId}")
    public Map<String, Object> getPlayerSecret(@PathVariable String playerId, @RequestParam String lobbyId) {
        LobbyService.Lobby lobby = lobbyService.getLobby(lobbyId);
        if (lobby == null) throw new RuntimeException("Lobby no encontrado");
        LobbyService.Player player = lobby.getPlayer(playerId);
        if (player == null) throw new RuntimeException("Jugador no encontrado");
        LobbyService.Round round = lobby.getCurrentRound();
        if (round == null) return Map.of("secret", "");
        boolean isImpostor = round.impostors.contains(playerId);
        String secret;
        if (round.allImpostors) {
            secret = "IMPOSTOR";
        } else if (round.allKnow) {
            secret = round.word.getText();
        } else {
            secret = isImpostor ? "IMPOSTOR" : round.word.getText();
        }
        return Map.of("secret", secret);
    }

    // Reingreso por URL: /partida/{lobbyId}/{playerId} (puede usarse desde el frontend)
    @GetMapping("/rejoin")
    public Map<String, Object> rejoin(@RequestParam String lobbyId, @RequestParam String playerId) {
        LobbyService.Lobby lobby = lobbyService.getLobby(lobbyId);
        if (lobby == null) throw new RuntimeException("Lobby no encontrado");
        LobbyService.Player player = lobby.getPlayer(playerId);
        if (player == null) throw new RuntimeException("Jugador no encontrado");
        Map<String, Object> resp = new HashMap<>();
        resp.put("lobbyId", lobbyId);
        resp.put("playerId", playerId);
        resp.put("playerName", player.name);
        resp.put("hostName", lobby.hostName);
        resp.put("isHost", player.name.equalsIgnoreCase(lobby.hostName));
        resp.put("players", lobby.players.values());
        resp.put("currentRound", lobby.currentRound == null ? null : Map.of(
            "roundNumber", lobby.currentRound.number
        ));
        return resp;
    }

    // DTOs
    public static class CreateLobbyRequest {
        public String hostName;
        public int impostorCount;
        public List<Long> categories;
    }
    public static class JoinLobbyRequest {
        public String lobbyId;
        public String playerName;
    }
    public static class StartRoundRequest {
        public String lobbyId;
    }
}
