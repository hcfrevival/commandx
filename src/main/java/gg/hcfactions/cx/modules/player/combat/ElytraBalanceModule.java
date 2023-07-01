package gg.hcfactions.cx.modules.player.combat;

import gg.hcfactions.cx.modules.ICXModule;
import gg.hcfactions.libs.bukkit.AresPlugin;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;

public final class ElytraBalanceModule implements ICXModule, Listener {
    @Getter public final AresPlugin plugin;
    @Getter public final String key;
    @Getter @Setter public boolean enabled;

    private boolean removeElytraOnProjectileDamage;
    private boolean removeElytraBhopping;

    public ElytraBalanceModule(AresPlugin plugin) {
        this.plugin = plugin;
        this.key = "combat.elytra.";
    }

    @Override
    public void onEnable() {
        loadConfig();
        plugin.registerListener(this);

        this.enabled = true;
    }

    @Override
    public void onDisable() {
        this.enabled = false;
    }

    @Override
    public void onReload() {
        loadConfig();
    }

    private void loadConfig() {
        final YamlConfiguration conf = getConfig();
        removeElytraOnProjectileDamage = conf.getBoolean(getKey() + "remove_elytra_on_projectile_damage");
        removeElytraBhopping = conf.getBoolean(getKey() + "remove_elytra_bhopping");
    }

    /**
     * Removes the elytra from the provided players chestplate slot and drops it
     * on the floor of their current location.
     *
     * @param player Player
     */
    private void removeElytraFromChestplate(Player player) {
        if (
                player.getEquipment() == null
                        || player.getEquipment().getChestplate() == null
                        || !player.getEquipment().getChestplate().getType().equals(Material.ELYTRA)
        ) {
            return;
        }

        final ItemStack elytra = player.getEquipment().getChestplate();

        player.getEquipment().setChestplate(new ItemStack(Material.AIR));
        player.getWorld().dropItemNaturally(player.getLocation(), elytra);
        player.sendMessage(ChatColor.RED + "Your elytra has fallen to your feet");
    }

    /**
     * Performs an air check to see if elytra flight should be possible
     *
     * @param player Player
     * @param dist Distance requirement
     * @return True if the player is high enough from the floor
     */
    private boolean isInAir(Player player, int dist) {
        final Location location = player.getLocation();

        for (int i = 0; i < dist; i++) {
            final Location newLocation = new Location(location.getWorld(), location.getBlockX(), location.getBlockY() - i, location.getBlockZ());

            if (newLocation.getBlock().getType().equals(Material.AIR) || !newLocation.getBlock().getType().isSolid()) {
                continue;
            }

            return false;
        }

        return true;
    }

    /**
     * Removes elytra if hit by a projectile
     * @param event ProjectileHitEvent
     */
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!removeElytraOnProjectileDamage) {
            return;
        }

        if (!(event.getHitEntity() instanceof final Player player)) {
            return;
        }

        if (player.isGliding()) {
            player.setGliding(false);
        }

        removeElytraFromChestplate(player);
    }

    /**
     * Removes elytra while attempting to b-hop
     * @param event EntityToggleGlideEvent
     */
    @EventHandler
    public void onPlayerToggleFlight(EntityToggleGlideEvent event) {
        if (!isEnabled() || !removeElytraBhopping || event.isCancelled()) {
            return;
        }

        if (!(event.getEntity() instanceof final Player player)) {
            return;
        }

        if (!event.isGliding()) {
            return;
        }

        if (!isInAir(player, 3)) {
            removeElytraFromChestplate(player);
            event.setCancelled(true);
        }
    }
}
