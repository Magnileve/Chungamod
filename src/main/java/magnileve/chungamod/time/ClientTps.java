package magnileve.chungamod.time;

import magnileve.chungamod.Ref;

public class ClientTps implements TickListener, Activity {
	
	private byte tickCount;
	private long time;
	
	public ClientTps() {
		tickCount = 0;
		time = System.currentTimeMillis();
		TickTimer.addListener(this);
		TickTimer.add(1);
	}
	@Override
	public void stop() {
		TickTimer.removeListener(this);
		Ref.runningActivities.remove(this);
	}

	@Override
	public void onTick(int tick) {
		tickCount++;
		if(tickCount == 0) {
			Ref.sendMessage("Current Client TPS: " + (1 / (((double) (System.currentTimeMillis() - time)) / 256000)));
			time = System.currentTimeMillis();
		}
		TickTimer.add(1);
	}
}
