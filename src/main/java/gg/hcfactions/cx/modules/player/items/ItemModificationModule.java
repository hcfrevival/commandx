package gg.hcfactions.cx.modules.player.items;

import gg.hcfactions.cx.modules.ICXModule;
import gg.hcfactions.libs.bukkit.AresPlugin;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class ItemModificationModule implements ICXModule, Listener {
    @Getter public final AresPlugin plugin;
    @Getter public final String key;
    @Getter @Setter public boolean enabled;

    private boolean disableChorusFruitTeleport;
    private boolean disableFishingPlayers;
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
