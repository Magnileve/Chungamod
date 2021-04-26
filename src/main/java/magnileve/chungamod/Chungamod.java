package magnileve.chungamod;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

import magnileve.chungamod.time.TickTimer;

@Mod(modid = Ref.MODID, name = Ref.NAME, version = Ref.VERSION, acceptedMinecraftVersions = Ref.ACCEPTED_MINECRAFT_VERSIONS)
public class Chungamod {

	private static Minecraft mc;
    private static Logger log;
    
    @Instance
    public static Chungamod instance;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
    	mc = Minecraft.getMinecraft();
        log = event.getModLog();
        log.info(Ref.MODID + ":Pre-Initialization");
        Settings.load(log);
        Ref.init(mc);
        TickTimer.init(log);
        Commands.init(mc, log);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        log.info(Ref.MODID + ":Initialization");
    }
    
    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
    	log.info(Ref.MODID + ":Post-Initialization");
    	Commands.init2();
    }
}