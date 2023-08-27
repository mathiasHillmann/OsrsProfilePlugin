package com.osrsprofile;

import com.google.inject.Provides;
import javax.inject.Inject;
import javax.inject.Provider;

import com.osrsprofile.tracker.PlayerTracker;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.task.Schedule;

import java.awt.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;

@Slf4j
@PluginDescriptor(
		name = "OSRS Profile"
)
public class OsrsProfilePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private OsrsProfileConfig config;

	@Inject
	private PlayerTracker playerTracker;

	@Inject
	private Provider<MenuManager> menuManager;

	private final int SECONDS_BETWEEN_UPLOADS = 60;

	private static final String MENU_OPTION = "Profile";

	@Override
	protected void startUp() {
		log.info("Player tracker started");
		menuManager.get().addPlayerMenuItem(MENU_OPTION);
	}

	@Override
	protected void shutDown() {
		log.info("Player tracker stopped");
		menuManager.get().removePlayerMenuItem(MENU_OPTION);
	}

	@Provides
	OsrsProfileConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(OsrsProfileConfig.class);
	}

	@Schedule(
			period = SECONDS_BETWEEN_UPLOADS,
			unit = ChronoUnit.SECONDS
	)
	public void submitToAPI()
	{
		if (client != null) {
			this.playerTracker.submitToApi();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		String accountHash = String.valueOf(client.getAccountHash());

		playerTracker.setAccountHash(accountHash, this.config);
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event) {
		if (event.getMenuAction() == MenuAction.RUNELITE_PLAYER && event.getMenuOption().equals(MENU_OPTION)) {
			Player player = event.getMenuEntry().getPlayer();
			if (player == null) {
				return;
			}

			String target = player.getName();

			try {
				if (Desktop.isDesktopSupported()) {
					Desktop desktop = Desktop.getDesktop();
					if (desktop.isSupported(Desktop.Action.BROWSE)) {
						String playerName = URLEncoder.encode(target, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");
						desktop.browse(URI.create("https://osrsprofile.com/player/" + playerName));
					} else {
						throw new Exception("Desktop not supported");
					}
				}
			} catch (Exception e) {
				log.error(e.toString());
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Unable to open the profile of the interacted player.", null);
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if (event.getGroup().equals("osrsprofile")) {
			playerTracker.fetchPlayerData(this.config);
		}
	}
}
