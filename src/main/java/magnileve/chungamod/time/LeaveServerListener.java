package magnileve.chungamod.time;

import org.apache.logging.log4j.Logger;

import magnileve.chungamod.Chung;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid=Chung.MODID)
public class LeaveServerListener {

private static Minecraft mc;
private static Logger log;

public static void init(Minecraft mcIn, Logger logIn) {
	mc = mcIn;
	log = logIn;
}

@SubscribeEvent
@SideOnly(value = Side.CLIENT)
public static void onClientDisconnectEvent(ClientDisconnectionFromServerEvent event) {
	log.info("Stopping running activities");
	for(Activity activity:Chung.runningActivities) {
		log.info("Class: {} Restartable: {} TickListener: {}", activity.getClass().getName(), activity instanceof RestartableActivity, activity instanceof TickListener);
		if(activity instanceof RestartableActivity) new RelogActivityListener(((RestartableActivity) activity), ((RestartableActivity) activity).getNearbyPosition(), mc, log);
		if(activity instanceof TickListener) TickTimer.removeListener((TickListener) activity);
	}
	Chung.runningActivities.clear();
}

}