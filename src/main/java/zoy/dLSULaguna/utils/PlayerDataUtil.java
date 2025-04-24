package zoy.dLSULaguna.utils;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import zoy.dLSULaguna.DLSULaguna;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class PlayerDataUtil {

    private static DLSULaguna plugin;
    private static NamespacedKey sectionKey;
    private static File playerStateFile;
    private static FileConfiguration playerStateConfig;

    // State caches
    private static final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();
    private static final Map<UUID, ItemStack[]> savedArmor = new HashMap<>();
    private static final Map<UUID, Location> savedLocations = new HashMap<>();
    private static final Map<UUID, GameMode> savedGameModes = new HashMap<>();
    private static final Map<UUID, List<PotionEffect>> savedEffects = new HashMap<>();
    private static final Map<UUID, Float> savedExp = new HashMap<>();
    private static final Map<UUID, Integer> savedLevel = new HashMap<>();
    private static final Map<UUID, Integer> savedFoodLevel = new HashMap<>();
    private static final Map<UUID, Double> savedHealth = new HashMap<>();

    /**
     * Initialize the PlayerDataUtil with plugin instance.
     */
    public static void initialize(DLSULaguna pluginInstance) {
        plugin = pluginInstance;
        sectionKey = new NamespacedKey(plugin, "section_name");
        createPlayerStateFile();
    }

    /**
     * Create or load the player_states.yml file.
     */
    private static void createPlayerStateFile() {
        File file = new File(plugin.getDataFolder(), "player_states.yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
                plugin.getLogger().info("Created player_states.yml");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create player_states.yml", e);
            }
        }
        playerStateFile = file;
        playerStateConfig = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Get section for online or offline player by UUID.
     */
    public static Optional<Section> getPlayerSection(UUID uuid) {
        Player online = plugin.getServer().getPlayer(uuid);
        if (online != null) {
            return getPlayerSection(online);
        }
        return PlayerStatsFileUtil.findSectionByUUID(uuid.toString());
    }

    /**
     * Get section for an online player.
     */
    public static Optional<Section> getPlayerSection(Player player) {
        if (plugin == null || sectionKey == null || player == null)
            return Optional.empty();

        PersistentDataContainer container = player.getPersistentDataContainer();

        if (container.has(sectionKey, PersistentDataType.STRING)) {
            return Optional.ofNullable(
                    container.get(sectionKey, PersistentDataType.STRING))
                    .flatMap((section) -> Section.fromString(section)); // What should happen if a section is invalid?
                                                                        // Right now it just returns null.
        }

        return Optional.empty();
    }

    /**
     * Set section for online or offline player by UUID.
     */
    public static void setPlayerSection(UUID uuid, Optional<Section> section) {
        Player online = plugin.getServer().getPlayer(uuid);
        if (online != null) {
            setPlayerSection(online, section);
        } else {
            // Queue offline assignment in stats file
            PlayerStatsFileUtil.setStatRaw(uuid, "section_name", section);
        }
    }

    /**
     * Set section for an online player.
     */
    public static void setPlayerSection(Player player, Optional<Section> maybeSection) {
        if (plugin == null || sectionKey == null || player == null)
            return;
        PersistentDataContainer container = player.getPersistentDataContainer();
        if (maybeSection.isPresent()) {
            final var section = maybeSection.get();
            container.set(sectionKey, PersistentDataType.STRING, section.toString());
        } else {
            container.remove(sectionKey);
        }
    }

    /**
     * Check if a player is online.
     */
    public static boolean isPlayerOnline(Player player) {
        if (plugin == null || player == null)
            return false;
        return plugin.getServer().getPlayer(player.getUniqueId()) != null;
    }

    /**
     * Check if a player is online by UUID.
     */
    public static boolean isPlayerOnline(UUID uuid) {
        if (plugin == null)
            return false;
        return plugin.getServer().getPlayer(uuid) != null;
    }

    /**
     * Remove section data for an online player.
     */
    public static void removePlayerSectionData(Player player) {
        setPlayerSection(player, Optional.empty());
    }

    /**
     * Remove section data for a player by UUID.
     */
    public static void removePlayerSectionData(UUID uuid) {
        setPlayerSection(uuid, Optional.empty());
    }

    /**
     * Load a player's saved state into memory.
     */
    public static void loadPlayerState(UUID playerUUID) {
        if (playerStateConfig == null)
            return;
        String key = playerUUID.toString();
        if (!playerStateConfig.contains(key))
            return;
        try {
            savedInventories.put(playerUUID,
                    deserializeItemStackArray(playerStateConfig.getList(key + ".inventory")));
            savedArmor.put(playerUUID,
                    deserializeItemStackArray(playerStateConfig.getList(key + ".armor")));
            String worldName = playerStateConfig.getString(key + ".location.world");
            World world = plugin.getServer().getWorld(worldName);
            Location loc = new Location(world,
                    playerStateConfig.getDouble(key + ".location.x"),
                    playerStateConfig.getDouble(key + ".location.y"),
                    playerStateConfig.getDouble(key + ".location.z"));
            savedLocations.put(playerUUID, loc);
            savedGameModes.put(playerUUID,
                    GameMode.valueOf(playerStateConfig.getString(key + ".gameMode", "SURVIVAL")));
            savedEffects.put(playerUUID,
                    deserializePotionEffects(playerStateConfig.getList(key + ".effects")));
            savedExp.put(playerUUID, (float) playerStateConfig.getDouble(key + ".exp"));
            savedLevel.put(playerUUID, playerStateConfig.getInt(key + ".level"));
            savedFoodLevel.put(playerUUID, playerStateConfig.getInt(key + ".foodLevel"));
            savedHealth.put(playerUUID, playerStateConfig.getDouble(key + ".health"));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load data for UUID: " + playerUUID);
            e.printStackTrace();
        }
    }

    /**
     * Save a player's current state to disk.
     */
    public static void savePlayerState(UUID playerUUID) {
        if (playerStateConfig == null)
            return;
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null || !player.isOnline()) {
            plugin.getLogger().warning("Attempted to save state for offline player: " + playerUUID);
            return;
        }
        String key = playerUUID.toString();
        savedInventories.put(playerUUID, player.getInventory().getContents());
        savedArmor.put(playerUUID, player.getInventory().getArmorContents());
        savedLocations.put(playerUUID, player.getLocation());
        savedGameModes.put(playerUUID, player.getGameMode());
        savedEffects.put(playerUUID, new ArrayList<>(player.getActivePotionEffects()));
        savedExp.put(playerUUID, player.getExp());
        savedLevel.put(playerUUID, player.getLevel());
        savedFoodLevel.put(playerUUID, player.getFoodLevel());
        savedHealth.put(playerUUID, player.getHealth());
        try {
            playerStateConfig.set(key + ".inventory", serializeItemStackArray(savedInventories.get(playerUUID)));
            playerStateConfig.set(key + ".armor", serializeItemStackArray(savedArmor.get(playerUUID)));
            Location loc = savedLocations.get(playerUUID);
            if (loc != null) {
                playerStateConfig.set(key + ".location.world", loc.getWorld().getName());
                playerStateConfig.set(key + ".location.x", loc.getX());
                playerStateConfig.set(key + ".location.y", loc.getY());
                playerStateConfig.set(key + ".location.z", loc.getZ());
            }
            playerStateConfig.set(key + ".gameMode", savedGameModes.get(playerUUID).name());
            playerStateConfig.set(key + ".effects", serializePotionEffects(savedEffects.get(playerUUID)));
            playerStateConfig.set(key + ".exp", savedExp.get(playerUUID));
            playerStateConfig.set(key + ".level", savedLevel.get(playerUUID));
            playerStateConfig.set(key + ".foodLevel", savedFoodLevel.get(playerUUID));
            playerStateConfig.set(key + ".health", savedHealth.get(playerUUID));
            playerStateConfig.save(playerStateFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player state for UUID: " + playerUUID);
            e.printStackTrace();
        }
    }

    /**
     * Restore a player's saved state from memory.
     */
    public static void restorePlayerState(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (savedInventories.containsKey(playerUUID)) {
            player.getInventory().setContents(savedInventories.get(playerUUID));
            player.getInventory().setArmorContents(savedArmor.get(playerUUID));
        }
        if (savedLocations.containsKey(playerUUID)) {
            player.teleport(savedLocations.get(playerUUID));
        }
        if (savedGameModes.containsKey(playerUUID)) {
            player.setGameMode(savedGameModes.get(playerUUID));
        }
        if (savedEffects.containsKey(playerUUID)) {
            player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
            player.addPotionEffects(savedEffects.get(playerUUID));
        }
        if (savedExp.containsKey(playerUUID)) {
            player.setExp(savedExp.get(playerUUID));
        }
        if (savedLevel.containsKey(playerUUID)) {
            player.setLevel(savedLevel.get(playerUUID));
        }
        if (savedFoodLevel.containsKey(playerUUID)) {
            player.setFoodLevel(savedFoodLevel.get(playerUUID));
        }
        if (savedHealth.containsKey(playerUUID)) {
            player.setHealth(savedHealth.get(playerUUID));
        }
        player.updateInventory();
        plugin.getLogger().info("Restored state for player: " + player.getName());
    }

    /**
     * Deserialize a list from YAML into an ItemStack array.
     */
    private static ItemStack[] deserializeItemStackArray(List<?> list) {
        if (list == null)
            return new ItemStack[0];
        return list.stream()
                .filter(o -> o instanceof ItemStack)
                .map(o -> (ItemStack) o)
                .toArray(ItemStack[]::new);
    }

    /**
     * Serialize an ItemStack array into a list of strings "MATERIAL:amount".
     */
    private static List<String> serializeItemStackArray(ItemStack[] items) {
        List<String> list = new ArrayList<>();
        if (items != null) {
            for (ItemStack item : items) {
                if (item != null) {
                    list.add(item.getType() + ":" + item.getAmount());
                }
            }
        }
        return list;
    }

    /**
     * Deserialize a list of potion effect maps into PotionEffect objects.
     */
    private static List<PotionEffect> deserializePotionEffects(List<?> list) {
        List<PotionEffect> effects = new ArrayList<>();
        if (list == null)
            return effects;
        for (Object obj : list) {
            if (obj instanceof Map<?, ?> map) {
                try {
                    String type = (String) map.get("type");
                    int duration = (int) map.get("duration");
                    int amplifier = (int) map.get("amplifier");
                    PotionEffectType pet = PotionEffectType.getByName(type);
                    if (pet != null) {
                        effects.add(new PotionEffect(pet, duration, amplifier));
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return effects;
    }

    /**
     * Serialize a list of PotionEffect into a list of maps for YAML.
     */
    private static List<Map<String, Object>> serializePotionEffects(List<PotionEffect> effects) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (effects != null) {
            for (PotionEffect effect : effects) {
                Map<String, Object> map = new HashMap<>();
                map.put("type", effect.getType().getName());
                map.put("duration", effect.getDuration());
                map.put("amplifier", effect.getAmplifier());
                list.add(map);
            }
        }
        return list;
    }
}
