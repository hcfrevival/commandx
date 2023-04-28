package gg.hcfactions.cx.modules.display;

import com.google.common.base.Joiner;
import gg.hcfactions.cx.modules.ICXModule;
import gg.hcfactions.libs.bukkit.AresPlugin;
import gg.hcfactions.libs.bukkit.scheduler.Scheduler;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public final class TablistModule implements ICXModule, Listener {
    @Getter public final AresPlugin plugin;
    @Getter public final String key;
    @Getter @Setter public boolean enabled;

    private String playerListHeader;
    private String playerListFooter;

    public TablistModule(AresPlugin plugin) {
        this.plugin = plugin;
        this.key = "display.tablist.";
    }

    @Override
    public void onEnable() {
        loadConfig();

        if (!isEnabled()) {
            return;
        }

        plugin.registerListener(this);
    }

    @Override
    public void onDisable() {
        setEnabled(false);
    }

    @Override
    public void onReload() {
        loadConfig();
    }

    private void loadConfig() {
        final YamlConfiguration conf = getConfig();
        this.enabled = conf.getBoolean(getKey() + "enabled");

        final List<String> headerContent = conf.getStringList(getKey() + ".header");
        final List<String> footerContent = conf.getStringList(getKey() + ".footer");
        playerListHeader = ChatColor.translateAlternateColorCodes('&', Joiner.on(ChatColor.RESET + "\n").join(headerContent));
        playerListFooter = ChatColor.translateAlternateColorCodes('&', Joiner.on(ChatColor.RESET + "\n").join(footerContent));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!isEnabled()) {
            return;
        }

        final Player player = event.getPlayer();

        new Scheduler(plugin).sync(() -> {
            player.setPlayerListHeader(playerListHeader);
            player.setPlayerListFooter(playerListFooter);
        }).delay(10L).run();
    }
}
