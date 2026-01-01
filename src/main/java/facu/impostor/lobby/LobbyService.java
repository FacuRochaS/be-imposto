package facu.impostor.lobby;

import facu.impostor.words.Word;
import facu.impostor.words.WordRepository;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class LobbyService {
    private final Map<String, Lobby> lobbies = new HashMap<>();
    private WordRepository wordRepository;

    public void setWordRepository(WordRepository wordRepository) {
        this.wordRepository = wordRepository;
    }

    public Lobby createLobby(String hostName, int impostorCount, List<Long> categoryIds) {
        // Elimina todas las lobbys cuyo host tenga el mismo nombre (case-insensitive)
        lobbies.values().removeIf(lobby -> lobby.hostName.equalsIgnoreCase(hostName));
        String lobbyId = UUID.randomUUID().toString();
        Lobby lobby = new Lobby(lobbyId, hostName, impostorCount, categoryIds, wordRepository);
        lobbies.put(lobbyId, lobby);
        return lobby;
    }

    public List<LobbySummary> listLobbies() {
        List<LobbySummary> list = new ArrayList<>();
        for (Lobby lobby : lobbies.values()) {
            list.add(new LobbySummary(lobby.id, lobby.hostName, lobby.players.size()));
        }
        return list;
    }

    public Lobby getLobby(String lobbyId) {
        return lobbies.get(lobbyId);
    }

    public Player joinLobby(String lobbyId, String playerName) {
        Lobby lobby = lobbies.get(lobbyId);
        if (lobby == null) throw new RuntimeException("Lobby no encontrado");
        return lobby.join(playerName);
    }

    public void removePlayer(String lobbyId, String playerId) {
        Lobby lobby = lobbies.get(lobbyId);
        if (lobby != null) lobby.players.remove(playerId);
    }

    public static class LobbySummary {
        public String id;
        public String hostName;
        public int playerCount;
        public LobbySummary(String id, String hostName, int playerCount) {
            this.id = id;
            this.hostName = hostName;
            this.playerCount = playerCount;
        }
    }

    public static class Lobby {
        public final String id;
        public final String hostName;
        public final Map<String, Player> players = new HashMap<>();
        public final Set<Long> usedWordIds = new HashSet<>();
        public final List<Long> activeCategoryIds;
        public final int impostorCount;
        public final WordRepository wordRepository;
        public Round currentRound;
        public int roundNumber = 0;
        public String hostToken;
        public Lobby(String id, String hostName, int impostorCount, List<Long> categoryIds, WordRepository wordRepository) {
            this.id = id;
            this.hostName = hostName;
            this.impostorCount = impostorCount;
            this.activeCategoryIds = new ArrayList<>(categoryIds);
            this.wordRepository = wordRepository;
            this.hostToken = UUID.randomUUID().toString();
        }
        public Player join(String name) {
            for (Player p : players.values()) {
                if (p.name.equalsIgnoreCase(name)) return p;
            }
            String playerId = UUID.randomUUID().toString();
            Player player = new Player(playerId, name);
            players.put(playerId, player);
            return player;
        }
        public Word pickRandomWord() {
            List<Word> candidates = wordRepository.findAll().stream()
                .filter(w -> w.getCategory() != null && activeCategoryIds.contains(w.getCategory().getId()))
                .filter(w -> !usedWordIds.contains(w.getId()))
                .toList();
            if (candidates.isEmpty()) {
                usedWordIds.clear();
                candidates = wordRepository.findAll().stream()
                    .filter(w -> w.getCategory() != null && activeCategoryIds.contains(w.getCategory().getId()))
                    .toList();
            }
            if (candidates.isEmpty()) return null;
            Word word = candidates.get(new Random().nextInt(candidates.size()));
            usedWordIds.add(word.getId());
            return word;
        }
        public Round startNewRound() {
            roundNumber++;
            Set<String> allPlayers = new HashSet<>(players.keySet());
            Set<String> impostors = new HashSet<>();
            boolean allImpostors = false;
            boolean allKnow = false;
            int roll = new Random().nextInt(100);
            if (roll < 5) {
                allImpostors = true;
                impostors.addAll(allPlayers);
            } else if (roll < 10) {
                allKnow = true;
            } else {
                List<String> playerList = new ArrayList<>(allPlayers);
                Collections.shuffle(playerList);
                for (int i = 0; i < Math.min(impostorCount, playerList.size()); i++) {
                    impostors.add(playerList.get(i));
                }
            }
            Word word = pickRandomWord();
            if (word == null) throw new RuntimeException("No hay palabras disponibles en las categorías seleccionadas");
            currentRound = new Round(roundNumber, word, impostors, allImpostors, allKnow);
            return currentRound;
        }
        public Round getCurrentRound() { return currentRound; }
        public Player getPlayer(String playerId) { return players.get(playerId); }
    }

    public static class Player {
        public final String id;
        public final String name;
        public Player(String id, String name) { this.id = id; this.name = name; }
    }
    public static class Round {
        public final int number;
        public final Word word;
        public final Set<String> impostors;
        public final boolean allImpostors;
        public final boolean allKnow;
        public Round(int number, Word word, Set<String> impostors, boolean allImpostors, boolean allKnow) {
            this.number = number;
            this.word = word;
            this.impostors = impostors;
            this.allImpostors = allImpostors;
            this.allKnow = allKnow;
        }
    }
}
