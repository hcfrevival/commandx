package gg.hcfactions.cx.command;

import gg.hcfactions.cx.CXPermissions;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.libs.acf.BaseCommand;
import gg.hcfactions.libs.acf.annotation.*;
import gg.hcfactions.libs.base.consumer.Promise;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@AllArgsConstructor
@CommandAlias("hologram|holo")
public final class HologramCommand extends BaseCommand {
    @Getter public CXService service;

    @Subcommand("create")
    @CommandPermission(CXPermissions.CX_ADMIN)
    @Description("Create a new hologram")
    @Syntax("<text>")
    public void onCreate(Player player, String text) {
        service.getHologramManager().getExecutor().createHologram(player, text);
    }

    @Subcommand("delete")
    @CommandPermission(CXPermissions.CX_ADMIN)
    @Description("Delete all holograms")
    @Syntax("<radius>")
    public void onDelete(Player player, String radiusName) {
        double d;
        try {
            d = Double.parseDouble(radiusName);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid radius");
            return;
        }

        service.getHologramManager().getExecutor().deleteHologram(player, d, new Promise() {
            @Override
            public void resolve() {
                player.sendMessage(ChatColor.GREEN + "Holograms deleted");
            }

            @Override
            public void reject(String s) {
                player.sendMessage(ChatColor.RED + "Failed to delete holograms: " + s);
            }
        });
    }

    @Subcommand("reload")
    @CommandPermission(CXPermissions.CX_ADMIN)
    @Description("Reload holograms")
    public void onReload(CommandSender sender) {
        service.getHologramManager().reloadHolograms();
        sender.sendMessage(ChatColor.GREEN + "Holograms reloaded");
    }
}
