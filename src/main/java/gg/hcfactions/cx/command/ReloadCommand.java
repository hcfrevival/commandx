package gg.hcfactions.cx.command;

import gg.hcfactions.cx.CXPermissions;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.libs.acf.BaseCommand;
import gg.hcfactions.libs.acf.annotation.CommandAlias;
import gg.hcfactions.libs.acf.annotation.CommandPermission;
import gg.hcfactions.libs.acf.annotation.Description;
import gg.hcfactions.libs.acf.annotation.Subcommand;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@AllArgsConstructor
@CommandAlias("cx")
public final class ReloadCommand extends BaseCommand {
    @Getter public final CXService service;

    @Subcommand("reload")
    @Description("Reload CommandX")
    @CommandPermission(CXPermissions.CX_MOD)
    public void onReload(Player player) {
        service.onReload();
        player.sendMessage(ChatColor.YELLOW + "CX Modules have been reloaded");
    }
}
