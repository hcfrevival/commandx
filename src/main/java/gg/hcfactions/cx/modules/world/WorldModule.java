package gg.hcfactions.cx.modules.world;

import com.destroystokyo.paper.event.entity.PreCreatureSpawnEvent;
import com.google.common.collect.Sets;
import gg.hcfactions.cx.CXPermissions;
import gg.hcfactions.cx.modules.ICXModule;
import gg.hcfactions.libs.bukkit.AresPlugin;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;
import java.util.Set;

public final class WorldModule implements ICXModule, Listener {
    @Getter public final AresPlugin plugin;
    @Getter public final String key;
    @Getter @Setter public boolean enabled;

    private boolean disablePistonBreakingDoors;
    private boolean disableEnderchests;
    private boolean disableEntityBlockChanges;
    private boolean disableExplosiveExploits;
    private boolean allowPearlingThroughTraps;
    private Set<EntityType> disabledEntities;
    private Set<EntityType> disabledSpawnerBlockBreaks;

    public WorldModule(AresPlugin plugin) {
        this.plugin = plugin;
        this.key = "world.general.";
        this.disabledEntities = Sets.newHashSet();
        this.disabledSpawnerBlockBreaks = Sets.newHashSet();
    }

    @Override
    public void onEnable() {
        loadConfig();

        if (!isEnabled()) {
            return;
        }

        plugin.registerListener(this);
    }

    @Override
    public void onDisable() {
        disabledEntities.clear();
        disabledSpawnerBlockBreaks.clear();

        this.enabled = false;
    }

    @Override
    public void onReload() {
        loadConfig();
    }

    private void loadConfig() {
        final YamlConfiguration conf = getConfig();
        enabled = conf.getBoolean(getKey() + "enabled");
        disablePistonBreakingDoors = conf.getBoolean(getKey() + "disable_piston_breaking_doors");
        disableEnderchests = conf.getBoolean(getKey() + "disable_enderchests");
        allowPearlingThroughTraps = conf.getBoolean(getKey() + "allow_pearling_through_traps");
        disableEntityBlockChanges = conf.getBoolean(getKey() + "disable_entity_block_changes");
        disableExplosiveExploits = conf.getBoolean(getKey() + "disable_explosive_exploits");

        final List<String> disabledEntityNames = conf.getStringList(getKey() + "disabled_entities");
        final List<String> disabledEntitySpawnerBreakNames = conf.getStringList(getKey() + "disabled_spawner_break");

        disabledEntities.clear();
        disabledSpawnerBlockBreaks.clear();

        for (String entityName : disabledEntityNames) {
            try {
                final EntityType type = EntityType.valueOf(entityName);
                disabledEntities.add(type);
            } catch (IllegalArgumentException e) {
                plugin.getAresLogger().error("bad entity type: " + entityName);
            }
        }

        for (String entityName : disabledEntitySpawnerBreakNames) {
            try {
                final EntityType type = EntityType.valueOf(entityName);
                disabledSpawnerBlockBreaks.add(type);
            } catch (IllegalArgumentException e) {
                plugin.getAresLogger().error("bad entity type: " + entityName);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isEnabled() || disabledSpawnerBlockBreaks.isEmpty()) {
            return;
        }

        final Player player = event.getPlayer();
        final Block block = event.getBlock();

        if (!block.getType().equals(Material.SPAWNER)) {
            return;
        }

        final CreatureSpawner spawner = (CreatureSpawner) block.getState();
        if (!disabledSpawnerBlockBreaks.contains(spawner.getSpawnedType())) {
            return;
        }

        if (player.hasPermission(CXPermissions.CX_ADMIN)) {
            return;
        }

        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "This type of Creature Spawner can not be broken");
    }

    @EventHandler
    public void onCreatureSpawn(PreCreatureSpawnEvent event) {
        if (!isEnabled() || disabledEntities.isEmpty() || event.isCancelled()) {
            return;
        }

        if (disabledEntities.contains(event.getType())
                && !(event.getReason().equals(CreatureSpawnEvent.SpawnReason.SPAWNER) || event.getReason().equals(CreatureSpawnEvent.SpawnReason.SPAWNER_EGG))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityBlockChange(EntityChangeBlockEvent event) {
        if (!isEnabled() || !disableEntityBlockChanges) {
            return;
        }

        if (event.getEntity() instanceof FallingBlock) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!isEnabled() || event.isCancelled() || !disablePistonBreakingDoors) {
            return;
        }

        final List<Block> modified = event.getBlocks();
        for (Block modifiedBlock : modified) {
            if (modifiedBlock == null || modifiedBlock.getType().equals(Material.AIR)) {
                continue;
            }

            if (modifiedBlock.getType().name().contains("_DOOR")) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onEnderchestInteract(PlayerInteractEvent event) {
        if (!isEnabled() || !disableEnderchests || event.useInteractedBlock().equals(Event.Result.DENY)) {
            return;
        }

        final Player player = event.getPlayer();
        final Block block = event.getClickedBlock();

        if (block == null || !block.getType().equals(Material.ENDER_CHEST)) {
            return;
        }

        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        if (player.hasPermission(CXPermissions.CX_ADMIN)) {
            return;
        }

        event.setUseInteractedBlock(Event.Result.DENY);
        player.sendMessage(ChatColor.RED + "Enderchests have been disabled");
    }

    @EventHandler
    public void onBombBlockInteract(PlayerInteractEvent event) {
        if (!isEnabled() || !disableExplosiveExploits) {
            return;
        }

        final Player player = event.getPlayer();
        final Block block = event.getClickedBlock();

        if (block == null || !block.getType().isInteractable()) {
            return;
        }

        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        if (player.hasPermission(CXPermissions.CX_ADMIN)) {
            return;
        }

        if (block.getType().name().endsWith("_BED") && block.getWorld().getEnvironment().equals(World.Environment.NETHER)) {
            player.sendMessage(ChatColor.RED + "You can not sleep in the nether");
            event.setCancelled(true);
            return;
        }

        if (block.getType().equals(Material.RESPAWN_ANCHOR) && !block.getWorld().getEnvironment().equals(World.Environment.NETHER)) {
            final RespawnAnchor anchor = (RespawnAnchor) block.getBlockData();
            if ((anchor.getCharges() + 1) >= anchor.getMaximumCharges()) {
                player.sendMessage(ChatColor.RED + "Respawn anchors are disabled outside of The Nether");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!isEnabled() || !disableExplosiveExploits) {
            return;
        }

        final Player player = event.getPlayer();
        final Block block = event.getBlock();

        if (!block.getType().equals(Material.END_CRYSTAL)) {
            return;
        }

        if (!player.hasPermission(CXPermissions.CX_ADMIN)) {
            player.sendMessage(ChatColor.RED + "End crystals are disabled");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!isEnabled() || !allowPearlingThroughTraps) {
            return;
        }

        final Projectile proj = event.getEntity();

        if (!(proj instanceof EnderPearl)) {
            return;
        }

        final Block hitBlock = event.getHitBlock();

        if (hitBlock == null) {
            return;
        }

        if (hitBlock.getType().equals(Material.TRIPWIRE)) {
            event.setCancelled(true);
        }

        else if (hitBlock.getType().name().endsWith("_FENCE_GATE")) {
            final BlockState state = hitBlock.getState();
            final BlockData data = state.getBlockData();

            if (data instanceof final Gate gate) {
                if (gate.isOpen()) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
