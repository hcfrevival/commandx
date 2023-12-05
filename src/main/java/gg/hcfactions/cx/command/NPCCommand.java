package gg.hcfactions.cx.command;

import gg.hcfactions.cx.CXPermissions;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.npc.IAresNPC;
import gg.hcfactions.cx.npc.impl.GenericNPC;
import gg.hcfactions.libs.acf.BaseCommand;
import gg.hcfactions.libs.acf.annotation.*;
import gg.hcfactions.libs.bukkit.location.impl.PLocatable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

@CommandAlias("npc")
@AllArgsConstructor
public final class NPCCommand extends BaseCommand {
    @Getter public final CXService service;
    /*
        /npc create <profile>
        /npc delete <profile>
        /npc addline <profile> <text>
        /npc remline <profile> <index>
        /npc skin <profile> <username>
        /npc list
     */

    @Subcommand("create")
    @CommandPermission(CXPermissions.CX_ADMIN)
    @Syntax("<profile> <display name>")
    public void onCreate(Player player, String profileUsername, String displayName) {
        if (service.getNpcManager().getNPC(profileUsername).isPresent()) {
            player.sendMessage(ChatColor.RED + "Failed to create NPC: Username conflict");
            return;
        }

        final GenericNPC npc = new GenericNPC(service.getPlugin(), profileUsername, displayName, new PLocatable(player));
        npc.spawn();

        service.getNpcManager().getNpcRepository().add(npc);
        service.getNpcManager().saveNpc(npc);
        player.sendMessage(ChatColor.GREEN + "NPC Created");
    }

    @Subcommand("delete")
    @CommandCompletion("@npcs")
    @Syntax("<profile>")
    @CommandPermission(CXPermissions.CX_ADMIN)
    public void onDelete(CommandSender sender, String profileUsername) {
        final Optional<IAresNPC> npcQuery = service.getNpcManager().getNPC(profileUsername);

        if (npcQuery.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "NPC not found");
            return;
        }

        final IAresNPC npc = npcQuery.get();
        npc.despawn();

        service.getNpcManager().getNpcRepository().remove(npc);
        service.getNpcManager().deleteNpc(npc);
        sender.sendMessage(ChatColor.GREEN + "NPC Deleted");
    }
}
