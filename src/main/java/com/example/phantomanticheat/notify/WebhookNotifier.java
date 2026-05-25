package com.example.phantomanticheat.notify;

import com.example.phantomanticheat.PhantomAnticheat;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class WebhookNotifier {
    private final PhantomAnticheat plugin;
    private final String url;

    public WebhookNotifier(PhantomAnticheat plugin, String url) {
        this.plugin = plugin;
        this.url = url;
    }

    public void sendAlert(String title, String body) {
        if (url == null || url.isEmpty()) return;
        try {
            URL u = new URL(url);
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            String payload = String.format("{\"content\":\"%s: %s\"}", escape(title), escape(body));
            try (OutputStream os = c.getOutputStream()) { os.write(payload.getBytes()); }
            int rc = c.getResponseCode();
            if (rc >= 400) plugin.getLogger().warning("Webhook returned " + rc);
        } catch (Exception e) { plugin.getLogger().warning("Webhook send failed: " + e.getMessage()); }
    }

    private String escape(String s) { return s.replace("\"","\\\"").replace("\n","\\n"); }
}
