package magnileve.chungamod.auto;

import magnileve.chungamod.time.Activity;
import magnileve.chungamod.time.TickListener;
import magnileve.chungamod.time.TickTimer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.CPacketPlayer;

public class RotationAntiAFK implements TickListener, Activity {

private Minecraft mc;
private int nextTick;
private float playerYaw;
private byte changeNumber;

/**
 * Sends rotation packets every 25 seconds to avoid afk detection.
 * @param mc instance of Mincerfat
 */
public RotationAntiAFK(Minecraft mc) {
	this.mc = mc;
	playerYaw = mc.player.rotationYaw;
	changeNumber = 0;
	TickTimer.addListener(this);
	nextTick = nextTick(1);
}

@Override
public boolean onTick(int tick) {
if(nextTick == tick) {
	switch(changeNumber) {
	case 0:
		playerYaw = mc.player.rotationYaw;
		mc.player.connection.sendPacket(new CPacketPlayer.Rotation((float) playerYaw + 1, (float) mc.player.rotationPitch, mc.player.onGround));
		break;
	case 1:
		mc.player.connection.sendPacket(new CPacketPlayer.Rotation((float) playerYaw - 1, (float) mc.player.rotationPitch, mc.player.onGround));
		break;
	case 2:
		mc.player.connection.sendPacket(new CPacketPlayer.Rotation((float) playerYaw, (float) mc.player.rotationPitch, mc.player.onGround));
		break;
	}
	if(changeNumber == 2) {
		changeNumber = 0;
		nextTick = nextTick(960);
	} else {
		changeNumber++;
		nextTick = nextTick(20);
	}
}
return false;
}

@Override
public void stop() {
	if(changeNumber != 0) mc.player.connection.sendPacket(new CPacketPlayer.Rotation((float) playerYaw, (float) mc.player.rotationPitch, mc.player.onGround));
}
}