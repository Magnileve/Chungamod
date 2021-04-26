package magnileve.chungamod;

import magnileve.chungamod.time.Activity;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid=Ref.MODID)
public class LeaveServerListener {
	@SubscribeEvent
	@SideOnly(value = Side.CLIENT)
	public static void onClientDisconnectEvent(ClientDisconnectionFromServerEvent event) {
		for(Activity activity:Ref.runningActivities) activity.stop();
		Ref.runningActivities.clear();
	}
}
