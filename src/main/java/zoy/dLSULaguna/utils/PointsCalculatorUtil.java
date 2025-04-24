package zoy.dLSULaguna.utils;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import zoy.dLSULaguna.DLSULaguna;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class PointsCalculatorUtil {

    private static DLSULaguna plugin;

    public static void initialize(DLSULaguna pluginInstance) {
        plugin = pluginInstance;
    }

    public static void calculateAllPlayerPoints() {
        if (plugin == null) {
            System.err.println("PointsCalculatorUtil not initialized!");
            return;
        }

        File playersStatsFile = plugin.getPlayersStatsFile();
        if (playersStatsFile == null || !playersStatsFile.exists()) {
            plugin.getLogger().warning(
                    "Cannot calculate player points: players_stats.yml does not exist or file object is null.");
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(playersStatsFile);
        plugin.getLogger().info("Calculating points contribution for **online** players...");
        boolean changesMade = false;

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerUUID = player.getUniqueId();
            String foundSectionPath = null;

            for (String sectionKey : config.getKeys(false)) {
                ConfigurationSection sectionData = config.getConfigurationSection(sectionKey);
                if (sectionData == null)
                    continue;

                if (sectionData.contains(playerUUID.toString())) {
                    foundSectionPath = sectionKey + "." + playerUUID.toString();
                    break;
                }
            }

            if (foundSectionPath == null) {
                plugin.getLogger().warning("Player " + player.getName() + " not found in players_stats.yml.");
                continue;
            }

            ConfigurationSection playerConfig = config.getConfigurationSection(foundSectionPath);
            if (playerConfig == null)
                continue;

            int totalPoints = 0;
            ConfigurationSection pointsDistribution = playerConfig.getConfigurationSection("PointsDistribution");
            if (pointsDistribution == null) {
                pointsDistribution = playerConfig.createSection("PointsDistribution");
            } else {
                for (String key : pointsDistribution.getKeys(false)) {
                    pointsDistribution.set(key, null);
                }
            }

            for (String statKey : playerConfig.getKeys(false)) {
                if (statKey.equalsIgnoreCase("Points") || statKey.equalsIgnoreCase("Username")
                        || statKey.equalsIgnoreCase("Last Log-in") || statKey.equalsIgnoreCase("PointsDistribution")
                        || playerConfig.isConfigurationSection(statKey) || playerConfig.isList(statKey)) {
                    continue;
                }

                Object actualValue = playerConfig.get(statKey);
                int pointsFromStat = 0;

                switch (statKey) {
                    case "Duel-points":
                    case "Bounty-points":
                        pointsFromStat = playerConfig.getInt(statKey, 0);
                        break;
                    case "Blocks broken":
                    case "Blocks placed":
                        pointsFromStat = (int) Math.floor(playerConfig.getInt(statKey, 0) / 300.0);
                        break;
                    case "Kills":
                    case "Mobs Killed":
                        pointsFromStat = (int) Math.floor(playerConfig.getInt(statKey, 0) / 100.0);
                        break;
                    case "Deaths":
                        pointsFromStat = -(int) Math.floor(playerConfig.getInt(statKey, 0) / 3.0);
                        break;
                    case "Distance":
                        pointsFromStat = (int) Math.floor(playerConfig.getDouble(statKey, 0.0) / 1000.0);
                        break;
                    case "Time Logged In":
                        pointsFromStat = (int) Math.floor(playerConfig.getLong(statKey, 0L) / 60.0);
                        break;
                    case "Fish Caught":
                        pointsFromStat = (int) Math.floor(playerConfig.getInt(statKey, 0) / 10.0);
                        break;
                    case "Items Crafted":
                        pointsFromStat = (int) Math.floor(playerConfig.getInt(statKey, 0) / 100.0);
                        break;
                    case "Trades Made":
                        pointsFromStat = (int) Math.floor(playerConfig.getInt(statKey, 0) / 10.0);
                        break;
                }

                if (pointsFromStat != 0) {
                    String actualValueStr = (actualValue != null) ? actualValue.toString() : "0";
                    pointsDistribution.set(statKey, pointsFromStat + " pts (from " + actualValueStr + ")");
                }

                totalPoints += pointsFromStat;
            }

            playerConfig.set("Points", totalPoints);
            config.set(foundSectionPath + ".PointsDistribution", pointsDistribution);
            plugin.getLogger()
                    .finer("Updated distribution for " + player.getName() + ": " + pointsDistribution.getValues(false));
            plugin.getLogger().finest("Updated total points for " + player.getName() + ": " + totalPoints);
            changesMade = true;
        }

        if (changesMade) {
            try {
                config.save(plugin.getPlayersStatsFile());
                plugin.getLogger()
                        .info("Online player points calculation complete. Saved changes to players_stats.yml.");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE,
                        "Failed to save players_stats.yml after calculating online player points.", e);
            }
        } else {
            plugin.getLogger().info("Online player points calculation complete. No changes detected.");
        }
    }

    public static void calculateAllSectionPoints() {
        if (plugin == null) {
            System.err.println("PointsCalculatorUtil not initialized!");
            return;
        }

        File playersStatsFile = plugin.getPlayersStatsFile();
        if (playersStatsFile == null || !playersStatsFile.exists()) {
            plugin.getLogger().warning(
                    "Cannot calculate section points: players_stats.yml does not exist or file object is null.");
            return;
        }

        FileConfiguration playersConfig = YamlConfiguration.loadConfiguration(playersStatsFile);
        plugin.getLogger().info("Calculating total points for each section...");
        boolean changesMade = false;
        Map<Section, Integer> sectionTotals = new HashMap<>();

        for (String sectionKey : playersConfig.getKeys(false)) {
            ConfigurationSection sectionPlayerData = playersConfig.getConfigurationSection(sectionKey);
            if (sectionPlayerData == null)
                continue;

            int currentSectionTotal = 0;
            for (String uuid : sectionPlayerData.getKeys(false)) {
                String playerPointsPath = sectionKey + "." + uuid + ".Points";
                if (playersConfig.contains(playerPointsPath)) {
                    currentSectionTotal += playersConfig.getInt(playerPointsPath, 0);
                }
            }

            /* orElseThrow is a bit of a hack */
            sectionTotals.put(Section.fromString(sectionKey).orElseThrow(), currentSectionTotal);
            plugin.getLogger().finer("Calculated total points for section " + sectionKey + ": " + currentSectionTotal);
        }

        for (Map.Entry<Section, Integer> entry : sectionTotals.entrySet()) {
            Section section = entry.getKey();
            int newTotal = entry.getValue();
            int oldTotal = SectionStatsFileUtil.getStatInt(section, "Points", 0);

            if (oldTotal != newTotal) {
                SectionStatsFileUtil.setPoints(section, newTotal);
                changesMade = true;
            }
        }

        if (changesMade) {
            plugin.getLogger().info("Section points calculation complete. Updated section_stats.yml.");
        } else {
            plugin.getLogger().info("Section points calculation complete. No changes detected in section totals.");
        }
    }
}
