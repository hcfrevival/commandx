package gg.hcfactions.cx.modules.player.combat;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import gg.hcfactions.cx.command.KnockbackCommand;
import gg.hcfactions.cx.event.PlayerSprintResetEvent;
import gg.hcfactions.cx.modules.ICXModule;
import gg.hcfactions.libs.bukkit.AresPlugin;
import gg.hcfactions.libs.bukkit.events.impl.PlayerDamagePlayerEvent;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class KnockbackModule implements ICXModule, Listener {
    @Getter public final AresPlugin plugin;
    @Getter public final String key;
    @Getter @Setter public boolean enabled;

    @Getter @Setter public boolean requireGroundCheck = false;
    @Getter @Setter public double knockbackHorizontal = 0.35D;
    @Getter @Setter public double knockbackVertical = 0.35D;
    @Getter @Setter public double knockbackVerticalLimit = 0.4D;
    @Getter @Setter public double knockbackExtraVertical = 0.085D;
    @Getter @Setter public double knockbackExtraHorizontal = 0.425D;

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
        loadConfig();

        if (!isEnabled()) {
            return;
        }

        plugin.registerCommand(new KnockbackCommand(this));
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
        loadConfig();
    }

    private void loadConfig() {
        final YamlConfiguration conf = getConfig();
        enabled = conf.getBoolean(getKey() + "enabled");
        requireGroundCheck = conf.getBoolean(getKey() + "ground_check");
        knockbackHorizontal = conf.getDouble(getKey() + "values.horizontal");
        knockbackVertical = conf.getDouble(getKey() + "values.vertical");
        knockbackExtraVertical = conf.getDouble(getKey() + "values.extra_vertical");
        knockbackExtraHorizontal = conf.getDouble(getKey() + "values.extra_horizontal");
        knockbackVerticalLimit = conf.getDouble(getKey() + "values.vertical_limit");
    }

    public void saveConfig() {
        final YamlConfiguration conf = getConfig();
        conf.set(getKey() + "values.horizontal", getKnockbackHorizontal());
        conf.set(getKey() + "values.vertical", getKnockbackVertical());
        conf.set(getKey() + "values.extra_vertical", getKnockbackExtraVertical());
        conf.set(getKey() + "values.extra_horizontal", getKnockbackExtraHorizontal());
        conf.set(getKey() + "values.vertical_limit", getKnockbackVerticalLimit());
        plugin.saveConfiguration("commandx", conf);
    }

    /**
     * Handles giving the player "first sprint" hit, where their knockback is increased to promote w-tapping
     * @param event PlayerToggleSprintEvent
     */
    @EventHandler
    public void onToggleSprint(PlayerToggleSprintEvent event) {
        final Player player = event.getPlayer();

        if (event.isSprinting()) {
            if (requireGroundCheck && !((LivingEntity)player).isOnGround()) {
                return;
            }

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

        for (AttributeModifier attr : Objects.requireNonNull(damaged.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)).getModifiers()) {
            if (damaged.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE) != null) {
                Objects.requireNonNull(damaged.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)).removeModifier(attr);
            }
        }

        if (damaged.getUniqueId().equals(damager.getUniqueId())) {
            return;
        }

        if (damaged.getNoDamageTicks() > damaged.getMaximumNoDamageTicks() / 2D) {
            return;
        }

        Player attacker = event.getDamager();

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
        int i = attacker.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.KNOCKBACK);

        if (recentlySprinted.contains(damager.getUniqueId())) {
            final PlayerSprintResetEvent resetEvent = new PlayerSprintResetEvent(attacker);
            Bukkit.getPluginManager().callEvent(resetEvent);

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
