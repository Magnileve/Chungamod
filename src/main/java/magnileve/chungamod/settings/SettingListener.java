package magnileve.chungamod.settings;

public interface SettingListener {

/**
 * Called when the value of a setting is changed.
 * @param setting name of setting
 * @param value new value of setting
 */
public void onNewValue(String setting, Object value);

/**
 * Checks if a listener is listening for a change in the value of a certain setting.
 * @param setting name of setting
 * @return true if the listener is listening for a change in the value of this setting
 */
public boolean hasSetting(String setting);

}