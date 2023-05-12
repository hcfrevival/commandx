package gg.hcfactions.cx.modules.player.combat;

import com.google.common.collect.Lists;
import gg.hcfactions.cx.modules.ICXModule;
import gg.hcfactions.libs.bukkit.AresPlugin;
import gg.hcfactions.libs.bukkit.scheduler.Scheduler;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.UUID;

public final class RegenModule implements ICXModule, Listener {
    @Getter public final AresPlugin plugin;
    @Getter public final String key;
    @Getter @Setter public boolean enabled;

    private int healRate;
    private List<UUID> recentlyHealed;

    public RegenModule(AresPlugin plugin) {
        this.plugin = plugin;
        this.key = "combat.regen.";
        this.recentlyHealed = Lists.newArrayList();
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
        recentlyHealed.clear();
        setEnabled(false);
    }

    @Override
    public void onReload() {
        loadConfig();
    }

    private void loadConfig() {
        final YamlConfiguration conf = getConfig();
        this.enabled = conf.getBoolean(getKey() + "enabled");
        this.healRate = conf.getInt(getKey() + "heal_rate");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        recentlyHealed.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (!isEnabled()) {
            return;
        }

        if (!(event.getEntity() instanceof final Player player)) {
            return;
        }

        if (!event.getRegainReason().equals(EntityRegainHealthEvent.RegainReason.SATIATED)) {
            return;
        }

        final float preExhaustion = player.getExhaustion();
        final float preSaturation = player.getSaturation();
        final double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();

        event.setCancelled(true);

        if (player.getHealth() < maxHealth && !recentlyHealed.contains(player.getUniqueId())) {
            player.setHealth(Math.min(player.getHealth() + 1.0, maxHealth));
            recentlyHealed.add(player.getUniqueId());
            new Scheduler(plugin).sync(() -> recentlyHealed.remove(player.getUniqueId())).delay(healRate * 20L).run();
        }

        new Scheduler(plugin).sync(() -> {
            player.setExhaustion(preExhaustion + 0.2F);
            player.setSaturation(preSaturation - 0.2F);
        }).delay(1L).run();
    }
}
