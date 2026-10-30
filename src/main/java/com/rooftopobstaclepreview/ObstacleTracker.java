package com.rooftopobstaclepreview;

import com.google.common.collect.Sets;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.client.eventbus.Subscribe;

/** Tracks the course the player is on, its obstacle objects and the player's progress round it. */
@RequiredArgsConstructor
class ObstacleTracker
{
	// Clicks that send the player somewhere, so call off heading for an obstacle; only the object options are on one.
	private static final Set<MenuAction> SENDS_PLAYER = Sets.union(
		EnumSet.range(MenuAction.ITEM_USE_ON_GAME_OBJECT, MenuAction.WALK),
		EnumSet.range(MenuAction.PLAYER_FIRST_OPTION, MenuAction.PLAYER_EIGHTH_OPTION));
	private static final Set<MenuAction> OBJECT_OPTIONS =
		EnumSet.range(MenuAction.GAME_OBJECT_FIRST_OPTION, MenuAction.GAME_OBJECT_FIFTH_OPTION);

	private final Client client;
	@Getter
	private final Set<TileObject> objects = new HashSet<>();
	@Getter
	private Course course;
	// Game ticks the player has stayed on the same tile.
	@Getter
	private int ticksStill;
	private CourseProgress progress;
	private WorldPoint lastTile;
	private int agilityXp = -1;

	int indexClicksAway(int clicks)
	{
		return progress.indexClicksAway(clicks);
	}

	int liveIndex()
	{
		return progress.liveIndex(lastTile);
	}

	boolean nextIsReady()
	{
		return progress.nextIsReady();
	}

	boolean showsWholeCourse()
	{
		return progress.showsWholeCourse();
	}

	/** Picks up obstacles already in the scene when the plugin is switched on; call on the client thread. */
	void scan()
	{
		WorldView worldView = client.getTopLevelWorldView();
		if (client.getGameState() != GameState.LOGGED_IN || worldView == null)
		{
			return;
		}
		for (Tile[][] plane : worldView.getScene().getTiles())
		{
			for (Tile[] column : plane)
			{
				for (Tile tile : column)
				{
					if (tile == null)
					{
						continue;
					}
					for (GameObject gameObject : tile.getGameObjects())
					{
						track(gameObject);
					}
					track(tile.getWallObject());
					track(tile.getDecorativeObject());
					track(tile.getGroundObject());
				}
			}
		}
	}

	private void track(TileObject object)
	{
		if (object != null && Course.isObstacle(object.getId()))
		{
			objects.add(object);
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		track(event.getGameObject());
	}

	@Subscribe
	public void onWallObjectSpawned(WallObjectSpawned event)
	{
		track(event.getWallObject());
	}

	@Subscribe
	public void onDecorativeObjectSpawned(DecorativeObjectSpawned event)
	{
		track(event.getDecorativeObject());
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		track(event.getGroundObject());
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		objects.remove(event.getGameObject());
	}

	@Subscribe
	public void onWallObjectDespawned(WallObjectDespawned event)
	{
		objects.remove(event.getWallObject());
	}

	@Subscribe
	public void onDecorativeObjectDespawned(DecorativeObjectDespawned event)
	{
		objects.remove(event.getDecorativeObject());
	}

	@Subscribe
	public void onGroundObjectDespawned(GroundObjectDespawned event)
	{
		objects.remove(event.getGroundObject());
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		// A map load discards every object; they spawn again afterwards.
		if (event.getGameState() == GameState.LOADING)
		{
			objects.clear();
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		MenuAction action = event.getMenuAction();
		if (progress != null && SENDS_PLAYER.contains(action))
		{
			progress.clicked(OBJECT_OPTIONS.contains(action) ? course.indexOf(event.getId()) : -1);
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (event.getSkill() != Skill.AGILITY)
		{
			return;
		}
		int before = agilityXp;
		agilityXp = event.getXp();
		if (progress != null && before >= 0 && agilityXp > before)
		{
			progress.xpDrop();
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (progress != null && event.getActor() == client.getLocalPlayer())
		{
			progress.fell();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}
		WorldPoint at = player.getWorldLocation();
		ticksStill = at.equals(lastTile) ? ticksStill + 1 : 0;
		lastTile = at;

		Course near = Course.near(at);
		if (near != course)
		{
			course = near;
			progress = near == null ? null : start(near, at);
		}
		if (progress != null)
		{
			progress.tick(at, player.getAnimation() != -1);
		}
	}

	// A player first seen on a roof, as after logging in, is part-way round. On the ground the lap has yet to start,
	// which is how progress begins anyway.
	private CourseProgress start(Course on, WorldPoint at)
	{
		CourseProgress started = new CourseProgress(on.obstacles);
		WorldView worldView = client.getTopLevelWorldView();
		int completed = at.getPlane() == 0 ? -1 : on.lastCompletedAt(at, (from, to) ->
			WorldPoint.isInScene(worldView, to.getX(), to.getY())
				&& from.toWorldArea().canTravelInDirection(worldView, to.getX() - from.getX(), to.getY() - from.getY()));
		if (completed >= 0)
		{
			started.resumeAfter(completed);
		}
		return started;
	}
}
