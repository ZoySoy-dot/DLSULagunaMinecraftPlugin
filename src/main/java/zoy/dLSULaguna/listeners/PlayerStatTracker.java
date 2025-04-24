package zoy.dLSULaguna.listeners;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.*;
import org.bukkit.scheduler.BukkitRunnable;

import zoy.dLSULaguna.DLSULaguna;
import zoy.dLSULaguna.utils.PlayerDataUtil;
import zoy.dLSULaguna.utils.PlayerStatsFileUtil;
import zoy.dLSULaguna.utils.Section;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;

public class PlayerStatTracker implements Listener {

    private final DLSULaguna plugin;
    private final NamespacedKey sectionKey;
    private final NamespacedKey afkKey;
    private final File blockFile;
    private final FileConfiguration blockConfig;
    private final Map<String, String> sectionNameMap = new HashMap<>();
    private long lastBlockSave = System.currentTimeMillis();

    public PlayerStatTracker(DLSULaguna plugin) {
        this.plugin = plugin;
        this.sectionKey = new NamespacedKey(plugin, "section_name");
        this.afkKey = new NamespacedKey(plugin, "AFK");

        this.blockFile = new File(plugin.getDataFolder(), "blocks.yml");
        if (!blockFile.exists()) {
            try {
                blockFile.getParentFile().mkdirs();
                blockFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create blocks.yml");
            }
        }
        this.blockConfig = YamlConfiguration.loadConfiguration(blockFile);

        sectionNameMap.put("A", "STEM11-A");
        sectionNameMap.put("B", "STEM11-B");
        sectionNameMap.put("C", "STEM11-C");
        sectionNameMap.put("D", "STEM11-D");
        sectionNameMap.put("E", "STEM11-E");
    }

    private void maybeSaveBlockFile() {
        long now = System.currentTimeMillis();
        if (now - lastBlockSave > 5000) {
            try {
                blockConfig.save(blockFile);
                lastBlockSave = now;
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save blocks.yml", e);
            }
        }
    }

    private String locationToString(Location loc) {
        return loc.getWorld().getName()
                + "," + loc.getBlockX()
                + "," + loc.getBlockY()
                + "," + loc.getBlockZ();
    }

    /**
     * Only track stats outside the BuildBattle world.
     */
    private boolean isTrackedWorld(World world) {
        World bbWorld = plugin.getBuildBattle().getBuildWorld();
        // if the BuildBattle world isn't loaded yet, we *do* track everywhere.
        // once it's loaded, skip stats in that world.
        return bbWorld == null || !world.equals(bbWorld);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!isTrackedWorld(event.getPlayer().getWorld()))
            return;
        String key = locationToString(event.getBlockPlaced().getLocation());
        blockConfig.set(key, event.getPlayer().getUniqueId().toString());
        PlayerStatsFileUtil.increaseStat(event.getPlayer(), "Blocks placed", 1);
        maybeSaveBlockFile();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!isTrackedWorld(player.getWorld()))
            return;

        String key = locationToString(event.getBlock().getLocation());
        if (blockConfig.contains(key)) {
            blockConfig.set(key, null);
            maybeSaveBlockFile();
            return;
        }

        String blockType = event.getBlock().getType().toString();
        PlayerStatsFileUtil.increaseStat(player, "Blocks broken", 1);
        PlayerStatsFileUtil.increaseStat(player, "Blocks broken type." + blockType, 1);
    }

    @EventHandler
    public void onPlayerKillMob(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null && isTrackedWorld(killer.getWorld())) {
            EntityType type = event.getEntityType();
            PlayerStatsFileUtil.increaseStat(killer, "Mobs Killed", 1);
            PlayerStatsFileUtil.increaseStat(killer, "Mobs Killed type." + type, 1);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (isTrackedWorld(victim.getWorld())) {
            PlayerStatsFileUtil.increaseStat(victim, "Deaths", 1);
        }

        if (killer != null && isTrackedWorld(killer.getWorld())) {
            PersistentDataContainer vicData = victim.getPersistentDataContainer();
            PersistentDataContainer killData = killer.getPersistentDataContainer();
            if (killData.has(sectionKey, PersistentDataType.STRING)
                    && vicData.has(sectionKey, PersistentDataType.STRING)) {
                PlayerStatsFileUtil.increaseStat(killer, "Kills", 1);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isTrackedWorld(player.getWorld()))
            return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to != null
                && (from.getBlockX() != to.getBlockX()
                        || from.getBlockY() != to.getBlockY()
                        || from.getBlockZ() != to.getBlockZ())) {
            double distance = from.distance(to);
            PlayerStatsFileUtil.increaseStat(player, "Distance", distance);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PersistentDataContainer data = player.getPersistentDataContainer();
        data.set(afkKey, PersistentDataType.BOOLEAN, false);

        String oldSection = data.get(sectionKey, PersistentDataType.STRING);
        String newSection = null;

        if (oldSection != null && sectionNameMap.containsKey(oldSection.toUpperCase())) {
            newSection = sectionNameMap.get(oldSection.toUpperCase());
            data.set(sectionKey, PersistentDataType.STRING, newSection);
            player.sendMessage("Your section has been automatically updated to " + newSection + "!");
        } else if (oldSection != null) {
            newSection = oldSection;
            player.sendMessage("You are in the section " + newSection);
        } else {
            player.sendMessage("You are not in a section");
        }

        PlayerStatsFileUtil.setStat(player, "Last Log-in",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, HH:mm:ss a")));
        PlayerStatsFileUtil.setStat(player, "Username", player.getName());

        if (!player.hasPlayedBefore() && newSection == null) {
            player.sendMessage(
                    "§a§lWelcome to the server, §b§lSTEM Students§a§l of §2§lDe La Salle University Laguna§a§l!");
            player.sendMessage("§7Please §e/joinsection <STEM11-Letter> §7to get started and be part of a section.");
        }

        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = (newSection != null)
                ? board.getTeam(newSection)
                : null;

        if (team == null && newSection != null) {
            team = board.registerNewTeam(newSection);
        }
        if (team != null) {
            team.addEntry(player.getName());
            player.setScoreboard(board);
        }

        startPingUpdater(player);
    }

    @EventHandler
    public void onPlayerCraft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player
                && isTrackedWorld(player.getWorld())) {
            PlayerStatsFileUtil.increaseStat(player, "Items Crafted", 1);
        }
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            Player player = event.getPlayer();
            if (isTrackedWorld(player.getWorld())) {
                PlayerStatsFileUtil.increaseStat(player, "Fish Caught", 1);
            }
        }
    }

    @EventHandler
    public void onTrade(InventoryClickEvent event) {
        if (event.getInventory().getType() == InventoryType.MERCHANT
                && event.getWhoClicked() instanceof Player player
                && isTrackedWorld(player.getWorld())) {
            PlayerStatsFileUtil.increaseStat(player, "Trades Made", 1);
        }
    }

    private void startPingUpdater(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                // What should happen if the player's section is Optional.empty()?
                String section = Section.toStringOptional(PlayerDataUtil.getPlayerSection(player));

                player.setPlayerListName("[" + section + "]" + player.getName() + " - " + player.getPing() + "ms");
            }
        }.runTaskTimer(plugin, 0L, 1200L);
    }

    // helper methods for externally checking placed blocks
    public boolean isPlayerPlaced(Location loc) {
        return blockConfig.contains(locationToString(loc));
    }

    public void removePlacedBlock(Location loc) {
        blockConfig.set(locationToString(loc), null);
        maybeSaveBlockFile();
    }
}
