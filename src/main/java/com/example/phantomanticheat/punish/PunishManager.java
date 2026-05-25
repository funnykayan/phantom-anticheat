package com.example.phantomanticheat.punish;

import com.example.phantomanticheat.PhantomAnticheat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.time.Duration;
import java.util.Date;

public class PunishManager {
    private final PhantomAnticheat plugin;

    public PunishManager(PhantomAnticheat plugin) {
        this.plugin = plugin;
    }

    public void warn(String player, String reason) {
        String msg = buildMessage("WARN", player, reason, 0, null);
        plugin.getLogger().warning(msg);
        if (plugin.getServer().getPlayer(player) != null) plugin.getServer().getPlayer(player).sendMessage("[AntiCheat] Warning: " + reason);
        plugin.getDataManager().appendAudit(msg);
    }

    public void kick(String player, String reason) {
        String evidence = plugin.getDataManager().getLastIp(player); // reuse as placeholder link
        String msg = buildMessage("KICK", player, reason, 0, evidence);
        plugin.getLogger().warning(msg);
        if (plugin.getServer().getPlayer(player) != null) plugin.getServer().getPlayer(player).kickPlayer(buildPlayerBanMessage(player, reason, 0, evidence));
        plugin.getDataManager().appendAudit(msg);
    }

    public void tempban(String player, Duration duration, String reason) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(player);
        Date until = Date.from(java.time.Instant.now().plus(duration));
        Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(player, reason, until, "PhantomAntiCheat");
        String evidence = plugin.getDataManager().getLastIp(player);
        String msg = buildMessage("TEMPBAN", player, reason, 0, evidence) + " until=" + until;
        if (plugin.getServer().getPlayer(player) != null) plugin.getServer().getPlayer(player).kickPlayer(buildPlayerBanMessage(player, reason, 0, evidence));
        plugin.getDataManager().appendAudit(msg);
    }

    private String buildMessage(String action, String player, String reason, int score, String evidence) {
        return String.format("%s %s reason=%s score=%d evidence=%s", action, player, reason, score, evidence == null ? "-" : evidence);
    }

    private String buildPlayerBanMessage(String player, String reason, int score, String evidence) {
        StringBuilder sb = new StringBuilder();
        sb.append("You were banned: ").append(reason).append("\n");
        sb.append("Detection score: ").append(score).append("\n");
        if (evidence != null) sb.append("Evidence: ").append(evidence).append("\n");
        sb.append("Appeal at server staff.");
        return sb.toString();
    }
}
