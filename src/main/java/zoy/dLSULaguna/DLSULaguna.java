package zoy.dLSULaguna;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import zoy.dLSULaguna.commands.*;
import zoy.dLSULaguna.listeners.*;
import zoy.dLSULaguna.utils.*;
import zoy.dLSULaguna.utils.playerevents.Bounties;
import zoy.dLSULaguna.utils.playerevents.BuildBattle;
import zoy.dLSULaguna.utils.playerevents.Duels;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public final class DLSULaguna extends JavaPlugin {

    private File playersStatsFile;
    private File sectionStatsFile;
    private File sectionsFile;

    private Bounties bounties;
    private BuildBattle buildBattle;
    private TrackPlayerCommand trackPlayerCommand;
    private Duels duels;

    @Override
    public void onEnable() {
        getLogger().info("DLSU Laguna Plugin Enabling...");

        setupDataFiles();
        initializeUtils();
        initializeGameManagers();
        registerCommands();
        registerListeners();
        startSchedulers();

        // Kick off the in‑game sidebar auto‑refresh: every 60s (1200 ticks)
        String title = ChatColor.YELLOW + "" + ChatColor.BOLD + "Section Points";
        ScoreboardUtil.startAutoDisplay("Points", title, 1200L);

        getLogger().info("DLSU Laguna Plugin Enabled Successfully.");
    }

    @Override
    public void onDisable() {
        getLogger().info("DLSU Laguna Plugin Disabling...");
        Bukkit.getScheduler().cancelTasks(this);
        getLogger().info("DLSU Laguna Plugin Disabled.");
    }

    private void setupDataFiles() {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            getLogger().severe("Could not create plugin data folder!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        sectionStatsFile = new File(dataFolder, "section_stats.yml");
        playersStatsFile = new File(dataFolder, "players_stats.yml");
        sectionsFile = new File(dataFolder, "sections.yml");

        createFile(sectionStatsFile, "section_stats.yml");
        createFile(playersStatsFile, "players_stats.yml");
        createFile(sectionsFile, "sections.yml");
    }

    private void createFile(File file, String name) {
        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    getLogger().info("Created " + name);
                }
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Could not create " + name + "!", e);
                getServer().getPluginManager().disablePlugin(this);
            }
        }
    }

    private void initializeUtils() {
        PlayerDataUtil.initialize(this);
        PlayerStatsFileUtil.initialize(this);
        SectionStatsFileUtil.initialize(this);
        SectionFileUtil.initialize(this);
        PointsCalculatorUtil.initialize(this);
        ScoreboardUtil.initialize(this);
        DiscordUtil.initialize();
    }

    private void initializeGameManagers() {
        bounties = new Bounties(this);
        buildBattle = new BuildBattle(this);
        // **Fix**: create the BuildBattle world immediately
        buildBattle.createBuildWorld();

        duels = new Duels(this);
        trackPlayerCommand = new TrackPlayerCommand(this);
    }

    private void registerCommands() {
        getCommand("joinsection").setExecutor(new JoinSection(this));
        getCommand("joinsection").setTabCompleter(new JoinSection(this));
        getCommand("leavesection").setExecutor(new LeaveSection(this));
        getCommand("tallypoints").setExecutor(new TallyPoints(this));
        getCommand("clearpoints").setExecutor(new ClearPoints(this));
        getCommand("clearplayerstats").setExecutor(new ClearPlayerStats());
        getCommand("doomsday").setExecutor(new DoomsDay(this));
        getCommand("sectionchat").setExecutor(new SectionChat());
        getCommand("seepoints").setExecutor(new SeePoints(this));
        getCommand("sectionleaderboard").setExecutor(new SectionLeaderboard(this));
        getCommand("setplayerpoints").setExecutor(new SetPlayerPoints(this));
        getCommand("createsection").setExecutor(new CreateSection());
        getCommand("deletesection").setExecutor(new DeleteSection());
        getCommand("bountylist").setExecutor(new BountyListCommand(this));
        getCommand("trackplayer").setExecutor(trackPlayerCommand);
        getCommand("duel").setExecutor(duels);
        getCommand("duelaccept").setExecutor((s, c, l, a) -> duels.duelAcceptCommand(s, c, l, a));
        getCommand("dueldeny").setExecutor((s, c, l, a) -> duels.duelDenyCommand(s, c, l, a));
        getCommand("buildbattle").setExecutor(buildBattle);
    }

    private void registerListeners() {
        // Core event listeners
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(
                new PlayerChatListener(this, (SectionChat) getCommand("sectionchat").getExecutor()),
                this);
        Bukkit.getPluginManager().registerEvents(new PlayerStatTracker(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerSectionListener(this), this);

        // Game managers as listeners
        Bukkit.getPluginManager().registerEvents(bounties, this);
        Bukkit.getPluginManager().registerEvents(duels, this);
        Bukkit.getPluginManager().registerEvents(trackPlayerCommand, this);
        Bukkit.getPluginManager().registerEvents(buildBattle, this);
    }

    private void startSchedulers() {
        // Bounty refresh every 20 mins
        bounties.startBountyScheduler();

        // Discord scoreboard update every minute
        String discordChannel = "1362873191256821780";
        Bukkit.getScheduler().runTaskTimerAsynchronously(
                this,
                new ScoreUpdateTask(this, discordChannel),
                20L * 10, // initial delay: 10s
                20L * 60 // repeat: 60s
        );
        getLogger().info("Scheduled ScoreUpdateTask to run every 1 minute.");
    }

    /**
     * Expose your BuildBattle instance so listeners can call
     * plugin.getBuildBattle().getBuildWorld()
     */
    public BuildBattle getBuildBattle() {
        return buildBattle;
    }

    public File getPlayersStatsFile() {
        return playersStatsFile;
    }

    public File getSectionStatsFile() {
        return sectionStatsFile;
    }

    public File getSectionsFile() {
        return sectionsFile;
    }
}
