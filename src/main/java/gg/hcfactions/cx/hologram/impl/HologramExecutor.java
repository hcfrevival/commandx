package gg.hcfactions.cx.hologram.impl;

import gg.hcfactions.cx.hologram.HologramManager;
import gg.hcfactions.cx.hologram.IHologramExecutor;
import gg.hcfactions.libs.base.consumer.Promise;
import gg.hcfactions.libs.bukkit.location.impl.PLocatable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;

@AllArgsConstructor
public final class HologramExecutor implements IHologramExecutor {
    @Getter public HologramManager manager;

    @Override
    public void createHologram(PLocatable locatable, String text) {
        final Hologram holo = new Hologram(text, locatable);
        holo.spawn();
        manager.getHologramRepository().add(holo);
        manager.saveHolograms();
        manager.getService().getPlugin().getAresLogger().info("spawned hologram at " + locatable.toString());
    }

    @Override
    public void createHologram(Player player, String text) {
        final Hologram holo = new Hologram(text, new PLocatable(player));

        holo.spawn();

        manager.getHologramRepository().add(holo);
        manager.saveHolograms();
        player.sendMessage(ChatColor.GREEN + "Hologram created");
    }

    @Override
    public void deleteHologram(Player player, double radius, Promise promise) {
        final List<Hologram> holograms = manager.getHologramRepository().stream().filter(h -> h.getLocation().isNearby(new PLocatable(player), radius)).toList();

        if (holograms.isEmpty()) {
            promise.reject("No holograms found");
            return;
        }

        holograms.forEach(hologram -> {
            hologram.despawn();
            manager.deleteHologram(hologram);
        });

        holograms.forEach(manager.getHologramRepository()::remove);
        promise.resolve();
    }
}
