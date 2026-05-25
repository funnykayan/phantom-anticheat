package com.example.phantomanticheat.commands;

import com.example.phantomanticheat.PhantomAnticheat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AnticheatCommand implements CommandExecutor, TabCompleter {

    private final PhantomAnticheat plugin;

    public AnticheatCommand(PhantomAnticheat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("PhantomAntiCheat v" + plugin.getDescription().getVersion());
            sender.sendMessage("Usage: /anticheat <reload|verbose|status>");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload":
                plugin.reloadConfig();
                sender.sendMessage("AntiCheat: config reloaded.");
                return true;
            case "verbose":
                if (args.length < 2) {
                    sender.sendMessage("Usage: /anticheat verbose <on|off>");
                    return true;
                }
                boolean on = args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true");
                plugin.getConfig().set("verbose", on);
                plugin.saveConfig();
                sender.sendMessage("Verbose set to " + on);
                return true;
            case "status":
                boolean verbose = plugin.getConfig().getBoolean("verbose", false);
                boolean speed = plugin.getConfig().getBoolean("checks.speed", true);
                boolean fly = plugin.getConfig().getBoolean("checks.fly", true);
                double max = plugin.getConfig().getDouble("max-speed", 5.0);
                sender.sendMessage("AntiCheat status:");
                sender.sendMessage(" - verbose: " + verbose);
                sender.sendMessage(" - checks.speed: " + speed);
                sender.sendMessage(" - checks.fly: " + fly);
                sender.sendMessage(" - max-speed: " + max);
                return true;
            case "inspect":
                if (!(sender instanceof org.bukkit.entity.Player)) { sender.sendMessage("Only players can use inspect."); return true; }
                if (args.length < 2) { sender.sendMessage("Usage: /anticheat inspect <player>"); return true; }
                new com.example.phantomanticheat.staff.StaffInspector(plugin).openInspect((org.bukkit.entity.Player) sender, args[1]);
                return true;
            case "replay":
                if (!(sender instanceof org.bukkit.entity.Player)) { sender.sendMessage("Only players can use replay."); return true; }
                if (args.length < 2) { sender.sendMessage("Usage: /anticheat replay <player>"); return true; }
                try {
                    java.nio.file.Path clip = plugin.getDataFolder().toPath().resolve("evidence").resolve(args[1] + "-latest.clip");
                    if (!java.nio.file.Files.exists(clip)) { sender.sendMessage("No clip available for " + args[1]); return true; }
                    plugin.getReplayService().playClipToSender(clip, (org.bukkit.entity.Player) sender);
                } catch (Exception ex) { sender.sendMessage("Failed to play clip: " + ex.getMessage()); }
                return true;
            case "feedback":
                if (args.length < 3) { sender.sendMessage("Usage: /anticheat feedback <player> <fp|tp>"); return true; }
                String who = args[1];
                String fb = args[2].toLowerCase();
                if (!fb.equals("fp") && !fb.equals("tp")) { sender.sendMessage("Use fp or tp"); return true; }
                boolean isTP = fb.equals("tp");
                plugin.getDataManager().recordFeedback(who, isTP);
                sender.sendMessage("Recorded feedback for " + who + ": " + fb);
                return true;
            case "stopspectate":
                if (!(sender instanceof org.bukkit.entity.Player)) { sender.sendMessage("Only players can use this."); return true; }
                plugin.getSpectateManager().stopSpectate((org.bukkit.entity.Player) sender);
                sender.sendMessage("Stopped spectating.");
                return true;
            case "reports":
                if (args.length >= 2 && args[1].equalsIgnoreCase("generate")) {
                    plugin.getReportScheduler().generateDailyCsv();
                    sender.sendMessage("Report generated.");
                    return true;
                }
                sender.sendMessage("Usage: /anticheat reports generate");
                return true;
            default:
                sender.sendMessage("Unknown subcommand. Use reload, verbose, or status.");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> sub = Arrays.asList("reload", "verbose", "status");
            List<String> out = new ArrayList<>();
            for (String s : sub) if (s.startsWith(args[0].toLowerCase())) out.add(s);
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("verbose")) {
            return Arrays.asList("on", "off");
        }
        return Collections.emptyList();
    }
}
