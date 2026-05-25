package com.example.phantomanticheat.staff;

import com.example.phantomanticheat.PhantomAnticheat;
import com.example.phantomanticheat.data.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class StaffInspector {
    private final PhantomAnticheat plugin;

    public StaffInspector(PhantomAnticheat plugin) { this.plugin = plugin; }

    public void openInspect(Player staff, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        Inventory inv = Bukkit.createInventory(null, 9, "Inspect: " + targetName);
        DataManager dm = plugin.getDataManager();
        int flags = dm.getLeaderboard().getOrDefault(targetName, 0);
        String ip = dm.getLastIp(targetName);

        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta m = info.getItemMeta();
        m.setDisplayName(ChatColor.YELLOW + "Info");
        List<String> lore = new ArrayList<>();
        lore.add("Flags: " + flags);
        lore.add("Last IP: " + (ip==null?"unknown":ip));
        m.setLore(lore);
        info.setItemMeta(m);
        inv.setItem(0, info);

        ItemStack replay = new ItemStack(Material.PAPER);
        ItemMeta rmeta = replay.getItemMeta();
        rmeta.setDisplayName(ChatColor.GREEN + "Play Evidence");
        replay.setItemMeta(rmeta);
        inv.setItem(1, replay);

        ItemStack punish = new ItemStack(Material.IRON_SWORD);
        ItemMeta pmeta = punish.getItemMeta();
        pmeta.setDisplayName(ChatColor.RED + "Quick Warn");
        punish.setItemMeta(pmeta);
        inv.setItem(2, punish);

        ItemStack tp = new ItemStack(Material.COMPASS);
        ItemMeta tpm = tp.getItemMeta();
        tpm.setDisplayName(ChatColor.GREEN + "Teleport to player");
        tp.setItemMeta(tpm);
        inv.setItem(3, tp);

        ItemStack spec = new ItemStack(Material.ENDER_EYE);
        ItemMeta sm = spec.getItemMeta();
        sm.setDisplayName(ChatColor.AQUA + "Spectate player");
        spec.setItemMeta(sm);
        inv.setItem(4, spec);

        ItemStack rb = new ItemStack(Material.BARRIER);
        ItemMeta rbm = rb.getItemMeta();
        rbm.setDisplayName(ChatColor.RED + "Rollback recent blocks");
        rb.setItemMeta(rbm);
        inv.setItem(5, rb);

        ItemStack tb = new ItemStack(Material.LEVER);
        ItemMeta tbm = tb.getItemMeta();
        tbm.setDisplayName(ChatColor.GOLD + "Temp-ban (chat)");
        tb.setItemMeta(tbm);
        inv.setItem(6, tb);

        ItemStack note = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta nm = note.getItemMeta();
        nm.setDisplayName(ChatColor.YELLOW + "Add note (chat)");
        note.setItemMeta(nm);
        inv.setItem(7, note);

        staff.openInventory(inv);
    }
}
