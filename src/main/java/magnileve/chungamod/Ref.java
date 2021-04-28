package magnileve.chungamod;

import java.util.LinkedList;

import magnileve.chungamod.time.Activity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;

public class Ref {
	public static final String MODID = "chungamod";
    public static final String NAME = "Chungamod";
    public static final String VERSION = "0.1.1";
    public static final String ACCEPTED_MINECRAFT_VERSIONS = "[1.12]";
    private static Minecraft mc;
    public static LinkedList<Activity> runningActivities;
    
    public static void init(Minecraft minecraft) {
    	mc = minecraft;
    	runningActivities = new LinkedList<Activity>();
    }
    
    //send message to player
    public static void sendMessage(String message) {
    	mc.player.sendMessage(new TextComponentString("[Chungamod] " + message));
    }
    
    //inverse of string.split(regex)
    public static String inverseSplit(String[] strings, String regex) {
    	if (strings.length == 0) return "";
    	String str = strings[0];
    	for (short i = 1; i < strings.length; i++) str = str + regex + strings[i];
    	return str;
    }
    
    //copied and pasted from the Mincerfat code becaues it has protected visibility there
    public static final Vec3d getVectorForRotation(float pitch, float yaw) {
        float f = MathHelper.cos(-yaw * 0.017453292F - (float)Math.PI);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - (float)Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3d((double)(f1 * f2), (double)f3, (double)(f * f2));
    }
}