package gg.hcfactions.cx.modules.player.combat;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import gg.hcfactions.cx.modules.ICXModule;
import gg.hcfactions.libs.bukkit.AresPlugin;
import gg.hcfactions.libs.bukkit.events.impl.PlayerDamagePlayerEvent;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class KnockbackModule implements ICXModule, Listener {
    @Getter public final AresPlugin plugin;
    @Getter public final String key;
    @Getter @Setter public boolean enabled;

    private double knockbackHorizontal = 0.4D;
    private double knockbackVertical = 0.4D;
    private double knockbackVerticalLimit = 0.4D;
    private double knockbackExtraVertical = 0.0D;
    private double knockbackExtraHorizontal = 0.0D;

    private final Map<UUID, Vector> velocityCache;
    private final Set<UUID> recentlySprinted;

    public KnockbackModule(AresPlugin plugin) {
        this.plugin = plugin;
        this.key = "combat.knockback.";
        this.velocityCache = Maps.newHashMap();
        this.recentlySprinted = Sets.newConcurrentHashSet();
    }

    @Override
    public void onEnable() {
        final YamlConfiguration conf = getConfig();
        enabled = conf.getBoolean(getKey() + "enabled");
        knockbackHorizontal = conf.getDouble(getKey() + "values.horizontal");
        knockbackVertical = conf.getDouble(getKey() + "values.vertical");
        knockbackExtraVertical = conf.getDouble(getKey() + "values.extra_vertical");
        knockbackExtraHorizontal = conf.getDouble(getKey() + "values.extra_horizontal");
        knockbackVerticalLimit = conf.getDouble(getKey() + "values.vertical_limit");

        if (!isEnabled()) {
            return;
        }

        plugin.registerListener(this);
    }

    @Override
    public void onDisable() {
        PlayerToggleSprintEvent.getHandlerList().unregister(this);
        PlayerVelocityEvent.getHandlerList().unregister(this);
        PlayerQuitEvent.getHandlerList().unregister(this);
        EntityDamageByEntityEvent.getHandlerList().unregister(this);
    }

    @Override
    public void onReload() {
        final YamlConfiguration conf = getConfig();
        enabled = conf.getBoolean(getKey() + "enabled");
        knockbackHorizontal = conf.getDouble(getKey() + "values.horizontal");
        knockbackVertical = conf.getDouble(getKey() + "values.vertical");
        knockbackExtraVertical = conf.getDouble(getKey() + "values.vertical_extra");
        knockbackExtraHorizontal = conf.getDouble(getKey() + "values.horizontal_extra");
        knockbackVerticalLimit = conf.getDouble(getKey() + "values.vertical_limit");
    }

    /**
     * Handles giving the player "first sprint" hit, where their knockback is increased to promote w-tapping
     * @param event PlayerToggleSprintEvent
     */
    @EventHandler
    public void onToggleSprint(PlayerToggleSprintEvent event) {
        final Player player = event.getPlayer();

        if (event.isSprinting()) {
            recentlySprinted.add(player.getUniqueId());
            return;
        }

        recentlySprinted.remove(player.getUniqueId());
    }

    /**
     * Handles removing recently sprinted players from memory
     * @param event PlayerQuitEvent
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        recentlySprinted.remove(player.getUniqueId());
        velocityCache.remove(player.getUniqueId());
    }

    /**
     * Disables standard player velocity to allow packet overriding it
     * @param event PlayerVelocityEvent
     */
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onPlayerVelocity(PlayerVelocityEvent event) {
        final Player player = event.getPlayer();

        if (!isEnabled() || !velocityCache.containsKey(player.getUniqueId())) {
            return;
        }

        event.setVelocity(velocityCache.get(player.getUniqueId()));
        velocityCache.remove(player.getUniqueId());
    }

    /**
     * Handles overwriting knockback velocity and sending the velocity packet immediately to prevent lag compensation
     * @param event PlayerDamagePlayerEvent
     */
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onPlayerDamagePlayer(PlayerDamagePlayerEvent event) {
        if (!isEnabled() || event.isCancelled()) {
            return;
        }

        final Player damaged = event.getDamaged();
        final Player damager = event.getDamager();

        if (damaged.getNoDamageTicks() > damaged.getMaximumNoDamageTicks() / 2D) {
            return;
        }

        Player attacker = (Player) event.getDamager();

        // Figure out base knockback direction
        double d0 = attacker.getLocation().getX() - damaged.getLocation().getX();
        double d1;

        for (d1 = attacker.getLocation().getZ() - damaged.getLocation().getZ();
             d0 * d0 + d1 * d1 < 1.0E-4D; d1 = (Math.random() - Math.random()) * 0.01D)
            d0 = (Math.random() - Math.random()) * 0.01D;

        double magnitude = Math.sqrt(d0 * d0 + d1 * d1);

        // Get player knockback taken before any friction applied
        Vector playerVelocity = damaged.getVelocity();

        // apply friction then add the base knockback
        playerVelocity.setX((playerVelocity.getX() / 2) - (d0 / magnitude * knockbackHorizontal));
        playerVelocity.setY((playerVelocity.getY() / 2) + knockbackVertical);
        playerVelocity.setZ((playerVelocity.getZ() / 2) - (d1 / magnitude * knockbackHorizontal));

        // Calculate bonus knockback for sprinting or knockback enchantment levels
        int i = attacker.getItemInHand().getEnchantmentLevel(Enchantment.KNOCKBACK);

        if (recentlySprinted.contains(damager.getUniqueId())) {
            i += 1;
        }

        if (playerVelocity.getY() > knockbackVerticalLimit)
            playerVelocity.setY(knockbackVerticalLimit);

        // Apply bonus knockback
        if (i > 0)
            playerVelocity.add(new Vector((-Math.sin(attacker.getLocation().getYaw() * 3.1415927F / 180.0F) *
                    (float) i * knockbackExtraHorizontal), knockbackExtraVertical,
                    Math.cos(attacker.getLocation().getYaw() * 3.1415927F / 180.0F) *
                            (float) i * knockbackExtraHorizontal));

        velocityCache.put(damaged.getUniqueId(), playerVelocity);
        recentlySprinted.remove(damager.getUniqueId());
    }
}
