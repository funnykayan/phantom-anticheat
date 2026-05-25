package com.example.phantomanticheat.listeners;

import com.example.phantomanticheat.PhantomAnticheat;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class MovementListener implements Listener {

    private final PhantomAnticheat plugin;
    private final com.example.phantomanticheat.detection.DetectionManager dm;

    public MovementListener(PhantomAnticheat plugin, com.example.phantomanticheat.detection.DetectionManager dm) {
        this.plugin = plugin;
        this.dm = dm;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getWorld() == null || event.getTo() == null) return;
        Player p = event.getPlayer();
        boolean verbose = plugin.getConfig().getBoolean("verbose", false);

        // speed check
        if (plugin.getConfig().getBoolean("checks.speed", true)) {
            double dx = event.getTo().getX() - event.getFrom().getX();
            double dz = event.getTo().getZ() - event.getFrom().getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            double max = plugin.getConfig().getDouble("max-speed", 5.0);
            if (dist > max) {
                if (verbose) {
                    plugin.getLogger().info("[Speed] " + p.getName() + " moved " + String.format("%.3f", dist) + " (>" + max + ")");
                    p.sendMessage("[AntiCheat] Speed exceeded: " + String.format("%.3f", dist));
                }
                event.setTo(event.getFrom());
            }
        }

        // fly check (basic)
        if (plugin.getConfig().getBoolean("checks.fly", true)) {
            if (p.isFlying() && p.getGameMode() != GameMode.CREATIVE && !p.isInsideVehicle()) {
                if (verbose) {
                    plugin.getLogger().info("[Fly] " + p.getName() + " is flying in non-creative mode.");
                    p.sendMessage("[AntiCheat] Flying detected (disabled in this mode).");
                }
                p.setFlying(false);
                p.setAllowFlight(false);
            }
        }
        // record movement as an action for bot detection
        dm.recordAction(p.getName());
        // record rotation deltas for aim/rotation checks
        float yaw = p.getLocation().getYaw();
        float pitch = p.getLocation().getPitch();
        dm.recordRotation(p.getName(), yaw, pitch);
    }
}
