package magnileve.chungamod.itemstorage;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class StorageUnit {

private final BlockPos location;
private final String shulkerName;
private final byte height;
private final EnumFacing facing;
private final boolean doubleChest;

private byte fillHeight;

public StorageUnit(BlockPos location, String shulkerName, byte height, EnumFacing facing, boolean doubleChest) {
	this.location = location;
	this.shulkerName = shulkerName;
	this.height = height;
	fillHeight = 0;
	this.facing = facing;
	this.doubleChest = doubleChest;
}

@Override
public String toString() {
	return (height + (doubleChest ? " double chest" : " single chest") + (height > 1 ? "s" : "") + " for " + shulkerName + " at " + location);
}

public String getName() {
	return shulkerName;
}

public BlockPos getBlockPos() {
	return location;
}

public byte getFillHeight() {
	return fillHeight;
}

public boolean nextUp() {
	fillHeight++;
	return fillHeight < height;
}

public EnumFacing getFacing() {
	return facing;
}

public boolean isDoubleChest() {
	return doubleChest;
}

}