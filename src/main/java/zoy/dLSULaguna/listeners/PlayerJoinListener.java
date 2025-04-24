package zoy.dLSULaguna.listeners;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import zoy.dLSULaguna.DLSULaguna;
import zoy.dLSULaguna.utils.PlayerStatsFileUtil;
import zoy.dLSULaguna.utils.Section;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PlayerJoinListener implements Listener {
    private final NamespacedKey sectionKey;
    private final NamespacedKey afkKey;
    private final Map<String, Section> sectionNameMap = new HashMap<>();

    public PlayerJoinListener(DLSULaguna plugin) {
        this.sectionKey = new NamespacedKey(plugin, "section_name");
        this.afkKey = new NamespacedKey(plugin, "AFK");
        // Initialize the section name mapping for migration
        sectionNameMap.put("A", Section.STEM11_A);
        sectionNameMap.put("B", Section.STEM11_B);
        sectionNameMap.put("C", Section.STEM11_C);
        sectionNameMap.put("D", Section.STEM11_D);
        sectionNameMap.put("E", Section.STEM11_E);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        PersistentDataContainer playerData = player.getPersistentDataContainer();
        playerData.set(afkKey, PersistentDataType.BOOLEAN, false); // Is this ok?

        String oldSection = playerData.get(sectionKey, PersistentDataType.STRING);
        Optional<Section> maybeNewSection = Optional.empty();

        if (oldSection != null && sectionNameMap.containsKey(oldSection.toUpperCase())) {
            final var newSection = sectionNameMap.get(oldSection.toUpperCase());
            maybeNewSection = Optional.of(newSection);
            // We can make it save a persistent version of [Section] later on.
            playerData.set(sectionKey, PersistentDataType.STRING, maybeNewSection.toString()); // Update the persistent
                                                                                               // data
            player.sendMessage("Your section has been automatically updated to " + newSection + "!");
        }
        // else if (oldSection != null) {
        // newSection = oldSection; // Keep the existing (potentially already updated)
        // section
        // player.sendMessage("You are in the section " + newSection);
        // }
        else {
            player.sendMessage("You are not in a section");
        }

        PlayerStatsFileUtil.setStat(player, "Last Log-in",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, HH:mm:ss a")));
        PlayerStatsFileUtil.setStat(player, "Username", player.getName());

        if (!player.hasPlayedBefore()) {
            player.sendMessage(
                    "§a§lWelcome to the server, §b§lSTEM Students§a§l of §2§lDe La Salle University Laguna§a§l!");
            return; // I added this missing early return?
        }

        if (maybeNewSection.isEmpty()) {
            player.sendMessage("§7Please §e/joinsection <SECTION> §7to get started and be part of a section.");
            return;
        }

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Team team = board.getTeam(maybeNewSection.toString());
        if (team == null && maybeNewSection != null) {
            team = board.registerNewTeam(maybeNewSection.toString());
        }
        if (team != null) {
            team.addEntry(player.getName());
            player.setScoreboard(board);
        } else {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard()); // Set default if no section
        }
    }
}