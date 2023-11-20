package gg.hcfactions.cx.npc;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import gg.hcfactions.libs.bukkit.AresPlugin;
import gg.hcfactions.libs.bukkit.location.impl.PLocatable;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R2.CraftServer;
import org.bukkit.craftbukkit.v1_20_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface IAresNPC {
    AresPlugin getPlugin();
    String getProfileUsername();
    String getInteractCommand();
    PLocatable getOrigin();
    String getDisplayName();
    ServerPlayer getEntity();
    Set<UUID> getViewers();

    boolean isSpawned();

    void setEntity(ServerPlayer sp);
    void setSpawned(boolean b);

    default boolean canSee(Player player) {
        return getViewers().contains(player.getUniqueId());
    }

    default boolean canSee(UUID uid) {
        return getViewers().contains(uid);
    }

    default ServerPlayer create() {
        if (isSpawned() || getEntity() != null) {
            return getEntity();
        }

        final MinecraftServer nmsServer = ((CraftServer) Bukkit.getServer()).getServer();
        final ServerLevel nmsWorld = ((CraftWorld)getOrigin().getBukkitLocation().getWorld()).getHandle();
        final GameProfile profile = new GameProfile(UUID.randomUUID(), getProfileUsername());
        final ServerPlayer npc = new ServerPlayer(nmsServer, nmsWorld, profile, ClientInformation.createDefault());
        final String[] skinData = NPCManager.getSkin(getProfileUsername());

        // set skin
        if (skinData != null) {
            profile.getProperties().put("textures", new Property("textures", skinData[0], skinData[1]));
        }

        return npc;
    }

    default void send(Player viewer) {
        if (canSee(viewer)) {
            return;
        }

        if (!isSpawned() || getEntity() == null) {
            setEntity(create());
        }

        final ServerGamePacketListenerImpl conn = ((CraftPlayer)viewer).getHandle().connection;

        // create packets
        final EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions = EnumSet.noneOf(ClientboundPlayerInfoUpdatePacket.Action.class);
        actions.add(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER);

        final ClientboundPlayerInfoUpdatePacket infoAdd = new ClientboundPlayerInfoUpdatePacket(
                actions, List.of(getEntity()));

        final ClientboundAddEntityPacket spawn = new ClientboundAddEntityPacket(getEntity());
        final ClientboundRotateHeadPacket head = new ClientboundRotateHeadPacket(getEntity(), (byte) ((int) (getOrigin().getYaw() * 256.0F / 360.0F)));

        // teleport to origin
        getEntity().absMoveTo(getOrigin().getX(), getOrigin().getY(), getOrigin().getZ(), getOrigin().getYaw(), getOrigin().getPitch());
        final ClientboundTeleportEntityPacket teleport = new ClientboundTeleportEntityPacket(getEntity());

        // send packets to viewer
        conn.send(infoAdd);
        conn.send(spawn);
        conn.send(head);
        conn.send(teleport);

        getViewers().add(viewer.getUniqueId());
    }

    default void spawn() {
        Bukkit.getOnlinePlayers().forEach(this::send);
        setSpawned(true);
    }

    default void spawn(Player viewer) {
        if (canSee(viewer)) {
            return;
        }

        final ServerGamePacketListenerImpl conn = ((CraftPlayer)viewer).getHandle().connection;
        final EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions = EnumSet.noneOf(ClientboundPlayerInfoUpdatePacket.Action.class);
        actions.add(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER);

        final ClientboundPlayerInfoUpdatePacket infoAdd = new ClientboundPlayerInfoUpdatePacket(
                actions, List.of(getEntity()));

        final ClientboundAddEntityPacket spawn = new ClientboundAddEntityPacket(getEntity());
        final ClientboundRotateHeadPacket head = new ClientboundRotateHeadPacket(getEntity(), (byte) ((int) (getOrigin().getYaw() * 256.0F / 360.0F)));

        conn.send(infoAdd);
        conn.send(spawn);
        conn.send(head);
    }

    default void despawn() {
        if (getEntity() == null || !isSpawned()) {
            return;
        }

        Bukkit.getOnlinePlayers().forEach(viewer -> {
            final ServerGamePacketListenerImpl conn = ((CraftPlayer)viewer).getHandle().connection;
            conn.send(new ClientboundRemoveEntitiesPacket(getEntity().getId()));
        });

        setSpawned(false);
        setEntity(null);
    }

    default void despawn(Player viewer) {
        if (!canSee(viewer)) {
            return;
        }

        final ServerGamePacketListenerImpl conn = ((CraftPlayer)viewer).getHandle().connection;
        conn.send(new ClientboundRemoveEntitiesPacket(getEntity().getId()));

        getViewers().remove(viewer.getUniqueId());
    }

    default void lookAt(LivingEntity target) {
        if (!isSpawned()) {
            return;
        }

        final Location entityLoc = getEntity().getBukkitEntity().getLocation();
        final Vector targetVec = target.getLocation().toVector();

        entityLoc.setDirection(targetVec.subtract(entityLoc.toVector())); //set the origin's direction to be the direction vector between point A and B.
        final float yaw = entityLoc.getYaw();

        final ClientboundRotateHeadPacket rotatePacket = new ClientboundRotateHeadPacket(getEntity(), (byte) ((yaw % 360) * 256 / 360));

        Bukkit.getOnlinePlayers().forEach(viewer -> {
            final ServerGamePacketListenerImpl conn = ((CraftPlayer)viewer).getHandle().connection;
            conn.send(rotatePacket);
        });
    }
}
