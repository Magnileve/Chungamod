package magnileve.chungamod;

import java.util.LinkedList;

import magnileve.chungamod.time.Activity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;

public class Chung {

public static final String MODID = "chungamod";
public static final String NAME = "Chungamod";
public static final String VERSION = "0.2";
public static final String ACCEPTED_MINECRAFT_VERSIONS = "[1.12]";
private static Minecraft mc;
public static LinkedList<Activity> runningActivities;

public static void init(Minecraft mcIn) {
	mc = mcIn;
	runningActivities = new LinkedList<Activity>();
}

/**
 * Sends a client-side message in chat.
 * @param message what shows up in chat
 */
public static void sendMessage(String message) {
	mc.player.sendMessage(new TextComponentString("[Chungamod] " + message));
}

/**
 * Inverse of String method split(regex).
 * @param strings array of split strings to be converted back into one string
 * @param regex what to put between each split string
 */
public static String inverseSplit(String[] strings, String regex) {
	if (strings.length == 0) return "";
	StringBuilder str = new StringBuilder(strings[0]);
	for (short i = 1; i < strings.length; i++) str.append(regex).append(strings[i]);
	return str.toString();
}

/**
 * Copied and pasted from the Mincerfat code becaues it has protected visibility there.
 * @param pitch pitch of rotation
 * @param yaw yaw of rotation
 * @return vector with rotation of the input pitch and yaw
 */
public static final Vec3d getVectorForRotation(float pitch, float yaw) {
	float f = MathHelper.cos(-yaw * 0.017453292F - (float)Math.PI);
	float f1 = MathHelper.sin(-yaw * 0.017453292F - (float)Math.PI);
	float f2 = -MathHelper.cos(-pitch * 0.017453292F);
	float f3 = MathHelper.sin(-pitch * 0.017453292F);
	return new Vec3d((double)(f1 * f2), (double)f3, (double)(f * f2));
}

/**
 * Gets packet for right-clicking on a block.  Blocks between the player and the input BlockPos do affect the side the block is used on, but not the BlockPos to be used.  Range is 4.5 blocks.
 * @param blockPos block to be used
 * @param yOffset offsets the y coordinate of the 3d vector to be raytraced to in order to determine which side of the block is hit first
 * @return a packet to use the input BlockPos ready to be sent to the server
 */
public static CPacketPlayerTryUseItemOnBlock useBlock(BlockPos blockPos, double yOffset) {
	double dx = blockPos.getX() - mc.player.posX + 0.5;
	double dy = blockPos.getY() - mc.player.posY + 0.5 + yOffset - mc.player.eyeHeight;
	double dz = blockPos.getZ() - mc.player.posZ + 0.5;
	
	double r = Math.sqrt(dx * dx + dy * dy + dz * dz);
	double yaw = -Math.atan2(dx, dz) / Math.PI * 180;
	if(yaw < 0) yaw = 360 + yaw;
	double pitch = -Math.asin(dy / r) / Math.PI * 180;
	
	Vec3d vec3d = mc.player.getPositionEyes(1.0F);
    Vec3d vec3d1 = Chung.getVectorForRotation((float) pitch, (float) yaw);
    Vec3d vec3d2 = vec3d.addVector(vec3d1.x * 4.5D, vec3d1.y * 4.5D, vec3d1.z * 4.5D);
    RayTraceResult rayTrace = mc.world.rayTraceBlocks(vec3d, vec3d2, false, false, true);
	
	BlockPos pos1 = rayTrace.getBlockPos();
	Vec3d vec1 = rayTrace.hitVec;
	float f = (float)(vec1.x - (double)pos1.getX());
    float f1 = (float)(vec1.y - (double)pos1.getY());
    float f2 = (float)(vec1.z - (double)pos1.getZ());
	return new CPacketPlayerTryUseItemOnBlock(blockPos, rayTrace.sideHit, EnumHand.MAIN_HAND, f, f1, f2);
}

}