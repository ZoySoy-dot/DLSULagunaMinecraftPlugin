package zoy.dLSULaguna.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import zoy.dLSULaguna.DLSULaguna;
import zoy.dLSULaguna.utils.PlayerStatsFileUtil;

import java.io.File;

public class SeePoints implements CommandExecutor {
    private final DLSULaguna plugin;

    public SeePoints(DLSULaguna plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (args.length != 1) {
            commandSender.sendMessage(ChatColor.RED + "Usage: /seepoints <player>");
            return true;
        }

        String uuid = PlayerStatsFileUtil.findUUIDByUsername(args[0]);
        if (uuid == null) {
            commandSender.sendMessage(ChatColor.RED + "Player not found.");
        }

        final var maybeSection = PlayerStatsFileUtil.findSectionByUUID(uuid);
        if (maybeSection.isEmpty()) {
            commandSender.sendMessage(ChatColor.RED + "Player is not in a section.");
            return true;
        }

        final var section = maybeSection.get();

        File playerStatsFile = new File(plugin.getDataFolder(), "players_stats.yml");
        FileConfiguration statsConfig = YamlConfiguration.loadConfiguration(playerStatsFile);
        String path = section + "." + uuid + ".PointsDistribution";

        if (!statsConfig.contains(path)) {
            commandSender.sendMessage(ChatColor.YELLOW + "User has no points...");
            return true;
        }
        commandSender.sendMessage(args[0] + " is in section: " + ChatColor.AQUA + section);
        for (String stat : statsConfig.getConfigurationSection(path).getKeys(false)) {
            String value = statsConfig.getString(path + "." + stat);
            if (stat.equals("Blocks broken type") || stat.equals("Mobs Killed type")
                    || stat.equals("PointsDistribution") || stat.equals("Username") || stat.equals("Last Log-in"))
                continue;
            commandSender.sendMessage(ChatColor.GREEN + args[0] + " has earned " + ChatColor.AQUA + value
                    + ChatColor.GREEN + " " + stat + " points!");
        }
        commandSender.sendMessage(ChatColor.GOLD + "★ " + ChatColor.YELLOW + args[0] + " has earned: "
                + ChatColor.AQUA + statsConfig.getInt(section + "." + uuid + ".Points") + ChatColor.GREEN + " pts");
        return true;
    }
}
