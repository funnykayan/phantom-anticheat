package com.example.phantomanticheat.data;

import com.example.phantomanticheat.PhantomAnticheat;
import com.example.phantomanticheat.detection.DetectionManager;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataManager {
    private final PhantomAnticheat plugin;
    private final Path base;

    // simple in-memory aggregates
    private final Map<String, Integer> flagCounts = new ConcurrentHashMap<>();
    private final Map<String, Integer> severitySum = new ConcurrentHashMap<>();
    private final Map<String, String> lastIp = new ConcurrentHashMap<>();

    public DataManager(PhantomAnticheat plugin) {
        this.plugin = plugin;
        this.base = plugin.getDataFolder().toPath();
        try {
            Files.createDirectories(base);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create data folder: " + e.getMessage());
        }
    }

    public synchronized void recordFlag(String player, String reason, int severity) {
        flagCounts.merge(player, 1, Integer::sum);
        severitySum.merge(player, severity, Integer::sum);
        String line = Instant.now().toString() + " FLAG " + player + " " + severity + " " + reason + "\n";
        writeAppend("flags.log", line);
    }

    public synchronized void recordBlockBreak(String player, org.bukkit.Material mat, org.bukkit.Location loc) {
        String line = Instant.now().toString() + "," + player + "," + mat.name() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + "\n";
        Path p = base.resolve("heatmaps").resolve(player + ".csv");
        writeAppend(p.getFileName().toString(), line);
        writeAppend("breaks.log", line);
    }

    private final Map<String, Integer> feedback = new ConcurrentHashMap<>();
    public synchronized void recordFeedback(String player, boolean truePositive) {
        feedback.merge(player, truePositive ? 1 : -1, Integer::sum);
        writeAppend("feedback.log", Instant.now().toString() + " " + (truePositive?"TP":"FP") + " " + player + "\n");
    }

    public int getFeedbackScore(String player) { return feedback.getOrDefault(player, 0); }

    public Map<String, Integer> getLeaderboard() {
        Map<String, Integer> copy = new HashMap<>(flagCounts);
        return copy;
    }

    public synchronized void recordPlayerIp(String player, String ip) {
        lastIp.put(player, ip);
        writeAppend("ips.log", Instant.now().toString() + " IP " + player + " " + ip + "\n");
    }

    public String getLastIp(String player) {
        return lastIp.get(player);
    }

    public synchronized void appendAudit(String line) {
        writeAppend("audit.log", Instant.now().toString() + " " + line + "\n");
    }

    private void writeAppend(String file, String content) {
        try {
            Path p = base.resolve(file);
            Files.createDirectories(p.getParent());
            Files.write(p, content.getBytes(StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to write " + file + ": " + e.getMessage());
        }
    }

    public void exportPlayerData(String player, Path dest) throws IOException {
        // very small export: copy flags.log and any per-player evidence
        Files.createDirectories(dest.getParent());
        Path flags = base.resolve("flags.log");
        if (Files.exists(flags)) {
            Files.write(dest, Files.readAllBytes(flags));
        } else {
            Files.write(dest, ("No flags for " + player + "\n").getBytes(StandardCharsets.UTF_8));
        }
    }

    // rollback recent block breaks for a player within the last `seconds` seconds
    public void rollbackRecentBlocks(String player, int seconds) {
        Path breaks = base.resolve("breaks.log");
        if (!Files.exists(breaks)) return;
        try {
            long cutoff = java.time.Instant.now().minusSeconds(seconds).toEpochMilli();
            Files.lines(breaks, StandardCharsets.UTF_8).filter(line -> line.contains("," + player + ",") || line.contains("," + player + ","))
                    .map(String::trim).forEach(line -> {
                try {
                    String[] parts = line.split(",");
                    // expected: timestamp,player,material,x,y,z
                    if (parts.length < 6) return;
                    String timeStr = parts[0];
                    long t = java.time.Instant.parse(timeStr).toEpochMilli();
                    if (t < cutoff) return;
                    String who = parts[1];
                    if (!who.equals(player)) return;
                    String mat = parts[2];
                    int x = Integer.parseInt(parts[3]);
                    int y = Integer.parseInt(parts[4]);
                    int z = Integer.parseInt(parts[5]);
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            org.bukkit.World w = plugin.getServer().getWorlds().get(0);
                            org.bukkit.block.Block b = w.getBlockAt(x, y, z);
                            org.bukkit.Material m = org.bukkit.Material.getMaterial(mat);
                            if (m != null) b.setType(m);
                        } catch (Exception ex) { plugin.getLogger().warning("Rollback place failed: " + ex.getMessage()); }
                    });
                } catch (Exception ignored) {}
            });
        } catch (Exception e) { plugin.getLogger().severe("Rollback failed: " + e.getMessage()); }
    }
}
