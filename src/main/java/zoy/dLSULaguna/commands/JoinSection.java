package zoy.dLSULaguna.commands;

import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import zoy.dLSULaguna.DLSULaguna;
import zoy.dLSULaguna.utils.Section;
import zoy.dLSULaguna.utils.SectionFileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class JoinSection implements CommandExecutor, TabCompleter {

    private final DLSULaguna plugin;
    private final NamespacedKey sectionKey;

    public JoinSection(DLSULaguna plugin) {
        this.plugin = plugin;
        this.sectionKey = new NamespacedKey(plugin, "section_name");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        File playerStatsFile = new File(plugin.getDataFolder(), "players_stats.yml");
        FileConfiguration playerStats = YamlConfiguration.loadConfiguration(playerStatsFile);

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("Usage: /joinsection <section name>");
            return false;
        }

        final var maybeSection = Section.fromString(args[0]);

        if (maybeSection.isEmpty()) {
            final var availableSections = SectionFileUtil.getSectionKeys();
            player.sendMessage("Invalid section. Available sections are: "
                    + String.join(", ", availableSections.stream().map((section) -> section.toString()).toList()));
            return true;
        }

        final var sectionName = maybeSection.get().toString();

        PersistentDataContainer playerData = player.getPersistentDataContainer();
        plugin.getLogger().info("Player " + player.getName() + " - Persistent Data Keys: " + playerData.getKeys());
        plugin.getLogger().info("Player " + player.getName() + " - Current Section: "
                + playerData.get(sectionKey, PersistentDataType.STRING));

        if (playerData.get(sectionKey, PersistentDataType.STRING) != null) {
            String currentSection = playerData.get(sectionKey, PersistentDataType.STRING);
            player.sendMessage(
                    "You are already in section " + currentSection + "! Please contact an admin if you made an error.");
            player.sendMessage("Note: Your stats will be reset if you join a new section.");
            return true;
        }

        playerData.set(sectionKey, PersistentDataType.STRING, sectionName);
        playerStats.set(sectionName + "." + player.getUniqueId() + ".Username", player.getName());
        player.sendMessage("You have joined section " + sectionName + "!");

        try {
            playerStats.save(playerStatsFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Error saving player stats: " + e.getMessage());
            throw new RuntimeException(e);
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            final var sections = SectionFileUtil.getSectionKeys();
            for (final var section : sections) {
                final var sectionName = section.toString();
                if (sectionName.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    suggestions.add(sectionName);
                }
            }
        }
        return suggestions;
    }
}