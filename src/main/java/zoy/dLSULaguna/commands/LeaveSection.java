package zoy.dLSULaguna.commands;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import zoy.dLSULaguna.DLSULaguna;
import zoy.dLSULaguna.utils.Section;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LeaveSection implements CommandExecutor, TabCompleter {
    private final DLSULaguna plugin;
    private final NamespacedKey section_key;

    public LeaveSection(DLSULaguna plugin) {
        this.plugin = plugin;
        this.section_key = new NamespacedKey(plugin, "section_name");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {

        // Permission check
        if (!sender.hasPermission("dlsulaguna.leavesection")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        // Argument check
        if (args.length != 1) {
            sender.sendMessage("§eUsage: /leavesection <player>");
            return true;
        }

        String targetName = args[0];
        Player victim = Bukkit.getPlayerExact(targetName);

        if (victim == null) {
            sender.sendMessage("§cPlayer '" + targetName + "' is not online.");
            return true;
        }

        final var maybeSection = Section
                .fromString(victim.getPersistentDataContainer().get(section_key, PersistentDataType.STRING));

        // Check if player has a section
        if (maybeSection.isEmpty()) {
            sender.sendMessage("§e" + victim.getName() + " is not assigned to any section.");
            return true;
        }

        String uuid = victim.getUniqueId().toString();
        String path = maybeSection.get() + "." + uuid;

        File statsFile = new File(plugin.getDataFolder(), "players_stats.yml");
        FileConfiguration statsConfig = YamlConfiguration.loadConfiguration(statsFile);

        // Remove section data
        statsConfig.set(path, null);

        try {
            statsConfig.save(statsFile);
            plugin.getLogger().info("Removed stats for: " + victim.getName());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save stats file for: " + victim.getName());
            e.printStackTrace();
        }

        // Remove from PDC
        victim.getPersistentDataContainer().remove(section_key);

        // Feedback
        sender.sendMessage("§aSection removed for §b" + victim.getName());
        victim.sendMessage("§cYour section has been cleared by an admin.");

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
