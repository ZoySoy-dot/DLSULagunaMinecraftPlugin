package zoy.dLSULaguna.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scoreboard.*;
import org.bukkit.entity.Player;
import zoy.dLSULaguna.DLSULaguna;

import java.util.Set;
import java.util.logging.Level;

/**
 * Utility class for managing and displaying the in-game sidebar scoreboard
 * showing section statistics in a thread-safe manner.
 */
public class ScoreboardUtil {
    private static DLSULaguna plugin;

    public static void initialize(DLSULaguna pluginInstance) {
        plugin = pluginInstance;
    }

    /**
     * Starts periodic updates of the section leaderboard on the main thread.
     * 
     * @param trackedStat   The statistic key to display (e.g., "Points").
     * @param title         The sidebar title.
     * @param intervalTicks The interval between updates, in ticks.
     */
    public static void startAutoDisplay(String trackedStat, String title, long intervalTicks) {
        if (plugin == null) {
            throw new IllegalStateException("ScoreboardUtil not initialized");
        }
        // Schedule entirely on the main thread to avoid async scoreboard operations
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            ScoreboardManager manager = Bukkit.getScoreboardManager();
            if (manager == null)
                return;

            Scoreboard board = manager.getNewScoreboard();
            Objective obj = board.registerNewObjective("sectionStats", Criteria.DUMMY, title);
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);

            Set<Section> sections = SectionStatsFileUtil.getSectionKeys();
            if (sections.isEmpty()) {
                obj.getScore(ChatColor.GRAY + "No sections found").setScore(0);
            } else {
                for (final var sect : sections) {
                    ConfigurationSection data = SectionStatsFileUtil.getSectionData(sect);
                    int pts = data != null ? data.getInt(trackedStat, 0) : 0;
                    obj.getScore(formatName(sect.toString())).setScore(pts); // Do we still need formatName with
                                                                             // [Section] being a type?
                }
            }

            // Apply to all online players
            for (Player p : Bukkit.getOnlinePlayers()) {
                try {
                    p.setScoreboard(board);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to set scoreboard for " + p.getName(), e);
                }
            }
        }, 0L, intervalTicks);
    }

    /**
     * One-off display: builds and shows immediately (no repeat).
     */
    public static void displayOnce(String trackedStat, String title) {
        startAutoDisplay(trackedStat, title, Long.MAX_VALUE);
    }

    /**
     * Ensures a safe-length display name.
     */
    private static String formatName(String key) {
        return key.length() > 40 ? key.substring(0, 40) : key;
    }
}