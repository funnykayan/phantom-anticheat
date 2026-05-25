package com.example.phantomanticheat.replay;

import com.example.phantomanticheat.PhantomAnticheat;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReplayService {

    public static class TickPos {
        public final long t;
        public final double x,y,z;
        public final float yaw,pitch;
        public TickPos(long t, double x, double y, double z, float yaw, float pitch) { this.t=t;this.x=x;this.y=y;this.z=z;this.yaw=yaw;this.pitch=pitch; }
        public String toLine(){return t+","+x+","+y+","+z+","+yaw+","+pitch+"\n";}
    }

    private final PhantomAnticheat plugin;
    private final Map<String, Deque<TickPos>> buffers = new ConcurrentHashMap<>();
    private final int capacity;

    public ReplayService(PhantomAnticheat plugin, int capacity) {
        this.plugin = plugin;
        this.capacity = capacity;
    }

    public void record(Player p) {
        Deque<TickPos> buf = buffers.computeIfAbsent(p.getName(), k -> new ArrayDeque<>());
        Location l = p.getLocation();
        long now = Instant.now().toEpochMilli();
        buf.addLast(new TickPos(now, l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch()));
        if (buf.size() > capacity) buf.removeFirst();
    }

    public void saveClip(String player, Path out) throws IOException {
        Deque<TickPos> buf = buffers.get(player);
        if (buf == null || buf.isEmpty()) throw new IOException("No data");
        StringBuilder sb = new StringBuilder();
        for (TickPos t : buf) sb.append(t.toLine());
        Files.createDirectories(out.getParent());
        Files.write(out, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    public void playClipToSender(Path clip, Player viewer) throws IOException {
        // simple playback: read lines and teleport viewer along
        java.util.List<String> lines = Files.readAllLines(clip, StandardCharsets.UTF_8);
        new BukkitRunnable() {
            int idx = 0;
            long prevT = -1;
            @Override
            public void run() {
                if (idx >= lines.size()) { cancel(); return; }
                String[] parts = lines.get(idx).split(",");
                long t = Long.parseLong(parts[0]);
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                double z = Double.parseDouble(parts[3]);
                float yaw = Float.parseFloat(parts[4]);
                float pitch = Float.parseFloat(parts[5]);
                viewer.teleport(new Location(viewer.getWorld(), x, y, z, yaw, pitch));
                long delay = 50;
                if (prevT > 0) delay = Math.max(1, (int)(t - prevT));
                prevT = t;
                idx++;
                if (idx < lines.size()) runTaskLater(plugin, delay);
            }
        }.runTask(plugin);
    }
}
