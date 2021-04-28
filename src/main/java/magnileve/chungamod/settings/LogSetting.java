package magnileve.chungamod.settings;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Logger;

public class LogSetting implements SettingListener {

private Logger log;
private Level defaultLevel;
private boolean currentSetting;

public LogSetting(Logger log) {
	this.log = log;
	currentSetting = (boolean) Settings.get("debug");
	if(currentSetting) {
		defaultLevel = log.getLevel();
		log.setLevel(Level.DEBUG);
		log.debug("Log level set to debug");
	}
}

@Override
public void onNewValue(String setting, Object value) {
	if(setting.equals("debug") && !((Boolean) value).equals(currentSetting)) {
		currentSetting = (Boolean) value;
		if((Boolean) value) {
			defaultLevel = log.getLevel();
			log.setLevel(Level.DEBUG);
			log.debug("Log level set to debug");
		} else log.setLevel(defaultLevel);
	}
}

@Override
public boolean hasSetting(String setting) {
	return setting.equals("debug");
}

}