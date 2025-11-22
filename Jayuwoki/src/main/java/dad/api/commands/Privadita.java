package dad.api.commands;

import dad.database.Player;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.*;

public class Privadita {

  // Attributes
  private MessageReceivedEvent event;
  private ListProperty<Player> players = new SimpleListProperty<>(FXCollections.observableArrayList());
  private StringProperty server = new SimpleStringProperty();
  private final ArrayList<String> roles = new ArrayList<>(List.of("Top", "Jungla", "Mid", "ADC", "Support"));

  // Helper class to store team split
  private static class TeamSplit {
    List<Player> blueTeam;
    List<Player> redTeam;

    TeamSplit(List<Player> blueTeam, List<Player> redTeam) {
      this.blueTeam = blueTeam;
      this.redTeam = redTeam;
    }
  }

  public Privadita(List<Player> players, MessageReceivedEvent event) {
    this.server.set(event.getGuild().getName());
    this.event = event;

    // Validate and fill this.players
    if (!CheckPrivaditaCommand(players, event)) {
      // Invalid command or players, do not start privadita
      return;
    }

    // Start the match setup
    StartPrivadita(event);
  }

  // java
  private void StartPrivadita(MessageReceivedEvent event) {
    Random rand = new Random();
    int privaditaEspecial = rand.nextInt(21); // 0..20
    System.out.println("Número aleatorio para privadita especial: " + privaditaEspecial);

    ObservableList<Player> playerList = this.players.get();

    if (playerList == null || playerList.size() != 10) {
      event.getChannel().sendMessage("Necesito exactamente 10 jugadores para hacer equipos balanceados.").queue();
      return;
    }

    List<Player> baseList = new ArrayList<>(playerList);
    TeamSplit split = createBalancedTeams(baseList);
    List<Player> blueTeam = split.blueTeam;
    List<Player> redTeam = split.redTeam;

    Collections.shuffle(blueTeam);
    Collections.shuffle(redTeam);

    // Prepara roles locales sin tocar `this.roles`
    List<String> baseRoles = new ArrayList<>(roles); // copia segura
    int roleCount = baseRoles.size(); // esperado 5

    List<String> blueRoles = new ArrayList<>(Collections.nCopies(5, ""));
    List<String> redRoles = new ArrayList<>(Collections.nCopies(5, ""));

    if (privaditaEspecial == 20) {
      // Caso especial: no asignar roles explícitos
      for (int i = 0; i < 5; i++) {
        blueRoles.set(i, "");
        redRoles.set(i, "");
      }
      // print special message to chat
        event.getChannel().sendMessage("\uD83D\uDD25 Freemolly labubu ayiyi ahora no tienen excusa").queue();
    } else if (privaditaEspecial >= 15) {
      // 15..19: un jugador por equipo recibe '*' en su rol
      int blueSpecialIndex = rand.nextInt(5);
      int redSpecialIndex = rand.nextInt(5);
      for (int i = 0; i < 5; i++) {
        String base = baseRoles.get(i % roleCount);
        blueRoles.set(i, (i == blueSpecialIndex) ? base + "*" : base);
        redRoles.set(i, (i == redSpecialIndex) ? base + "*" : base);
      }
      //print to chat who are the special players
        event.getChannel().sendMessage("\uD83D\uDD25 Atención: " + blueTeam.get(blueSpecialIndex).getName() + " y " + redTeam.get(redSpecialIndex).getName() + " pueden cambiar su rol con cualquier miembro del equipo (*)").queue();

    } else {
      // Caso normal: roles por defecto
      for (int i = 0; i < 5; i++) {
        String base = baseRoles.get(i % roleCount);
        blueRoles.set(i, base);
        redRoles.set(i, base);
      }
    }

    // Asignar roles finales (una sola vez)
    for (int i = 0; i < 5; i++) {
      blueTeam.get(i).setRole(blueRoles.get(i));
      redTeam.get(i).setRole(redRoles.get(i));
    }

    // Rebuild internal players list (0-4 Blue, 5-9 Red)
    ObservableList<Player> orderedPlayers = FXCollections.observableArrayList();
    orderedPlayers.addAll(blueTeam);
    orderedPlayers.addAll(redTeam);
    this.players.set(orderedPlayers);

    // Calcular promedios y enviar mensaje (igual que antes)
    int blueTotalElo = blueTeam.stream().mapToInt(Player::getElo).sum();
    int redTotalElo = redTeam.stream().mapToInt(Player::getElo).sum();
    double blueAvgElo = blueTotalElo / (double) blueTeam.size();
    double redAvgElo = redTotalElo / (double) redTeam.size();

    StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append("```")
            .append("\nBlue Team (avg Elo: ")
            .append(String.format(java.util.Locale.US, "%.1f", blueAvgElo))
            .append(")\n");

    for (int i = 0; i < 5; i++) {
      Player player = this.players.get(i);
      messageBuilder.append(player.getName())
              .append(" (Elo: ").append(player.getElo()).append(")")
              .append(" -> ")
              .append(player.getRole())
              .append("\n");
    }

    messageBuilder.append("\nRed Team (avg Elo: ")
            .append(String.format(java.util.Locale.US, "%.1f", redAvgElo))
            .append(")\n");

    for (int i = 5; i < 10; i++) {
      Player player = this.players.get(i);
      messageBuilder.append(player.getName())
              .append(" (Elo: ").append(player.getElo()).append(")")
              .append(" -> ")
              .append(player.getRole())
              .append("\n");
    }

    messageBuilder.append("```");
    event.getChannel().sendMessage(messageBuilder.toString()).queue();
  }


  // Brute-force optimal split by Elo
  private TeamSplit createBalancedTeams(List<Player> playerList) {
    int n = playerList.size(); // should be 10
    int totalElo = playerList.stream().mapToInt(Player::getElo).sum();

    int bestMask = 0;
    int bestDiff = Integer.MAX_VALUE;

    int limit = 1 << n; // 2^n

    for (int mask = 0; mask < limit; mask++) {
      // We only want teams of size n/2 (5 players)
      if (Integer.bitCount(mask) != n / 2) {
        continue;
      }

      int eloTeamA = 0;
      for (int i = 0; i < n; i++) {
        if ((mask & (1 << i)) != 0) {
          eloTeamA += playerList.get(i).getElo();
        }
      }

      int eloTeamB = totalElo - eloTeamA;
      int diff = Math.abs(eloTeamA - eloTeamB);

      if (diff < bestDiff) {
        bestDiff = diff;
        bestMask = mask;

        // Early exit if perfect balance
        if (diff == 0) {
          break;
        }
      }
    }

    List<Player> blueTeam = new ArrayList<>();
    List<Player> redTeam = new ArrayList<>();

    for (int i = 0; i < n; i++) {
      if ((bestMask & (1 << i)) != 0) {
        blueTeam.add(playerList.get(i));
      } else {
        redTeam.add(playerList.get(i));
      }
    }

    return new TeamSplit(blueTeam, redTeam);
  }

  // Function to check the command and fill the player list
  private boolean CheckPrivaditaCommand(List<Player> playerList, MessageReceivedEvent event) {
    Set<String> uniqueNames = new HashSet<>();

    for (Player player : playerList) {
      String name = player.getName();

      // Check duplicated names
      if (!uniqueNames.add(name)) {
        event.getChannel().sendMessage(name + " está repetido, prueba otra vez.").queue();
        return false;
      }

      // Check trolling names
      if (name.startsWith("$")) {
        event.getChannel().sendMessage("A donde vas listillo.").queue();
        return false;
      }

      this.players.add(player);
    }

    return true;
  }

  public void ResultadoPrivadita(String ganador, MessageReceivedEvent event) {
    System.out.println("Elo de todos los jugadores:");
    for (Player p : players) {
      System.out.println(p.getName() + " - Elo: " + p.getElo());
    }

    double averageEloEquipo1 = players.subList(0, 5).stream()
            .mapToInt(Player::getElo)
            .average()
            .orElse(0);

    double averageEloEquipo2 = players.subList(5, 10).stream()
            .mapToInt(Player::getElo)
            .average()
            .orElse(0);

    StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append("Resultado de la partida:\n");

    switch (ganador) {
      case "blue":
        messageBuilder.append("\n🏆 **Equipo Azul ha ganado!** 🏆\n");
        break;
      case "red":
        messageBuilder.append("\n🔥 **Equipo Rojo ha ganado!** 🔥\n");
        break;
      default:
        event.getChannel().sendMessage("Pon el equipo bien tolete").queue();
        return;
    }

    messageBuilder.append("```\n**Cambios de Elo:**\n");

    for (int i = 0; i < 5; i++) {
      if (ganador.equals("blue")) {
        players.get(i).setWins(players.get(i).getWins() + 1);
        players.get(i + 5).setLosses(players.get(i + 5).getLosses() + 1);
        int oldEloWin = players.get(i).getElo();
        int oldEloLose = players.get(i + 5).getElo();
        players.get(i).ActualizarElo(averageEloEquipo2, true);
        players.get(i + 5).ActualizarElo(averageEloEquipo1, false);

        messageBuilder.append(players.get(i).getName()).append(": ")
                .append(oldEloWin).append(" ➝ ").append(players.get(i).getElo())
                .append(" (+").append(players.get(i).getElo() - oldEloWin).append(")\n");

        messageBuilder.append(players.get(i + 5).getName()).append(": ")
                .append(oldEloLose).append(" ➝ ").append(players.get(i + 5).getElo())
                .append(" (").append(players.get(i + 5).getElo() - oldEloLose).append(")\n");
      } else {
        players.get(i + 5).setWins(players.get(i + 5).getWins() + 1);
        players.get(i).setLosses(players.get(i).getLosses() + 1);
        int oldEloWin = players.get(i + 5).getElo();
        int oldEloLose = players.get(i).getElo();
        players.get(i + 5).ActualizarElo(averageEloEquipo1, true);
        players.get(i).ActualizarElo(averageEloEquipo2, false);

        messageBuilder.append(players.get(i + 5).getName()).append(": ")
                .append(oldEloWin).append(" ➝ ").append(players.get(i + 5).getElo())
                .append(" (+").append(players.get(i + 5).getElo() - oldEloWin).append(")\n");

        messageBuilder.append(players.get(i).getName()).append(": ")
                .append(oldEloLose).append(" ➝ ").append(players.get(i).getElo())
                .append(" (").append(players.get(i).getElo() - oldEloLose).append(")\n");
      }
    }

    messageBuilder.append("```");
    event.getChannel().sendMessage(messageBuilder.toString()).queue();
  }

  public MessageReceivedEvent getEvent() {
    return event;
  }

  public void setEvent(MessageReceivedEvent event) {
    this.event = event;
  }

  public ObservableList<Player> getPlayers() {
    return players.get();
  }

  public ListProperty<Player> playersProperty() {
    return players;
  }

  public void setPlayers(ObservableList<Player> players) {
    this.players.set(players);
  }

  public String getServer() {
    return server.get();
  }

  public StringProperty serverProperty() {
    return server;
  }

  public void setServer(String server) {
    this.server.set(server);
  }

  public ArrayList<String> getRoles() {
    return roles;
  }
}
