package com.osrsprofile;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("osrsprofile")
public interface OsrsProfileConfig extends Config
{
	@ConfigItem(
		keyName = "trackSkills",
		name = "Track skills",
		description = "Whether to track skills or not"
	)
	default boolean trackSkills()
	{
		return true;
	}

	@ConfigItem(
			keyName = "trackQuests",
			name = "Track quests",
			description = "Whether to track quests or not"
	)
	default boolean trackQuests()
	{
		return true;
	}

	@ConfigItem(
			keyName = "trackDiaries",
			name = "Track achievement diaries",
			description = "Whether to track achievement diaries or not"
	)
	default boolean trackDiaries()
	{
		return true;
	}

	@ConfigItem(
			keyName = "trackCombat",
			name = "Track combat achievements",
			description = "Whether to track combat achievements or not"
	)
	default boolean trackCombat()
	{
		return true;
	}

	@ConfigItem(
			keyName = "trackMinigames",
			name = "Track minigames",
			description = "Whether to track minigames or not"
	)
	default boolean trackMinigames()
	{
		return true;
	}

	@ConfigItem(
			keyName = "trackBossKills",
			name = "Track boss kills",
			description = "Whether to track boss kills or not"
	)
	default boolean trackBossKills()
	{
		return true;
	}

	@ConfigItem(
			keyName = "trackSlayerMonstersKills",
			name = "Track slayer monsters kills",
			description = "Whether to track slayer monsters kills or not"
	)
	default boolean trackSlayerMonstersKills()
	{
		return true;
	}
}
