package zoy.dLSULaguna.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import zoy.dLSULaguna.DLSULaguna;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
        reloadConfig();

        // Schedule periodic flush of pending stat updates every 5 seconds
        Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin,
                PlayerStatsFileUtil::flushPending,
                100L,
                100L
        );
    }

    /** Reload config from disk before batch operations */
    private static synchronized void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(statsFile);
    }

    /**
     * Batch-save cached stats for offline players asynchronously.
     */
    public static void batchSave(Set<UUID> playersToSave) {
        reloadConfig();
        for (UUID uuid : playersToSave) {
            Map<String, Object> stats = statCache.remove(uuid);
            if (stats == null) continue;

            String section = PlayerDataUtil.getPlayerSection(uuid);
            if (section == null) continue;

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

    /** Queue a stat update for an online player. */
    public static void setStat(Player player, String statKey, Object value) {
        setStat(player.getUniqueId(), PlayerDataUtil.getPlayerSection(player), statKey, value);
    }

    /**
     * Queue a stat update when you have uuid and section.
     */
    public static void setStat(UUID uuid, String section, String statKey, Object value) {
        if (section == null) return;
        statCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(statKey, value);
        pendingSave.add(uuid);
    }

    /**
     * Immediately sets a stat for offline player, saving directly to file.
     */
    public static void setStatRaw(UUID uuid, String statKey, Object value) {
        reloadConfig();
        String section = PlayerDataUtil.getPlayerSection(uuid);
        if (section == null) return;
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
    public static void increaseStat(Player player, String statKey, Number delta) {
        increaseStat(player.getUniqueId(), PlayerDataUtil.getPlayerSection(player), statKey, delta);
    }

    /**
     * Increase a stat by delta when you have uuid and section.
     * Reads from cache first, then disk, preserves integer vs double.
     */
    public static void increaseStat(UUID uuid, String section, String statKey, Number delta) {
        if (section == null) return;

        Map<String, Object> cache = statCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());

        Number current;
        if (cache.containsKey(statKey) && cache.get(statKey) instanceof Number) {
            current = (Number) cache.get(statKey);
        } else {
            // use appropriate getter based on delta type
            if (delta instanceof Double) {
                current = getStatDouble(uuid, section, statKey, 0.0);
            } else {
                current = getStatInt(uuid, section, statKey, 0);
            }
        }

        Number result;
        if (current instanceof Double || delta instanceof Double) {
            result = current.doubleValue() + delta.doubleValue();
        } else {
            result = current.intValue() + delta.intValue();
        }

        cache.put(statKey, result);
        pendingSave.add(uuid);
    }

    /** Get a stat when you have uuid and section. */
    public static Object getStat(UUID uuid, String section, String statKey) {
        if (section == null) return null;
        reloadConfig();
        return config.get(section + "." + uuid + "." + statKey);
    }

    /**
     * Get a cached or on-disk integer stat.
     */
    public static int getStatInt(UUID uuid, String section, String statKey, int defaultValue) {
        Map<String, Object> cache = statCache.get(uuid);
        if (cache != null && cache.containsKey(statKey) && cache.get(statKey) instanceof Number) {
            return ((Number) cache.get(statKey)).intValue();
        }
        reloadConfig();
        return config.getInt(section + "." + uuid + "." + statKey, defaultValue);
    }

    /**
     * Get a cached or on-disk double stat.
     */
    public static double getStatDouble(UUID uuid, String section, String statKey, double defaultValue) {
        Map<String, Object> cache = statCache.get(uuid);
        if (cache != null && cache.containsKey(statKey) && cache.get(statKey) instanceof Number) {
            return ((Number) cache.get(statKey)).doubleValue();
        }
        reloadConfig();
        return config.getDouble(section + "." + uuid + "." + statKey, defaultValue);
    }

    public static int getStatInt(Player player, String statKey, int defaultValue) {
        return getStatInt(player.getUniqueId(), PlayerDataUtil.getPlayerSection(player), statKey, defaultValue);
    }

    public static double getStatDouble(Player player, String statKey, double defaultValue) {
        return getStatDouble(player.getUniqueId(), PlayerDataUtil.getPlayerSection(player), statKey, defaultValue);
    }

    public static boolean removePlayerEntry(String section, String uuid) {
        reloadConfig();
        if (section == null || uuid == null) return false;
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

    public static String findSectionByUUID(String uuid) {
        reloadConfig();
        for (String section : config.getKeys(false)) {
            ConfigurationSection sec = config.getConfigurationSection(section);
            if (sec != null && sec.contains(uuid)) {
                return section;
            }
        }
        return null;
    }

    public static String findUUIDByUsername(String username) {
        reloadConfig();
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
        String section = PlayerDataUtil.getPlayerSection(player);
        if (section == null) {
            player.sendMessage(ChatColor.YELLOW + "You are not currently assigned to a section.");
            return;
        }
        reloadConfig();
        String path = section + "." + player.getUniqueId() + ".Points";
        int points = config.getInt(path, 0);
        player.sendMessage(ChatColor.GOLD + "★ " + ChatColor.YELLOW
                + "Your current contribution: " + ChatColor.AQUA + points + ChatColor.GREEN + " pts");
    }

    public static void clearPlayerStatsFully(Player player) {
        String section = PlayerDataUtil.getPlayerSection(player);
        String uuid = player.getUniqueId().toString();
        boolean entryRemoved = removePlayerEntry(section, uuid);
        boolean countDecremented = SectionStatsFileUtil.decrementMemberCount(section);
        PlayerDataUtil.removePlayerSection(player.getUniqueId());
        if (entryRemoved && countDecremented) {
            player.sendMessage(ChatColor.GREEN + "Your stats have been cleared! Please join a new section if you wish to participate again.");
            plugin.getLogger().info("Fully cleared stats for player "
                    + player.getName() + " from section " + section);
        } else if (section != null) {
            player.sendMessage(ChatColor.RED + "An error occurred while clearing your stats. Please contact an admin.");
        } else {
            player.sendMessage(ChatColor.YELLOW + "Your section assignment was cleared, but no corresponding stats were found.");
        }
    }

    public static void flushPending() {
        Set<UUID> toSave = new HashSet<>(pendingSave);
        pendingSave.removeAll(toSave);
        if (!toSave.isEmpty()) {
            batchSave(toSave);
        }
    }
}
