package gg.hcfactions.cx.modules.player.combat;

import com.google.common.collect.Lists;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.modules.ICXModule;
import gg.hcfactions.libs.bukkit.events.impl.PlayerLingeringSplashEvent;
import gg.hcfactions.libs.bukkit.remap.ERemappedEffect;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Objects;

public final class PotionLimitModule implements ICXModule, Listener {
    @Getter public final CXService service;
    @Getter public final String key;
    @Getter @Setter public boolean enabled;
    @Getter public final List<PotionLimit> potionLimits;

    public PotionLimitModule(CXService service) {
        this.service = service;
        this.key = "combat.potion_limits.";
        this.potionLimits = Lists.newArrayList();
    }

    @Override
    public void onEnable() {
        loadConfig();

        if (!isEnabled()) {
            return;
        }

        getPlugin().registerListener(this);
    }

    @Override
    public void onDisable() {
        potionLimits.clear();
        setEnabled(false);
    }

    @Override
    public void onReload() {
        loadConfig();
    }

    private void loadConfig() {
        final YamlConfiguration conf = getConfig();
        this.enabled = conf.getBoolean(getKey() + "enabled");

        potionLimits.clear();

        for (String effectName : Objects.requireNonNull(conf.getConfigurationSection(getKey() + "limits")).getKeys(false)) {
            final PotionEffectType type = ERemappedEffect.getEffect(effectName);
            final boolean disabled = conf.getBoolean(getKey() + "limits." + effectName + ".disabled");
            final boolean extendable = conf.getBoolean(getKey() + "limits." + effectName + ".extendable");
            final boolean amplifiable = conf.getBoolean(getKey() + "limits." + effectName + ".amplifiable");
            final boolean canSplash = conf.get(getKey() + "limits." + effectName + ".can_splash") == null || conf.getBoolean(getKey() + "limits." + effectName + "can_splash");

            if (type == null) {
                getPlugin().getAresLogger().error("bad effect type: " + effectName);
                continue;
            }

            final PotionLimit limit = new PotionLimit(type, disabled, extendable, amplifiable, canSplash);
            potionLimits.add(limit);
        }

        getPlugin().getAresLogger().info("loaded " + potionLimits.size() + " potion limits");
    }

    /**
     * Returns the limit for the provided Potion Effect Type
     * @param type Potion Effect Type
     * @return Potion Limit
     */
    public PotionLimit getPotionLimit(PotionEffectType type) {
        return potionLimits.stream().filter(p -> p.type().equals(type)).findFirst().orElse(null);
    }

    /**
     * Handles limiting splash potions
     * @param event PotionSplashEvent
     */
    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        if (!isEnabled()) {
            return;
        }

        for (PotionEffect effect : event.getPotion().getEffects()) {
            final PotionLimit limit = getPotionLimit(effect.getType());
            final boolean extended = (effect.getType().equals(PotionEffectType.POISON)
                    ? effect.getDuration() > 45*20
                    : effect.getDuration() > 90*20);

            if (limit == null) {
                continue;
            }

            if (!limit.canSplash()) {
                event.setCancelled(true);
                event.getAffectedEntities().clear();
                return;
            }

            if (limit.disabled()) {
                event.setCancelled(true);
                event.getAffectedEntities().clear();
                return;
            }

            if (!limit.amplifiable() && effect.getAmplifier() > 0) {
                event.setCancelled(true);
                event.getAffectedEntities().clear();
                return;
            }

            if (!limit.extendable() && extended) {
                event.setCancelled(true);
                event.getAffectedEntities().clear();
            }
        }
    }

    /**
     * Handles limiting lingering potions
     * @param event Lingering Splash Potions
     */
    @EventHandler
    public void onLingeringSplash(PlayerLingeringSplashEvent event) {
        if (!isEnabled()) {
            return;
        }

        final PotionLimit limit = getPotionLimit(event.getCloud().getBasePotionData().getType().getEffectType());

        if (limit == null) {
            return;
        }

        if (limit.disabled()) {
            event.setCancelled(true);
            return;
        }

        if (!limit.canSplash()) {
            event.setCancelled(true);
            return;
        }

        if (!limit.amplifiable() && event.getCloud().getBasePotionData().isUpgraded()) {
            event.setCancelled(true);
            return;
        }

        if (!limit.extendable() && event.getCloud().getBasePotionData().isExtended()) {
            event.setCancelled(true);
        }
    }

    /**
     * Handles limiting drinkable potions
     * @param event PlayerItemConsumeEvent
     */
    @EventHandler
    public void onConsumeItem(PlayerItemConsumeEvent event) {
        if (!isEnabled()) {
            return;
        }

        final Player player = event.getPlayer();
        final ItemStack item = event.getItem();

        if (!item.getType().equals(Material.POTION)) {
            return;
        }

        final PotionMeta meta = (PotionMeta)item.getItemMeta();
        final PotionLimit limit = getPotionLimit(Objects.requireNonNull(meta).getBasePotionData().getType().getEffectType());

        if (limit == null) {
            return;
        }

        if (limit.disabled()) {
            player.sendMessage(ChatColor.RED + "This potion has been disabled");
            event.setCancelled(true);
            return;
        }

        if (!limit.amplifiable() && meta.getBasePotionData().isUpgraded()) {
            player.sendMessage(ChatColor.RED + "This potion has been disabled");
            event.setCancelled(true);
            return;
        }

        if (!limit.extendable() && meta.getBasePotionData().isExtended()) {
            player.sendMessage(ChatColor.RED + "This potion has been disabled");
            event.setCancelled(true);
        }
    }

    /**
     * Removes limited effects upon changing worlds
     * @param event PlayerChangedWorldEvent
     */
    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        final Player player = event.getPlayer();
        final List<PotionEffectType> toRemove = Lists.newArrayList();

        for (PotionEffect effect : player.getActivePotionEffects()) {
            final PotionLimit limit = getPotionLimit(effect.getType());
            final boolean extended = (effect.getType().equals(PotionEffectType.POISON)
                    ? effect.getDuration() > 45*20
                    : effect.getDuration() > 90*20);

            if (effect.isInfinite()) {
                continue;
            }

            if (limit == null) {
                continue;
            }

            if (limit.isDisabled()) {
                toRemove.add(effect.getType());
                continue;
            }

            if (!limit.isAmplifiable() && effect.getAmplifier() > 0) {
                toRemove.add(effect.getType());
                continue;
            }

            if (!limit.isExtendable() && extended) {
                toRemove.add(effect.getType());
            }
        }

        if (toRemove.isEmpty()) {
            return;
        }

        toRemove.forEach(removedEffectType -> {
            player.removePotionEffect(removedEffectType);
            player.sendMessage(ChatColor.RED + "Removed Effect" + ChatColor.RESET + ": "
                    + StringUtils.capitalize(ERemappedEffect.getRemappedEffect(removedEffectType).name().toLowerCase()).replaceAll("_", " "));
        });
    }

    /**
     * Limits tipped arrow potion effects
     * @param event ProjectileHitEvent
     */
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onTippedArrowHit(ProjectileHitEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!(event.getEntity() instanceof final Arrow arrow)) {
            return;
        }

        final PotionEffectType type = arrow.getBasePotionData().getType().getEffectType();

        if (type == null) {
            return;
        }

        final PotionLimit limit = getPotionLimit(type);

        if (limit == null) {
            return;
        }

        if (limit.isDisabled()) {
            if (arrow.getShooter() instanceof final Player player) {
                player.sendMessage(ChatColor.RED + "This Tipped Arrow is disabled");
            }

            event.setCancelled(true);
            return;
        }

        if (!limit.canSplash()) {
            if (arrow.getShooter() instanceof final Player player) {
                player.sendMessage(ChatColor.RED + "This Tipped Arrow is disabled");
            }

            event.setCancelled(true);
            return;
        }

        if (!limit.isAmplifiable() && arrow.getBasePotionData().isUpgraded()) {
            if (arrow.getShooter() instanceof final Player player) {
                player.sendMessage(ChatColor.RED + "This Tipped Arrow can not be amplified");
            }

            event.setCancelled(true);
            return;
        }

        if (!limit.isExtendable() && arrow.getBasePotionData().isExtended()) {
            if (arrow.getShooter() instanceof final Player player) {
                player.sendMessage(ChatColor.RED + "This Tipped Arrow can not be extended");
            }

            event.setCancelled(true);
        }
    }

    public record PotionLimit(@Getter PotionEffectType type, @Getter boolean disabled, @Getter boolean extendable,
                              @Getter boolean amplifiable, @Getter boolean canSplash) {}
}
