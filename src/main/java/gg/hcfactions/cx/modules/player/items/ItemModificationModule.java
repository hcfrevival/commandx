package gg.hcfactions.cx.modules.player.items;

import gg.hcfactions.cx.modules.ICXModule;
import gg.hcfactions.libs.bukkit.AresPlugin;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

public class ItemModificationModule implements ICXModule, Listener {
    @Getter public final AresPlugin plugin;
    @Getter public final String key;
    @Getter @Setter public boolean enabled;

    private boolean disableChorusFruitTeleport;
    private boolean disableFishingPlayers;
    private boolean disableShield;
    private boolean reduceAxeDamage;
    private double reduceAxeDamagePercent;

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
    }

    @Override
    public void onReload() {
        loadConfig();
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

        if (event.getAction().equals(InventoryAction.MOVE_TO_OTHER_INVENTORY)) {
            player.sendMessage(ChatColor.RED + "This item can not be moved to your off-hand");
            event.setCancelled(true);
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
        if (!isEnabled() || !reduceAxeDamage) {
            return;
        }

        final Entity damager = event.getDamager();

        if (!(damager instanceof final Player player)) {
            return;
        }

        if (!player.getInventory().getItemInMainHand().getType().name().endsWith("_AXE")) {
            return;
        }

        event.setDamage(event.getDamage() * reduceAxeDamagePercent);
    }
}
