package magnileve.chungamod.settings;

import magnileve.chungamod.Chung;
import net.arikia.dev.drpc.DiscordEventHandlers;
import net.arikia.dev.drpc.DiscordRPC;
import net.arikia.dev.drpc.DiscordRichPresence;

public class DiscordRPCManager implements SettingListener {

private final String[] SETTING_LIST = {"visible", "upper_line", "lower_line", "minecraft_name", "minecraft_version"};
private final String[] STATUS_LIST = {"Playing \\Mincerfat \\version", "The funniest client", "Version " + Chung.VERSION, "v" + Chung.VERSION + " | \\Mincerfat \\version", "9b9t.org", "9b9t.com", "2b2t.org", "2b2t.com", "Hypixel.net", "All hail Big Chungus", "Big Chungus on top", "ok", "ok chains on top", "Building highways", "Digging nether tunnels", "NHS on top", "Chungamod on top", "Chungia on top", "9b9t on top", "9b9t > 2b2t"};
private final String[] MINCERFAT_NAME_LIST = {"Minecraft", "Mincerfat", "Minceraft", "block game"};
private final String[] MINCERFAT_VERSION_LIST = {"1.12", "1.12.2"};
private final long START_TIME;
private boolean visible;

private static DiscordRPCManager instance;
private byte details;
private byte state;
private byte mincerfatName;
private byte mincerfatVersion;

public DiscordRPCManager() {
	 START_TIME = System.currentTimeMillis() / 1000;
	 Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if((boolean) Settings.get("discordrpc", "visible")) DiscordRPC.discordShutdown();
			}
		});
	 visible = (boolean) Settings.get("discordrpc", "visible");
	 details = (byte) Settings.get("discordrpc", "upper_line");
	 state = (byte) Settings.get("discordrpc", "lower_line");
	 mincerfatName = (byte) Settings.get("discordrpc", "minecraft_name");
	 mincerfatVersion = (byte) Settings.get("discordrpc", "minecraft_version");
	 if(visible) {
		 DiscordRPC.discordInitialize("832742372420091964", new DiscordEventHandlers.Builder().build(), true);
		 discordRPCupdate();
	 }
	 instance = this;
}

@Override
public void onNewValue(String setting, Object value) {
	switch(setting) {
	case "upper_line":
		if((byte) value > STATUS_LIST.length) {
			Chung.sendMessage("Value must not be greater than " + STATUS_LIST.length);
			Settings.set("discordrpc", setting, details);
		} else details = (byte) value;
		break;
	case "lower_line":
		if((byte) value > STATUS_LIST.length) {
			Chung.sendMessage("Value must not be greater than " + STATUS_LIST.length);
			Settings.set("discordrpc", setting, state);
		} else state = (byte) value;
		break;
	case "minecraft_name":
		if((byte) value > STATUS_LIST.length) {
			Chung.sendMessage("Value must not be greater than " + MINCERFAT_NAME_LIST.length);
			Settings.set("discordrpc", setting, mincerfatName);
		} else mincerfatName = (byte) value;
		break;
	case "minecraft_version":
		if((byte) value > STATUS_LIST.length) {
			Chung.sendMessage("Value must not be greater than " + MINCERFAT_VERSION_LIST.length);
			Settings.set("discordrpc", setting, mincerfatVersion);
		} else mincerfatVersion = (byte) value;
		break;
	case "visible":
		if(!visible == (boolean) value) {
			visible = (boolean) value;
			if(visible) DiscordRPC.discordInitialize("832742372420091964", new DiscordEventHandlers.Builder().build(), true);
			else DiscordRPC.discordShutdown();
		}
	}
	if(visible) discordRPCupdate();
}

@Override
public boolean hasSetting(String setting) {
	for(String thisSetting:SETTING_LIST) if(setting.equals("discordrpc " + thisSetting)) return true;
	return false;
}

public static DiscordRPCManager getDiscordRPCManager() {
	return instance;
}

public String getSettingValues() {
	StringBuilder str = new StringBuilder().append("Status lines:");
	byte i = 0;
	for(String value:STATUS_LIST) {
		i++;
		str.append("\n" + i + ". " + format(value));
	}
	i = 0;
	str.append("\nMinecraft names:");
	for(String value:MINCERFAT_NAME_LIST) {
		i++;
		str.append("\n" + i + ". " + format(value));
	}
	i = 0;
	str.append("\nMinecraft versions:");
	for(String value:MINCERFAT_VERSION_LIST) {
		i++;
		str.append("\n" + i + ". " + format(value));
	}
	return str.toString();
}

private void discordRPCupdate() {
	DiscordRPC.discordUpdatePresence(new DiscordRichPresence.Builder(format(STATUS_LIST[state - 1])).setBigImage("chungamod-logo", "All hail Big Chungus").setDetails(format(STATUS_LIST[details - 1])).setStartTimestamps(START_TIME).build());
}

private String format(String string) {
	return string.replaceFirst("\\\\Mincerfat", MINCERFAT_NAME_LIST[mincerfatName - 1]).replaceFirst("\\\\version", MINCERFAT_VERSION_LIST[mincerfatVersion - 1]);
}

}
