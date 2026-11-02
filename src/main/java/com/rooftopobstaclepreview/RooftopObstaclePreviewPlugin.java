package com.rooftopobstaclepreview;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Rooftop Obstacle Preview",
	description = "Shows where each rooftop obstacle's clickbox will be when it is your next click, coloured by click order",
	tags = {"agility", "rooftop", "clickbox", "overlay", "course"}
)
public class RooftopObstaclePreviewPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private EventBus eventBus;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private RooftopObstaclePreviewConfig config;

	@Inject
	private ItemManager itemManager;

	@Inject
	private Notifier notifier;

	private ObstacleTracker tracker;
	private GhostClickboxOverlay overlay;

	@Override
	protected void startUp()
	{
		tracker = new ObstacleTracker(client, notifier, config);
		overlay = new GhostClickboxOverlay(client, tracker, config, itemManager);
		eventBus.register(tracker);
		clientThread.invoke(tracker::scan);
		overlayManager.add(overlay);
	}

	@Provides
	RooftopObstaclePreviewConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RooftopObstaclePreviewConfig.class);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		eventBus.unregister(tracker);
	}
}
