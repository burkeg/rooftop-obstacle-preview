package com.rooftopobstaclepreview;

import net.runelite.api.coords.WorldPoint;

/** Which obstacle the player last completed, whether they have started the next, and what they clicked since. */
final class CourseProgress
{
	// A minute of game ticks: how long the whole course stays on show once the player does something after a lap.
	private static final int LINGER_TICKS = 100;

	private final RooftopObstacle[] obstacles;
	private int landedOn;
	private int lastClicked = -1;
	private boolean pendingLanding;
	// Seen off any obstacle since landing, so the next animation is not the tail of the one just done.
	private boolean settled;
	private boolean left;
	// Clicked anywhere since the last landing or fall. Until then the player may be AFK, so the minute waits.
	private boolean actedSinceLanding;
	private int ticksOffLap = LINGER_TICKS;

	// A lap starts where the last obstacle lands the player.
	CourseProgress(RooftopObstacle[] obstacles)
	{
		this.obstacles = obstacles;
		landedOn = obstacles.length - 1;
	}

	/** For a player first seen part-way round. */
	void resumeAfter(int index)
	{
		landedOn = index;
	}

	/**
	 * A click that sends the player somewhere: the obstacle's index, or -1 for anywhere else. The game ignores clicks
	 * from the moment an obstacle starts until its xp drops, even once the player is standing on its landing tile.
	 */
	void clicked(int index)
	{
		if (left && !pendingLanding)
		{
			return;
		}
		lastClicked = index;
		actedSinceLanding = true;
	}

	void xpDrop()
	{
		pendingLanding = true;
	}

	/** A failed obstacle drops the player off the course, so the lap starts over. */
	void fell()
	{
		landedOn = obstacles.length - 1;
		lastClicked = -1;
		pendingLanding = false;
		settled = false;
		left = false;
		actedSinceLanding = false;
	}

	/** Call once per game tick with the player's tile and whether they are animating, as on an obstacle. */
	void tick(WorldPoint player, boolean animating)
	{
		if (pendingLanding)
		{
			landedOn = landingIndex(player);
			pendingLanding = false;
			settled = false;
			left = false;
			actedSinceLanding = false;
		}
		// Setting off is starting the next obstacle after clicking it. Walking up to it is not, since another
		// click can still call that off.
		if (!animating)
		{
			settled = true;
		}
		else if (settled && !left && lastClicked == (landedOn + 1) % obstacles.length)
		{
			left = true;
		}
		if (onLap())
		{
			ticksOffLap = 0;
		}
		else if (actedSinceLanding)
		{
			ticksOffLap = Math.min(ticksOffLap + 1, LINGER_TICKS);
		}
	}

	/**
	 * Whether to show every obstacle rather than just the first: on a lap, or within a minute of first doing
	 * something after finishing or falling off one.
	 */
	boolean showsWholeCourse()
	{
		return onLap() || ticksOffLap < LINGER_TICKS;
	}

	// Part-way round, or setting out with a click on the first obstacle.
	private boolean onLap()
	{
		return landedOn != obstacles.length - 1 || left || lastClicked == 0;
	}

	/**
	 * The obstacle the player is heading for but has not started, when they are off the tile it is clicked from, so
	 * its box belongs on its real clickbox; or -1. A click elsewhere can still call it off and make it next again.
	 */
	int liveIndex(WorldPoint player)
	{
		return left || player.equals(obstacles[landedOn].landing) ? -1 : (landedOn + 1) % obstacles.length;
	}

	/**
	 * Whether a click on the next obstacle would be acted on now: the one before it has landed the player and they are
	 * not on their way to another. Mid-obstacle the game ignores the click, and before that it would call off the
	 * obstacle they are heading for.
	 */
	boolean nextIsReady()
	{
		return !left && lastClicked != (landedOn + 1) % obstacles.length;
	}

	/**
	 * The index of the obstacle that many clicks away: 0 is the next to click, the last is the one just done. Clicking
	 * the next obstacle moves on at once, on the bet that the click is not called off.
	 */
	int indexClicksAway(int clicks)
	{
		int unstarted = (landedOn + (left ? 1 : 0) + 1) % obstacles.length;
		return (unstarted + (lastClicked == unstarted ? 1 : 0) + clicks) % obstacles.length;
	}

	private int landingIndex(WorldPoint player)
	{
		int index = RooftopObstacle.landingOn(obstacles, player);
		if (index >= 0)
		{
			return index;
		}
		// Off every landing tile, the obstacle clicked before setting off, or else the one next in lap order.
		return lastClicked < 0 ? (landedOn + 1) % obstacles.length : lastClicked;
	}
}
