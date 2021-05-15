package magnileve.chungamod.auto;

import java.util.Queue;

import magnileve.chungamod.Chung;
import magnileve.chungamod.time.Activity;
import magnileve.chungamod.time.TickListener;
import magnileve.chungamod.time.TickTimer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;

public class BaritoneTravel implements TickListener, Activity {

private Minecraft mc;
private Queue<BlockPos> destinations;
private BlockPos destination;
private BlockPos playerPos;
private int nextTick;
private boolean restartingBaritone;

/**
 * Runs Baritone to travel to a queue of destinations and restarts Baritone if it stops.
 * @param mc instance of Mincerfat
 * @param destinations queue of destinations
 */
public BaritoneTravel(Minecraft mc, Queue<BlockPos> destinations) {
	this.mc = mc;
	destination = destinations.remove();
	this.destinations = destinations;
	playerPos = mc.player.getPosition();
	TickTimer.addListener(this);
	nextTick = nextTick(1);
	restartingBaritone = false;
}

@Override
public boolean onTick(int tick) {
if(tick == nextTick) {
	if(playerPos.equals(mc.player.getPosition())) {
		if(restartingBaritone) {
			restartingBaritone = false;
			mc.player.sendChatMessage("#goto ~" + (destination.getX() - (int) Math.floor(mc.player.posX)) + " ~" + (destination.getY() - (int) Math.floor(mc.player.posY)) + " ~" + (destination.getZ() - (int) Math.floor(mc.player.posZ)));
			nextTick = nextTick(400);
		} else {
			if(playerPos.getDistance(destination.getX(), destination.getY(), destination.getZ()) < 10) {
				if(destinations.isEmpty()) {
					Chung.sendMessage("Final destination reached");
					Chung.runningActivities.remove(this);
					if(!playerPos.equals(destination)) stop();
					return true;
				} else destination = destinations.remove();
			}
			restartingBaritone = true;
			mc.player.sendChatMessage("#cancel");
			nextTick = nextTick(40);
		}
	} else {
		nextTick = nextTick(400);
	}
	playerPos = mc.player.getPosition();
}
return false;
}

@Override
public void stop() {
	mc.player.sendChatMessage("#cancel");
}

}