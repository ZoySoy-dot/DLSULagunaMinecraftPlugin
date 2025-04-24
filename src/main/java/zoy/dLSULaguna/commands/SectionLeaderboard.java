package zoy.dLSULaguna.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import zoy.dLSULaguna.DLSULaguna;
import zoy.dLSULaguna.utils.Section;

import java.io.File;
import java.util.*;

public class SectionLeaderboard implements CommandExecutor {
    private final DLSULaguna plugin;

    public SectionLeaderboard(DLSULaguna plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage("§cUsage: /sectionleaderboard <section>");
            return true;
        }

        final var askedSection = args[0].toUpperCase();
        final var maybeSection = Section.fromString(askedSection);

        if (maybeSection.isEmpty()) {
            sender.sendMessage("§cSection not found: " + askedSection);
            return true;
        }

        File playerStatsFile = new File(plugin.getDataFolder(), "players_stats.yml");
        FileConfiguration playerData = YamlConfiguration.loadConfiguration(playerStatsFile);

        if (!playerData.contains(askedSection)) {
            sender.sendMessage("§cSection not found: " + askedSection);
            return true;
        }

        ConfigurationSection sectionData = playerData.getConfigurationSection(askedSection);
        if (sectionData == null) {
            sender.sendMessage("§cNo player data in that section.");
            return true;
        }

        // Collect UUIDs and points
        Map<String, Integer> pointsMap = new HashMap<>();
        for (String uuid : sectionData.getKeys(false)) {
            int points = sectionData.getInt(uuid + ".Points");
            pointsMap.put(uuid, points);
        }

        // Sort entries by points descending
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(pointsMap.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        // Send leaderboard
        sender.sendMessage("§6Top players in STEM11-§e" + askedSection + "§6:");
        int rank = 1;
        for (Map.Entry<String, Integer> entry : sorted) {
            String playerName = Bukkit.getOfflinePlayer(UUID.fromString(entry.getKey())).getName();
            sender.sendMessage("§7#" + rank + " §a" + (playerName != null ? playerName : entry.getKey()) + " §8- §b"
                    + entry.getValue() + " pts");
            rank++;
        }

        return true;
    }
}
