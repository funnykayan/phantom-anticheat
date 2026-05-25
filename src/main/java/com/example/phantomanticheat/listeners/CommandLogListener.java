package com.example.phantomanticheat.listeners;

import com.example.phantomanticheat.PhantomAnticheat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandLogListener implements Listener {
    private final PhantomAnticheat plugin;

    public CommandLogListener(PhantomAnticheat plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage(); // e.g., "/anticheat status"

        // Strip the leading "/" and split
        String[] parts = message.substring(1).split(" ");
        String commandName = parts[0];
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);

        // Log to webhook
        plugin.getWebhookNotifier().logCommand(player.getName(), commandName, args);
    }
}