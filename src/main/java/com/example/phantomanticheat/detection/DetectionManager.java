package com.example.phantomanticheat.detection;

import com.example.phantomanticheat.PhantomAnticheat;
import org.bukkit.Material;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DetectionManager {

    private final PhantomAnticheat plugin;
    private final Map<String, PlayerStats> stats = new ConcurrentHashMap<>();
    private final Set<Material> ores = Set.of(
            Material.COAL_ORE, Material.DIAMOND_ORE, Material.IRON_ORE,
            Material.GOLD_ORE, Material.EMERALD_ORE, Material.LAPIS_ORE,
            Material.REDSTONE_ORE, Material.COPPER_ORE
    );

    public DetectionManager(PhantomAnticheat plugin) {
        this.plugin = plugin;
    }

    public PlayerStats getStats(String player) {
        return stats.computeIfAbsent(player, k -> new PlayerStats());
    }

    public void recordPlace(String player) {
        PlayerStats s = getStats(player);
        long now = Instant.now().toEpochMilli();
        s.placeTimestamps.add(now);
        if (s.placeTimestamps.size() > 100) s.placeTimestamps.removeFirst();
        checkFastPlace(player, s);
    }

    public void recordAttack(String player) {
        PlayerStats s = getStats(player);
        s.attackTimestamps.add(Instant.now().toEpochMilli());
        if (s.attackTimestamps.size() > 50) s.attackTimestamps.removeFirst();
        checkKillAura(player, s);
    }

    public void recordAction(String player) {
        PlayerStats s = getStats(player);
        long now = Instant.now().toEpochMilli();
        s.actionTimestamps.add(now);
        if (s.actionTimestamps.size() > 100) s.actionTimestamps.removeFirst();
        checkBot(player, s);
    }

    public void recordBlockBreak(String player, Material m) {
        PlayerStats s = getStats(player);
        s.totalBlocks++;
        if (ores.contains(m)) s.oreBlocks++;
        if (++s.recentBreaks > 200) s.recentBreaks = 200;
        checkXray(player, s);
    }

    public void recordInventoryAction(String player) {
        PlayerStats s = getStats(player);
        long now = Instant.now().toEpochMilli();
        s.invTimestamps.add(now);
        if (s.invTimestamps.size() > 200) s.invTimestamps.removeFirst();
        checkInventory(player, s);
    }

    public void recordRotation(String player, double yaw, double pitch) {
        PlayerStats s = getStats(player);
        long now = Instant.now().toEpochMilli();
        double lastYaw = s.lastYaw;
        double lastPitch = s.lastPitch;
        if (!Double.isNaN(lastYaw)) {
            double dy = Math.abs(angleDelta(yaw, lastYaw));
            double dp = Math.abs(pitch - lastPitch);
            s.yawDeltas.add(dy);
            s.pitchDeltas.add(dp);
            if (s.yawDeltas.size() > 200) s.yawDeltas.removeFirst();
            if (s.pitchDeltas.size() > 200) s.pitchDeltas.removeFirst();
            checkRotation(player, s, dy, dp);
        }
        s.lastYaw = yaw;
        s.lastPitch = pitch;
        s.lastRotationTime = now;
    }

    private double angleDelta(double a, double b) {
        double d = a - b;
        while (d <= -180) d += 360;
        while (d > 180) d -= 360;
        return d;
    }

    private void checkFastPlace(String player, PlayerStats s) {
        if (s.placeTimestamps.size() < 4) return;
        long now = Instant.now().toEpochMilli();
        long window = plugin.getConfig().getInt("checks.fastplace.window-ms", 500);
        int count = 0;
        for (long t : s.placeTimestamps) if (t >= now - window) count++;
        int threshold = plugin.getConfig().getInt("checks.fastplace.threshold", 8);
        if (count > threshold) logDetection(String.format("[FastPlace] %s placed %d blocks in %dms", player, count, window));
    }

    private void checkInventory(String player, PlayerStats s) {
        if (s.invTimestamps.size() < 8) return;
        long now = Instant.now().toEpochMilli();
        long window = plugin.getConfig().getInt("checks.inventory.window-ms", 1000);
        int count = 0;
        for (long t : s.invTimestamps) if (t >= now - window) count++;
        int threshold = plugin.getConfig().getInt("checks.inventory.threshold", 20);
        if (count > threshold) logDetection(String.format("[Inventory] %s had %d clicks in %dms", player, count, window));
    }

    private void checkRotation(String player, PlayerStats s, double dy, double dp) {
        double maxYaw = plugin.getConfig().getDouble("checks.rotation.max-yaw-per-tick", 90.0);
        double maxPitch = plugin.getConfig().getDouble("checks.rotation.max-pitch-per-tick", 45.0);
        if (dy > maxYaw || dp > maxPitch) {
            logDetection(String.format("[Rotation] %s yawDelta=%.2f pitchDelta=%.2f", player, dy, dp));
        }
        // detect very low variance (aimlock-like)
        if (s.yawDeltas.size() >= 6) {
            double avg = s.yawDeltas.stream().mapToDouble(d -> d).average().orElse(0.0);
            double var = s.yawDeltas.stream().mapToDouble(d -> (d - avg) * (d - avg)).sum() / s.yawDeltas.size();
            double std = Math.sqrt(var);
            double stdThresh = plugin.getConfig().getDouble("checks.aim.std-threshold", 0.5);
            if (std < stdThresh && avg > 0.5) {
                logDetection(String.format("[AimAssist] %s yawStd=%.3f avg=%.3f samples=%d", player, std, avg, s.yawDeltas.size()));
            }
        }
    }

    private void checkKillAura(String player, PlayerStats s) {
        // simple heuristic: many attacks in short time -> possible kill aura
        if (s.attackTimestamps.size() < 6) return;
        long now = Instant.now().toEpochMilli();
        long windowStart = now - 1000; // 1 second
        int count = 0;
        for (long t : s.attackTimestamps) if (t >= windowStart) count++;
        double threshold = plugin.getConfig().getDouble("checks.kill-aura.threshold-per-sec", 8.0);
        if (count > threshold) {
            String msg = String.format("[KillAura] %s attacked %d times in the last second", player, count);
            logDetection(msg);
        }
    }

    private void checkBot(String player, PlayerStats s) {
        // heuristic: extremely regular actions (low variance) and many actions
        if (s.actionTimestamps.size() < 8) return;
        long first = s.actionTimestamps.peekFirst();
        long last = s.actionTimestamps.peekLast();
        double avgInterval = (last - first) / (double) Math.max(1, s.actionTimestamps.size() - 1);
        double variance = 0.0;
        for (long t : s.actionTimestamps) {
            double d = t - first;
            variance += (d - avgInterval) * (d - avgInterval);
        }
        variance /= Math.max(1, s.actionTimestamps.size());
        double std = Math.sqrt(variance);
        double stdThreshold = plugin.getConfig().getDouble("checks.bot.std-threshold-ms", 20.0);
        double avgThreshold = plugin.getConfig().getDouble("checks.bot.avg-interval-ms", 200.0);
        if (avgInterval < avgThreshold && std < stdThreshold) {
            String msg = String.format("[Bot] %s avgInterval=%.1fms std=%.1fms actions=%d", player, avgInterval, std, s.actionTimestamps.size());
            logDetection(msg);
        }
    }

    private void checkXray(String player, PlayerStats s) {
        // heuristic: high ore ratio within recent blocks
        if (s.totalBlocks < 20) return;
        double ratio = s.oreBlocks / (double) s.totalBlocks;
        double threshold = plugin.getConfig().getDouble("checks.xray.ore-ratio", 0.35);
        if (ratio > threshold && s.totalBlocks >= plugin.getConfig().getInt("checks.xray.min-breaks", 50)) {
            String msg = String.format("[XRay] %s oreRatio=%.2f (ores=%d total=%d)", player, ratio, s.oreBlocks, s.totalBlocks);
            logDetection(msg);
            // reset counters to avoid repeated spam
            s.totalBlocks = 0; s.oreBlocks = 0;
        }
    }

    public synchronized void logDetection(String line) {
        String out = Instant.now().toString() + " " + line + System.lineSeparator();
        plugin.getLogger().warning(line);
        if (plugin.getConfig().getBoolean("verbose", false)) plugin.getLogger().info("[Verbose] " + line);
        try {
            Path p = plugin.getDataFolder().toPath().resolve("detections.log");
            Files.createDirectories(p.getParent());
            Files.write(p, out.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to write detection log: " + e.getMessage());
        }
    }
    
    private void onFlag(String player, String reason, int severity) {
        // record flag to DataManager, save evidence clip and possibly auto-punish
        plugin.getDataManager().recordFlag(player, reason, severity);
        try {
            Path clip = plugin.getDataFolder().toPath().resolve("evidence").resolve(player + "-latest.clip");
            plugin.getReplayService().saveClip(player, clip);
        } catch (IOException ignored) {}
        // notify webhook if configured
        try {
            String hook = plugin.getConfig().getString("webhook.url", "");
            if (hook != null && !hook.isEmpty()) {
                new com.example.phantomanticheat.notify.WebhookNotifier(plugin, hook).sendAlert("Detection: " + player, reason + " (sev=" + severity + ")");
            }
        } catch (Exception ignored) {}

        // spawn temporary hologram marker at last known rotation/position if available
        try {
            com.example.phantomanticheat.detection.DetectionManager.PlayerStats s = getStats(player);
            // best-effort: spawn armor stand at player last known world location stored in replay buffer if present
            java.nio.file.Path clip = plugin.getDataFolder().toPath().resolve("evidence").resolve(player + "-latest.clip");
            if (java.nio.file.Files.exists(clip)) {
                // create simple hologram in player's world at last recorded pos (not exact, but useful)
                // attempt to read last line of clip
                java.util.List<String> lines = java.nio.file.Files.readAllLines(clip);
                if (!lines.isEmpty()) {
                    String last = lines.get(lines.size()-1);
                    String[] p = last.split(",");
                    org.bukkit.World w = plugin.getServer().getWorlds().get(0);
                    double x = Double.parseDouble(p[1]);
                    double y = Double.parseDouble(p[2]);
                    double z = Double.parseDouble(p[3]);
                    org.bukkit.Location loc = new org.bukkit.Location(w, x, y+1.0, z);
                    org.bukkit.entity.ArmorStand as = w.spawn(loc, org.bukkit.entity.ArmorStand.class);
                    as.setVisible(false);
                    as.setSmall(true);
                    as.setCustomName("[FLAG] " + player + ": " + reason);
                    as.setCustomNameVisible(true);
                    // remove after configurable seconds
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> as.remove(), plugin.getConfig().getInt("markers.duration-seconds", 20) * 20L);
                }
            }
        } catch (Exception ignored) {}

        // configurable auto punish
        if (severity >= plugin.getConfig().getInt("auto-punish.threshold", 10)) {
            String action = plugin.getConfig().getString("auto-punish.action", "warn");
            if ("kick".equalsIgnoreCase(action)) plugin.getPunishManager().kick(player, reason);
            else if ("tempban".equalsIgnoreCase(action)) plugin.getPunishManager().tempban(player, java.time.Duration.ofMinutes(plugin.getConfig().getInt("auto-punish.temp-minutes", 60)), reason);
            else plugin.getPunishManager().warn(player, reason);
        }
    }

    public synchronized void appendChat(String line) {
        appendToFile("chat.log", line);
    }

    public synchronized void appendCommand(String line) {
        appendToFile("commands.log", line);
    }

    private void appendToFile(String fileName, String line) {
        String out = Instant.now().toString() + " " + line + System.lineSeparator();
        try {
            Path p = plugin.getDataFolder().toPath().resolve(fileName);
            Files.createDirectories(p.getParent());
            Files.write(p, out.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to write " + fileName + ": " + e.getMessage());
        }
        if (plugin.getConfig().getBoolean("verbose", false)) plugin.getLogger().info("[Verbose] " + line);
    }

    public static class PlayerStats {
        public final Deque<Long> attackTimestamps = new ArrayDeque<>();
        public final Deque<Long> actionTimestamps = new ArrayDeque<>();
        public final Deque<Long> placeTimestamps = new ArrayDeque<>();
        public final Deque<Long> invTimestamps = new ArrayDeque<>();
        public final Deque<Double> yawDeltas = new ArrayDeque<>();
        public final Deque<Double> pitchDeltas = new ArrayDeque<>();
        public double lastYaw = Double.NaN;
        public double lastPitch = Double.NaN;
        public long lastRotationTime = 0;
        public int critCount = 0;
        public int hitCount = 0;
        int totalBlocks = 0;
        int oreBlocks = 0;
        int recentBreaks = 0;
    }
}
