package gg.hcfactions.cx.hologram;

import com.google.common.collect.Sets;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.hologram.impl.Hologram;
import gg.hcfactions.cx.hologram.impl.HologramExecutor;
import gg.hcfactions.libs.bukkit.location.impl.PLocatable;
import gg.hcfactions.libs.bukkit.utils.Configs;
import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class HologramManager {
    @Getter public CXService service;
    @Getter public HologramExecutor executor;
    @Getter public Set<Hologram> hologramRepository;

    public HologramManager(CXService service) {
        this.service = service;
        this.executor = new HologramExecutor(this);
        this.hologramRepository = Sets.newHashSet();
    }

    public void loadHolograms() {
        final YamlConfiguration conf = service.getPlugin().loadConfiguration("holograms");

        if (conf.get("data") == null) {
            service.getPlugin().getAresLogger().warn("holograms.yml is empty. skipping...");
            return;
        }

        for (String hid : Objects.requireNonNull(conf.getConfigurationSection("data")).getKeys(false)) {
            final UUID hologramId = UUID.fromString(hid);
            final String hologramText = conf.getString("data." + hid + ".text");
            final PLocatable location = Configs.parsePlayerLocation(conf, "data." + hid + ".location");

            final Hologram hologram = new Hologram(hologramId, hologramText, location);
            hologramRepository.add(hologram);
        }

        service.getPlugin().getAresLogger().info("loaded " + hologramRepository.size() + " holograms");
    }

    public void saveHolograms() {
        final YamlConfiguration conf = service.getPlugin().loadConfiguration("holograms");

        for (Hologram holo : hologramRepository) {
            conf.set("data." + holo.getUniqueId().toString() + ".text", holo.getText());
            Configs.writePlayerLocation(conf, "data." + holo.getUniqueId().toString() + ".location", holo.getLocation());
        }

        service.getPlugin().saveConfiguration("holograms", conf);
        service.getPlugin().getAresLogger().info("saved " + hologramRepository.size() + " holograms");
    }

    public void deleteHologram(Hologram hologram) {
        final YamlConfiguration conf = service.getPlugin().loadConfiguration("holograms");
        conf.set("data." + hologram.getUniqueId().toString(), null);
        service.getPlugin().saveConfiguration("holograms", conf);
        service.getPlugin().getAresLogger().info("deleted hologram (" + hologram.getUniqueId().toString() + ")");
    }

    public void reloadHolograms() {
        despawnHolograms();
        loadHolograms();
        spawnHolograms();
    }

    public void spawnHolograms() {
        service.getPlugin().getAresLogger().info("spawning " + hologramRepository.size() + " holograms");
        hologramRepository.forEach(Hologram::spawn);
    }

    public void despawnHolograms() {
        service.getPlugin().getAresLogger().info("despawning " + hologramRepository.size() + " holograms");

        hologramRepository.forEach(Hologram::despawn);
        hologramRepository.clear();
    }
}
