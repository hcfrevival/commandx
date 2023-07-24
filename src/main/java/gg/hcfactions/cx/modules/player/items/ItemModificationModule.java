package gg.hcfactions.cx.modules.player.items;

import gg.hcfactions.cx.CXPermissions;
import gg.hcfactions.cx.modules.ICXModule;
import gg.hcfactions.libs.bukkit.AresPlugin;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

public class ItemModificationModule implements ICXModule, Listener {
    @Getter public final AresPlugin plugin;
    @Getter public final String key;
    @Getter @Setter public boolean enabled;

    private boolean disableChorusFruitTeleport;
    private boolean disableFishingPlayers;
    private boolean disableShield;
    private boolean disableSpectralMultishot;
    private boolean reduceTridentDamage;
    private double reduceTridentDamagePercent;
    private boolean reduceAxeDamage;
    private double reduceAxeDamagePercent;
    private boolean milkExcludesInfiniteEffects;
    private boolean disableFireworkCrossbows;

    public ItemModificationModule(AresPlugin plugin) {
        this.plugin = plugin;
        this.key = "items.modifications.";
        this.enabled = false;
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
        setEnabled(false);
    }

    private void loadConfig() {
        final YamlConfiguration conf = getConfig();
        this.enabled = conf.getBoolean(getKey() + "enabled");
        this.disableShield = conf.getBoolean(getKey() + "disable_shields");
        this.disableChorusFruitTeleport = conf.getBoolean(getKey() + "disable_chorus_fruit_teleport");
        this.disableFishingPlayers = conf.getBoolean(getKey() + "disable_fishing_players");
        this.reduceAxeDamage = conf.getBoolean(getKey() + "reduce_axe_damage.enabled");
        this.reduceAxeDamagePercent = conf.getDouble(getKey() + "reduce_axe_damage.reduction");
        this.reduceTridentDamage = conf.getBoolean(getKey() + "reduce_trident_damage.enabled");
        this.reduceTridentDamagePercent = conf.getDouble(getKey() + "reduce_trident_damage.reduction");
        this.milkExcludesInfiniteEffects = conf.getBoolean(getKey() + "milk_excludes_infinite_effects");
        this.disableFireworkCrossbows = conf.getBoolean(getKey() + "disable_crossbow_fireworks");
        this.disableSpectralMultishot = conf.getBoolean(getKey() + "disable_spectral_multishot");
    }

    @Override
    public void onReload() {
        loadConfig();
    }

    @EventHandler
    public void onPlayerDrinkMilk(PlayerItemConsumeEvent event) {
        if (!isEnabled() || !milkExcludesInfiniteEffects) {
            return;
        }

        final Player player = event.getPlayer();
        final ItemStack item = event.getItem();

        if (!item.getType().equals(Material.MILK_BUCKET)) {
            return;
        }

        event.setCancelled(true);

        player.getActivePotionEffects().forEach(potionEffect -> {
            if (!potionEffect.isInfinite()) {
                player.removePotionEffect(potionEffect.getType());
            }
        });
    }

    @EventHandler
    public void onHandSwap(PlayerSwapHandItemsEvent event) {
        if (!isEnabled() || !disableShield) {
            return;
        }

        final Player player = event.getPlayer();
        final ItemStack item = event.getOffHandItem();

        if (item == null || !item.getType().equals(Material.SHIELD)) {
            return;
        }

        player.sendMessage(ChatColor.RED + "This item can not be moved to your off-hand");
        event.setCancelled(true);
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onCrossbowLimit(EntityShootBowEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!(event.getEntity() instanceof final Player player)) {
            return;
        }

        if (player.hasPermission(CXPermissions.CX_MOD)) {
            return;
        }

        final ItemStack consumable = event.getConsumable();

        if (consumable == null) {
            return;
        }

        if (disableFireworkCrossbows && consumable.getType().equals(Material.FIREWORK_ROCKET)) {
            player.sendMessage(ChatColor.RED + "Fireworks launched by Crossbows are disabled");
            event.setCancelled(true);
            return;
        }

        if (disableSpectralMultishot && consumable.getType().equals(Material.SPECTRAL_ARROW)) {
            player.sendMessage(ChatColor.RED + "Spectral Arrows launched by Crossbows are disabled");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!isEnabled() || !disableShield) {
            return;
        }

        if (!(event.getWhoClicked() instanceof final Player player)) {
            return;
        }

        for (Integer i : event.getNewItems().keySet()) {
            final ItemStack item = event.getNewItems().get(i);

            if (i == 45 && item.getType().equals(Material.SHIELD)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "This item can not be moved to your off-hand");
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!isEnabled() || !disableShield) {
            return;
        }

        if (!(event.getWhoClicked() instanceof final Player player)) {
            return;
        }

        final ItemStack item = event.getCurrentItem();
        if (item == null || !item.getType().equals(Material.SHIELD)) {
            return;
        }

        player.sendMessage(ChatColor.RED + "This item can not be moved to your off-hand");
        event.setCancelled(true);
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!isEnabled() || !disableShield) {
            return;
        }

        if (!event.getRecipe().getResult().getType().equals(Material.SHIELD)) {
            return;
        }

        event.setResult(Event.Result.DENY);
        event.setCurrentItem(null);
        event.setCancelled(true);

        if (event.getWhoClicked() instanceof final Player player) {
            player.sendMessage(ChatColor.RED + "This item can not be crafted");
        }
    }

    @EventHandler
    public void onBlockShield(PlayerInteractEvent event) {
        if (!isEnabled() || !disableShield) {
            return;
        }

        if (event.getAction().equals(Action.PHYSICAL)) {
            return;
        }

        final Player player = event.getPlayer();

        if (!player.isBlocking() && !player.isHandRaised()) {
            return;
        }

        final ItemStack mainHand = player.getInventory().getItemInMainHand();
        final ItemStack offHand = player.getInventory().getItemInOffHand();

        if (mainHand.getType().equals(Material.SHIELD)) {
            player.getInventory().setItemInMainHand(null);
            player.sendMessage(ChatColor.RED + "Shields are disabled in the main-hand");
        }

        if (offHand.getType().equals(Material.SHIELD)) {
            player.getInventory().setItemInOffHand(null);
            player.sendMessage(ChatColor.RED + "Shields are disabled in the off-hand");
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!isEnabled() || !disableChorusFruitTeleport) {
            return;
        }

        if (event.getCause().equals(PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (!isEnabled() || !disableFishingPlayers) {
            return;
        }

        if (!(event.getCaught() instanceof Player)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!isEnabled()) {
            return;
        }

        final Entity damager = event.getDamager();

        if (!(damager instanceof final Player player)) {
            return;
        }

        if (reduceAxeDamage && player.getInventory().getItemInMainHand().getType().name().endsWith("_AXE")) {
            event.setDamage(event.getDamage() * reduceAxeDamagePercent);
            return;
        }

        if (reduceTridentDamage && player.getInventory().getItemInMainHand().getType().equals(Material.TRIDENT)) {
            event.setDamage(event.getDamage() * reduceTridentDamagePercent);
        }
    }
}
