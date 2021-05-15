package magnileve.chungamod;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

import magnileve.chungamod.settings.CustomChatSuffix;
import magnileve.chungamod.settings.Settings;
import magnileve.chungamod.time.LeaveServerListener;
import magnileve.chungamod.time.TickTimer;

@Mod(modid = Chung.MODID, name = Chung.NAME, version = Chung.VERSION, acceptedMinecraftVersions = Chung.ACCEPTED_MINECRAFT_VERSIONS)
public class ChungamodInitialization {

	private static Minecraft mc;
    private static Logger log;
    
    @Instance
    public static ChungamodInitialization instance;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
    	mc = Minecraft.getMinecraft();
        log = (Logger) event.getModLog();
        log.info(Chung.MODID + ": Pre-Initialization");
        Settings.init(log);
        Chung.init(mc);
        TickTimer.init(log);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        log.info(Chung.MODID + ": Initialization");
        Commands.init(mc, log);
        LeaveServerListener.init(mc, log);
    }
    
    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
    	log.info(Chung.MODID + ": Post-Initialization");
    	Commands.init2();
    	CustomChatSuffix.init(mc, log);
    }
}