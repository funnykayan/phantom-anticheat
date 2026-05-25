package com.example.phantomanticheat.staff;

import com.example.phantomanticheat.PhantomAnticheat;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpectateManager {
    private final PhantomAnticheat plugin;
    private final Map<String, SpectateState> states = new ConcurrentHashMap<>();

    private static class SpectateState {
        public final GameMode prevMode;
        public final Location prevLocation;
        public SpectateState(GameMode m, Location l) { prevMode = m; prevLocation = l; }
    }

    public SpectateManager(PhantomAnticheat plugin) { this.plugin = plugin; }

    public void startSpectate(Player staff, Player target) {
        if (states.containsKey(staff.getName())) return; // already spectating
        states.put(staff.getName(), new SpectateState(staff.getGameMode(), staff.getLocation()));
        staff.setGameMode(GameMode.SPECTATOR);
        staff.teleport(target.getLocation());
    }

    public void stopSpectate(Player staff) {
        SpectateState s = states.remove(staff.getName());
        if (s == null) return;
        try {
            staff.setGameMode(s.prevMode);
            if (s.prevLocation != null) staff.teleport(s.prevLocation);
        } catch (Exception ignored) {}
    }

    public boolean isSpectating(String staff) { return states.containsKey(staff); }
}
