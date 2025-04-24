package zoy.dLSULaguna.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import zoy.dLSULaguna.utils.Section;
import zoy.dLSULaguna.utils.SectionFileUtil;

public class CreateSection implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /createsection <section name>");
            return true;
        }

        final var sectionString = args[0];
        final var maybeSection = Section.fromString(sectionString);

        if (maybeSection.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Invalid section name [" + sectionString + "]");
            return true;
        }

        SectionFileUtil.createSection(maybeSection.get());
        sender.sendMessage(ChatColor.GREEN + "Created a section " + sectionString);
        return true;
    }
}
