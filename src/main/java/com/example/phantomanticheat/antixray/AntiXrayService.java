package com.example.phantomanticheat.antixray;

import com.example.phantomanticheat.PhantomAnticheat;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AntiXrayService implements Listener {
    private final PhantomAnticheat plugin;
    private final Map<ChunkKey, List<BlockPos>> chunkOres = new ConcurrentHashMap<>();
    private final Map<UUID, Set<BlockPos>> playerFakes = new ConcurrentHashMap<>();
    private final Map<ChunkKey, Integer> chunkPlayerCounts = new ConcurrentHashMap<>();
    private final Set<Material> oreTypes;
    private final int revealDistance;
    private final long scanIntervalTicks;
    private final boolean enabled;
    private final int activeMaxPlayerY;
    private final int scanMinY;
    private final int scanMaxY;
    private final int perPlayerChunkRadius;

    public static class BlockPos {
        public final String world;
        public final int x,y,z;
        public BlockPos(String world, int x, int y, int z) { this.world = world; this.x=x; this.y=y; this.z=z; }
        @Override public boolean equals(Object o){ if(this==o) return true; if(!(o instanceof BlockPos)) return false; BlockPos b=(BlockPos)o; return x==b.x&&y==b.y&&z==b.z&&Objects.equals(world,b.world); }
        @Override public int hashCode(){ return Objects.hash(world,x,y,z); }
    }

    private static class ChunkKey {
        public final String world;
        public final int cx, cz;
        public ChunkKey(String world, int cx, int cz) { this.world = world; this.cx = cx; this.cz = cz; }
        @Override public boolean equals(Object o){ if (this==o) return true; if (!(o instanceof ChunkKey)) return false; ChunkKey k=(ChunkKey)o; return cx==k.cx && cz==k.cz && Objects.equals(world,k.world); }
        @Override public int hashCode(){ return Objects.hash(world, cx, cz); }
    }

    public AntiXrayService(PhantomAnticheat plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("anti-xray.enabled", true);
        this.revealDistance = plugin.getConfig().getInt("anti-xray.reveal-distance", 3);
        int intervalSec = plugin.getConfig().getInt("anti-xray.scan-interval-seconds", 5);
        this.activeMaxPlayerY = plugin.getConfig().getInt("anti-xray.active-max-player-y", 64);
        this.scanMinY = plugin.getConfig().getInt("anti-xray.scan-min-y", 0);
        this.scanMaxY = plugin.getConfig().getInt("anti-xray.scan-max-y", 64);
        this.perPlayerChunkRadius = plugin.getConfig().getInt("anti-xray.player-chunk-radius", 1);
        this.scanIntervalTicks = Math.max(1, intervalSec) * 20L;
        this.oreTypes = EnumSet.of(
                Material.COAL_ORE, Material.DIAMOND_ORE, Material.EMERALD_ORE,
                Material.GOLD_ORE, Material.IRON_ORE, Material.LAPIS_ORE,
                Material.REDSTONE_ORE, Material.NETHER_QUARTZ_ORE, Material.DEEPSLATE_COAL_ORE,
                Material.DEEPSLATE_DIAMOND_ORE, Material.DEEPSLATE_EMERALD_ORE, Material.DEEPSLATE_GOLD_ORE,
                Material.DEEPSLATE_IRON_ORE, Material.DEEPSLATE_LAPIS_ORE, Material.DEEPSLATE_REDSTONE_ORE
        );

        if (!enabled) return;

        // initial scan of loaded chunks
        // Do NOT scan all loaded chunks; only scan chunks that are loaded due to player presence.
        // Initialize player-loaded chunks for players currently online
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            addPlayerChunks(p);
        }

        // schedule periodic task to update per-player fake blocks
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::updateAllPlayers, 20L, scanIntervalTicks);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("AntiXrayService enabled. RevealDist="+revealDistance+" intervalSec="+intervalSec);
    }

    private void scanChunk(Chunk c) {
        if (c == null || !c.isLoaded()) return;
        ChunkKey key = new ChunkKey(c.getWorld().getName(), c.getX(), c.getZ());
        List<BlockPos> found = new ArrayList<>();
        World w = c.getWorld();
        int baseX = c.getX()<<4;
        int baseZ = c.getZ()<<4;
        int maxY = Math.min(w.getMaxHeight(), Math.max(scanMaxY, scanMinY)); // clamp
        int minY = Math.max(0, scanMinY);
        int cappedMaxY = Math.min(maxY, scanMaxY);
        for (int dx=0; dx<16; dx++){
            for (int dz=0; dz<16; dz++){
                for (int y=minY; y<=cappedMaxY; y++){
                    Block b = w.getBlockAt(baseX+dx, y, baseZ+dz);
                    if (oreTypes.contains(b.getType())) {
                        found.add(new BlockPos(w.getName(), b.getX(), b.getY(), b.getZ()));
                        if (plugin.getConfig().getBoolean("verbose", false)) plugin.getLogger().info("[AntiXray] Found ore " + b.getType() + " at " + b.getX()+","+b.getY()+","+b.getZ());
                    }
                }
            }
        }
        chunkOres.put(key, found);
        if (plugin.getConfig().getBoolean("verbose", false)) plugin.getLogger().info("[AntiXray] Scanned chunk " + c.getX() + "," + c.getZ() + " found=" + found.size());
    }

    private void removeChunk(Chunk c){ ChunkKey key = new ChunkKey(c.getWorld().getName(), c.getX(), c.getZ()); chunkOres.remove(key); if (plugin.getConfig().getBoolean("verbose", false)) plugin.getLogger().info("[AntiXray] Unloaded chunk " + c.getX()+","+c.getZ()); }

    private void updateAllPlayers() {
        if (!enabled) return;
        Collection<? extends Player> players = plugin.getServer().getOnlinePlayers();
        for (Player p : players) updatePlayer(p);
    }

    private void updatePlayer(Player p) {
        if (!enabled) return;
        UUID id = p.getUniqueId();
        Set<BlockPos> fakeSet = playerFakes.computeIfAbsent(id, k->Collections.newSetFromMap(new ConcurrentHashMap<>()));
        Location ploc = p.getLocation();
        double revealDistSq = (double)revealDistance*revealDistance;

        int view = perPlayerChunkRadius;
        int pcx = ploc.getBlockX() >> 4;
        int pcz = ploc.getBlockZ() >> 4;
        // if player is above the active max Y, clear any fake blocks and skip
        if (ploc.getY() > activeMaxPlayerY) {
            if (!fakeSet.isEmpty()) {
                clearPlayerFakes(p, fakeSet);
            }
            return;
        }
        for (int dx = -view; dx <= view; dx++) {
            for (int dz = -view; dz <= view; dz++) {
                int cx = pcx + dx;
                int cz = pcz + dz;
                ChunkKey key = new ChunkKey(ploc.getWorld().getName(), cx, cz);
                List<BlockPos> list = chunkOres.get(key);
                if (list == null) continue;
                for (BlockPos bp : list) {
                    double dx2 = bp.x + 0.5 - ploc.getX();
                    double dy = bp.y + 0.5 - ploc.getY();
                    double dz2 = bp.z + 0.5 - ploc.getZ();
                    double distSq = dx2*dx2 + dy*dy + dz2*dz2;
                    Location bLoc = new Location(ploc.getWorld(), bp.x, bp.y, bp.z);
                    if (distSq <= revealDistSq) {
                        if (fakeSet.remove(bp)) {
                            BlockData real = ploc.getWorld().getBlockAt(bp.x, bp.y, bp.z).getBlockData();
                            p.sendBlockChange(bLoc, real);
                            if (plugin.getConfig().getBoolean("verbose", false)) plugin.getLogger().info("[AntiXray] Revealed " + bp.x+","+bp.y+","+bp.z+ " to " + p.getName());
                        }
                    } else {
                        if (!fakeSet.contains(bp)) {
                            BlockData fake = Material.STONE.createBlockData();
                            p.sendBlockChange(bLoc, fake);
                            fakeSet.add(bp);
                            if (plugin.getConfig().getBoolean("verbose", false)) plugin.getLogger().info("[AntiXray] Hid " + bp.x+","+bp.y+","+bp.z+ " from " + p.getName());
                        }
                    }
                }
            }
        }
    }

    private void clearPlayerFakes(Player p, Set<BlockPos> fakeSet) {
        try {
            World w = p.getWorld();
            for (BlockPos bp : fakeSet) {
                if (!bp.world.equals(w.getName())) continue;
                Location bLoc = new Location(w, bp.x, bp.y, bp.z);
                BlockData real = w.getBlockAt(bp.x, bp.y, bp.z).getBlockData();
                p.sendBlockChange(bLoc, real);
                if (plugin.getConfig().getBoolean("verbose", false)) plugin.getLogger().info("[AntiXray] Cleared fake for " + p.getName() + " at " + bp.x+","+bp.y+","+bp.z);
            }
        } catch (Exception ignored) {}
        fakeSet.clear();
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) { if (!enabled) return; ChunkKey k = new ChunkKey(e.getChunk().getWorld().getName(), e.getChunk().getX(), e.getChunk().getZ()); if (chunkPlayerCounts.getOrDefault(k,0) > 0) scanChunk(e.getChunk()); }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent e) { if (!enabled) return; removeChunk(e.getChunk()); }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e){ if (!enabled) return; Chunk c = e.getBlock().getChunk(); ChunkKey k = new ChunkKey(c.getWorld().getName(), c.getX(), c.getZ()); if (chunkPlayerCounts.getOrDefault(k,0)>0) scanChunk(c); }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e){ if (!enabled) return; Chunk c = e.getBlock().getChunk(); ChunkKey k = new ChunkKey(c.getWorld().getName(), c.getX(), c.getZ()); if (chunkPlayerCounts.getOrDefault(k,0)>0) scanChunk(c); }

    @EventHandler
    public void onPlayerWorldChange(PlayerChangedWorldEvent e){ if (!enabled) return; playerFakes.remove(e.getPlayer().getUniqueId()); }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) { if (!enabled) return; addPlayerChunks(e.getPlayer()); }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) { if (!enabled) return; removePlayerChunks(e.getPlayer()); playerFakes.remove(e.getPlayer().getUniqueId()); }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) { if (!enabled) return; if (e.getFrom().getChunk().equals(e.getTo().getChunk())) return; removePlayerChunksForChunkCenter(e.getPlayer(), e.getFrom()); addPlayerChunksForChunkCenter(e.getPlayer(), e.getTo()); }

    private void addPlayerChunks(Player p) { addPlayerChunksForChunkCenter(p, p.getLocation()); }

    private void removePlayerChunks(Player p) { removePlayerChunksForChunkCenter(p, p.getLocation()); }

    private void addPlayerChunksForChunkCenter(Player p, Location loc) {
        int view = plugin.getServer().getViewDistance();
        int pcx = loc.getBlockX() >> 4;
        int pcz = loc.getBlockZ() >> 4;
        for (int dx = -view; dx <= view; dx++) for (int dz = -view; dz <= view; dz++) {
            int cx = pcx + dx, cz = pcz + dz;
            ChunkKey key = new ChunkKey(loc.getWorld().getName(), cx, cz);
            int prev = chunkPlayerCounts.getOrDefault(key,0);
            chunkPlayerCounts.put(key, prev+1);
            if (prev==0) {
                // first player loading this chunk -> scan if chunk loaded
                Chunk c = loc.getWorld().isChunkLoaded(cx, cz) ? loc.getWorld().getChunkAt(cx, cz) : null;
                if (c!=null && c.isLoaded()) scanChunk(c);
            }
        }
    }

    private void removePlayerChunksForChunkCenter(Player p, Location loc) {
        int view = plugin.getServer().getViewDistance();
        int pcx = loc.getBlockX() >> 4;
        int pcz = loc.getBlockZ() >> 4;
        for (int dx = -view; dx <= view; dx++) for (int dz = -view; dz <= view; dz++) {
            int cx = pcx + dx, cz = pcz + dz;
            ChunkKey key = new ChunkKey(loc.getWorld().getName(), cx, cz);
            int prev = chunkPlayerCounts.getOrDefault(key,0);
            if (prev <= 1) { chunkPlayerCounts.remove(key); // became zero
                // unload ore index
                List<BlockPos> removed = chunkOres.remove(key);
                if (removed != null && plugin.getConfig().getBoolean("verbose", false)) plugin.getLogger().info("[AntiXray] Removed ore index for chunk " + cx + "," + cz);
            } else {
                chunkPlayerCounts.put(key, prev-1);
            }
        }
    }
}
