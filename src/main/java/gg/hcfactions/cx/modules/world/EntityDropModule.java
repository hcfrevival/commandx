package gg.hcfactions.cx.modules.world;

import gg.hcfactions.cx.modules.ICXModule;
import gg.hcfactions.libs.bukkit.AresPlugin;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Vindicator;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Random;

public final class EntityDropModule implements ICXModule, Listener {
    @Getter public final AresPlugin plugin;
    @Getter public final String key;
    @Getter @Setter public boolean enabled;

    private final Random random;

    private boolean totemDropChanceEnabled;
    private float totemDropChance;

    public EntityDropModule(AresPlugin plugin) {
        this.plugin = plugin;
        this.key = "world.entitydrops.";
        this.random = new Random();
    }

    @Override
    public void onEnable() {
        loadConfig();

        plugin.registerListener(this);

        setEnabled(true);
    }

    @Override
    public void onDisable() {
        loadConfig();

        EntityDeathEvent.getHandlerList().unregister(this);

        setEnabled(false);
    }

    @Override
    public void onReload() {
        loadConfig();
    }

    private void loadConfig() {
        final YamlConfiguration conf = getConfig();

        totemDropChanceEnabled = conf.getBoolean(getKey() + "types.totem.enabled");
        totemDropChance = (float)conf.getDouble(getKey() + "types.totem.chance");
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onEntityDrop(EntityDeathEvent event) {
        final Entity entity = event.getEntity();

        if (entity instanceof Vindicator && totemDropChanceEnabled) {
            final double roll = random.nextFloat(100.0f);

            if (roll > totemDropChance) {
                event.getDrops().clear();
            }
        }
    }
}
