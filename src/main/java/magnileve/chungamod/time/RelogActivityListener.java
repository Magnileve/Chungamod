package magnileve.chungamod.time;

import org.apache.logging.log4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;

public class RelogActivityListener implements TickListener {

private Minecraft mc;
private Logger log;
private RestartableActivity activity;
private BlockPos nearbyPos;
private int nextTick;

/**
 * Waits until the player has logged in at a similar position to the activity's location, then restarts the activity.
 * @param activity the activity to be restarted
 * @param nearbyPos a BlockPos having to do with the activity
 * @param mc instance of Mincerfat
 * @param log instance of logger
 */
public RelogActivityListener(RestartableActivity activity, BlockPos nearbyPos, Minecraft mc, Logger log) {
	this.mc = mc;
	this.log = log;
	this.activity = activity;
	this.nearbyPos = nearbyPos;
	TickTimer.addListener(this);
	nextTick = nextTick(200);
}

@Override
public boolean onTick(int tick) {
if(tick == nextTick) {
	BlockPos playerPos = null;
	try {
		playerPos = mc.player.getPosition();
	} catch(NullPointerException e) {}
	if(playerPos != null && playerPos.getDistance(nearbyPos.getX(), nearbyPos.getY(), nearbyPos.getZ()) < 250) {
		log.info("Restarting activity " + activity.getClass().getName());
		activity.restart();
		return false;
	} else nextTick = nextTick(200);
}
return true;
}

}