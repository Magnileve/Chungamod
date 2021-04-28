package magnileve.chungamod.time;

import java.util.LinkedList;

import org.apache.logging.log4j.core.Logger;

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

public static void init(Logger logIn) {
	tick = 0;
	listenTicks = new LinkedList<Integer>();
	listeners = new LinkedList<TickListener>();
	log = logIn;
}

@SubscribeEvent
@SideOnly(value = Side.CLIENT)
public static void onTick(ClientTickEvent event) {
	tick++;
	if(!listenTicks.isEmpty() && listenTicks.getFirst() == tick) {
		listenTicks.remove();
		for(TickListener listener:listeners) listener.onTick(tick);
	}
}

public static void add(int futureTicks) {
	if(futureTicks > 0) {
		int i = 0;
		if(listenTicks.isEmpty()) {
			listenTicks.add(tick + futureTicks);
			return;
		}
		for(int tickCount:listenTicks) {
			if(tickCount > tick + futureTicks) {
				listenTicks.add(i, tick + futureTicks);
				return;
			}
			if(tickCount == tick + futureTicks) return;
			i++;
		}
		listenTicks.add(tick + futureTicks);
		
	} else {
		log.error("Trying to add a tick in the past to the tick listener: " + futureTicks);
		throw new NumberFormatException("Value must be above 0. Value: \"" + futureTicks + "\"");
	}
}

public static void addListener(TickListener listener) {
	listeners.add(listener);
}

public static void removeListener(TickListener listener) {
	listeners.remove(listener);
}

public static int current() {
	return tick;
}

}