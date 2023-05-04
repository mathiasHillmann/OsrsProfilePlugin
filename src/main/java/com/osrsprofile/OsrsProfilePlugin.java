package com.osrsprofile;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.task.Schedule;

import java.time.temporal.ChronoUnit;

@Slf4j
@PluginDescriptor(
		name = "OsrsProfilePlugin"
)
public class OsrsProfilePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private OsrsProfileConfig config;

	private PlayerTracker playerTracker;

	private final int SECONDS_BETWEEN_UPLOADS = 10;
	private final int SECONDS_BETWEEN_MANIFEST_CHECKS = 20*60;

	@Override
	protected void startUp() throws Exception {
		log.info("Player tracker started");
		this.playerTracker = new PlayerTracker();
	}

	@Override
	protected void shutDown() throws Exception {
		log.info("Player tracker stopped");
	}

	@Schedule(
			period = SECONDS_BETWEEN_UPLOADS,
			unit = ChronoUnit.SECONDS
	)
	public void submitToAPI()
	{
		if (client != null && client.getGameState() == GameState.LOGGED_IN) {
			this.playerTracker.submitToApi(client);
		}
	}

	@Schedule(
			period = SECONDS_BETWEEN_MANIFEST_CHECKS,
			unit = ChronoUnit.SECONDS
	)
	public void resyncPlayerModel()
	{
		this.playerTracker.fetchPlayerModel();
	}

	@Provides
	OsrsProfileConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(OsrsProfileConfig.class);
	}
}
