package gg.hcfactions.cx.listener;

import gg.hcfactions.cx.CXService;
import gg.hcfactions.libs.bukkit.location.impl.PLocatable;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public record NPCListener(@Getter CXService service) implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        service.getNpcManager().getVisibleNPC(new PLocatable(player)).forEach(npc -> npc.spawn(player));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        service.getNpcManager().getNpcRepository().forEach(npc -> npc.getViewers().remove(player.getUniqueId()));
    }

    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        final Player player = event.getPlayer();
        final PLocatable loc = new PLocatable(player);
        service.getNpcManager().getVisibleNPC(loc).forEach(npc -> npc.spawn(player));
        service.getNpcManager().getInvisibleNPC(loc).forEach(npc -> npc.despawn(player));
    }
}
