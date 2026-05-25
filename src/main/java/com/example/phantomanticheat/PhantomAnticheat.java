package com.example.phantomanticheat;

import com.example.phantomanticheat.commands.AnticheatCommand;
import com.example.phantomanticheat.detection.DetectionManager;
import com.example.phantomanticheat.listeners.ChatCommandListener;
import com.example.phantomanticheat.listeners.CombatListener;
import com.example.phantomanticheat.listeners.MovementListener;
import com.example.phantomanticheat.listeners.BlockListener;
import com.example.phantomanticheat.listeners.PlacementListener;
import com.example.phantomanticheat.listeners.InventoryListener;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PhantomAnticheat extends JavaPlugin {

    private static PhantomAnticheat instance;
    private DetectionManager detectionManager;
    private com.example.phantomanticheat.data.DataManager dataManager;
    private com.example.phantomanticheat.replay.ReplayService replayService;
    private com.example.phantomanticheat.punish.PunishManager punishManager;
    private com.example.phantomanticheat.staff.PendingActionManager pendingActionManager;
    private com.example.phantomanticheat.staff.SpectateManager spectateManager;
    private com.example.phantomanticheat.reports.ReportScheduler reportScheduler;
    private com.example.phantomanticheat.antixray.AntiXrayService antiXrayService;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        FileConfiguration cfg = getConfig();
        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        // register listeners and commands
        detectionManager = new DetectionManager(this);
        dataManager = new com.example.phantomanticheat.data.DataManager(this);
        replayService = new com.example.phantomanticheat.replay.ReplayService(this, getConfig().getInt("replay.buffer-size", 600));
        punishManager = new com.example.phantomanticheat.punish.PunishManager(this);
        pendingActionManager = new com.example.phantomanticheat.staff.PendingActionManager(this);
        spectateManager = new com.example.phantomanticheat.staff.SpectateManager(this);
        reportScheduler = new com.example.phantomanticheat.reports.ReportScheduler(this);
        antiXrayService = new com.example.phantomanticheat.antixray.AntiXrayService(this);
        getServer().getPluginManager().registerEvents(new MovementListener(this, detectionManager), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this, detectionManager), this);
        getServer().getPluginManager().registerEvents(new BlockListener(this, detectionManager), this);
        getServer().getPluginManager().registerEvents(new ChatCommandListener(this, detectionManager), this);
        getServer().getPluginManager().registerEvents(new PlacementListener(this, detectionManager), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this, detectionManager), this);
        AnticheatCommand cmd = new AnticheatCommand(this);
        getCommand("anticheat").setExecutor(cmd);
        getCommand("anticheat").setTabCompleter(cmd);

        getServer().getPluginManager().registerEvents(new com.example.phantomanticheat.listeners.PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new com.example.phantomanticheat.listeners.InspectorClickListener(this), this);
        getServer().getPluginManager().registerEvents(new com.example.phantomanticheat.listeners.ChatInputListener(this), this);

        // schedule daily reports (minutes)
        int rpt = getConfig().getInt("reports.interval-minutes", 1440);
        reportScheduler.scheduleDaily(rpt);

        getLogger().info("PhantomAntiCheat enabled. Verbose=" + cfg.getBoolean("verbose", false));
    }

    @Override
    public void onDisable() {
        getLogger().info("PhantomAntiCheat disabled.");
    }

    public static PhantomAnticheat getInstance() {
        return instance;
    }

    public com.example.phantomanticheat.data.DataManager getDataManager() { return dataManager; }

    public com.example.phantomanticheat.replay.ReplayService getReplayService() { return replayService; }

    public com.example.phantomanticheat.punish.PunishManager getPunishManager() { return punishManager; }

    public com.example.phantomanticheat.staff.PendingActionManager getPendingActionManager() { return pendingActionManager; }

    public com.example.phantomanticheat.staff.SpectateManager getSpectateManager() { return spectateManager; }

    public com.example.phantomanticheat.reports.ReportScheduler getReportScheduler() { return reportScheduler; }
}
