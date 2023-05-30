package gg.hcfactions.cx.hologram.impl;

import gg.hcfactions.libs.bukkit.location.impl.PLocatable;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.Objects;
import java.util.UUID;

public final class Hologram {
    @Getter public UUID uniqueId;
    @Getter public String text;
    @Getter public PLocatable location;

    private ArmorStand entity;

    /**
     * Create a new hologram entity
     * @param text Display Text
     * @param location Location
     */
    public Hologram(String text, PLocatable location) {
        this.uniqueId = UUID.randomUUID();
        this.text = ChatColor.translateAlternateColorCodes('&', text);
        this.location = location;
    }

    /**
     * Create a new hologram entity with an existing UUID
     * @param uniqueId UUID
     * @param text Display Text
     * @param location Location
     */
    public Hologram(UUID uniqueId, String text, PLocatable location) {
        this.uniqueId = uniqueId;
        this.text = ChatColor.translateAlternateColorCodes('&', text);
        this.location = location;
    }

    /**
     * Spawn the hologram entity in to the world
     */
    public void spawn() {
        entity = (ArmorStand) Objects.requireNonNull(location.getBukkitLocation().getWorld()).spawnEntity(location.getBukkitLocation(), EntityType.ARMOR_STAND);
        entity.setVisible(false);
        entity.setGravity(false);
        entity.setCustomName(text);
        entity.setCustomNameVisible(true);
    }

    /**
     * Remove the hologram entity from the world
     */
    public void despawn() {
        entity.remove();

        // cleans up any entity mismatch that may happen
        for (Entity otherEntities : entity.getNearbyEntities(1.0, 1.0, 1.0)) {
            if (!(otherEntities instanceof ArmorStand)) {
                continue;
            }

            otherEntities.remove();
        }
    }

    /**
     * Updates the existing text on this hologram entity
     * @param text Hologram text
     */
    public void setText(String text) {
        entity.setCustomName(ChatColor.translateAlternateColorCodes('&', text));
    }
}
