package gg.hcfactions.cx.npc;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.npc.impl.GenericNPC;
import gg.hcfactions.libs.bukkit.location.impl.PLocatable;
import gg.hcfactions.libs.bukkit.scheduler.Scheduler;
import gg.hcfactions.libs.bukkit.utils.Configs;
import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public final class NPCManager {
    @Getter public final CXService service;
    @Getter public BukkitTask npcLookTask;
    @Getter public Set<IAresNPC> npcRepository;

    public NPCManager(CXService service) {
        this.service = service;
        this.npcRepository = Sets.newConcurrentHashSet();

        this.npcLookTask = new Scheduler(service.getPlugin()).sync(() -> {
            npcRepository.forEach(npc -> {
                final Collection<Entity> nearbyEntities = Objects.requireNonNull(npc.getOrigin().getBukkitLocation().getWorld())
                        .getNearbyEntities(npc.getOrigin().getBukkitLocation(), 5.0, 5.0, 5.0);

                final List<Entity> nearbyLivingEntities = nearbyEntities
                        .stream()
                        .filter(ent -> ent instanceof LivingEntity && !ent.getName().equalsIgnoreCase(npc.getProfileUsername()))
                        .collect(Collectors.toList());

                if (!nearbyLivingEntities.isEmpty()) {
                    final LivingEntity target = (LivingEntity) nearbyLivingEntities.get(0);
                    npc.lookAt(target);
                }
            });
        }).repeat(0L, 10L).run();
    }

    public Optional<IAresNPC> getNPC(String profileUsername) {
        return npcRepository.stream().filter(npc -> npc.getProfileUsername().equalsIgnoreCase(profileUsername)).findFirst();
    }

    public List<IAresNPC> getVisibleNPC(PLocatable location) {
        return npcRepository.stream().filter(npc ->
                npc.getOrigin().getWorldName().equalsIgnoreCase(location.getWorldName())
                && npc.getOrigin().getDistance(location) > 0.0
                && npc.getOrigin().getDistance(location) < 24.0).collect(Collectors.toList());
    }

    public List<IAresNPC> getInvisibleNPC(PLocatable location) {
        final List<IAresNPC> toRemove = Lists.newArrayList(npcRepository);
        toRemove.removeAll(getVisibleNPC(location));
        return toRemove;
    }

    public void loadNpcs() {
        final YamlConfiguration conf = service.getPlugin().loadConfiguration("npc");

        if (!conf.contains("data") || conf.get("data") == null) {
            return;
        }

        for (String profileUsername : conf.getConfigurationSection("data").getKeys(false)) {
            final String path = "data." + profileUsername + ".";
            final String displayName = conf.getString(path + "display_name");
            final String interactCommand = conf.getString(path + "interact_command");
            final PLocatable origin = Configs.parsePlayerLocation(conf, path + "origin");

            final GenericNPC generic = new GenericNPC(service.getPlugin(), profileUsername, displayName, interactCommand, origin);
            npcRepository.add(generic);
        }
    }

    public void unloadNpcs() {
        npcRepository.forEach(IAresNPC::despawn);
        npcRepository.clear();
    }

    public void spawnNpcs() {
        npcRepository.forEach(IAresNPC::spawn);
    }

    public void saveNpc(IAresNPC npc) {
       final YamlConfiguration conf = service.getPlugin().loadConfiguration("npc");
       final String path = "data." + npc.getProfileUsername() + ".";

       conf.set(path + "display_name", npc.getDisplayName());
       conf.set(path + "interact_command", npc.getInteractCommand());
       Configs.writePlayerLocation(conf, path + "origin", npc.getOrigin());

       service.getPlugin().saveConfiguration("npc", conf);
    }

    public void deleteNpc(IAresNPC npc) {

    }

    public static String[] getSkin(String username) {
        try {
            // uuid query
            final URL idQueryUrl = new URL("https://api.mojang.com/users/profiles/minecraft/" + username);
            InputStreamReader reader = new InputStreamReader(idQueryUrl.openStream());

            final String uuid = new JsonParser().parse(reader).getAsJsonObject().get("id").getAsString();

            // skin query
            final URL skinQueryUrl = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
            reader = new InputStreamReader(skinQueryUrl.openStream());

            final JsonObject property = new JsonParser().parse(reader).getAsJsonObject().get("properties").getAsJsonArray().get(0).getAsJsonObject();
            final String tex = property.get("value").getAsString();
            final String signature = property.get("signature").getAsString();

            return new String[]{tex, signature};
        } catch (IOException e) {
            return null;
        }
    }
}
