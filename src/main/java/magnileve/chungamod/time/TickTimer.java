package magnileve.chungamod.time;

import java.util.LinkedList;

import org.apache.logging.log4j.Logger;

import magnileve.chungamod.Ref;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid=Ref.MODID)
public class TickTimer {

private static int tick;
private static LinkedList<Integer> listenTicks;
private static LinkedList<TickListener> listeners;
private static Logger log;

public static void init(Logger logger) {
	tick = 0;
	listenTicks = new LinkedList<Integer>();
	listeners = new LinkedList<TickListener>();
	log = logger;
}

@SubscribeEvent
@SideOnly(value = Side.CLIENT)
public static void onTick(ClientTickEvent event) {
	tick++;
	if(!listenTicks.isEmpty() && listenTicks.peekFirst() == tick) {
		listenTicks.remove();
		for(TickListener listener:listeners) listener.onTick(tick);
	}
}

public static void add(int futureTicks) {
	if (futureTicks > 0) {
		futureTicks += tick;
		int i = 0;
		if(listenTicks.isEmpty()) listenTicks.add(i, futureTicks);
		else for(int tickCount:listenTicks) {
			if(tickCount > tick) {
				listenTicks.add(i, futureTicks);
				break;
			}
			if(tickCount == tick) break;
			i++;
		}
	} else {
		log.fatal("Trying to add a tick in the past to the tick listener");
		throw new RuntimeException("Trying to add a tick in the past to the tick listener");
	}
}

public static void addListener(TickListener listener) {
	listeners.add(listener);
}

public static void removeListener(TickListener listener) {
	for(TickListener recordedListener:listeners) if(recordedListener == listener) {
		listeners.remove(listener);
		break;
	}
}

public static int current() {
	return tick;
}

}