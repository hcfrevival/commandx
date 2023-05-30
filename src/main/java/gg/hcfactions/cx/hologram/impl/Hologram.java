package gg.hcfactions.cx.hologram.impl;

import com.google.common.collect.Lists;
import gg.hcfactions.cx.hologram.EHologramOrder;
import gg.hcfactions.libs.bukkit.location.impl.PLocatable;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.List;
import java.util.Objects;

public final class Hologram {
    @Getter public int id;
    @Getter public List<String> text;
    @Getter public PLocatable origin;
    @Getter public EHologramOrder order;

    public Hologram(int id, List<String> text, PLocatable origin, EHologramOrder order) {
        this.id = id;
        this.text = Lists.newArrayList();
        this.origin = origin;
        this.order = order;

        for (String line : text) {
            this.text.add(ChatColor.translateAlternateColorCodes('&', line));
        }
    }

    /**
     * Spawn the entity(s) in to the world
     */
    public void spawn() {
        double offset = 0.0;

        for (String line : text) {
            final double newY = (order.equals(EHologramOrder.DESCENDING)) ? origin.getY() - offset : origin.getY() + offset;
            final ArmorStand as = (ArmorStand) Objects.requireNonNull(origin.getBukkitLocation().getWorld()).spawnEntity(
                    new PLocatable(origin.getWorldName(), origin.getX(), newY, origin.getZ(), origin.getYaw(), origin.getPitch()).getBukkitLocation(),
                    EntityType.ARMOR_STAND
            );

            as.setCustomName(line);
            as.setInvisible(true);
            as.setCustomNameVisible(true);
            as.setGravity(false);

            if (order.equals(EHologramOrder.DESCENDING)) {
                offset -= 0.25;
                continue;
            }

            offset += 0.25;
        }
    }

    /**
     * Despawn all entities associated with this hologram from the world
     */
    public void despawn() {
        final double searchRadius = text.size()*0.3;

        for (Entity nearby : Objects.requireNonNull(origin.getBukkitLocation().getWorld()).getNearbyEntities(origin.getBukkitLocation(), 1.0, searchRadius, 1.0)) {
            if (!(nearby instanceof ArmorStand)) {
                continue;
            }

            nearby.remove();
        }
    }

    /**
     * Add a new line to this hologram
     * @param newLine New text to be added
     */
    public void addLine(String newLine) {
        final double newY = order.equals(EHologramOrder.DESCENDING) ? origin.getY() - (text.size()*0.3) : origin.getY() + (text.size()*0.3);
        final ArmorStand as = (ArmorStand) Objects.requireNonNull(origin.getBukkitLocation().getWorld()).spawnEntity(
                new PLocatable(origin.getWorldName(), origin.getX(), newY, origin.getZ(), origin.getYaw(), origin.getPitch()).getBukkitLocation(),
                EntityType.ARMOR_STAND
        );

        as.setCustomName(ChatColor.translateAlternateColorCodes('&', newLine));
        as.setCustomNameVisible(true);
        as.setGravity(false);
        as.setInvisible(true);

        text.add(ChatColor.translateAlternateColorCodes('&', newLine));
    }

    /**
     * Update an existing line on this hologram
     * @param index Index
     * @param newText New text to be added
     * @return True if update was performed
     */
    public boolean updateLine(int index, String newText) {
        if (index > text.size()) {
            addLine(newText);
            return true;
        }

        final String line = text.get(index);
        final double searchRadius = text.size()*0.3;
        final String formatted = ChatColor.translateAlternateColorCodes('&', newText);

        for (Entity nearby : Objects.requireNonNull(origin.getBukkitLocation().getWorld()).getNearbyEntities(origin.getBukkitLocation(), 1.0, searchRadius, 1.0)) {
            if (!(nearby instanceof final ArmorStand as)) {
                continue;
            }

            if (as.getCustomName() == null || !as.getCustomName().equals(line)) {
                continue;
            }

            as.setCustomName(formatted);
            text.set(index, formatted);
            return true;
        }

        return false;
    }

    /**
     * Remove an existing line from this hologram
     * @param index Index
     * @return True if update was performed
     */
    public boolean removeLine(int index) {
        if (index > text.size()) {
            return false;
        }

        final String line = text.get(index);
        final double searchRadius = text.size()*0.3;

        for (Entity nearby : Objects.requireNonNull(origin.getBukkitLocation().getWorld()).getNearbyEntities(origin.getBukkitLocation(), 1.0, searchRadius, 1.0)) {
            if (!(nearby instanceof final ArmorStand as)) {
                continue;
            }

            if (as.getCustomName() == null || !as.getCustomName().equals(line)) {
                continue;
            }

            as.remove();
            return true;
        }

        return false;
    }
}
