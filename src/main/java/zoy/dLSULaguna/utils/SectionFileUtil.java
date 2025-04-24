package zoy.dLSULaguna.utils;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import zoy.dLSULaguna.DLSULaguna;

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

public class SectionFileUtil {
    private static DLSULaguna plugin;
    private static File sectionsFile;
    private static FileConfiguration sectionConfig;

    // Static initialization method
    public static void initialize(DLSULaguna pluginInstance) {
        plugin = pluginInstance;
        sectionsFile = new File(plugin.getDataFolder(), "sections.yml");
        sectionConfig = YamlConfiguration.loadConfiguration(sectionsFile);
    }

    public static void createSection(Section section) {
        if (plugin == null) {
            plugin.getLogger().severe("SectionFileUtil has not been initialized!");
            return;
        }
        if (sectionConfig == null) {
            sectionConfig = YamlConfiguration.loadConfiguration(sectionsFile);
        }
        ConfigurationSection sectionsSection = sectionConfig.getConfigurationSection("sections");
        if (sectionsSection == null) {
            sectionsSection = sectionConfig.createSection("sections");
        }

        final var sectionName = section.toString();

        if (!sectionsSection.contains(sectionName)) {
            sectionsSection.set(sectionName, true); // You can set a dummy value for now
            try {
                sectionConfig.save(sectionsFile);
                plugin.getLogger().info("Added new section: " + sectionName + " to sections.yml");
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save sections.yml! " + e.getMessage());
            }
        } else {
            plugin.getLogger().warning("Section '" + sectionName + "' already exists in sections.yml.");
        }
    }

    public static void deleteSection(Section section) {
        final var sectionName = section.toString();

        ConfigurationSection sectionsSection = sectionConfig.getConfigurationSection("sections");
        if (sectionsSection != null && sectionsSection.contains(sectionName)) {
            sectionsSection.set(sectionName, null); // Set to null to delete the key
            try {
                sectionConfig.save(sectionsFile);
                if (plugin != null) {
                    plugin.getLogger().info("Deleted section: " + sectionName + " from sections.yml");
                }
            } catch (IOException e) {
                if (plugin != null) {
                    plugin.getLogger().severe(
                            "Could not save sections.yml after deleting " + sectionName + "! " + e.getMessage());
                }
            }
        } else if (plugin != null) {
            plugin.getLogger().warning("Section '" + sectionName + "' does not exist in sections.yml.");
        }
    }

    public static java.util.Set<Section> getSectionKeys() {
        ConfigurationSection sectionsSection = sectionConfig.getConfigurationSection("sections");
        if (sectionsSection != null) {
            return sectionsSection
                    .getKeys(false)
                    .stream()
                    .flatMap((sectionString) -> Section.fromString(sectionString).stream()) // Is it ok to discard the
                                                                                            // sections that don't fit
                                                                                            // the [Section] format?
                    .collect(Collectors.toSet());
        }
        return new java.util.HashSet<>(); // Return an empty set if the section doesn't exist
    }
}