package magnileve.chungamod.settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.apache.logging.log4j.Logger;

import net.minecraft.util.math.BlockPos;

public class Settings {

private static final HashMap<String, Object> SETTING_VALUES = settingValues();
private static final HashMap<String, Object> DEFAULT_SETTINGS = defaultSettings();
public static final String SETTINGS_LIST = settingList();
private static Logger log;
private static HashMap<String, Object> settings;
private static LinkedList<SettingListener> listeners;

private static final HashMap<String, Object> settingValues() {
	HashMap<String, Object> settingValues = new HashMap<>();
	settingValues.put("prefix", Type.STRING_ONE_WORD);
	Object[] discordRPCSettings = {"visible upper_line lower_line minecraft_name minecraft_version", Type.BOOLEAN, Type.BYTE_POSITIVE, Type.BYTE_POSITIVE, Type.BYTE_POSITIVE, Type.BYTE_POSITIVE};
	settingValues.put("discordrpc", discordRPCSettings);
	settingValues.put("debug", Type.BOOLEAN);
	settingValues.put("tick_delay", Type.BYTE_POSITIVE);
	Object[] autoSortSettings = {"pos1 pos2 source chest_open_tick_delay source_empty_timeout overflow", Type.BLOCKPOS, Type.BLOCKPOS, Type.BLOCKPOS, Type.SHORT, Type.SHORT, Type.BLOCKPOS};
	settingValues.put("autosort", autoSortSettings);
	Object[] chatSuffixSettings = {"on suffix ignored_prefixes add_space", Type.BOOLEAN, Type.STRING, Type.STRING, Type.BOOLEAN};
	settingValues.put("custom_chat_suffix", chatSuffixSettings);
	return settingValues;
}

private static final HashMap<String, Object> defaultSettings() {
	HashMap<String, Object> defaultSettings = new HashMap<>();
	defaultSettings.put("prefix", ",");
	Object[] discordRPCSettings = {"visible upper_line lower_line minecraft_name minecraft_version", true, new Byte((byte) 1), new Byte((byte) 2), new Byte((byte) 1), new Byte((byte) 1)};
	defaultSettings.put("discordrpc", discordRPCSettings);
	defaultSettings.put("debug", false);
	defaultSettings.put("tick_delay", new Byte((byte) 2));
	Object[] autoSortSettings = {"pos1 pos2 source chest_open_tick_delay source_empty_timeout overflow", null, null, null, new Short((short) 2), new Short((short) 2), null};
	defaultSettings.put("autosort", autoSortSettings);
	Object[] chatSuffixSettings = {"on suffix ignored_prefixes add_space", true, "`Chungamod", "\"/\"", true};
	defaultSettings.put("custom_chat_suffix", chatSuffixSettings);
	return defaultSettings;
}

private static final String settingList() {
	Iterator<Entry<String, Object>> iterator = SETTING_VALUES.entrySet().iterator();
	StringBuilder str = new StringBuilder().append(iterator.next().getKey());
	while(iterator.hasNext()) str.append(", " + iterator.next().getKey());
	return str.toString();
}

public static void init(Logger logIn) {
	log = logIn;
	settings = load();
	listeners = new LinkedList<SettingListener>();
}

/**
 * Gets the value of a setting that is not a feature setting.
 * @param setting name of setting
 * @return value of setting
 */
public static Object get(String setting) {
	Object returnSetting = settings.get(setting);
	if(returnSetting == null && getValue(setting) instanceof Type && (Type) getValue(setting) != Type.BLOCKPOS) {
		log.error("Setting null for " + setting);
		settings.put(setting, DEFAULT_SETTINGS.get(setting));
		returnSetting = settings.get(setting);
	}
	if(returnSetting instanceof Integer[]) {
		Integer[] integersValue = (Integer[]) returnSetting;
		BlockPos blockPosValue = new BlockPos(integersValue[0], integersValue[1], integersValue[2]);
		returnSetting = (Object) blockPosValue;
	}
	return returnSetting;
}

/**
 * Gets the value of a setting that is a feature setting.
 * @param feature name of feature
 * @param setting name of setting
 * @return value of setting
 */
public static Object get(String feature, String setting) {
	Object returnSetting;
	try {
		Object[] featureSettings = (Object[]) settings.get(feature);
		String[] settingNames = ((String) (featureSettings[0])).split(" ");
		byte i = 0;
		while(!settingNames[i].equalsIgnoreCase(setting)) i++;
		returnSetting = featureSettings[i + 1];
	} catch(Exception e) {
		log.error("Settings corrupted for " + feature);
		settings.put(feature, DEFAULT_SETTINGS.get(feature));
		Object[] featureSettings = (Object[]) settings.get(feature);
		String[] settingNames = ((String) (featureSettings[0])).split(" ");
		byte i = 0;
		while(!settingNames[i].equalsIgnoreCase(setting)) i++;
		returnSetting = featureSettings[i + 1];
	}
	if(returnSetting instanceof Integer[]) {
		Integer[] integersValue = (Integer[]) returnSetting;
		BlockPos blockPosValue = new BlockPos(integersValue[0], integersValue[1], integersValue[2]);
		returnSetting = (Object) blockPosValue;
	}
	return returnSetting;
}

/**
 * Sets the value of a setting that is not a feature setting.
 * @param setting name of setting
 * @param value new value of setting
 */
public static void set(String setting, Object value) {
	if(value instanceof BlockPos) {
		BlockPos blockPosValue = (BlockPos) value;
		Integer[] integersValue = {blockPosValue.getX(), blockPosValue.getY(), blockPosValue.getZ()};
		value = (Object) integersValue;
	}
	settings.put(setting, value);
	save(settings);
	for(SettingListener listener:listeners) if(listener.hasSetting(setting)) listener.onNewValue(setting, value);
}

/**
 * Sets the value of a setting that is a feature setting.
 * @param feature name of feature
 * @param setting name of setting
 * @param value new value of setting
 */
public static void set(String feature, String setting, Object value) {
	if(value instanceof BlockPos) {
		BlockPos blockPosValue = (BlockPos) value;
		Integer[] integersValue = {blockPosValue.getX(), blockPosValue.getY(), blockPosValue.getZ()};
		value = (Object) integersValue;
	}
	try {
		Object[] featureSettings = (Object[]) settings.get(feature);
		String[] settingNames = ((String) (featureSettings[0])).split(" ");
		byte i = 0;
		while(!settingNames[i].equalsIgnoreCase(setting)) i++;
		featureSettings[i + 1] = value;
		settings.put(feature, featureSettings);
	} catch(Exception e) {
		log.error("Settings corrupted for " + feature);
		settings.put(feature, DEFAULT_SETTINGS.get(feature));
		Object[] featureSettings = (Object[]) settings.get(feature);
		String[] settingNames = ((String) (featureSettings[0])).split(" ");
		byte i = 0;
		while(!settingNames[i].equalsIgnoreCase(setting)) i++;
		featureSettings[i + 1] = value;
		settings.put(feature, featureSettings);
	}
	save(settings);
	for(SettingListener listener:listeners) if(listener.hasSetting(feature + " " + setting)) listener.onNewValue(setting, value);
}

/**
 * Gets the type of value for a setting.
 * @param setting name of setting
 * @return type of value the setting holds
 */
public static Object getValue(String setting) {
	return SETTING_VALUES.get(setting);
}

/**
 * Adds listener for changing setting values
 * @param listener listener to be added
 */
public static void addListener(SettingListener listener) {
	listeners.add(listener);
}

/**
 * Removes listener for changing setting values
 * @param listener listener to be removed
 */
public static void removeListener(SettingListener listener) {
	listeners.remove(listener);
}

@SuppressWarnings("unchecked")
private static HashMap<String, Object> load() {
	HashMap<String, Object> loadedSettings;
	Object getSettingsResult = null;
	HashMap<String, Object> getSettings;
	FileInputStream fileIn = null;
	try {
		fileIn = new FileInputStream(".\\chungamod\\settings.ser");
	} catch (IOException e) {
		log.info("Settings file not found");
		getSettings = DEFAULT_SETTINGS;
		try {
			new FileOutputStream(".\\chungamod\\settings.ser").close();
		} catch (IOException e1) {
			File file = new File(".\\chungamod");
			file.mkdir();
		}
		save(getSettings);
		getSettingsResult = getSettings;
	}
	if(fileIn != null) {
		try {
			ObjectInputStream in = new ObjectInputStream(fileIn);
			getSettings = (HashMap<String, Object>) in.readObject();
	        in.close();
	        fileIn.close();
	        getSettingsResult = getSettings;
	        log.debug("Settings loaded");
		} catch (Exception e) {
			getSettingsResult = e;
		}
	}
	if(getSettingsResult instanceof HashMap<?, ?>) loadedSettings = (HashMap<String, Object>) getSettingsResult;
	else {
		try {
			Exception e = (Exception) getSettingsResult;
			log.error("Error loading settings");
			e.printStackTrace();
		} catch(Exception e) {
			log.error("Settings file corrupted");
			e.printStackTrace();
		}
		loadedSettings = DEFAULT_SETTINGS;
		save(loadedSettings);
	}
	return loadedSettings;
}

private static void save(HashMap<String, Object> settings) {
	try {
		FileOutputStream fileOut = new FileOutputStream(".\\chungamod\\settings.ser");
        ObjectOutputStream out = new ObjectOutputStream(fileOut);
        out.writeObject(settings);
        out.close();
        fileOut.close();
        log.debug("Settings saved");
     } catch (IOException e) {
        e.printStackTrace();
     }
}

public static enum Type {
	OBJECT_ARRAY, STRING, STRING_ONE_WORD, BOOLEAN, BLOCKPOS, BYTE, BYTE_POSITIVE, SHORT
}

}