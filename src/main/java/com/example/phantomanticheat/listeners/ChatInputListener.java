package com.example.phantomanticheat.listeners;

import com.example.phantomanticheat.PhantomAnticheat;
import com.example.phantomanticheat.staff.PendingActionManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.time.Duration;

public class ChatInputListener implements Listener {
    private final PhantomAnticheat plugin;

    public ChatInputListener(PhantomAnticheat plugin) { this.plugin = plugin; }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        String sender = e.getPlayer().getName();
        if (!plugin.getPendingActionManager().has(sender)) return;
        e.setCancelled(true);
        PendingActionManager.PendingAction act = plugin.getPendingActionManager().take(sender);
        if (act == null) return;
        switch (act.type) {
            case TEMPBAN:
                try {
                    String[] parts = e.getMessage().split(" ", 2);
                    int mins = Integer.parseInt(parts[0]);
                    String reason = parts.length > 1 ? parts[1] : "No reason";
                    // tempban touches Bukkit API (ban list / kick) — run on main thread
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        try {
                            plugin.getPunishManager().tempban(act.target, Duration.ofMinutes(mins), reason);
                            e.getPlayer().sendMessage("Temp-banned " + act.target + " for " + mins + " minutes");
                        } catch (Exception ex) {
                            e.getPlayer().sendMessage("Tempban failed: " + ex.getMessage());
                        }
                    });
                } catch (Exception ex) {
                    // parsing failed — send message back on main thread
                    plugin.getServer().getScheduler().runTask(plugin, () -> e.getPlayer().sendMessage("Invalid format. Use: <minutes> <reason>"));
                }
                break;
            case NOTE:
                // writing to files is safe async; append audit and notify staff
                plugin.getDataManager().appendAudit("NOTE by " + sender + " for " + act.target + ": " + e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin, () -> e.getPlayer().sendMessage("Note saved for " + act.target));
                break;
            case ENDSPECTATE:
                // spectate stop modifies player gamemode/location — run on main thread
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.getSpectateManager().stopSpectate(e.getPlayer());
                    e.getPlayer().sendMessage("Stopped spectating.");
                });
                break;
        }
    }
}
