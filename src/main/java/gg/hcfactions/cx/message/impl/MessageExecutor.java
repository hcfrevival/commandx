package gg.hcfactions.cx.message.impl;

import gg.hcfactions.cx.CXPermissions;
import gg.hcfactions.cx.message.IMessageExecutor;
import gg.hcfactions.cx.message.MessageManager;
import gg.hcfactions.libs.base.consumer.Promise;
import gg.hcfactions.libs.bukkit.services.impl.account.AccountService;
import gg.hcfactions.libs.bukkit.services.impl.account.model.AresAccount;
import gg.hcfactions.libs.bukkit.utils.Players;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public record MessageExecutor(@Getter MessageManager manager) implements IMessageExecutor {
    @Override
    public void sendMessage(Player sender, Player receiver, String message, Promise promise) {
        final AccountService acs = (AccountService)manager.getService().getPlugin().getService(AccountService.class);
        final boolean admin = sender.hasPermission(CXPermissions.CX_MOD);

        if (acs == null) {
            promise.reject("Failed to obtain Account Service");
            return;
        }

        if (receiver == null) {
            promise.reject("Player not found");
            return;
        }

        if (receiver.getUniqueId().equals(sender.getUniqueId())) {
            promise.reject("You can not message yourself");
            return;
        }

        final AresAccount senderAccount = acs.getCachedAccount(sender.getUniqueId());
        final AresAccount receiverAccount = acs.getCachedAccount(receiver.getUniqueId());

        if (senderAccount == null) {
            promise.reject("Failed to obtain your account");
            return;
        }

        if (receiverAccount == null) {
            promise.reject("Player not found");
            return;
        }

        if (!senderAccount.getSettings().isEnabled(AresAccount.Settings.SettingValue.PRIVATE_MESSAGES_ENABLED)) {
            promise.reject("You have disabled private messages");
            return;
        }

        if (senderAccount.getSettings().isIgnoring(receiver)) {
            promise.reject("You are ignoring this player");
            return;
        }

        if (!admin && (receiverAccount.getSettings().isIgnoring(sender.getUniqueId()) || !receiverAccount.getSettings().isEnabled(AresAccount.Settings.SettingValue.PRIVATE_MESSAGES_ENABLED))) {
            promise.reject("This player has private messages disabled");
            return;
        }

        receiver.sendMessage(ChatColor.GRAY + "(From " + sender.getName() + "): " + ChatColor.RESET + message);
        sender.sendMessage(ChatColor.GRAY + "(To " + receiver.getName() + "): " + ChatColor.RESET + message);

        if (receiverAccount.getSettings().isEnabled(AresAccount.Settings.SettingValue.PRIVATE_MESSAGES_PING_ENABLED)) {
            Players.playSound(receiver, Sound.BLOCK_NOTE_BLOCK_PLING);
        }

        manager.setRecentlyMessaged(sender, receiver);
        promise.resolve();
    }

    @Override
    public void sendReply(Player sender, String message, Promise promise) {
        final AccountService service = (AccountService)manager.getService().getPlugin().getService(AccountService.class);
        final Optional<UUID> replyId = manager.getRecentlyMessaged(sender);

        if (service == null) {
            promise.reject("Failed to obtain Account Service");
            return;
        }

        if (replyId.isEmpty()) {
            promise.reject("Nobody has recently messaged you");
            return;
        }

        final Player receiver = Bukkit.getPlayer(replyId.get());
        final boolean admin = sender.hasPermission(CXPermissions.CX_MOD);

        if (receiver == null) {
            promise.reject("Player not found");
            return;
        }

        final AresAccount senderAccount = service.getCachedAccount(sender.getUniqueId());
        final AresAccount receiverAccount = service.getCachedAccount(receiver.getUniqueId());

        if (senderAccount == null) {
            promise.reject("Failed to obtain your account");
            return;
        }

        if (receiverAccount == null) {
            promise.reject("Player not found");
            return;
        }

        if (!senderAccount.getSettings().isEnabled(AresAccount.Settings.SettingValue.PRIVATE_MESSAGES_ENABLED)) {
            promise.reject("You have disabled private messages");
            return;
        }

        if (senderAccount.getSettings().isIgnoring(receiver.getUniqueId())) {
            promise.reject("You are ignoring this player");
            return;
        }

        if (!admin && (receiverAccount.getSettings().isIgnoring(sender.getUniqueId()) || !receiverAccount.getSettings().isEnabled(AresAccount.Settings.SettingValue.PRIVATE_MESSAGES_ENABLED))) {
            promise.reject("This player has private messages disabled");
            return;
        }

        receiver.sendMessage(ChatColor.GRAY + "(From " + sender.getName() + "): " + ChatColor.RESET + message);
        sender.sendMessage(ChatColor.GRAY + "(To " + receiver.getName() + "): " + ChatColor.RESET + message);

        if (receiverAccount.getSettings().isEnabled(AresAccount.Settings.SettingValue.PRIVATE_MESSAGES_PING_ENABLED)) {
            Players.playSound(receiver, Sound.BLOCK_NOTE_BLOCK_PLING);
        }

        manager.setRecentlyMessaged(sender, receiver);
        promise.resolve();
    }
}
