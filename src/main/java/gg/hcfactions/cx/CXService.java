package gg.hcfactions.cx;

import com.google.common.collect.Lists;
import gg.hcfactions.cx.broadcasts.BroadcastManager;
import gg.hcfactions.cx.command.*;
import gg.hcfactions.cx.hologram.HologramManager;
import gg.hcfactions.cx.kits.KitManager;
import gg.hcfactions.cx.listener.HologramListener;
import gg.hcfactions.cx.listener.SignListener;
import gg.hcfactions.cx.listener.WarpGatewayListener;
import gg.hcfactions.cx.message.MessageManager;
import gg.hcfactions.cx.modules.chat.ChatModule;
import gg.hcfactions.cx.modules.display.TablistModule;
import gg.hcfactions.cx.modules.player.combat.*;
import gg.hcfactions.cx.modules.player.exploit.ExploitPatchModule;
import gg.hcfactions.cx.modules.player.items.ItemModificationModule;
import gg.hcfactions.cx.modules.player.items.ItemVelocityModule;
import gg.hcfactions.cx.modules.player.vanish.VanishManager;
import gg.hcfactions.cx.modules.reboot.RebootModule;
import gg.hcfactions.cx.modules.world.*;
import gg.hcfactions.cx.rollback.RollbackManager;
import gg.hcfactions.cx.warp.WarpManager;
import gg.hcfactions.libs.bukkit.AresPlugin;
import gg.hcfactions.libs.bukkit.scheduler.Scheduler;
import gg.hcfactions.libs.bukkit.services.IAresService;
import gg.hcfactions.libs.bukkit.utils.Enchants;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

import java.util.List;

@Getter
public final class CXService implements IAresService {
    public final AresPlugin plugin;
    public final NamespacedKey namespacedKey;
    public final String name = "Command X";

    public MessageManager messageManager;
    public VanishManager vanishManager;
    public WarpManager warpManager;
    public KitManager kitManager;
    public BroadcastManager broadcastManager;
    public HologramManager hologramManager;
    public RollbackManager rollbackManager;

    public RebootModule rebootModule;
    public AnimationModule animationModule;
    public KnockbackModule knockbackModule;
    public ItemVelocityModule itemVelocityModule;
    public WorldModule worldModule;
    public ChatModule chatModule;
    public PotionLimitModule potionLimitModule;
    public EnchantLimitModule enchantLimitModule;
    public MobstackModule mobstackModule;
    public ItemModificationModule itemModificationModule;
    public RegenModule regenModule;
    public TablistModule tablistModule;
    public ExploitPatchModule exploitPatchModule;
    public EXPBonusModule expBonusModule;
    public DurabilityModule durabilityModule;
    public ElytraBalanceModule elytraBalanceModule;
    public ShulkerModule shulkerModule;
    public EntityDropModule entityDropModule;
    public PotionPrecisionModule potionPrecisionModule;

    public CXService(AresPlugin plugin) {
        this.plugin = plugin;
        this.namespacedKey = new NamespacedKey(plugin, "cx");
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
        plugin.registerCommand(new HologramCommand(this));
        plugin.registerCommand(new RollbackCommand(this));

        plugin.registerListener(new SignListener(this));
        plugin.registerListener(new WarpGatewayListener(this));
        plugin.registerListener(new HologramListener(this));

        messageManager = new MessageManager(this);
        vanishManager = new VanishManager(this);
        rollbackManager = new RollbackManager(this);

        broadcastManager = new BroadcastManager(this);
        broadcastManager.loadBroadcasts();
        broadcastManager.startBroadcaster();

        warpManager = new WarpManager(this);
        warpManager.loadWarps();
        warpManager.loadGateways();

        kitManager = new KitManager(this);
        kitManager.loadKits();

        hologramManager = new HologramManager(this);
        hologramManager.loadHolograms();
        new Scheduler(plugin).sync(() -> hologramManager.spawnHolograms()).delay(20L).run();

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

        plugin.getCommandManager().getCommandCompletions().registerAsyncCompletion("enchants", ctx -> {
            final List<String> names = Lists.newArrayList();

            for (Enchantment enc : Enchants.getAllEnchantments()) {
                names.add(enc.getKey().getKey().toLowerCase());
            }

            return names;
        });

        plugin.getCommandManager().getCommandCompletions().registerAsyncCompletion("materials", ctx -> {
            final List<String> names = Lists.newArrayList();

            for (Material material : Material.values()) {
                names.add(material.name());
            }

            return names;
        });

        animationModule = new AnimationModule(this);
        knockbackModule = new KnockbackModule(this);
        itemVelocityModule = new ItemVelocityModule(this);
        worldModule = new WorldModule(this);
        chatModule = new ChatModule(this);
        potionLimitModule = new PotionLimitModule(this);
        enchantLimitModule = new EnchantLimitModule(this);
        itemModificationModule = new ItemModificationModule(this);
        mobstackModule = new MobstackModule(this);
        regenModule = new RegenModule(this);
        tablistModule = new TablistModule(this);
        rebootModule = new RebootModule(this);
        exploitPatchModule = new ExploitPatchModule(this);
        expBonusModule = new EXPBonusModule(this);
        durabilityModule = new DurabilityModule(this);
        elytraBalanceModule = new ElytraBalanceModule(this);
        shulkerModule = new ShulkerModule(this);
        entityDropModule = new EntityDropModule(this);
        potionPrecisionModule = new PotionPrecisionModule(this);
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
        exploitPatchModule.onEnable();
        expBonusModule.onEnable();
        durabilityModule.onEnable();
        elytraBalanceModule.onEnable();
        shulkerModule.onEnable();
        entityDropModule.onEnable();
        potionPrecisionModule.onEnable();
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
        exploitPatchModule.onDisable();
        expBonusModule.onDisable();
        durabilityModule.onDisable();
        elytraBalanceModule.onDisable();
        shulkerModule.onDisable();
        entityDropModule.onDisable();
        potionPrecisionModule.onDisable();

        hologramManager.despawnHolograms();
    }

    @Override
    public void onReload() {
        warpManager.loadWarps();
        warpManager.loadGateways();
        kitManager.loadKits();
        broadcastManager.loadBroadcasts();
        hologramManager.reloadHolograms();

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
        exploitPatchModule.onReload();
        expBonusModule.onReload();
        durabilityModule.onReload();
        elytraBalanceModule.onReload();
        shulkerModule.onReload();
        entityDropModule.onReload();
        potionPrecisionModule.onReload();
    }
}
