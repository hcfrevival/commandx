package gg.hcfactions.cx.warp;

import com.google.common.collect.Lists;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.warp.impl.Warp;
import gg.hcfactions.cx.warp.impl.WarpExecutor;
import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class WarpManager {
    @Getter public final CXService service;
    @Getter public final WarpExecutor executor;
    @Getter public List<Warp> warpRepository;

    public WarpManager(CXService service) {
        this.service = service;
        this.executor = new WarpExecutor(this);
        this.warpRepository = Lists.newArrayList();
    }

    public Optional<Warp> getWarp(String warpName) {
        return warpRepository.stream().filter(w -> w.getName().equalsIgnoreCase(warpName)).findFirst();
    }

    public void loadWarps() {
        if (!warpRepository.isEmpty()) {
            warpRepository.clear();
        }

        final YamlConfiguration conf = service.getPlugin().loadConfiguration("warps");

        if (conf.getConfigurationSection("data") == null) {
            service.getPlugin().getAresLogger().warn("could not find data in warps.yml. skipping...");
            return;
        }

        for (String warpName : Objects.requireNonNull(conf.getConfigurationSection("data")).getKeys(false)) {
            final String key = "data." + warpName + ".";
            final double x = conf.getDouble(key + "x");
            final double y = conf.getDouble(key + "y");
            final double z = conf.getDouble(key + "z");
            final float yaw = (float)conf.getDouble(key + "yaw");
            final float pitch = (float)conf.getDouble(key + "pitch");
            final String worldName = conf.getString(key + "world");

            final Warp warp = new Warp(x, y, z, yaw, pitch, worldName, warpName);
            warpRepository.add(warp);
        }

        service.getPlugin().getAresLogger().info("loaded " + warpRepository.size() + " warps");
    }

    public void saveWarp(Warp warp) {
        final YamlConfiguration conf = service.getPlugin().loadConfiguration("warps");
        final String key = "data." + warp.getName() + ".";

        conf.set(key + "x", warp.getX());
        conf.set(key + "y", warp.getY());
        conf.set(key + "z", warp.getZ());
        conf.set(key + "yaw", warp.getYaw());
        conf.set(key + "pitch", warp.getPitch());

        service.getPlugin().saveConfiguration("warps", conf);
    }

    public void deleteWarp(Warp warp) {
        final YamlConfiguration conf = service.getPlugin().loadConfiguration("warps");
        conf.set("data." + warp.getName(), null);
        service.getPlugin().saveConfiguration("warps", conf);
    }
}
