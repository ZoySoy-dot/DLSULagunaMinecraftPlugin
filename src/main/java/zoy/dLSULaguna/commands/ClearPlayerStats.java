package zoy.dLSULaguna.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import zoy.dLSULaguna.utils.PlayerStatsFileUtil;

public class ClearPlayerStats implements CommandExecutor {
    public ClearPlayerStats() {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Permission check
        if (!sender.hasPermission("dlsulaguna.clearplayerstats")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        // Usage check
        if (args.length != 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /clearplayerstats <player>");
            return true;
        }

        Player victim = Bukkit.getPlayerExact(args[0]);
        if (victim == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' is not online.");
            return true;
        }

        // Perform clear
        PlayerStatsFileUtil.clearPlayerStatsFully(victim);

        sender.sendMessage(ChatColor.GREEN + "Cleared stats for " + ChatColor.AQUA + victim.getName());
        victim.sendMessage(ChatColor.RED + "☠ Your stats have been cleared by an admin.");

        return true;
    }
}
