package gg.hcfactions.cx.modules.player.combat;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedEnumEntityUseAction;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import gg.hcfactions.cx.modules.ICXModule;
import gg.hcfactions.libs.bukkit.AresPlugin;
import gg.hcfactions.libs.bukkit.events.impl.PlayerDamagePlayerEvent;
import gg.hcfactions.libs.bukkit.scheduler.Scheduler;
import gg.hcfactions.libs.bukkit.utils.Worlds;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public final class AnimationModule implements ICXModule, Listener {
    @Getter public final AresPlugin plugin;
    @Getter public final String key;
    @Getter @Setter public boolean enabled;

    // config values
    private double maxReach;
    private int noDamageTicks;

    private BukkitTask attackQueueTask;
    private final Set<UUID> attackCooldowns;
    private final Queue<QueuedAttack> queuedAttacks;

    public AnimationModule(AresPlugin plugin) {
        this.plugin = plugin;
        this.key = "combat.animation.";
        this.enabled = false;
        this.attackCooldowns = Sets.newConcurrentHashSet();
        this.queuedAttacks = Queues.newConcurrentLinkedQueue();
    }

    @Override
    public void onEnable() {
        if (plugin.getRegisteredProtocolManager() == null) {
            plugin.getAresLogger().error("failed to register animation module: protocollib not registered");
            return;
        }

        final YamlConfiguration conf = getConfig();
        enabled = conf.getBoolean(getKey() + "enabled");
        maxReach = conf.getDouble(getKey() + "max_reach");
        noDamageTicks = conf.getInt(getKey() + "no_damage_ticks");

        plugin.registerListener(this);

        implPacketListener();

        attackQueueTask = new Scheduler(plugin).sync(() -> {
            while (!queuedAttacks.isEmpty()) {
                final QueuedAttack attack = queuedAttacks.remove();

                if (!attackCooldowns.contains(attack.getAttacked().getUniqueId())) {
                    attackCooldowns.add(attack.getAttacked().getUniqueId());
                    attack.getAttacked().damage(attack.getDamage(), attack.getAttacker());
                    new Scheduler(plugin).sync(() -> attackCooldowns.remove(attack.getAttacked().getUniqueId())).delay(noDamageTicks).run();
                }
            }
        }).repeat(0L, 1L).run();

        this.enabled = true;
    }

    @Override
    public void onReload() {
        final YamlConfiguration conf = getConfig();
        maxReach = conf.getDouble(getKey() + "max_reach");
        noDamageTicks = conf.getInt(getKey() + "no_damage_ticks");
    }

    @Override
    public void onDisable() {
        queuedAttacks.clear();
        attackCooldowns.clear();

        attackQueueTask.cancel();
        attackQueueTask = null;

        PlayerJoinEvent.getHandlerList().unregister(this);
        PlayerQuitEvent.getHandlerList().unregister(this);
        PlayerChangedWorldEvent.getHandlerList().unregister(this);

        enabled = false;
    }

    private void implPacketListener() {
        plugin.getRegisteredProtocolManager().addPacketListener(new PacketAdapter(plugin, ListenerPriority.LOWEST, PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                final PacketContainer packet = event.getPacket();
                final WrappedEnumEntityUseAction useAction = packet.getEnumEntityUseActions().read(0);
                final EnumWrappers.EntityUseAction action = useAction.getAction();

                if (!action.equals(EnumWrappers.EntityUseAction.ATTACK)) {
                    return;
                }

                event.setCancelled(true);

                new Scheduler(plugin).sync(() -> {
                    final Player damager = event.getPlayer();
                    final Entity entity = packet.getEntityModifier(event).readSafely(0);

                    if (entity == null) {
                        return;
                    }

                    if (entity instanceof Player && !((Player)entity).getGameMode().equals(GameMode.SURVIVAL)) {
                        return;
                    }

                    if (entity instanceof final LivingEntity damaged) {
                        final double distance = damager.getLocation().distanceSquared(damaged.getLocation());

                        if (damaged.isDead()) {
                            return;
                        }

                        if (distance > (maxReach * maxReach)) {
                            return;
                        }

                        double initialDamage = Objects.requireNonNull(damager.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).getValue();
                        boolean criticalHit = false;

                        if (!((LivingEntity) damager).isOnGround() && damager.getVelocity().getY() < 0) {
                            initialDamage *= 1.25;
                            criticalHit = true;
                            Worlds.playSound(damaged.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT);
                        }

                        queuedAttacks.add(new QueuedAttack(damager, damaged, initialDamage, criticalHit));
                    }
                }).run();
            }
        });
    }

    /**
     * Handles setting the attack speed for a player upon logging in
     * @param event PlayerJoinEvent
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!isEnabled()) {
            return;
        }

        final Player player = event.getPlayer();
        Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_ATTACK_SPEED)).setBaseValue(2048.0);
        player.saveData();
    }

    /**
     * Handles reverting attack speed for a player quitting the server
     * @param event PlayerQuitEvent
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_ATTACK_SPEED)).setBaseValue(4.0);
        player.saveData();
    }

    /**
     * Handles updating attack speed for a player changing worlds
     * @param event PlayerChangedWorldEvent
     */
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (!isEnabled()) {
            return;
        }

        final Player player = event.getPlayer();
        Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_ATTACK_SPEED)).setBaseValue(2048.0);
        player.saveData();
    }

    /**
     * Handles applying fire aspect on attack
     * @param event PlayerDamagePlayerEvent
     */
    @EventHandler (priority = EventPriority.MONITOR)
    public void onPlayerDamagePlayer(PlayerDamagePlayerEvent event) {
        if (!isEnabled()) {
            return;
        }

        if (event.isCancelled()) {
            return;
        }

        final Player damager = event.getDamager();
        final Player damaged = event.getDamaged();
        final ItemStack hand = damager.getInventory().getItemInMainHand();

        if (damaged.getUniqueId().equals(damaged.getUniqueId())) {
            return;
        }

        if (hand.hasItemMeta() && Objects.requireNonNull(hand.getItemMeta()).hasEnchant(Enchantment.FIRE_ASPECT)) {
            damaged.setFireTicks(80 * hand.getItemMeta().getEnchantLevel(Enchantment.FIRE_ASPECT));
        }
    }

    /**
     * Handles applying fire aspect to non-player entities
     * @param event EntityDamageByEntityEvent
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!isEnabled()) {
            return;
        }

        if (event.isCancelled()) {
            return;
        }

        final Entity damager = event.getDamager();
        final Entity entity = event.getEntity();

        if (event.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK)) {
            event.setCancelled(true);
        }

        if (!(damager instanceof Player) || entity instanceof Player) {
            return;
        }

        final Player player = (Player)damager;
        final ItemStack hand = player.getInventory().getItemInMainHand();

        if (player.getUniqueId().equals(entity.getUniqueId())) {
            return;
        }

        if (hand.hasItemMeta() && Objects.requireNonNull(hand.getItemMeta()).hasEnchant(Enchantment.FIRE_ASPECT)) {
            entity.setFireTicks(80 * hand.getItemMeta().getEnchantLevel(Enchantment.FIRE_ASPECT));
        }
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onNoDamageTickApplied(EntityDamageEvent event) {
        final EntityDamageEvent.DamageCause cause = event.getCause();

        if (!(event.getEntity() instanceof final Player player)) {
            return;
        }

        if (!cause.equals(EntityDamageEvent.DamageCause.POISON) && !cause.equals(EntityDamageEvent.DamageCause.WITHER) && !cause.equals(EntityDamageEvent.DamageCause.FIRE_TICK)) {
            return;
        }

        player.setNoDamageTicks(0);
    }

    @AllArgsConstructor
    public final class QueuedAttack {
        @Getter public final Player attacker;
        @Getter public final LivingEntity attacked;
        @Getter public final double damage;
        @Getter public final boolean critical;
    }
}
