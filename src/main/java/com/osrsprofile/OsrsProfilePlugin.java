package com.osrsprofile;

import javax.inject.Inject;
import javax.inject.Provider;

import com.google.inject.Provides;
import com.osrsprofile.exporter.PlayerExporter;
import com.osrsprofile.tracker.PlayerTracker;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.menus.WidgetMenuOption;
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
	private PlayerExporter playerExporter;

	@Inject
	private Provider<MenuManager> menuManager;

	@Inject
	private ChatCommandManager chatCommandManager;

	private final int SECONDS_BETWEEN_UPLOADS = 60;

	private final int SECONDS_BETWEEN_FETCHES = 300;

	private static final String MENU_OPTION = "Profile";

	private static final String MENU_TARGET = "Player";

	private static final String EXPORT_MODEL = "Export Model to OSRS Profile";

	private static final String EXPORT_MODEL_COMMAND = "!exportmodel";

	private static final WidgetMenuOption FIXED_EQUIPMENT_TAB_EXPORT = new WidgetMenuOption(EXPORT_MODEL,
			MENU_TARGET, ComponentID.FIXED_VIEWPORT_EQUIPMENT_TAB);
	private static final WidgetMenuOption RESIZABLE_EQUIPMENT_TAB_EXPORT = new WidgetMenuOption(EXPORT_MODEL,
			MENU_TARGET, ComponentID.RESIZABLE_VIEWPORT_EQUIPMENT_TAB);
	private static final WidgetMenuOption RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_TAB_EXPORT = new WidgetMenuOption(EXPORT_MODEL,
			MENU_TARGET, ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_TAB);

	@Override
	protected void startUp() {
		log.info("Player tracker started");
		menuManager.get().addPlayerMenuItem(MENU_OPTION);

		menuManager.get().addManagedCustomMenu(FIXED_EQUIPMENT_TAB_EXPORT,this::exportLocalPlayerModel);
		menuManager.get().addManagedCustomMenu(RESIZABLE_EQUIPMENT_TAB_EXPORT,this::exportLocalPlayerModel);
		menuManager.get().addManagedCustomMenu(RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_TAB_EXPORT,this::exportLocalPlayerModel);

		chatCommandManager.registerCommandAsync(EXPORT_MODEL_COMMAND, this::exportLocalPlayerModel);

		this.playerTracker.fetchVars(this.config);
	}

	@Override
	protected void shutDown() {
		log.info("Player tracker stopped");
		menuManager.get().removePlayerMenuItem(MENU_OPTION);

		menuManager.get().removeManagedCustomMenu(FIXED_EQUIPMENT_TAB_EXPORT);
		menuManager.get().removeManagedCustomMenu(RESIZABLE_EQUIPMENT_TAB_EXPORT);
		menuManager.get().removeManagedCustomMenu(RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_TAB_EXPORT);

		chatCommandManager.unregisterCommand(EXPORT_MODEL_COMMAND);
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

	@Schedule(
			period = SECONDS_BETWEEN_FETCHES,
			unit = ChronoUnit.SECONDS
	)
	public void getVarsFromApi()
	{
		if (client != null) {
			this.playerTracker.fetchVars(this.config);
		}
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
			playerTracker.fetchVars(this.config);
		}
	}

	// From menu
	public void exportLocalPlayerModel(MenuEntry entry)
	{
		this.playerExporter.export();
	}

	// From chat command
	private void exportLocalPlayerModel(ChatMessage chatMessage, String s) {
		this.playerExporter.export();
	}
}
