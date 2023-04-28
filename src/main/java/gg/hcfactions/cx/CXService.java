package gg.hcfactions.cx;

import gg.hcfactions.cx.command.EssentialCommand;
import gg.hcfactions.cx.command.ReloadCommand;
import gg.hcfactions.cx.modules.chat.ChatModule;
import gg.hcfactions.cx.modules.display.TablistModule;
import gg.hcfactions.cx.modules.player.combat.*;
import gg.hcfactions.cx.modules.player.items.ItemModificationModule;
import gg.hcfactions.cx.modules.player.items.ItemVelocityModule;
import gg.hcfactions.cx.modules.world.MobstackModule;
import gg.hcfactions.cx.modules.world.WorldModule;
import gg.hcfactions.libs.bukkit.AresPlugin;
import gg.hcfactions.libs.bukkit.services.IAresService;
import lombok.Getter;

public final class CXService implements IAresService {
    @Getter public final AresPlugin plugin;
    @Getter public final String name = "Command X";

    private AnimationModule animationModule;
    private KnockbackModule knockbackModule;
    private ItemVelocityModule itemVelocityModule;
    private WorldModule worldModule;
    private ChatModule chatModule;
    private PotionLimitModule potionLimitModule;
    private EnchantLimitModule enchantLimitModule;
    private MobstackModule mobstackModule;
    private ItemModificationModule itemModificationModule;
    private RegenModule regenModule;
    private TablistModule tablistModule;

    public CXService(AresPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        plugin.registerCommand(new EssentialCommand());
        plugin.registerCommand(new ReloadCommand(this));

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
    }

    @Override
    public void onReload() {
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
    }
}
