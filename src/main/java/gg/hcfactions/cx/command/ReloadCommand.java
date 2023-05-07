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

    @Subcommand("reload all")
    @Description("Reload CommandX")
    @CommandPermission(CXPermissions.CX_MOD)
    public void onReload(Player player) {
        service.onReload();
        player.sendMessage(ChatColor.YELLOW + "CX Modules have been reloaded");
    }

    @Subcommand("reload knockback")
    @Description("Reload knockback values")
    @CommandPermission(CXPermissions.CX_ADMIN)
    public void onReloadKnockback(Player player) {
        service.getKnockbackModule().onReload();
        player.sendMessage(ChatColor.YELLOW + "Reloaded knockback module");
    }

    @Subcommand("reload limits")
    @Description("Reload Enchantment/Potion Limits")
    @CommandPermission(CXPermissions.CX_ADMIN)
    public void onReloadLimits(Player player) {
        service.getEnchantLimitModule().onReload();
        player.sendMessage(ChatColor.YELLOW + "Reloaded enchant/potion limits module");
    }

    @Subcommand("reload itemvelocity")
    @Description("Reload Item Velocity")
    @CommandPermission(CXPermissions.CX_ADMIN)
    public void onReloadItemVelocity(Player player) {
        service.getItemVelocityModule().onReload();
        player.sendMessage(ChatColor.YELLOW + "Reloaded item velocity module");
    }
}
