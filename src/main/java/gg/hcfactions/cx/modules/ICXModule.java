package gg.hcfactions.cx.modules;

import gg.hcfactions.libs.bukkit.AresPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

public interface ICXModule {
    AresPlugin getPlugin();
    String getKey();
    boolean isEnabled();

    void onEnable();
    void onDisable();
    default void onReload() {}

    void setEnabled(boolean b);

    default YamlConfiguration getConfig() {
        return getPlugin().loadConfiguration("commandx");
    }
}
