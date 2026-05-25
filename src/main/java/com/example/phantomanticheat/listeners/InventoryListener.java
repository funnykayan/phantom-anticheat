package com.example.phantomanticheat.listeners;

import com.example.phantomanticheat.PhantomAnticheat;
import com.example.phantomanticheat.detection.DetectionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class InventoryListener implements Listener {

    private final PhantomAnticheat plugin;
    private final DetectionManager dm;

    public InventoryListener(PhantomAnticheat plugin, DetectionManager dm) {
        this.plugin = plugin;
        this.dm = dm;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        if (p.hasPermission("phantomanticheat.bypass")) return;
        dm.recordInventoryAction(p.getName());
        if (plugin.getConfig().getBoolean("checks.inventory.notify", false) && plugin.getConfig().getBoolean("verbose", false)) {
            plugin.getLogger().info("[Inventory] " + p.getName() + " clicked inventory");
        }
    }
}
