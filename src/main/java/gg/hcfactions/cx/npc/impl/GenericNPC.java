package gg.hcfactions.cx.npc.impl;

import com.google.common.collect.Sets;
import gg.hcfactions.cx.hologram.EHologramOrder;
import gg.hcfactions.cx.hologram.impl.Hologram;
import gg.hcfactions.cx.npc.IAresNPC;
import gg.hcfactions.libs.bukkit.AresPlugin;
import gg.hcfactions.libs.bukkit.location.impl.PLocatable;
import gg.hcfactions.libs.bukkit.scheduler.Scheduler;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class GenericNPC implements IAresNPC {
    @Getter public final AresPlugin plugin;
    @Getter public final String profileUsername;
    @Getter public final String displayName;
    @Getter public final String interactCommand;
    @Getter public final PLocatable origin;
    @Getter public final Set<UUID> viewers;
    @Getter @Setter public Hologram hologram;
    @Getter @Setter public ServerPlayer entity;
    @Getter @Setter public boolean spawned;

    public GenericNPC(AresPlugin plugin, String profileUsername, PLocatable origin) {
        this(plugin, profileUsername, profileUsername, null, origin);
    }

    public GenericNPC(AresPlugin plugin, String profileUsername, String displayName, PLocatable origin) {
        this(plugin, profileUsername, displayName, null, origin);
    }

    public GenericNPC(AresPlugin plugin, String profileUsername, String displayName, String interactCommand, PLocatable origin) {
        this.plugin = plugin;
        this.profileUsername = profileUsername;
        this.displayName = displayName;
        this.interactCommand = interactCommand;
        this.origin = origin;
        this.entity = null;
        this.hologram = null;
        this.viewers = Sets.newConcurrentHashSet();
        this.spawned = false;
    }

    @Override
    public void spawn() {
        IAresNPC.super.spawn();

        final PLocatable hologramOrigin = new PLocatable(origin.getWorldName(), origin.getX(), origin.getY() - 0.25, origin.getZ(), origin.getYaw(), origin.getPitch());

        this.hologram = new Hologram(new Random().nextInt(100000), List.of(ChatColor.translateAlternateColorCodes('&', displayName)), hologramOrigin, EHologramOrder.ASCENDING);
        this.hologram.spawn();

        this.spawned = true;
    }

    @Override
    public void despawn() {
        IAresNPC.super.despawn();

        if (this.hologram != null) {
            this.hologram.despawn();
        }

        this.spawned = false;
    }

    @Override
    public void send(Player viewer) {
        IAresNPC.super.send(viewer);

        // Hides nameplate
        Team invisTeam = viewer.getScoreboard().getTeam("npc");

        if (invisTeam == null) {
            invisTeam = viewer.getScoreboard().registerNewTeam("npc");
            invisTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        }

        invisTeam.addEntry(profileUsername);
    }

    @Override
    public void spawn(Player viewer) {
        IAresNPC.super.spawn(viewer);

        // Hides nameplate, waits 1 second before sending
        // to allow for scoreboard creation on init login
        new Scheduler(plugin).sync(() -> {
            Team invisTeam = viewer.getScoreboard().getTeam("npc");

            if (invisTeam == null) {
                invisTeam = viewer.getScoreboard().registerNewTeam("npc");
                invisTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            }

            if (!invisTeam.hasEntry(profileUsername)) {
                invisTeam.addEntry(profileUsername);
            }
        }).delay(10L).run();
    }
}
