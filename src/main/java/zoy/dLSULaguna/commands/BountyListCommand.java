package zoy.dLSULaguna.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import zoy.dLSULaguna.DLSULaguna;
import zoy.dLSULaguna.utils.Section;

import java.io.File;
import java.util.*;

public class BountyListCommand implements CommandExecutor {

    private final DLSULaguna plugin;
    private final long cooldownDuration = 24 * 60 * 60 * 1000L; // 24 hours in milliseconds - MATCHES Bounties.java

    public BountyListCommand(DLSULaguna plugin) {
        this.plugin = plugin;
    }

    private Optional<Section> getPlayerSection(UUID playerUuid) {
        // Load the players' stats YAML file
        File playersStatsFile = new File(plugin.getDataFolder(), "players_stats.yml");
        FileConfiguration playersStatsConfig = YamlConfiguration.loadConfiguration(playersStatsFile);

        final var playerUuidString = playerUuid.toString();

        // Loop through the sections in the YAML file
        for (String sectionKey : playersStatsConfig.getKeys(false)) {
            // Check if the player's UUID exists in the section
            if (playersStatsConfig.contains(sectionKey + "." + playerUuidString.toString())) {
                // Return the section name if the player's UUID is found
                // What happens if the returned string is invalid?
                return Section.fromString(sectionKey);
            }
        }

        return Optional.empty(); // Return null if no matching section is found
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("bountylist")) {
            sendBountyList(sender);
            return true;
        }
        return false;
    }

    public void sendBountyList(CommandSender sender) {
        File bountyFile = new File(plugin.getDataFolder(), "bounty_stats.yml");

        if (!bountyFile.exists()) {
            sender.sendMessage(ChatColor.RED + "No bounty data found.");
            return;
        }

        FileConfiguration bountyConfig = YamlConfiguration.loadConfiguration(bountyFile);
        Set<String> uuidStrings = bountyConfig.getKeys(false);

        if (uuidStrings.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "There are currently no active bounties.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "=== Active Bounties ===");

        // Create a list to store bounty information for sorting
        List<BountyEntry> bountyEntries = new ArrayList<>();
        for (String uuidStr : uuidStrings) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                String name = bountyConfig.getString(uuidStr + ".Username", "Unknown");
                int baseReward = bountyConfig.getInt(uuidStr + ".Reward", 0); // Get the stored reward
                long lastClaimTime = bountyConfig.getLong(uuidStr + ".LastClaimTime", 0L);
                String claimedBy = bountyConfig.getString(uuidStr + ".ClaimedBy");

                bountyEntries.add(new BountyEntry(uuid, name, baseReward, lastClaimTime, claimedBy));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[BountyListCommand] Invalid UUID in bounty_stats.yml: " + uuidStr);
            }
        }

        // Sort the bounty entries based on the stored reward
        bountyEntries.sort(Comparator.comparingInt(BountyEntry::getBaseReward).reversed());

        for (BountyEntry entry : bountyEntries) {
            UUID uuid = entry.getUuid();
            String name = entry.getName();
            int potentialReward = entry.getBaseReward(); // Use the stored reward as the potential reward
            long lastClaimTime = entry.getLastClaimTime();
            String claimedBy = entry.getClaimedBy();

            // Possibly use Section.toStringOptional instead?
            String sectionDisplay = ChatColor.GRAY +
                    getPlayerSection(uuid).map(
                            (section) -> "[" + section + "]").orElse("(Section: Unknown)");

            String cooldownDisplay = "";
            long currentTime = System.currentTimeMillis();
            if (claimedBy != null && currentTime - lastClaimTime < cooldownDuration && lastClaimTime != 0L) {
                long remainingTime = cooldownDuration - (currentTime - lastClaimTime);
                long remainingHours = (remainingTime / (1000 * 60 * 60)) % 24;
                long remainingMinutes = (remainingTime / (1000 * 60)) % 60;
                String hoursText = remainingHours > 0 ? remainingHours + "h " : "";
                cooldownDisplay = ChatColor.GRAY + " (Cooldown: " + hoursText + remainingMinutes + "m)";
                sender.sendMessage(ChatColor.GRAY + name + sectionDisplay + cooldownDisplay + ChatColor.GRAY
                        + " - Potential Reward: " + ChatColor.GREEN + potentialReward + " points");
            } else {
                sender.sendMessage(ChatColor.GRAY + sectionDisplay + ChatColor.RED + name + " - Potential Reward: "
                        + ChatColor.GREEN + potentialReward + " points");
            }
        }
    }

    // Helper class to store bounty information for sorting
    private static class BountyEntry {
        private final UUID uuid;
        private final String name;
        private final int baseReward;
        private final long lastClaimTime;
        private final String claimedBy;

        public BountyEntry(UUID uuid, String name, int baseReward, long lastClaimTime, String claimedBy) {
            this.uuid = uuid;
            this.name = name;
            this.baseReward = baseReward;
            this.lastClaimTime = lastClaimTime;
            this.claimedBy = claimedBy;
        }

        public UUID getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }

        public int getBaseReward() {
            return baseReward;
        }

        public long getLastClaimTime() {
            return lastClaimTime;
        }

        public String getClaimedBy() {
            return claimedBy;
        }
    }
}
