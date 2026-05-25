package com.example.phantomanticheat.listeners;

import com.example.phantomanticheat.PhantomAnticheat;
import com.example.phantomanticheat.detection.DetectionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class PlacementListener implements Listener {

    private final PhantomAnticheat plugin;
    private final DetectionManager dm;

    public PlacementListener(PhantomAnticheat plugin, DetectionManager dm) {
        this.plugin = plugin;
        this.dm = dm;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (p.hasPermission("phantomanticheat.bypass")) return;
        dm.recordPlace(p.getName());
        if (plugin.getConfig().getBoolean("checks.fastplace.notify", false) && plugin.getConfig().getBoolean("verbose", false)) {
            plugin.getLogger().info("[Place] " + p.getName() + " placed " + e.getBlock().getType().name());
        }
    }
}
