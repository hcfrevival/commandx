package gg.hcfactions.cx.modules.player.combat;

import com.google.common.collect.Lists;
import gg.hcfactions.cx.modules.ICXModule;
import gg.hcfactions.libs.bukkit.AresPlugin;
import gg.hcfactions.libs.bukkit.events.impl.PlayerLingeringSplashEvent;
import gg.hcfactions.libs.bukkit.remap.ERemappedEffect;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Objects;

public final class PotionLimitModule implements ICXModule, Listener {
    @Getter public final AresPlugin plugin;
    @Getter public final String key;
    @Getter @Setter public boolean enabled;
    @Getter public final List<PotionLimit> potionLimits;

    public PotionLimitModule(AresPlugin plugin) {
        this.plugin = plugin;
        this.key = "combat.potion_limits.";
        this.potionLimits = Lists.newArrayList();
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

            if (type == null) {
                plugin.getAresLogger().error("bad effect type: " + effectName);
                continue;
            }

            final PotionLimit limit = new PotionLimit(type, disabled, extendable, amplifiable);
            potionLimits.add(limit);
        }

        plugin.getAresLogger().info("loaded " + potionLimits.size() + " potion limits");
    }

    /**
     * Returns the limit for the provided Potion Effect Type
     * @param type Potion Effect Type
     * @return Potion Limit
     */
    public PotionLimit getPotionLimit(PotionEffectType type) {
        return potionLimits.stream().filter(p -> p.getType().equals(type)).findFirst().orElse(null);
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
            final boolean extended = effect.getDuration() > 90*20;

            if (limit == null) {
                continue;
            }

            if (limit.isDisabled()) {
                event.setCancelled(true);
                event.getAffectedEntities().clear();
                return;
            }

            if (!limit.isAmplifiable() && effect.getAmplifier() > 0) {
                event.setCancelled(true);
                event.getAffectedEntities().clear();
                return;
            }

            if (!limit.isExtendable() && extended) {
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

        if (limit.isDisabled()) {
            event.setCancelled(true);
            return;
        }

        if (!limit.isAmplifiable() && event.getCloud().getBasePotionData().isUpgraded()) {
            event.setCancelled(true);
            return;
        }

        if (!limit.isExtendable() && event.getCloud().getBasePotionData().isExtended()) {
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

        if (limit.isDisabled()) {
            player.sendMessage(ChatColor.RED + "This potion has been disabled");
            event.setCancelled(true);
            return;
        }

        if (!limit.isAmplifiable() && meta.getBasePotionData().isUpgraded()) {
            player.sendMessage(ChatColor.RED + "This potion has been disabled");
            event.setCancelled(true);
            return;
        }

        if (!limit.isExtendable() && meta.getBasePotionData().isExtended()) {
            player.sendMessage(ChatColor.RED + "This potion has been disabled");
            event.setCancelled(true);
        }
    }

    @AllArgsConstructor
    public final class PotionLimit {
        @Getter public final PotionEffectType type;
        @Getter public final boolean disabled;
        @Getter public final boolean extendable;
        @Getter public final boolean amplifiable;
    }
}
