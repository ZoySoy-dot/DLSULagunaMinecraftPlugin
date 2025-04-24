package zoy.dLSULaguna.utils;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import zoy.dLSULaguna.DLSULaguna;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PlayerStatsFileUtil {

    private static DLSULaguna plugin;
    private static File statsFile;
    private static FileConfiguration config;
    private static final Set<UUID> pendingSave = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Map<String, Object>> statCache = new ConcurrentHashMap<>();

    /**
     * Initialize stats file and config.
     */
    public static void initialize(DLSULaguna pluginInstance) {
        plugin = pluginInstance;
        statsFile = plugin.getPlayersStatsFile();

        if (!statsFile.exists()) {
            try {
                if (statsFile.getParentFile() != null)
                    statsFile.getParentFile().mkdirs();
                statsFile.createNewFile();
                plugin.getLogger().info("Created players_stats.yml");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create players_stats.yml on initialize!", e);
            }
        }
        config = YamlConfiguration.loadConfiguration(statsFile);
        // Schedule periodic flush of pending stat updates every 5 seconds
        Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin,
                PlayerStatsFileUtil::flushPending,
                100L,
                100L);
    }

    /**
     * Batch-save cached stats for offline players asynchronously.
     */
    public static void batchSave(Set<UUID> playersToSave) {
        for (UUID uuid : playersToSave) {
            Map<String, Object> stats = statCache.remove(uuid);
            if (stats == null)
                continue;

            Optional<Section> maybeSection = PlayerDataUtil.getPlayerSection(uuid);

            if (maybeSection.isEmpty())
                continue;

            final var section = maybeSection.get();

            String base = section + "." + uuid;
            for (Map.Entry<String, Object> entry : stats.entrySet()) {
                config.set(base + "." + entry.getKey(), entry.getValue());
            }
        }
        try {
            config.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not batch save players_stats.yml!", e);
        }
    }

    /**
     * Queue a stat update for an online player.
     */
    public static void setStat(Player player, String statKey, Object value) {
        queueStat(player.getUniqueId(), statKey, value);
    }

    /**
     * Queue a stat update when you have UUID and section.
     */
    public static void setStat(UUID uuid, String section, String statKey, Object value) {
        if (section == null)
            return;
        statCache.putIfAbsent(uuid, new ConcurrentHashMap<>());
        statCache.get(uuid).put(statKey, value);
        pendingSave.add(uuid);
    }

    /**
     * Immediately sets a stat for offline player, saving directly to file.
     */
    public static void setStatRaw(UUID uuid, String statKey, Object value) {
        final var maybeSection = PlayerDataUtil.getPlayerSection(uuid);

        if (maybeSection.isEmpty())
            return;

        final var section = maybeSection.get();

        String path = section + "." + uuid + "." + statKey;
        config.set(path, value);
        try {
            config.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save players_stats.yml during raw set!", e);
        }
    }

    /**
     * Increase a numeric stat for an online player.
     */
    public static void increaseStat(Player player, String statKey, Object change) {
        increaseStat(player.getUniqueId(), PlayerDataUtil.getPlayerSection(player), statKey, change);
    }

    /**
     * Increase a stat by delta when you have uuid and section.
     */
    public static void increaseStat(UUID uuid, Optional<Section> maybeSection, String statKey, Object change) {
        if (maybeSection.isEmpty())
            return;

        final var section = maybeSection.get();
        String path = section + "." + uuid + "." + statKey;
        Object currentVal = config.get(path);
        Object result;
        if (change instanceof Integer) {
            int curr = currentVal instanceof Number ? ((Number) currentVal).intValue() : 0;
            result = curr + (Integer) change;
        } else if (change instanceof Double) {
            double curr = currentVal instanceof Number ? ((Number) currentVal).doubleValue() : 0.0;
            result = curr + (Double) change;
        } else {
            plugin.getLogger().warning("Invalid stat change type for key: " + statKey);
            return;
        }
        queueStat(uuid, statKey, result);
    }

    /**
     * Helper to queue stat updates in cache.
     */
    private static void queueStat(UUID uuid, String statKey, Object value) {
        statCache.putIfAbsent(uuid, new ConcurrentHashMap<>());
        statCache.get(uuid).put(statKey, value);
        pendingSave.add(uuid);
    }

    /**
     * Get a stat for online player.
     */
    public static Object getStat(Player player, String statKey) {
        return getStat(player.getUniqueId(), PlayerDataUtil.getPlayerSection(player), statKey);
    }

    /**
     * Get a stat when you have uuid and section.
     */
    public static Optional<Object> getStat(UUID uuid, Optional<Section> section, String statKey) {
        if (section.isEmpty())
            return Optional.empty();
        return Optional.ofNullable(config.get(section + "." + uuid + "." + statKey));
    }

    public static int getStatInt(Player player, String statKey, int defaultValue) {
        return getStatInt(player.getUniqueId(), PlayerDataUtil.getPlayerSection(player), statKey, defaultValue);
    }

    public static int getStatInt(UUID uuid, Optional<Section> section, String statKey, int defaultValue) {
        return getStat(uuid, section, statKey).map(
                (Object val) -> val instanceof Number ? ((Number) val).intValue() : defaultValue).orElse(defaultValue);
    }

    public static double getStatDouble(Player player, String statKey, double defaultValue) {
        return getStatDouble(player.getUniqueId(), PlayerDataUtil.getPlayerSection(player), statKey, defaultValue);
    }

    public static double getStatDouble(UUID uuid, Optional<Section> section, String statKey, double defaultValue) {
        return getStat(uuid, section, statKey).map(
                (Object val) -> val instanceof Number ? ((Number) val).doubleValue() : defaultValue)
                .orElse(defaultValue);
    }

    /**
     * Remove a player's entire entry.
     */
    public static boolean removePlayerEntry(Section section, UUID uuid) {
        if (section == null || uuid == null)
            return false;

        // Is this correct?
        String path = section + "." + uuid;

        if (config.contains(path)) {
            config.set(path, null);
            try {
                config.save(statsFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save players_stats.yml during removal!", e);
            }
            return true;
        }
        return false;
    }

    public static Optional<Section> findSectionByUUID(String uuid) {
        for (String section : config.getKeys(false)) {
            ConfigurationSection sec = config.getConfigurationSection(section);
            if (sec != null && sec.isConfigurationSection(uuid)) {
                return Section.fromString(section); // What should happen if the section is invalid?
            }
        }
        return Optional.empty();
    }

    public static String findUUIDByUsername(String username) {
        for (String section : config.getKeys(false)) {
            ConfigurationSection sec = config.getConfigurationSection(section);
            if (sec != null) {
                for (String uuid : sec.getKeys(false)) {
                    if (sec.isString(uuid + ".Username")
                            && sec.getString(uuid + ".Username").equalsIgnoreCase(username)) {
                        return uuid;
                    }
                }
            }
        }
        return null;
    }

    public static void showPlayerPoints(Player player) {
        final var maybeSection = PlayerDataUtil.getPlayerSection(player);
        if (maybeSection.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "You are not currently assigned to a section.");
            return;
        }

        final var section = maybeSection.get();

        String path = section + "." + player.getUniqueId() + ".Points";
        int points = config.getInt(path);
        player.sendMessage(ChatColor.GOLD + "★ " + ChatColor.YELLOW + "Your current contribution: " + ChatColor.AQUA
                + points + ChatColor.GREEN + " pts");
    }

    /* Does the refactored logic of this track? */
    public static void clearPlayerStatsFully(Player player) {
        if (plugin == null)
            return;

        final var maybeSection = PlayerDataUtil.getPlayerSection(player);

        PlayerDataUtil.removePlayerSectionData(player);

        if (maybeSection.isEmpty()) {
            player.sendMessage(
                    ChatColor.YELLOW + "Your section assignment was cleared, but no corresponding stats were found.");

            return;
        }

        final var section = maybeSection.get();

        UUID uuid = player.getUniqueId();

        if (removePlayerEntry(section, uuid) && SectionStatsFileUtil.decrementMemberCount(section)) {
            player.sendMessage(ChatColor.GREEN
                    + "Your stats have been cleared! Please join a new section if you wish to participate again.");
            plugin.getLogger()
                    .info("Fully cleared stats for player " + player.getName() + " from section " + section);
        } else {
            player.sendMessage(
                    ChatColor.RED + "An error occurred while clearing your stats. Please contact an admin.");
        }
    }

    /**
     * Flush queued stat updates to disk.
     */
    public static void flushPending() {
        Set<UUID> toSave = new HashSet<>(pendingSave);
        pendingSave.removeAll(toSave);
        if (!toSave.isEmpty()) {
            batchSave(toSave);
        }
    }
}
