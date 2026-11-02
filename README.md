# Rooftop Obstacle Preview

Shows where every rooftop obstacle's clickbox will be on screen when it is your next click, so your mouse can be there
before you land.

- Each box is drawn as seen from the tile the previous obstacle lands you on, so the boxes stay put as you run and
  only move with the camera.
- Colour is click order: red is next, cooling to blue for the obstacle just done. Red moves on as soon as you click
  it, and back if you call the click off by clicking elsewhere before the obstacle starts. Until it starts, the box
  you are heading for stays on its real clickbox.
- The red box gets a gold border once a click on it would count: not while you are still heading for, or part-way
  through, the obstacle before it, when the game would call that off or ignore the click.
- Gold outlines where the next two or more boxes overlap: click again without moving the mouse.
- A squirrel rides the next obstacle's real clickbox, so you can follow it into the red box. It can be turned off.
- Away from a lap, only the first obstacle shows. The whole course shows on a lap, and for a minute after you next do
  something once it ends or you fall off.
- Optionally, a RuneLite notification the moment you are off an obstacle's animation after it lands or drops you,
  unless you have already clicked on. The idle notifier plugin does not fire for rooftop obstacles. Off by default;
  sound, tray and focus settings are the client's notification settings, or your own for this plugin.

https://github.com/user-attachments/assets/2a9f74c2-72b2-49da-9c31-0856c8a9a588

