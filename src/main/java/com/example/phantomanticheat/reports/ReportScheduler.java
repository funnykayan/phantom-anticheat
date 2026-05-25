package com.example.phantomanticheat.reports;

import com.example.phantomanticheat.PhantomAnticheat;
import com.example.phantomanticheat.data.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

public class ReportScheduler {
    private final PhantomAnticheat plugin;

    public ReportScheduler(PhantomAnticheat plugin) { this.plugin = plugin; }

    public void scheduleDaily(int intervalMinutes) {
        new BukkitRunnable() {
            @Override
            public void run() {
                generateDailyCsv();
            }
        }.runTaskTimerAsynchronously(plugin, 20L, 20L * 60L * intervalMinutes);
    }

    public void generateDailyCsv() {
        try {
            DataManager dm = plugin.getDataManager();
            Map<String, Integer> leaderboard = dm.getLeaderboard();
            Path out = plugin.getDataFolder().toPath().resolve("reports").resolve("flags_summary-" + Instant.now().toString().replace(':','-') + ".csv");
            Files.createDirectories(out.getParent());
            StringBuilder sb = new StringBuilder();
            sb.append("player,flags,severity,feedback\n");
            for (Map.Entry<String, Integer> e : leaderboard.entrySet()) {
                int sev = 0; // severity not exposed directly
                int fb = dm.getFeedbackScore(e.getKey());
                sb.append(e.getKey()).append(",").append(e.getValue()).append(",").append(sev).append(",").append(fb).append("\n");
            }
            Files.write(out, sb.toString().getBytes(StandardCharsets.UTF_8));
            plugin.getLogger().info("Generated report " + out.toString());
        } catch (IOException ex) {
            plugin.getLogger().severe("Report generation failed: " + ex.getMessage());
        }
    }
}
