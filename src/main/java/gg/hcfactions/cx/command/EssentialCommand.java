package gg.hcfactions.cx.command;

import gg.hcfactions.cx.CXPermissions;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.menu.InvseeMenu;
import gg.hcfactions.libs.acf.BaseCommand;
import gg.hcfactions.libs.acf.annotation.*;
import gg.hcfactions.libs.bukkit.utils.Players;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

@AllArgsConstructor
public final class EssentialCommand extends BaseCommand {
    @Getter public final CXService service;

    @CommandAlias("world")
    @CommandPermission(CXPermissions.CX_MOD)
    @Description("Change your world")
    @Syntax("<world>")
    @CommandCompletion("@worlds")
    public void onChangeWorld(Player player, String worldName) {
        final World world = Bukkit.getWorld(worldName);

        if (world == null) {
            player.sendMessage(ChatColor.RED + "World not found");
            return;
        }

        player.teleport(world.getSpawnLocation());
        player.sendMessage(ChatColor.YELLOW + "World has been changed to " + ChatColor.BLUE + world.getName());
    }

    @CommandAlias("broadcast")
    @CommandPermission(CXPermissions.CX_MOD)
    @Syntax("[-p] <message>")
    @Description("Broadcast a message")
    public void onBroadcast(CommandSender sender, String message) {
        final String[] split = message.split(" ");
        final boolean asPlayer = (split.length > 1 && split[0].equalsIgnoreCase("-p"));

        if (asPlayer) {
            final String trimmed = message.substring(3);
            Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "[" + ChatColor.DARK_RED + sender.getName() + ChatColor.LIGHT_PURPLE + "] " + trimmed);
            return;
        }

        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "[" + ChatColor.DARK_RED + "Admin" + ChatColor.LIGHT_PURPLE + "] " + message);
    }

    @CommandAlias("rename")
    @Description("Rename the item in your hand")
    @Syntax("<name>")
    @CommandPermission(CXPermissions.CX_MOD)
    public void onRenameItem(Player player, String name) {
        final ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand.getType().equals(Material.AIR)) {
            player.sendMessage(ChatColor.RED + "You are not holding an item");
            return;
        }

        final ItemMeta meta = hand.getItemMeta();
        if (meta == null) {
            player.sendMessage(ChatColor.RED + "Item does not have any meta data");
            return;
        }

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        hand.setItemMeta(meta);

        player.sendMessage(ChatColor.YELLOW + "Item has been renamed to " + ChatColor.translateAlternateColorCodes('&', name));
    }

    @CommandAlias("repair")
    @Description("Repair your items/armor")
    @Syntax("[-a]")
    @CommandPermission(CXPermissions.CX_MOD)
    public void onRepairItem(Player player, @Optional @Values("-a") String all) {
        if (all != null && all.equalsIgnoreCase("-a")) {
            for (ItemStack armor : player.getInventory().getArmorContents()) {
                if (armor == null || armor.getType().equals(Material.AIR)) {
                    continue;
                }

                armor.setDurability((short)0);
            }

            player.sendMessage(ChatColor.YELLOW + "Your armor has been repaired");
            return;
        }

        final ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().equals(Material.AIR)) {
            player.sendMessage(ChatColor.RED + "You are not holding an item");
            return;
        }

        hand.setDurability((short)0);
        player.sendMessage(ChatColor.YELLOW + "Your item has been repaired");
    }

    @CommandAlias("gamemode|gm")
    @Description("Change your gamemode")
    @Syntax("<survival|creative|adventure|spectator>")
    @CommandCompletion("@players")
    @CommandPermission(CXPermissions.CX_MOD)
    public void onGamemodeChange(
            Player player, @Values("creative|survival|adventure|spectator|c|s|a|spec") String gamemodeName,
            @Optional String username
    ) {
        GameMode gamemode = null;

        if (gamemodeName.equalsIgnoreCase("s") || gamemodeName.equalsIgnoreCase("survival")) {
            gamemode = GameMode.SURVIVAL;
        } else if (gamemodeName.equalsIgnoreCase("c") || gamemodeName.equalsIgnoreCase("creative")) {
            gamemode = GameMode.CREATIVE;
        } else if (gamemodeName.equalsIgnoreCase("a") || gamemodeName.equalsIgnoreCase("adventure")) {
            gamemode = GameMode.ADVENTURE;
        } else if (gamemodeName.equalsIgnoreCase("spec") || gamemodeName.equalsIgnoreCase("spectator")) {
            gamemode = GameMode.SPECTATOR;
        }

        if (gamemode == null) {
            player.sendMessage(ChatColor.RED + "Invalid gamemode");
            return;
        }

        if (username != null) {
            final Player otherPlayer = Bukkit.getPlayer(username);

            if (otherPlayer == null) {
                player.sendMessage(ChatColor.RED + "Player not found");
                return;
            }

            if (otherPlayer.getGameMode().equals(gamemode)) {
                player.sendMessage(ChatColor.RED + "Player gamemode is already set");
                return;
            }

            otherPlayer.setGameMode(gamemode);
            otherPlayer.sendMessage(ChatColor.YELLOW + "Your gamemode has been changed to " + ChatColor.BLUE + StringUtils.capitalize(gamemode.name().toLowerCase()));
            player.sendMessage(ChatColor.GOLD + otherPlayer.getName() + ChatColor.YELLOW + "'s gamemode has been changed to " + ChatColor.BLUE + StringUtils.capitalize(gamemode.name().toLowerCase()));
            return;
        }

        if (player.getGameMode().equals(gamemode)) {
            player.sendMessage(ChatColor.RED + "Your gamemode is already set");
            return;
        }

        player.setGameMode(gamemode);
        player.sendMessage(ChatColor.YELLOW + "Your gamemode has been changed to " + ChatColor.BLUE + StringUtils.capitalize(gamemode.name().toLowerCase()));
    }

    @CommandAlias("clear")
    @Description("Clear an inventory")
    @Syntax("[username]")
    @CommandCompletion("@players")
    @CommandPermission(CXPermissions.CX_ADMIN)
    public void onClearInventory(Player player, @Optional String username) {
        if (username != null) {
            final Player otherPlayer = Bukkit.getPlayer(username);
            if (otherPlayer == null) {
                player.sendMessage(ChatColor.RED + "Player not found");
                return;
            }

            otherPlayer.getInventory().clear();
            otherPlayer.getInventory().setArmorContents(null);
            otherPlayer.sendMessage(ChatColor.YELLOW + "Your inventory was cleared by " +  ChatColor.BLUE + player.getName());
            player.sendMessage(ChatColor.YELLOW + "You have cleared " + ChatColor.BLUE + otherPlayer.getName() + ChatColor.YELLOW + "'s inventory");
            return;
        }

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.sendMessage(ChatColor.YELLOW + "Your inventory has been cleared");
    }

    @CommandAlias("heal")
    @Description("Heal a player")
    @Syntax("[username]")
    @CommandCompletion("@players")
    @CommandPermission(CXPermissions.CX_ADMIN)
    public void onHeal(Player player, @Optional String username) {
        if (username != null) {
            final Player otherPlayer = Bukkit.getPlayer(username);
            if (otherPlayer == null) {
                player.sendMessage(ChatColor.RED + "Player not found");
                return;
            }

            Players.resetHealth(otherPlayer);
            otherPlayer.sendMessage(ChatColor.YELLOW + "You have been healed by " + ChatColor.BLUE + player.getName());
            player.sendMessage(ChatColor.YELLOW + "You have healed " + ChatColor.BLUE + otherPlayer.getName());
            return;
        }

        Players.resetHealth(player);
        player.sendMessage(ChatColor.YELLOW + "You have been healed");
    }

    @CommandAlias("invsee")
    @Description("Spectate a players inventory")
    @CommandCompletion("@players")
    @Syntax("<username>")
    @CommandPermission(CXPermissions.CX_ADMIN)
    public void onInvsee(Player player, String username) {
        final Player otherPlayer = Bukkit.getPlayer(username);
        if (otherPlayer == null) {
            player.sendMessage(ChatColor.RED + "Player not found");
            return;
        }

        final InvseeMenu menu = new InvseeMenu(service, player, otherPlayer);
        menu.open();
    }

    /*
        /tp johnsama - teleport to player
        /tp johnsama Symotic - teleport player to player
        /tp 100 100 100 - teleport to specific coordinates in current world
        /tp 100 100 100 world - teleport to specific coordinates in specific world
        /tpall - teleport all players on the server to current location
     */

    @CommandAlias("teleport|tp")
    @Description("Teleport to a player")
    @Syntax("<name>")
    @CommandCompletion("@players")
    @CommandPermission(CXPermissions.CX_MOD)
    public void onTeleport(Player player, String username) {
        final Player otherPlayer = Bukkit.getPlayer(username);

        if (otherPlayer == null) {
            player.sendMessage(ChatColor.RED + "Player not found");
            return;
        }

        player.teleport(otherPlayer);
        player.sendMessage(ChatColor.YELLOW + "Teleported to " + ChatColor.BLUE + otherPlayer.getName());
    }

    @CommandAlias("teleport|tp")
    @Description("Teleport a player to another player")
    @Syntax("<name> <name>")
    @CommandCompletion("@players")
    @CommandPermission(CXPermissions.CX_MOD)
    public void onTeleport(Player player, String username, String otherUsername) {
        final Player fromPlayer = Bukkit.getPlayer(username);
        final Player toPlayer = Bukkit.getPlayer(otherUsername);

        if (fromPlayer == null) {
            player.sendMessage(ChatColor.RED + username + " not found");
            return;
        }

        if (toPlayer == null) {
            player.sendMessage(ChatColor.RED + otherUsername + " not found");
            return;
        }

        fromPlayer.teleport(toPlayer);
        fromPlayer.sendMessage(ChatColor.YELLOW + "You have been teleported to " + ChatColor.BLUE + toPlayer.getName());
    }

    @CommandAlias("teleport|tp")
    @Description("Teleport to specific coordinates in your current world")
    @Syntax("<x> <y> <z>")
    @CommandPermission(CXPermissions.CX_MOD)
    public void onTeleport(Player player, String namedX, String namedY, String namedZ) {
        double x, y, z;
        try {
            x = Double.parseDouble(namedX);
            y = Double.parseDouble(namedY);
            z = Double.parseDouble(namedZ);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid coordinates");
            return;
        }

        final Location location = new Location(player.getWorld(), x, y, z);
        player.teleport(location);
        player.sendMessage(ChatColor.YELLOW + "Teleported to " + ChatColor.BLUE + x + ", " + y + ", " + z + ChatColor.YELLOW + " in " + ChatColor.BLUE + player.getWorld().getName());
    }

    @CommandAlias("teleport|tp")
    @Description("Teleport to specific coordinates in a specific world")
    @Syntax("<x> <y> <z> [world]")
    @CommandCompletion("@worlds")
    @CommandPermission(CXPermissions.CX_MOD)
    public void onTeleport(Player player, String namedX, String namedY, String namedZ, String namedWorld) {
        final World world = Bukkit.getWorld(namedWorld);

        if (world == null) {
            player.sendMessage(ChatColor.RED + "World not found");
            return;
        }

        double x, y, z;
        try {
            x = Double.parseDouble(namedX);
            y = Double.parseDouble(namedY);
            z = Double.parseDouble(namedZ);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid coordinates");
            return;
        }

        final Location location = new Location(world, x, y, z);
        player.teleport(location);
        player.sendMessage(ChatColor.YELLOW + "Teleported to " + ChatColor.BLUE + x + ", " + y + ", " + z + ChatColor.YELLOW + " in " + ChatColor.BLUE + world.getName());
    }

    @CommandAlias("tpall")
    @Description("Teleport all players in the server to your current location")
    @CommandPermission(CXPermissions.CX_ADMIN)
    public void onTeleportAll(Player player) {
        Bukkit.getOnlinePlayers().forEach(p -> {
            if (!p.getUniqueId().equals(player.getUniqueId())) {
                p.teleport(player);
                p.sendMessage(ChatColor.YELLOW + "You have been teleported to " + ChatColor.BLUE + player.getName());
            }
        });

        player.sendMessage(ChatColor.YELLOW + "Teleported " + ChatColor.BLUE + Bukkit.getOnlinePlayers().size() + " players" + ChatColor.YELLOW + " to your current location");
    }
}
