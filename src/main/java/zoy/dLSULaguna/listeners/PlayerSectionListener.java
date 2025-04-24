package zoy.dLSULaguna.listeners;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import zoy.dLSULaguna.DLSULaguna;

public class PlayerSectionListener implements Listener {
    private final NamespacedKey sectionDataKey;

    public PlayerSectionListener(DLSULaguna plugin) {
        this.sectionDataKey = new NamespacedKey(plugin, "section_name");

        // every 3600 ticks (~3 minutes)
        Bukkit.getScheduler().scheduleSyncRepeatingTask(
                plugin,
                () -> {
                    World bbWorld = plugin.getBuildBattle().getBuildWorld();
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        // if BuildBattle world doesn't exist yet or player isn't in it
                        if (bbWorld == null || !p.getWorld().equals(bbWorld)) {
                            // and they haven't picked a section yet
                            if (p.getPersistentDataContainer().get(
                                    sectionDataKey, PersistentDataType.STRING) == null) {
                                p.kickPlayer("You have not selected a section");
                            }
                        }
                    }
                },
                0L,
                3600L);
    }
}
