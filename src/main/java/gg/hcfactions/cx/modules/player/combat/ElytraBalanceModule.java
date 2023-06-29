package gg.hcfactions.cx.modules.player.combat;

import gg.hcfactions.cx.modules.ICXModule;
import gg.hcfactions.libs.base.util.Time;
import gg.hcfactions.libs.bukkit.AresPlugin;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class ElytraBalanceModule implements ICXModule, Listener {
    @Getter public final AresPlugin plugin;
    @Getter public final String key;
    @Getter @Setter public boolean enabled;

    private boolean removeElytraOnProjectileDamage;
    private boolean removeElytraBhopping;
    private boolean preventFireworkStacking;

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
        preventFireworkStacking = conf.getBoolean(getKey() + "prevent_firework_stacking");
    }

    /**
     * Removes the elytra from the provided players chestplate slot and drops it
     * on the floor of their current location.
     *
     * @param player Player
     */
    private void removeElytraFromChestplate(Player player) {
        Bukkit.broadcastMessage("removeElytraFromChestPlate called");
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

    /**
     * Catch-all which prevents using firework stacks obtained from non-crafting scenarios
     * @param event PlayerInteractEvent
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!isEnabled() || !preventFireworkStacking || !event.useItemInHand().equals(Event.Result.ALLOW)) {
            return;
        }

        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && !event.getAction().equals(Action.RIGHT_CLICK_AIR)) {
            return;
        }

        final ItemStack hand = event.getItem();
        final Player player = event.getPlayer();

        if (hand == null || !hand.getType().equals(Material.FIREWORK_ROCKET)) {
            return;
        }

        if (hand.getAmount() > 1) {
            player.sendMessage(ChatColor.RED + "Firework stacking is disabled. You may only hold 1 Firework Rocket at a time.");
            event.setUseItemInHand(Event.Result.DENY);
        }
    }

    /**
     * Sets stack size for firework crafting
     * @param event PrepareItemCraftEvent
     */
    @EventHandler (priority = EventPriority.MONITOR)
    public void onPrepareCraftItem(PrepareItemCraftEvent event) {
        if (!isEnabled() || !preventFireworkStacking) {
            return;
        }

        final Recipe recipe = event.getRecipe();

        if (recipe == null) {
            return;
        }

        final ItemStack result = recipe.getResult();

        if (!result.getType().equals(Material.FIREWORK_ROCKET)) {
            return;
        }

        result.setAmount(1);
        event.getInventory().setResult(result);
    }

    /**
     * Assigns unique identifiers to the internal item stack to prevent
     * stacking the item cleanly
     * @param event CraftItemEvent
     */
    @EventHandler (priority = EventPriority.MONITOR)
    public void onCraftItem(CraftItemEvent event) {
        if (!isEnabled() || !preventFireworkStacking || event.isCancelled()) {
            return;
        }

        final ItemStack item = event.getCurrentItem();

        if (item == null || !item.getType().equals(Material.FIREWORK_ROCKET)) {
            return;
        }

        final ItemMeta meta = item.getItemMeta();
        final NamespacedKey key = new NamespacedKey(plugin, "craftable-firework");

        if (meta == null) {
            return;
        }

        meta.getPersistentDataContainer().set(key, PersistentDataType.LONG, Time.now());

        item.setAmount(1);
        item.setItemMeta(meta);

        event.getRecipe().getResult().setAmount(1);
        event.setCurrentItem(item);
    }
}
