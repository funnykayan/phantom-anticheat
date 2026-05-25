package com.example.phantomanticheat.listeners;

import com.example.phantomanticheat.PhantomAnticheat;
import com.example.phantomanticheat.detection.DetectionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class ChatCommandListener implements Listener {

    private final PhantomAnticheat plugin;
    private final DetectionManager dm;

    public ChatCommandListener(PhantomAnticheat plugin, DetectionManager dm) {
        this.plugin = plugin;
        this.dm = dm;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        String line = String.format("CHAT %s: %s", p.getName(), e.getMessage());
        dm.appendChat(line);
        dm.recordAction(p.getName());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        String line = String.format("CMD %s: %s", p.getName(), e.getMessage());
        dm.appendCommand(line);
        dm.recordAction(p.getName());
    }
}
