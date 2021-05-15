package magnileve.chungamod.time;

import magnileve.chungamod.Chung;

public class ClientTps implements TickListener, Activity {

private byte tickCount;
private long time;

/**
 * Measures and sends client tps in chat every 256 ticks.
 */
public ClientTps() {
	tickCount = 0;
	time = System.currentTimeMillis();
	TickTimer.addListener(this);
	TickTimer.add(1);
}
@Override
public void stop() {
	
}

@Override
public boolean onTick(int tick) {
	tickCount++;
	if(tickCount == 0) {
		Chung.sendMessage("Current Client TPS: " + (1 / (((double) (System.currentTimeMillis() - time)) / 256000)));
		time = System.currentTimeMillis();
	}
	TickTimer.add(1);
	return false;
}
}
