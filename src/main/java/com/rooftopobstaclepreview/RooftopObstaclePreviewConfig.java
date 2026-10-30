package com.rooftopobstaclepreview;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("rooftopobstaclepreview")
public interface RooftopObstaclePreviewConfig extends Config
{
	@ConfigItem(
		keyName = "squirrel",
		name = "Squirrel",
		description = "A squirrel rides the next obstacle's clickbox where it really is on screen, so your eye can follow"
			+ " it into the red box as you head for the tile it is clicked from."
	)
	default boolean squirrel()
	{
		return true;
	}
}
