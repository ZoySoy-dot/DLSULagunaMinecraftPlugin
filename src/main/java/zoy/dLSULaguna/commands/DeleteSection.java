package zoy.dLSULaguna.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/* Delete section command doesn't actually delete section? */
public class DeleteSection implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /deleteSection <section>");
        }
        String section = args[0];
        sender.sendMessage(ChatColor.GREEN + "Deleting section " + section);
        return true;
    }
}
