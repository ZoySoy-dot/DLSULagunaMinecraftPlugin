package zoy.dLSULaguna.listeners;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import zoy.dLSULaguna.DLSULaguna;
import zoy.dLSULaguna.commands.SectionChat;
import zoy.dLSULaguna.utils.PlayerDataUtil;
import zoy.dLSULaguna.utils.Section;

import java.io.File;
import java.util.UUID;

public class PlayerChatListener implements Listener {
    private final DLSULaguna plugin;
    private File donatorsFile;
    private FileConfiguration donatorsConfig;

    public PlayerChatListener(DLSULaguna plugin, SectionChat sectionChat) {
        this.plugin = plugin;
        donatorsFile = new File(plugin.getDataFolder(), "donators.yml");
        donatorsConfig = YamlConfiguration.loadConfiguration(donatorsFile);
        if (!donatorsFile.exists()) {
            try {
                donatorsFile.getParentFile().mkdirs();
                donatorsFile.createNewFile();
            } catch (Exception e) {
                plugin.getLogger().warning("Could not create donators.yml");
            }
        }
    }

    // @EventHandler
    // public void onPlayerJoin(PlayerJoinEvent event) {
    // Player player = event.getPlayer();
    // }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        final var maybeSection = PlayerDataUtil.getPlayerSection(player);
        final var sectionDisplay = Section.toStringOptional(maybeSection);

        if (SectionChat.isSectionChatEnabled(uuid) && maybeSection.isPresent()) {
            final var section = maybeSection.get();

            // Send it only to players in the same section
            event.setCancelled(true);
            String message = "[" + sectionDisplay + " Private] " + ChatColor.RESET + player.getName() + ": "
                    + ChatColor.WHITE
                    + event.getMessage();

            for (Player p : plugin.getServer().getOnlinePlayers()) {
                final var maybeOtherSection = PlayerDataUtil.getPlayerSection(p);

                // What should happen if the other player doesn't have a section?
                if (maybeOtherSection.isEmpty()) {
                    continue;
                }

                if (maybeOtherSection.get().equals(section)) {
                    p.sendMessage(message);
                }
            }
        } else {
            // What should [sectionDisplay] be if [maybeSection] is [Optional.empty()]?

            // Section chat is off: let the message go globally, but format it
            event.setFormat("[" + sectionDisplay + "] " + ChatColor.RESET + "%s: " + ChatColor.WHITE + "%s");

            if (donatorsConfig.contains(player.getName())) {
                event.setFormat(
                        "[" + sectionDisplay + "] " + ChatColor.RESET + "%s(Donator): " + ChatColor.WHITE + "%s");
            }
        }
    }

}
