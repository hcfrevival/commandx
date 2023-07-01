package gg.hcfactions.cx.listener;

import com.google.common.collect.Sets;
import gg.hcfactions.cx.CXPermissions;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.warp.impl.WarpGateway;
import gg.hcfactions.libs.bukkit.events.impl.PlayerBigMoveEvent;
import gg.hcfactions.libs.bukkit.scheduler.Scheduler;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class WarpGatewayListener implements Listener {
    @Getter public final CXService service;
    private final Set<UUID> recentlyTeleported;

    public WarpGatewayListener(CXService service) {
        this.service = service;
        this.recentlyTeleported = Sets.newConcurrentHashSet();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerBigMoveEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final Player player = event.getPlayer();
        final UUID uniqueId = player.getUniqueId();

        service.getWarpManager().getGateway(event.getTo().getBlock()).ifPresent(gateway -> {
            if (recentlyTeleported.contains(uniqueId)) {
                player.sendMessage(ChatColor.RED + "Gateway cooling down...");
                event.setCancelled(true);
                return;
            }

            gateway.teleport(player);
            recentlyTeleported.add(uniqueId);

            new Scheduler(service.getPlugin()).sync(() -> recentlyTeleported.remove(uniqueId)).delay(10 * 20L).run();
        });
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final Block block = event.getBlock();
        final Player player = event.getPlayer();

        if (!block.getType().equals(Material.END_GATEWAY)) {
            return;
        }

        final Optional<WarpGateway> gateQuery = service.getWarpManager().getGateway(block);

        if (gateQuery.isEmpty()) {
            return;
        }

        if (!player.hasPermission(CXPermissions.CX_MOD)) {
            player.sendMessage(ChatColor.RED + "You do not have permission to unlink this gateway");
            return;
        }

        service.getWarpManager().getGatewayRepository().remove(gateQuery.get());
        player.sendMessage(ChatColor.YELLOW + "Gateway unlinked");
    }
}
