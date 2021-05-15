package magnileve.chungamod.settings;

import java.util.LinkedList;

import org.apache.logging.log4j.Logger;

import magnileve.chungamod.Chung;
import magnileve.chungamod.time.TickTimer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid=Chung.MODID)
public class CustomChatSuffix implements SettingListener {

private static Minecraft mc;
private static Logger log;
private static LinkedList<String> ignoredPrefixes;
private static String suffix;
private static String chungamodPrefix;
private static boolean off;
private static boolean addingSuffix;

public static void init(Minecraft mcIn, Logger logIn) {
	Settings.addListener(new CustomChatSuffix());
	mc = mcIn;
	log = logIn;
	addingSuffix = false;
}

public CustomChatSuffix() {
	suffix = (boolean) Settings.get("custom_chat_suffix", "add_space") ? " ".concat((String) Settings.get("custom_chat_suffix", "suffix")) : (String) Settings.get("custom_chat_suffix", "suffix");
	chungamodPrefix = (String) Settings.get("prefix");
	off = !(boolean) Settings.get("custom_chat_suffix", "on");
	String[] array = ((String) Settings.get("custom_chat_suffix", "ignored_prefixes")).substring(1).split("\"");
	ignoredPrefixes = new LinkedList<>();
	boolean even = true;
	for(String str:array) {
		if(even) {
			even = false;
			ignoredPrefixes.add(str);
		} else {
			even = true;
			if(!str.equals(" ")) break;
		}
	}
	if(even && array.length != 0) {
		Settings.set("custom_chat_suffix", null);
		array = ((String) Settings.get("custom_chat_suffix", "ignored_prefixes")).substring(1).split("\"");
		ignoredPrefixes.clear();
		for(byte i = 0; i < array.length; i += 2) ignoredPrefixes.add(array[i]);
	}
}

@SubscribeEvent(priority = EventPriority.HIGHEST)
@SideOnly(value = Side.CLIENT)
public static void onClientChatEvent(ClientChatEvent event) {
	if(off) return;
	String message = event.getMessage();
	if(message.startsWith(chungamodPrefix)) return;
	for(String prefix:ignoredPrefixes) if(message.startsWith(prefix)) return;
	addingSuffix = true;
	mc.ingameGUI.getChatGUI().addToSentMessages(message);
	event.setMessage(message.concat(suffix));
}

@SubscribeEvent(priority = EventPriority.LOWEST)
@SideOnly(value = Side.CLIENT)
public static void onClientChatEvent2(ClientChatEvent event) {
	if(addingSuffix) {
		addingSuffix = false;
		event.setCanceled(true);
		if(net.minecraftforge.client.ClientCommandHandler.instance.executeCommand(mc.player, event.getMessage()) != 0) return;
	    mc.player.sendChatMessage(event.getMessage());
	}
}

@Override
public void onNewValue(String setting, Object value) {
	switch(setting) {
	case "prefix":
		chungamodPrefix = (String) value;
		break;
	case "on":
		off = !(boolean) value;
		break;
	case "suffix":
		suffix = (boolean) Settings.get("custom_chat_suffix", "add_space") ? " ".concat((String) value) : (String) value;
		break;
	case "ignored_prefixes":
		String[] array = ((String) value).substring(1).split("\"");
		LinkedList<String> list = new LinkedList<>();
		boolean even = true;
		if(((String) value).charAt(0) == '\"' && ((String) value).charAt(((String) value).length() - 1) == '\"') for(String str:array) {
			if(even) {
				even = false;
				list.add(str);
			} else {
				even = true;
				if(!str.equals(" ")) break;
			}
		}
		if(even) {
			log.info("Syntax error in ignored prefixes");
			StringBuilder str = new StringBuilder();
			for(String prefix:ignoredPrefixes) if(!prefix.isEmpty()) str.append("\" \"").append(prefix);
			TickTimer.addListener(tick -> {
				Settings.set("custom_chat_suffix", "ignored_prefixes", str.length() == 0 ? "" : str.append("\"").substring(2));
				Chung.sendMessage("Syntax error: separate prefixes with spaces, and put each prefix in quotes");
				return true;
			});
			TickTimer.add(1);
		} else ignoredPrefixes = list;
		break;
	case "add_space":
		suffix = (boolean) value ? " ".concat((String) Settings.get("custom_chat_suffix", "suffix")) : (String) Settings.get("custom_chat_suffix", "suffix");
		break;
	}
}

@Override
public boolean hasSetting(String setting) {
	return setting.equals("prefix") || setting.startsWith("custom_chat_suffix");
}

}