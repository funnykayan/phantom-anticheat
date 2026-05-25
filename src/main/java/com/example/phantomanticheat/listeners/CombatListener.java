package com.example.phantomanticheat.listeners;

import com.example.phantomanticheat.PhantomAnticheat;
import com.example.phantomanticheat.detection.DetectionManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class CombatListener implements Listener {

    private final PhantomAnticheat plugin;
    private final DetectionManager dm;

    public CombatListener(PhantomAnticheat plugin, DetectionManager dm) {
        this.plugin = plugin;
        this.dm = dm;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();
        if (!(damager instanceof Player)) return;
        Player p = (Player) damager;
        if (p.hasPermission("phantomanticheat.bypass")) return;

        // record an attack for kill-aura heuristics
        dm.recordAttack(p.getName());

        // reach and aim assist checks
        if (plugin.getConfig().getBoolean("checks.reach", true)) {
            double reachMax = plugin.getConfig().getDouble("checks.reach.max", 4.5);
            if (e.getEntity() instanceof org.bukkit.entity.LivingEntity) {
                org.bukkit.entity.LivingEntity target = (org.bukkit.entity.LivingEntity) e.getEntity();
                double dx = target.getLocation().getX() - p.getLocation().getX();
                double dy = target.getLocation().getY() + target.getEyeHeight() - (p.getLocation().getY() + p.getEyeHeight());
                double dz = target.getLocation().getZ() - p.getLocation().getZ();
                double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
                if (dist > reachMax + 0.3) {
                    dm.logDetection(String.format("[Reach] %s hit %.2f blocks (max=%.2f)", p.getName(), dist, reachMax));
                }
                // aim-assist: compare player's yaw toward target vs current yaw
                float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
                double yawDiff = Math.abs(dm.getStats(p.getName()).lastYaw - targetYaw);
                if (yawDiff > 180) yawDiff = 360 - yawDiff;
                if (yawDiff < plugin.getConfig().getDouble("checks.aim.yaw-snap-threshold", 2.0)) {
                    dm.logDetection(String.format("[AimSnap] %s yawDiff=%.2f", p.getName(), yawDiff));
                }
            }
        }

        // criticals heuristic: check if player is always critting or never critting
        if (plugin.getConfig().getBoolean("checks.criticals", true)) {
            boolean wasCrit = p.getFallDistance() > 0.0f && !p.isOnGround() && !p.isInsideVehicle();
            com.example.phantomanticheat.detection.DetectionManager.PlayerStats s = dm.getStats(p.getName());
            s.critCount += (wasCrit ? 1 : 0);
            s.hitCount++;
            if (s.hitCount >= 8) {
                double critRatio = s.critCount / (double) s.hitCount;
                double low = plugin.getConfig().getDouble("checks.criticals.low-ratio", 0.05);
                double high = plugin.getConfig().getDouble("checks.criticals.high-ratio", 0.95);
                if (critRatio < low) dm.logDetection(String.format("[Criticals] %s rarely crits (ratio=%.2f)", p.getName(), critRatio));
                if (critRatio > high) dm.logDetection(String.format("[Criticals] %s almost always crits (ratio=%.2f)", p.getName(), critRatio));
                s.hitCount = 0; s.critCount = 0;
            }
        }

        if (plugin.getConfig().getBoolean("checks.kill-aura.notify", true)) {
            // optional immediate verbose log
            if (plugin.getConfig().getBoolean("verbose", false)) {
                plugin.getLogger().info("[Combat] " + p.getName() + " hit " + e.getEntity().getName());
            }
        }
    }
}
