package magnileve.chungamod.itemstorage;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class StorageUnit {

private BlockPos location;
private String shulkerName;
private byte height;
private byte fillHeight;
private EnumFacing facing;
private boolean doubleChest;

protected StorageUnit(BlockPos location, String shulkerName, byte height, EnumFacing facing, boolean doubleChest) {
	this.location = location;
	this.shulkerName = shulkerName;
	this.height = height;
	fillHeight = 0;
	this.facing = facing;
	this.doubleChest = doubleChest;
}

protected String getName() {
	return shulkerName;
}

protected BlockPos getBlockPos() {
	return location;
}

protected byte getFillHeight() {
	return fillHeight;
}

protected boolean nextUp() {
	fillHeight++;
	return fillHeight < height;
}

protected EnumFacing getFacing() {
	return facing;
}

protected boolean doubleChest() {
	return doubleChest;
}

}