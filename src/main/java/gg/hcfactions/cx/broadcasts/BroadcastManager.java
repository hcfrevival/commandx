package gg.hcfactions.cx.broadcasts;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.libs.bukkit.scheduler.Scheduler;
import gg.hcfactions.libs.bukkit.services.impl.account.AccountService;
import gg.hcfactions.libs.bukkit.services.impl.account.model.AresAccount;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Objects;
import java.util.Queue;

public final class BroadcastManager {
    @Getter public CXService service;
    @Getter public final List<String> messageRepository;
    @Getter public final Queue<String> queuedMessages;
    @Getter public BukkitTask messageTask;

    private boolean enabled;
    private String broadcastPrefix;
    private int broadcastInterval;

    public BroadcastManager(CXService service) {
        this.service = service;
        this.messageRepository = Lists.newArrayList();
        this.queuedMessages = Queues.newArrayDeque();
    }

    /**
     * Clear queued messages, add all from message repository
     */
    private void resetQueue() {
        queuedMessages.clear();
        queuedMessages.addAll(messageRepository);
    }

    /**
     * Load config from broadcasts.yml
     */
    public void loadBroadcasts() {
        final YamlConfiguration conf = service.getPlugin().loadConfiguration("broadcasts");
        final List<String> unformatted = conf.getStringList("messages");

        if (!messageRepository.isEmpty()) {
            messageRepository.clear();
            queuedMessages.clear();
        }

        enabled = conf.getBoolean("enabled");
        broadcastPrefix = ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(conf.getString("prefix")));
        broadcastInterval = conf.getInt("interval");
        unformatted.forEach(msg -> messageRepository.add(ChatColor.translateAlternateColorCodes('&', msg)));

        queuedMessages.addAll(messageRepository);

        service.getPlugin().getAresLogger().info("loaded {} broadcasts", messageRepository.size());
    }

    /**
     * Start the broadcast task
     */
    public void startBroadcaster() {
        if (!enabled) {
            return;
        }

        messageTask = new Scheduler(service.getPlugin()).sync(() -> {
            final AccountService accountService = (AccountService)service.getPlugin().getService(AccountService.class);

            if (accountService == null) {
                service.getPlugin().getAresLogger().error("failed to run broadcast task: account service is null");
                return;
            }

            if (queuedMessages.isEmpty()) {
                resetQueue();
            }

            final String message = getQueuedMessages().remove();

            Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
                final AresAccount cached = accountService.getCachedAccount(onlinePlayer.getUniqueId());

                if (cached == null || cached.getSettings().isEnabled(AresAccount.Settings.SettingValue.BROADCASTS_ENABLED)) {
                    onlinePlayer.sendMessage(broadcastPrefix + " " + message);
                }
            });
        }).repeat(broadcastInterval*20L, broadcastInterval*20L).run();
    }
}
