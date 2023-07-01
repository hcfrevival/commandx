package gg.hcfactions.cx.warp.impl;

import gg.hcfactions.cx.warp.IWarpExecutor;
import gg.hcfactions.cx.warp.WarpManager;
import gg.hcfactions.libs.base.consumer.Promise;
import gg.hcfactions.libs.bukkit.location.impl.BLocatable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

@AllArgsConstructor
public final class WarpExecutor implements IWarpExecutor {
    @Getter public WarpManager manager;

    @Override
    public void createWarp(Player player, String warpName, Promise promise) {
        if (manager.getWarp(warpName).isPresent()) {
            promise.reject("Warp name is already in use");
            return;
        }

        final Warp warp = new Warp(player, warpName);
        manager.getWarpRepository().add(warp);
        manager.saveWarp(warp);
        promise.resolve();
    }

    @Override
    public void removeWarp(Player player, String warpName, Promise promise) {
        final Optional<Warp> existing = manager.getWarp(warpName);

        if (existing.isEmpty()) {
            promise.reject("Warp not found");
            return;
        }

        manager.deleteWarp(existing.get());
        promise.resolve();
    }

    @Override
    public void listWarps(Player player) {
        if (manager.getWarpRepository().isEmpty()) {
            player.sendMessage(ChatColor.RED + "No warps set");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "Warp List (" + ChatColor.YELLOW + manager.getWarpRepository().size() + ChatColor.GOLD + ")");

        manager.getWarpRepository().forEach(w -> player.spigot().sendMessage(
                new ComponentBuilder(" ")
                        .color(net.md_5.bungee.api.ChatColor.RESET)
                        .append(" - ")
                        .color(net.md_5.bungee.api.ChatColor.YELLOW)
                        .append(w.getName() + " ")
                        .color(net.md_5.bungee.api.ChatColor.YELLOW)
                        .append("[")
                        .color(net.md_5.bungee.api.ChatColor.GOLD)
                        .append(w.toString())
                        .color(net.md_5.bungee.api.ChatColor.BLUE)
                        .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/warp " + w.getName()))
                        .append("]")
                        .color(net.md_5.bungee.api.ChatColor.GOLD)
                        .create()));
    }

    @Override
    public void createGateway(Player player, String destinationName, Promise promise) {
        final Optional<Warp> warpQuery = manager.getWarp(destinationName);

        if (warpQuery.isEmpty()) {
            promise.reject("Warp not found");
            return;
        }

        final Warp warp = warpQuery.get();
        final Block origin = player.getLocation().getBlock();
        final Block portalBlock = origin.getRelative(BlockFace.DOWN);
        final WarpGateway gateway = new WarpGateway(manager.getService(), UUID.randomUUID(), warp.getName(), new BLocatable(portalBlock));

        manager.getGatewayRepository().add(gateway);
        manager.saveGateway(gateway);
        portalBlock.setType(Material.END_GATEWAY);

        promise.resolve();
    }

    @Override
    public void deleteGateway(Player player, String destinationName, Promise promise) {
        final boolean removed = manager.getGatewayRepository().removeIf(g -> g.getDestinationName().equalsIgnoreCase(destinationName));

        if (!removed) {
            promise.reject("No gateways found");
            return;
        }

        promise.resolve();
    }
}
