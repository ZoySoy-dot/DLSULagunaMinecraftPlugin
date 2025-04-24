package zoy.dLSULaguna.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import zoy.dLSULaguna.DLSULaguna;
import zoy.dLSULaguna.utils.PlayerStatsFileUtil;
import zoy.dLSULaguna.utils.Section;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class SetPlayerPoints implements CommandExecutor {
    private final DLSULaguna plugin;

    public SetPlayerPoints(DLSULaguna plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (args.length != 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /setplayerpoints <player> <category> <points>");
            return false;
        }

        String victimName = args[0];
        String category = args[1];
        String pointsStr = args[2];
        int points;

        try {
            points = Integer.parseInt(pointsStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Error: Points must be a valid number.");
            return false;
        }

        String victimUUID = PlayerStatsFileUtil.findUUIDByUsername(victimName);
        if (victimUUID == null) {
            sender.sendMessage(ChatColor.RED + "Error: Player '" + victimName + "' not found in the stats.");
            return false;
        }

        Optional<Section> victimSection = PlayerStatsFileUtil.findSectionByUUID(victimUUID);

        if (victimSection.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Error: Player '" + victimName + "' does not belong to a section.");
            return false;
        }

        File playerStatsFile = new File(plugin.getDataFolder(), "players_stats.yml");
        FileConfiguration playerStatsConfig = YamlConfiguration.loadConfiguration(playerStatsFile);

        String path = victimSection + "." + victimUUID + "." + category;

        // Check if the category already exists for the player
        if (playerStatsConfig.contains(path)) {
            playerStatsConfig.set(path, points);
            try {
                playerStatsConfig.save(playerStatsFile);
                sender.sendMessage(
                        ChatColor.GREEN + "Updated " + category + " points for " + victimName + " to " + points + ".");
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save to players_stats.yml!");
                e.printStackTrace();
                sender.sendMessage(ChatColor.RED + "An error occurred while saving player stats.");
                return false;
            }
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Category '" + category + "' does not exist for player '" + victimName
                    + "'. No changes made.");
        }

        return true;
    }
}