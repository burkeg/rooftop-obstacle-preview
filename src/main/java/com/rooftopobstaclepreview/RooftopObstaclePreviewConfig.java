package com.rooftopobstaclepreview;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Notification;

@ConfigGroup("rooftopobstaclepreview")
public interface RooftopObstaclePreviewConfig extends Config
{
	@ConfigItem(
		keyName = "squirrel",
		name = "Squirrel",
		description = "A squirrel rides the next obstacle's clickbox where it really is on screen, so your eye can follow"
			+ " it into the red box as you head for the tile it is clicked from.",
		position = 1
	)
	default boolean squirrel()
	{
		return true;
	}

	@ConfigItem(
		keyName = "idleNotification",
		name = "Idle notification",
		description = "Notify the moment you are off an obstacle's animation after it lands or drops you, unless you have"
			+ " already clicked on. The idle notifier plugin does not fire for rooftop obstacles.",
		position = 2
	)
	default Notification idleNotification()
	{
		return Notification.OFF;
	}
}
