package gg.hcfactions.cx;

import com.google.common.collect.Lists;
import gg.hcfactions.cx.command.*;
import gg.hcfactions.cx.kits.KitManager;
import gg.hcfactions.cx.listener.SignListener;
import gg.hcfactions.cx.message.MessageManager;
import gg.hcfactions.cx.modules.chat.ChatModule;
import gg.hcfactions.cx.modules.display.TablistModule;
import gg.hcfactions.cx.modules.player.combat.*;
import gg.hcfactions.cx.modules.player.items.ItemModificationModule;
import gg.hcfactions.cx.modules.player.items.ItemVelocityModule;
import gg.hcfactions.cx.modules.player.vanish.VanishManager;
import gg.hcfactions.cx.modules.reboot.RebootModule;
import gg.hcfactions.cx.modules.world.MobstackModule;
import gg.hcfactions.cx.modules.world.WorldModule;
import gg.hcfactions.cx.warp.WarpManager;
import gg.hcfactions.libs.bukkit.AresPlugin;
import gg.hcfactions.libs.bukkit.services.IAresService;
import lombok.Getter;

import java.util.List;

public final class CXService implements IAresService {
    @Getter public final AresPlugin plugin;
    @Getter public final String name = "Command X";

    @Getter public MessageManager messageManager;
    @Getter public VanishManager vanishManager;
    @Getter public WarpManager warpManager;
    @Getter public KitManager kitManager;

    @Getter public RebootModule rebootModule;
    @Getter public AnimationModule animationModule;
    @Getter public KnockbackModule knockbackModule;
    @Getter public ItemVelocityModule itemVelocityModule;
    @Getter public WorldModule worldModule;
    @Getter public ChatModule chatModule;
    @Getter public PotionLimitModule potionLimitModule;
    @Getter public EnchantLimitModule enchantLimitModule;
    @Getter public MobstackModule mobstackModule;
    @Getter public ItemModificationModule itemModificationModule;
    @Getter public RegenModule regenModule;
    @Getter public TablistModule tablistModule;

    public CXService(AresPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        plugin.registerCommand(new EssentialCommand(this));
        plugin.registerCommand(new ReloadCommand(this));
        plugin.registerCommand(new MessageCommand(this));
        plugin.registerCommand(new RebootCommand(this));
        plugin.registerCommand(new VanishCommand(this));
        plugin.registerCommand(new WarpCommand(this));
        plugin.registerCommand(new KitCommand(this));

        plugin.registerListener(new SignListener(this));

        messageManager = new MessageManager(this);
        vanishManager = new VanishManager(this);

        warpManager = new WarpManager(this);
        warpManager.loadWarps();

        kitManager = new KitManager(this);
        kitManager.loadKits();

        // command completions
        plugin.getCommandManager().getCommandCompletions().registerAsyncCompletion("warps", ctx -> {
            final List<String> names = Lists.newArrayList();
            warpManager.getWarpRepository().forEach(w -> names.add(w.getName()));
            return names;
        });

        plugin.getCommandManager().getCommandCompletions().registerAsyncCompletion("kits", ctx -> {
            final List<String> names = Lists.newArrayList();
            kitManager.getKitRepository().forEach(k -> names.add(k.getName()));
            return names;
        });

        animationModule = new AnimationModule(plugin);
        knockbackModule = new KnockbackModule(plugin);
        itemVelocityModule = new ItemVelocityModule(plugin);
        worldModule = new WorldModule(plugin);
        chatModule = new ChatModule(plugin);
        potionLimitModule = new PotionLimitModule(plugin);
        enchantLimitModule = new EnchantLimitModule(plugin);
        itemModificationModule = new ItemModificationModule(plugin);
        mobstackModule = new MobstackModule(plugin);
        regenModule = new RegenModule(plugin);
        tablistModule = new TablistModule(plugin);
        rebootModule = new RebootModule(plugin);

        animationModule.onEnable();
        knockbackModule.onEnable();
        itemVelocityModule.onEnable();
        worldModule.onEnable();
        chatModule.onEnable();
        potionLimitModule.onEnable();
        enchantLimitModule.onEnable();
        itemModificationModule.onEnable();
        mobstackModule.onEnable();
        regenModule.onEnable();
        tablistModule.onEnable();
        rebootModule.onEnable();
    }

    @Override
    public void onDisable() {
        animationModule.onDisable();
        knockbackModule.onDisable();
        itemVelocityModule.onDisable();
        worldModule.onDisable();
        chatModule.onDisable();
        potionLimitModule.onDisable();
        enchantLimitModule.onDisable();
        itemModificationModule.onDisable();
        mobstackModule.onDisable();
        regenModule.onDisable();
        tablistModule.onDisable();
        rebootModule.onDisable();
    }

    @Override
    public void onReload() {
        warpManager.loadWarps();

        animationModule.onReload();
        knockbackModule.onReload();
        itemVelocityModule.onReload();
        worldModule.onReload();
        chatModule.onReload();
        potionLimitModule.onReload();
        enchantLimitModule.onReload();
        itemModificationModule.onReload();
        mobstackModule.onReload();
        regenModule.onReload();
        tablistModule.onReload();
        rebootModule.onReload();
    }
}
