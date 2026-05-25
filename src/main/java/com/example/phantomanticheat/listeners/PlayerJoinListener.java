package com.example.phantomanticheat.listeners;

import com.example.phantomanticheat.PhantomAnticheat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
    private final PhantomAnticheat plugin;

    public PlayerJoinListener(PhantomAnticheat plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        String ip = "unknown";
        try {
            if (e.getPlayer() != null && e.getPlayer().getAddress() != null && e.getPlayer().getAddress().getAddress() != null) {
                ip = e.getPlayer().getAddress().getAddress().getHostAddress();
            }
        } catch (Exception ignored) {}
        plugin.getDataManager().recordPlayerIp(e.getPlayer().getName(), ip);
        // optional IP reputation check
        try {
            String provider = plugin.getConfig().getString("ip.reputation.provider", "");
            String key = plugin.getConfig().getString("ip.reputation.key", "");
            double threshold = plugin.getConfig().getDouble("ip.reputation.threshold", 0.8);
            if (provider != null && !provider.isEmpty()) {
                final String fip = ip;
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        String url = null;
                        if (provider.equalsIgnoreCase("ipqualityscore")) {
                            url = "https://ipqualityscore.com/api/json/ip/" + key + "/" + fip;
                        } else if (provider.equalsIgnoreCase("ipinfo")) {
                            url = "https://ipinfo.io/" + fip + (key==null||key.isEmpty()?"/json":"/json?token="+key);
                        }
                        if (url == null) return;
                        java.net.URL u = new java.net.URL(url);
                        java.net.HttpURLConnection con = (java.net.HttpURLConnection) u.openConnection();
                        con.setConnectTimeout(4000);
                        con.setReadTimeout(6000);
                        con.setRequestMethod("GET");
                        int rc = con.getResponseCode();
                        if (rc != 200) return;
                        java.io.InputStream is = con.getInputStream();
                        String body = new String(is.readAllBytes());
                        // simple detection: look for "proxy":true or high fraud_score
                        boolean flagged = false;
                        if (body.contains("\"proxy\":true") || body.contains("\"vpn\":true")) flagged = true;
                        try {
                            int idx = body.indexOf("fraud_score");
                            if (idx>0) {
                                String sub = body.substring(idx);
                                String num = sub.replaceAll("[^0-9]"," ").trim().split(" ")[0];
                                int score = Integer.parseInt(num);
                                if (score >= (int)(threshold*100)) flagged = true;
                            }
                        } catch (Exception ignored) {}
                        if (flagged) {
                            plugin.getDataManager().recordFlag(e.getPlayer().getName(), "ip-reputation:"+provider, 4);
                        }
                    } catch (Exception ignored) {}
                });
            }
        } catch (Exception ignored) {}
        // check IP blacklist
        try {
            java.util.List<String> blocks = plugin.getConfig().getStringList("ip.blacklist");
            for (String cidr : blocks) {
                if (cidr == null || cidr.isEmpty()) continue;
                if (ipInCidr(ip, cidr)) {
                    plugin.getDataManager().recordFlag(e.getPlayer().getName(), "ip-blacklist:" + cidr, 5);
                    if (plugin.getConfig().getBoolean("ip.blacklist.kick", false)) {
                        if (plugin.getServer().getPlayer(e.getPlayer().getName()) != null)
                            plugin.getServer().getPlayer(e.getPlayer().getName()).kickPlayer("IP blacklisted");
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private boolean ipInCidr(String ip, String cidr) {
        try {
            if (ip == null || ip.isEmpty()) return false;
            String[] parts = cidr.split("/");
            java.net.InetAddress addr = java.net.InetAddress.getByName(parts[0]);
            byte[] target = java.net.InetAddress.getByName(ip).getAddress();
            byte[] network = addr.getAddress();
            int prefix = parts.length > 1 ? Integer.parseInt(parts[1]) : (network.length*8);
            int targetInt = 0, netInt = 0;
            for (byte b : target) targetInt = (targetInt << 8) | (b & 0xFF);
            for (byte b : network) netInt = (netInt << 8) | (b & 0xFF);
            int mask = prefix == 32 ? -1 : ~((1 << (32 - prefix)) - 1);
            return (targetInt & mask) == (netInt & mask);
        } catch (Exception e) { return false; }
    }
}
