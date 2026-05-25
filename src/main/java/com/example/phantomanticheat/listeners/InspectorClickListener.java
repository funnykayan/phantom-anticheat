package com.example.phantomanticheat.listeners;

import com.example.phantomanticheat.PhantomAnticheat;
import com.example.phantomanticheat.staff.StaffInspector;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class InspectorClickListener implements Listener {
    private final PhantomAnticheat plugin;

    public InspectorClickListener(PhantomAnticheat plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        if (e.getView().getTitle() == null || !e.getView().getTitle().startsWith("Inspect: ")) return;
        e.setCancelled(true);
        String target = e.getView().getTitle().substring(9);
        int slot = e.getRawSlot();
        switch (slot) {
            case 1: // play evidence
                try {
                    java.nio.file.Path clip = plugin.getDataFolder().toPath().resolve("evidence").resolve(target + "-latest.clip");
                    if (!java.nio.file.Files.exists(clip)) { p.sendMessage("No clip for " + target); break; }
                    plugin.getReplayService().playClipToSender(clip, p);
                } catch (Exception ex) { p.sendMessage("Failed to play clip: " + ex.getMessage()); }
                break;
            case 2: // quick warn
                plugin.getPunishManager().warn(target, "Manual staff warning");
                p.sendMessage(ChatColor.GREEN + "Warned " + target);
                break;
            case 3: // teleport
                Player t = plugin.getServer().getPlayerExact(target);
                if (t != null) p.teleport(t.getLocation());
                break;
            case 4: // spectate
                Player st = plugin.getServer().getPlayerExact(target);
                if (st != null) {
                    plugin.getSpectateManager().startSpectate(p, st);
                    p.sendMessage("Now spectating " + target + " — use /anticheat stopspectate");
                } else p.sendMessage("Player not online");
                break;
            case 5: // rollback
                p.closeInventory();
                p.sendMessage("Running rollback for " + target);
                plugin.getDataManager().rollbackRecentBlocks(target, 120);
                break;
            case 6: // temp-ban via chat
                p.closeInventory();
                p.sendMessage("Type tempban in chat as: <minutes> <reason>");
                plugin.getPendingActionManager().put(p.getName(), new com.example.phantomanticheat.staff.PendingActionManager.PendingAction(com.example.phantomanticheat.staff.PendingActionManager.ActionType.TEMPBAN, target));
                break;
            case 7: // add-note via chat
                p.closeInventory();
                p.sendMessage("Type note in chat to attach to " + target);
                plugin.getPendingActionManager().put(p.getName(), new com.example.phantomanticheat.staff.PendingActionManager.PendingAction(com.example.phantomanticheat.staff.PendingActionManager.ActionType.NOTE, target));
                break;
            default:
                break;
        }
    }
}
