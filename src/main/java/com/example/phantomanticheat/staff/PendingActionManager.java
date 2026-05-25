package com.example.phantomanticheat.staff;

import com.example.phantomanticheat.PhantomAnticheat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PendingActionManager {
    public enum ActionType { TEMPBAN, NOTE, ENDSPECTATE }

    public static class PendingAction {
        public final ActionType type;
        public final String target;
        public PendingAction(ActionType type, String target) { this.type = type; this.target = target; }
    }

    private final PhantomAnticheat plugin;
    private final Map<String, PendingAction> map = new ConcurrentHashMap<>();

    public PendingActionManager(PhantomAnticheat plugin) { this.plugin = plugin; }

    public void put(String staff, PendingAction a) { map.put(staff, a); }
    public PendingAction take(String staff) { return map.remove(staff); }
    public boolean has(String staff) { return map.containsKey(staff); }
}
