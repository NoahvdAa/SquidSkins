package me.noahvdaa.squidskins;

import org.bukkit.plugin.java.JavaPlugin;

public final class SquidSkins extends JavaPlugin {
	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(new PlayerEvents(), this);
	}
}
