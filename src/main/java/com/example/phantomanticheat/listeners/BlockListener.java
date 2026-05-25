package com.example.phantomanticheat.listeners;

import com.example.phantomanticheat.PhantomAnticheat;
import com.example.phantomanticheat.detection.DetectionManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockListener implements Listener {

    private final PhantomAnticheat plugin;
    private final DetectionManager dm;

    public BlockListener(PhantomAnticheat plugin, DetectionManager dm) {
        this.plugin = plugin;
        this.dm = dm;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (p.hasPermission("phantomanticheat.bypass")) return;
        Block b = e.getBlock();
        Material m = b.getType();
        dm.recordBlockBreak(p.getName(), m);
        // record heatmap CSV via DataManager as well
        plugin.getDataManager().recordBlockBreak(p.getName(), m, b.getLocation());

        if (plugin.getConfig().getBoolean("checks.xray.notify", true) && plugin.getConfig().getBoolean("verbose", false)) {
            plugin.getLogger().info("[Block] " + p.getName() + " broke " + m.name());
        }
    }
}
