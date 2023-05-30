package gg.hcfactions.cx.hologram;

import gg.hcfactions.libs.base.consumer.Promise;
import gg.hcfactions.libs.bukkit.location.impl.PLocatable;
import org.bukkit.entity.Player;

public interface IHologramExecutor {
    /**
     * Creates a new hologram
     * @param locatable Location to spawn hologram at
     * @param text Text
     */
    void createHologram(PLocatable locatable, String text);

    /**
     * Creates a new hologram
     * @param player Player (using their location)
     * @param text Hologram Text
     */
    void createHologram(Player player, String text);

    /**
     * Deletes all holograms within the provided radius of the player
     * @param player Player
     * @param radius Radius
     * @param promise Promise
     */
    void deleteHologram(Player player, double radius, Promise promise);
}
