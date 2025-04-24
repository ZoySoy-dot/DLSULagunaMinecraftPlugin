package zoy.dLSULaguna.utils;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import zoy.dLSULaguna.DLSULaguna;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Utility class for direct interactions with the 'section_stats.yml' file.
 * Handles reading, writing, and updating aggregated section statistics.
 */
public class SectionStatsFileUtil {

    private static DLSULaguna plugin;
    private static File sectionStatsFile;
    private static File playerStatsFile; // Needed for recalculation
    // private static File sectionsFile;

    /**
     * Initializes the utility class with the plugin instance and prepares file
     * objects.
     * Must be called once on plugin enable.
     * 
     * @param pluginInstance The main plugin instance.
     */
    public static void initialize(DLSULaguna pluginInstance) {
        plugin = pluginInstance;
        sectionStatsFile = new File(plugin.getDataFolder(), "section_stats.yml");
        playerStatsFile = new File(plugin.getDataFolder(), "players_stats.yml"); // Initialize player stats file ref too
        // sectionsFile = new File(plugin.getDataFolder(), "sections.yml");
        // Optional: Create section_stats.yml if it doesn't exist
        /*
         * if (!sectionStatsFile.exists()) {
         * // ... creation logic ...
         * }
         */
    }

    /** Loads the section_stats.yml FileConfiguration */
    private static FileConfiguration loadConfig() {
        if (plugin == null || sectionStatsFile == null) {
            throw new IllegalStateException("SectionStatsFileUtil not initialized!");
        }
        if (!sectionStatsFile.exists()) {
            plugin.getLogger().fine("section_stats.yml does not exist, creating it now.");
            try {
                if (sectionStatsFile.getParentFile() != null)
                    sectionStatsFile.getParentFile().mkdirs();
                sectionStatsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create missing section_stats.yml!", e);
                return new YamlConfiguration(); // Return empty config on error
            }
        }
        return YamlConfiguration.loadConfiguration(sectionStatsFile);
    }

    /** Saves the FileConfiguration to section_stats.yml */
    private static void saveConfig(FileConfiguration config) {
        if (plugin == null || sectionStatsFile == null) {
            System.err.println("SectionStatsFileUtil not initialized! Cannot save config.");
            return;
        }
        try {
            config.save(sectionStatsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save section_stats.yml!", e);
        }
    }

    /**
     * Increases a numeric statistic for a specific section in section_stats.yml.
     * Handles Integer and Double types.
     * 
     * @param section The section identifier (e.g., "A", "B").
     * @param statKey The statistic key (e.g., "Blocks broken", "Points").
     * @param change  The amount to add (can be negative). Must be Integer or
     *                Double.
     */
    public static void increaseStat(Section section, String statKey, Object change) {
        if (section == null || statKey == null)
            return;

        FileConfiguration config = loadConfig();
        String path = section + "." + statKey;

        if (change instanceof Integer) {
            int delta = (Integer) change;
            int current = config.getInt(path, 0); // Default to 0
            config.set(path, current + delta);
        } else if (change instanceof Double) {
            double delta = (Double) change;
            double current = config.getDouble(path, 0.0); // Default to 0.0
            config.set(path, current + delta);
        } else {
            plugin.getLogger().warning("Unsupported stat type for section increase: " + statKey + " ("
                    + (change != null ? change.getClass().getSimpleName() : "null") + ") in section " + section);
            return; // Don't save if type is wrong
        }
        saveConfig(config);
        plugin.getLogger().finest("Increased stat '" + statKey + "' for section " + section);
    }

    /**
     * Directly sets the 'Points' value for a specific section in section_stats.yml.
     * 
     * @param section The section identifier.
     * @param value   The new point value (should be Integer or compatible).
     */
    public static void setPoints(Section section, Object value) {
        if (section == null)
            return;
        FileConfiguration config = loadConfig();
        config.set(section + ".Points", value); // Overwrites existing value or creates the path
        saveConfig(config);
        plugin.getLogger().fine("Set points for section " + section + " to " + value);
    }

    /**
     * Gets a specific integer stat for a section.
     * 
     * @param section      The section identifier.
     * @param statKey      The statistic key.
     * @param defaultValue The default value if not found.
     * @return The integer value or default.
     */
    public static int getStatInt(Section section, String statKey, int defaultValue) {
        if (section == null || statKey == null)
            return defaultValue;
        FileConfiguration config = loadConfig();
        return config.getInt(section + "." + statKey, defaultValue);
    }

    /**
     * Gets a specific double stat for a section.
     * 
     * @param section      The section identifier.
     * @param statKey      The statistic key.
     * @param defaultValue The default value if not found.
     * @return The double value or default.
     */
    public static double getStatDouble(Section section, String statKey, double defaultValue) {
        if (section == null || statKey == null)
            return defaultValue;
        FileConfiguration config = loadConfig();
        return config.getDouble(section + "." + statKey, defaultValue);
    }

    /**
     * Gets the entire ConfigurationSection for a given section key.
     * Useful for iterating through all stats of a section.
     * 
     * @param section The section identifier.
     * @return The ConfigurationSection or null if not found.
     */
    public static ConfigurationSection getSectionData(Section section) {
        if (section == null)
            return null;
        FileConfiguration config = loadConfig();
        return config.getConfigurationSection(section.toString());
    }

    /**
     * Gets all top-level section keys (e.g., "A", "B", "C").
     * 
     * @return A Set of section keys.
     */
    public static java.util.Set<Section> getSectionKeys() {
        FileConfiguration config = loadConfig();
        return config.getKeys(false)
                .stream()
                .flatMap((sectionString) -> Section.fromString(sectionString).stream()) // Is it ok to discard the
                                                                                        // sections that don't fit
                                                                                        // the [Section] format?
                .collect(Collectors.toSet());
    }

    /**
     * Decrements the 'Members' count for a given section by one.
     * Ensures the count does not go below zero.
     * 
     * @param section The section identifier.
     * @return true if the count was successfully decremented and saved, false
     *         otherwise.
     */
    public static boolean decrementMemberCount(Section section) /* Should this be a section type? */ {
        if (section == null)
            return false;
        FileConfiguration config = loadConfig();
        String memberPath = section + ".Members";
        int currentMembers = config.getInt(memberPath, 0); // Default to 0

        if (currentMembers > 0) {
            config.set(memberPath, currentMembers - 1);
            saveConfig(config);
            plugin.getLogger().info("Decremented member count for section " + section + " to " + (currentMembers - 1));
            return true;
        } else {
            plugin.getLogger().warning("Attempted to decrement member count for section " + section
                    + ", but count was already " + currentMembers);
            return false; // Indicate count was not changed (or already zero)
        }
    }

    /**
     * Recalculates all aggregated statistics for every section based on current
     * player data.
     * Reads from players_stats.yml and overwrites section_stats.yml.
     * Excludes 'Points' and 'PointsDistribution' from aggregation as they are
     * handled by PointsCalculatorUtil.
     * This is a potentially intensive operation.
     */
    public static void recalculateAggregateStats() {
        if (plugin == null || playerStatsFile == null) {
            System.err.println("SectionStatsFileUtil not initialized! Cannot recalculate stats.");
            return;
        }
        if (!playerStatsFile.exists()) {
            plugin.getLogger().warning("Cannot recalculate section stats: players_stats.yml does not exist.");
            return;
        }

        FileConfiguration playerStats = YamlConfiguration.loadConfiguration(playerStatsFile);
        FileConfiguration newSectionStats = new YamlConfiguration(); // Start fresh

        plugin.getLogger().info("Recalculating aggregated section stats (excluding points)...");

        for (String sectionKey : playerStats.getKeys(false)) {
            ConfigurationSection sectionPlayerData = playerStats.getConfigurationSection(sectionKey);
            if (sectionPlayerData == null)
                continue;

            int memberCount = 0;
            Map<String, Object> aggregatedStats = new HashMap<>();

            for (String uuid : sectionPlayerData.getKeys(false)) {
                ConfigurationSection playerData = sectionPlayerData.getConfigurationSection(uuid);
                if (playerData == null)
                    continue;
                memberCount++;

                for (String statKey : playerData.getKeys(false)) {
                    // --- Skip non-aggregatable fields ---
                    if (statKey.equalsIgnoreCase("Username") ||
                            statKey.equalsIgnoreCase("Last Log-in") ||
                            statKey.equalsIgnoreCase("Points") || // Handled by PointsCalculatorUtil
                            statKey.equalsIgnoreCase("PointsDistribution")) // Handled by PointsCalculatorUtil
                    {
                        continue;
                    }
                    // Skip lists or sections unless specifically handled
                    if (playerData.isList(statKey) || (playerData.isConfigurationSection(statKey)
                            && !statKey.equals("ExampleNestedStatSection"))) { // Adjust if you have known nested
                                                                               // sections to aggregate
                        continue;
                    }

                    Object value = playerData.get(statKey);

                    // --- Aggregate Numerical Stats ---
                    if (value instanceof Integer) {
                        int currentTotal = (int) aggregatedStats.getOrDefault(statKey, 0);
                        aggregatedStats.put(statKey, currentTotal + (Integer) value);
                    } else if (value instanceof Double) {
                        double currentTotal = (double) aggregatedStats.getOrDefault(statKey, 0.0);
                        aggregatedStats.put(statKey, currentTotal + (Double) value);
                    } else if (value instanceof Long) { // Handle Long if necessary (e.g., time)
                        long currentTotal = (long) aggregatedStats.getOrDefault(statKey, 0L);
                        aggregatedStats.put(statKey, currentTotal + (Long) value);
                    }
                    // Add other numeric types if needed (Float, Short, Byte)

                    // --- Handle known nested sections if required ---
                    /*
                     * else if (statKey.equals("ExampleNestedStatSection") &&
                     * playerData.isConfigurationSection(statKey)) {
                     * ConfigurationSection subStatSection =
                     * playerData.getConfigurationSection(statKey);
                     * for(String subStatKey : subStatSection.getKeys(false)){
                     * if (subStatSection.isInt(subStatKey)){
                     * int subValue = subStatSection.getInt(subStatKey);
                     * String combinedPath = statKey + "." + subStatKey;
                     * int currentTotal = (int) aggregatedStats.getOrDefault(combinedPath, 0);
                     * aggregatedStats.put(combinedPath, currentTotal + subValue);
                     * }
                     * }
                     * }
                     */
                }
            }

            // Write aggregated data for this section
            if (memberCount > 0 || !aggregatedStats.isEmpty()) {
                newSectionStats.set(sectionKey + ".Members", memberCount);
                for (Map.Entry<String, Object> entry : aggregatedStats.entrySet()) {
                    newSectionStats.set(sectionKey + "." + entry.getKey(), entry.getValue());
                }
                plugin.getLogger().fine("Aggregated stats for section " + sectionKey + ": Members=" + memberCount);
            }
        }

        // Save the results (overwrites old section_stats.yml)
        saveConfig(newSectionStats);
        plugin.getLogger().info("Section aggregate stats recalculated and saved.");
    }

}