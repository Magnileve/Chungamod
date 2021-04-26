package magnileve.chungamod;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import org.apache.logging.log4j.Logger;

import net.minecraft.util.math.BlockPos;

public class Settings {

private static final HashMap<String, Object> SETTING_VALUES = settingValues();
private static final HashMap<String, Object> DEFAULT_SETTINGS = defaultSettings();
private static Logger log;
private static HashMap<String, Object> settings;

private static final HashMap<String, Object> settingValues() {
	HashMap<String, Object> settingValues = new HashMap<>();
	settingValues.put("prefix", Type.STRING_ONE_WORD);
	settingValues.put("discordrpc", Type.BOOLEAN);
	settingValues.put("debug", Type.BOOLEAN);
	Object[] autoSortSettings = {"pos1 pos2 source sourceemptyimeout overflow", Type.BLOCKPOS, Type.BLOCKPOS, Type.BLOCKPOS, Type.SHORT, Type.BLOCKPOS};
	settingValues.put("autosort", autoSortSettings);
	return settingValues;
}

private static final HashMap<String, Object> defaultSettings() {
	HashMap<String, Object> defaultSettings = new HashMap<>();
	defaultSettings.put("prefix", ",");
	defaultSettings.put("discordrpc", true);
	defaultSettings.put("debug", false);
	Object[] autoSortSettings = {"pos1 pos2 source sourceemptytimeout overflow", null, null, null, new Short((short) 2), null};
	defaultSettings.put("autosort", autoSortSettings);
	return defaultSettings;
}

public static Object get(String setting) {
	Object returnSetting = settings.get(setting);
	if(returnSetting instanceof Integer[]) {
		Integer[] integersValue = (Integer[]) returnSetting;
		BlockPos blockPosValue = new BlockPos(integersValue[0], integersValue[1], integersValue[2]);
		returnSetting = (Object) blockPosValue;
	}
	return returnSetting;
}

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

public static void set(String setting, Object value) {
	if(value instanceof BlockPos) {
		BlockPos blockPosValue = (BlockPos) value;
		Integer[] integersValue = {blockPosValue.getX(), blockPosValue.getY(), blockPosValue.getZ()};
		value = (Object) integersValue;
	}
	settings.put(setting, value);
	save(settings);
}

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
}

public static Object getValue(String setting) {
	return SETTING_VALUES.get(setting);
}

public static Class<?> getValue(String feature, String setting) {
	Object[] featureSettings = (Object[]) SETTING_VALUES.get(feature);
	String[] settingNames = ((String) (featureSettings[0])).split(" ");
	byte i = 0;
	while(!settingNames[i].equalsIgnoreCase(setting)) i++;
	return (Class<?>) featureSettings[i + 1];
}

@SuppressWarnings("unchecked")
protected static void load(Logger logger) {
	log = logger;
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
	settings = loadedSettings;
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
	OBJECT_ARRAY, STRING, BOOLEAN, BLOCKPOS, SHORT, STRING_ONE_WORD
}

}