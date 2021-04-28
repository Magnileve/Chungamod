package magnileve.chungamod.settings;

public interface SettingListener {
	public void onNewValue(String setting, Object value);
	public boolean hasSetting(String setting);
}