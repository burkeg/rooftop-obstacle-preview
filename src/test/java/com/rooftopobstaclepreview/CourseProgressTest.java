package com.rooftopobstaclepreview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

public class CourseProgressTest
{
	// Three obstacles: 0 lands on A, 1 on B and the last on C, where a lap starts.
	private static final WorldPoint A = new WorldPoint(10, 10, 3);
	private static final WorldPoint B = new WorldPoint(30, 20, 3);
	private static final WorldPoint C = new WorldPoint(40, 40, 0);

	private final CourseProgress progress = new CourseProgress(new RooftopObstacle[]{
		new RooftopObstacle(A, 1), new RooftopObstacle(B, 2), new RooftopObstacle(C, 3)});

	@Test
	public void lapStartsAtTheFirstObstacle()
	{
		walk(C);
		assertEquals(0, next());
		assertEquals(-1, progress.liveIndex(C));
	}

	@Test
	public void clickingTheNextObstacleMovesOnAtOnce()
	{
		walk(C);
		progress.clicked(0);
		assertEquals(1, next());
		assertEquals(0, progress.indexClicksAway(2));
	}

	@Test
	public void boxStaysLiveUntilTheObstacleStarts()
	{
		walk(C);
		progress.clicked(0);
		walk(C.dx(1));
		walk(C.dx(2));
		assertEquals(1, next());
		assertEquals(0, progress.liveIndex(C.dx(2)));

		animate(C.dx(3));
		assertEquals(1, next());
		assertEquals(-1, progress.liveIndex(C.dx(3)));
	}

	@Test
	public void callingOffTheClickMakesItNextAgain()
	{
		walk(C);
		progress.clicked(0);
		walk(C.dx(1));
		progress.clicked(-1);
		assertEquals(0, next());
		walk(C.dx(2));
		assertEquals(0, progress.liveIndex(C.dx(2)));

		// Nor is an animation after that the obstacle.
		animate(C.dx(2));
		assertEquals(0, next());
		assertEquals(0, progress.liveIndex(C.dx(2)));

		progress.clicked(0);
		assertEquals(1, next());
		walk(C.dx(3));
		animate(C.dx(4));
		assertEquals(1, next());
		assertEquals(-1, progress.liveIndex(C.dx(4)));
	}

	@Test
	public void clickingAnotherObstacleCallsOffTheClick()
	{
		walk(C);
		progress.clicked(0);
		walk(C.dx(1));
		progress.clicked(2);
		assertEquals(0, next());
		animate(C.dx(2));
		assertEquals(0, next());
		assertEquals(0, progress.liveIndex(C.dx(2)));
	}

	@Test
	public void walkingOffWithoutClickingKeepsTheNextBoxLive()
	{
		walk(C);
		walk(C.dx(1));
		assertEquals(0, next());
		assertEquals(0, progress.liveIndex(C.dx(1)));
	}

	@Test
	public void clicksMidObstacleAreIgnored()
	{
		walk(C);
		progress.clicked(0);
		animate(C.dx(1));
		animate(A);
		progress.clicked(1);
		assertEquals(1, next());

		// Nor does this obstacle's animation still finishing on landing start the next.
		land(A);
		animate(A);
		walk(A);
		assertEquals(1, next());
		assertEquals(-1, progress.liveIndex(A));

		progress.clicked(1);
		assertEquals(2, next());
		walk(A.dy(1));
		assertEquals(1, progress.liveIndex(A.dy(1)));
		animate(A.dy(1));
		assertEquals(2, next());
		assertEquals(-1, progress.liveIndex(A.dy(1)));
	}

	@Test
	public void nextIsReadyOnceTheObstacleBeforeItHasLanded()
	{
		walk(C);
		assertTrue(progress.nextIsReady());

		progress.clicked(0);
		assertFalse(progress.nextIsReady());
		walk(C.dx(1));
		animate(C.dx(2));
		assertFalse(progress.nextIsReady());

		land(A);
		assertTrue(progress.nextIsReady());

		// Calling off the walk to it makes the obstacle it was for next, and ready.
		progress.clicked(1);
		assertFalse(progress.nextIsReady());
		progress.clicked(-1);
		assertTrue(progress.nextIsReady());
	}

	@Test
	public void clickingOnTheXpDropCounts()
	{
		walk(C);
		progress.clicked(0);
		animate(C.dx(1));
		progress.xpDrop();
		progress.clicked(1);
		animate(A);
		assertEquals(2, next());
	}

	@Test
	public void landingTileIdentifiesTheObstacle()
	{
		walk(C);
		land(B);
		assertEquals(2, next());
	}

	@Test
	public void unknownLandingTileFollowsLapOrder()
	{
		walk(C);
		land(A.dx(1));
		assertEquals(1, next());
		assertEquals(1, progress.liveIndex(A.dx(1)));
		assertEquals(-1, progress.liveIndex(A));
	}

	@Test
	public void clickOnAnotherObstacleOverridesLapOrder()
	{
		walk(B);
		progress.clicked(2);
		land(C.dx(1));
		assertEquals(0, next());
	}

	@Test
	public void ignoredClickMidObstacleKeepsLapOrder()
	{
		walk(C);
		progress.clicked(0);
		animate(C.dx(1));
		progress.clicked(2);
		land(A.dx(1));
		assertEquals(1, next());
		assertEquals(0, progress.indexClicksAway(2));
	}

	@Test
	public void reachingTheLandingTileATickLateIsNotStarting()
	{
		walk(C);
		land(A.dy(-1));
		progress.clicked(1);
		animate(A);
		walk(A.dy(1));
		assertEquals(1, progress.liveIndex(A.dy(1)));

		animate(A.dy(1));
		assertEquals(-1, progress.liveIndex(A.dy(1)));
	}

	@Test
	public void lastObstacleWrapsToTheFirst()
	{
		walk(B);
		land(C);
		assertEquals(0, next());

		progress.clicked(0);
		assertEquals(1, next());
		walk(C.dx(1));
		animate(C.dx(1));
		assertEquals(1, next());
	}

	@Test
	public void fallRestartsTheLap()
	{
		walk(C);
		land(A);
		progress.clicked(1);

		WorldPoint fallen = new WorldPoint(5, 5, 0);
		progress.fell();
		animate(fallen);
		assertEquals(0, next());
		assertEquals(0, progress.liveIndex(fallen));

		walk(fallen.dx(1));
		progress.clicked(0);
		assertEquals(1, next());
		walk(fallen.dx(2));
		animate(fallen.dx(3));
		assertEquals(1, next());
		assertEquals(-1, progress.liveIndex(fallen.dx(3)));
	}

	@Test
	public void resumesPartWayRound()
	{
		progress.resumeAfter(0);
		walk(A.dy(4));
		assertEquals(1, next());
		assertEquals(1, progress.liveIndex(A.dy(4)));

		progress.clicked(1);
		assertEquals(2, next());
		walk(A.dy(5));
		animate(A.dy(5));
		assertEquals(2, next());
		assertEquals(-1, progress.liveIndex(A.dy(5)));
	}

	@Test
	public void awayFromALapOnlyTheFirstObstacleShows()
	{
		walk(C.dx(5));
		assertFalse(progress.showsWholeCourse());

		progress.clicked(0);
		assertTrue(progress.showsWholeCourse());
		progress.clicked(-1);
		assertFalse(progress.showsWholeCourse());
	}

	@Test
	public void wholeCourseShowsOnALapAndForAMinuteAfterTheNextClick()
	{
		progress.resumeAfter(1);
		walk(B.dx(1));
		assertTrue(progress.showsWholeCourse());

		// Gone AFK on landing.
		land(C);
		walkFor(500, C);
		assertTrue(progress.showsWholeCourse());

		progress.clicked(-1);
		walkFor(99, C.dx(1));
		assertTrue(progress.showsWholeCourse());
		walk(C.dx(1));
		assertFalse(progress.showsWholeCourse());
	}

	@Test
	public void wholeCourseShowsAfterAFallUntilAMinuteAfterTheNextClick()
	{
		progress.resumeAfter(1);
		walk(B.dx(1));
		progress.clicked(2);
		progress.fell();
		walkFor(500, C.dx(5));
		assertTrue(progress.showsWholeCourse());

		progress.clicked(-1);
		walkFor(100, C.dx(5));
		assertFalse(progress.showsWholeCourse());
	}

	@Test
	public void idleOnceOffTheAnimationOfTheObstacleThatLandedYou()
	{
		walk(C);
		progress.clicked(0);
		animate(C.dx(1));
		assertFalse(progress.becameIdle());
		land(A);
		assertFalse(progress.becameIdle());
		animate(A);
		assertFalse(progress.becameIdle());

		walk(A);
		assertTrue(progress.becameIdle());
		walk(A);
		assertFalse(progress.becameIdle());
	}

	@Test
	public void idleAtOnceWhenTheAnimationEndsOnTheXpDrop()
	{
		walk(C);
		progress.clicked(0);
		animate(C.dx(1));
		progress.xpDrop();
		walk(A);
		assertTrue(progress.becameIdle());
	}

	@Test
	public void clickingOnBeforeTheAnimationEndsIsNotIdle()
	{
		walk(C);
		progress.clicked(0);
		animate(C.dx(1));
		progress.xpDrop();
		progress.clicked(1);
		animate(A);
		walk(A);
		assertFalse(progress.becameIdle());

		// A walk elsewhere is acting too.
		animate(A.dy(1));
		land(B);
		progress.clicked(-1);
		walk(B);
		assertFalse(progress.becameIdle());

		// Though the next landing is idle again.
		progress.clicked(2);
		animate(B.dx(1));
		land(C);
		walk(C);
		assertTrue(progress.becameIdle());
	}

	@Test
	public void clickTheGameIgnoredMidObstacleLeavesYouIdle()
	{
		walk(C);
		progress.clicked(0);
		animate(C.dx(1));
		progress.clicked(1);
		land(A);
		walk(A);
		assertTrue(progress.becameIdle());
	}

	@Test
	public void idleOnceOffTheFall()
	{
		walk(C);
		land(A);
		walk(A);
		progress.clicked(1);
		animate(A.dy(1));

		WorldPoint fallen = new WorldPoint(5, 5, 0);
		progress.fell();
		animate(fallen);
		assertFalse(progress.becameIdle());
		walk(fallen);
		assertTrue(progress.becameIdle());
		walk(fallen);
		assertFalse(progress.becameIdle());
	}

	@Test
	public void standingAboutIsNotIdle()
	{
		walk(C);
		assertFalse(progress.becameIdle());
		walk(C.dx(1));
		assertFalse(progress.becameIdle());

		// Nor is an animation that is not an obstacle ending.
		land(A);
		walk(A);
		assertTrue(progress.becameIdle());
		animate(A);
		walk(A);
		assertFalse(progress.becameIdle());
	}

	@Test
	public void resumingPartWayRoundIsNotIdle()
	{
		progress.resumeAfter(0);
		animate(A);
		walk(A);
		assertFalse(progress.becameIdle());
	}

	private int next()
	{
		return progress.indexClicksAway(0);
	}

	private void walk(WorldPoint tile)
	{
		progress.tick(tile, false);
	}

	private void walkFor(int ticks, WorldPoint tile)
	{
		for (int tick = 0; tick < ticks; tick++)
		{
			walk(tile);
		}
	}

	private void animate(WorldPoint tile)
	{
		progress.tick(tile, true);
	}

	// The xp drops as the obstacle's animation finishes.
	private void land(WorldPoint tile)
	{
		progress.xpDrop();
		animate(tile);
	}
}
