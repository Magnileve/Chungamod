package magnileve.chungamod.auto.itemstorage;

import java.util.LinkedList;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import magnileve.chungamod.Chung;
import magnileve.chungamod.settings.Settings;
import magnileve.chungamod.time.Activity;
import magnileve.chungamod.time.TickListener;
import magnileve.chungamod.time.TickTimer;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.network.play.client.CPacketClickWindow;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.chunk.Chunk;

public class AutoSort implements TickListener, Activity {

private final BlockPos source;
private final BlockPos overflow;
private final short sourceEmptyTimeout;
private final byte inventorySpace;
private final EnumFacing sourceFacing;
private final EnumFacing overflowFacing;
private final boolean sourceDoubleChest;
private final boolean overflowDoubleChest;

private Minecraft mc;
private Logger log;
private NetHandlerPlayClient connection;
private LinkedList<StorageUnit> sortLocations;
private Status status;
private BlockPos destination;
private StorageUnit unit;
private double prevDistance;
private int nextTick;
private byte slot;
private byte moveSlot;
private byte stuckCheck;
private EnumFacing destinationOffset;
private boolean goToOverflow;

/**
 * Automatically sorts shulker boxes into chests with the shulker boxes' names labeled on signs.
 * @param mcIn instance of Mincerfat
 * @param pos1 looks for signs with shulker box names connected to a stack of chests between this and pos2
 * @param pos2 looks for signs with shulker box names connected to a stack of chests between this and pos1
 * @param sourceIn takes shulker boxes out of the chest at this location
 * @param sourceEmptyTimeout how many seconds to wait when the source is empty before stopping
 * @param overflowIn where to put shulker boxes which either don't have a designated chest or have full storage
 * @param logIn instance of logger
 */
public AutoSort(Minecraft mcIn, BlockPos pos1, BlockPos pos2, BlockPos sourceIn, short sourceEmptyTimeout, BlockPos overflowIn, Logger logIn) {
	this(mcIn, getStorageUnits(mcIn, pos1, pos2, logIn), sourceIn, sourceEmptyTimeout, overflowIn, logIn);
}

/**
 * Automatically sorts shulker boxes into chests with the shulker boxes' names labeled on signs.
 * @param mcIn instance of Mincerfat
 * @param sortLocationsIn stacks of chests to put shulker boxes with matching names into
 * @param sourceIn takes shulker boxes out of the chest at this location
 * @param sourceEmptyTimeout how many seconds to wait when the source is empty before stopping
 * @param overflowIn where to put shulker boxes which either don't have a designated chest or have full storage
 * @param logIn instance of logger
 */
public AutoSort(Minecraft mcIn, LinkedList<StorageUnit> sortLocationsIn, BlockPos sourceIn, short sourceEmptyTimeout, BlockPos overflowIn, Logger logIn) {
	mc = mcIn;
	connection = mc.player.connection;
	log = logIn;
	source = sourceIn;
	this.sourceEmptyTimeout = sourceEmptyTimeout;
	overflow = overflowIn;
	stuckCheck = 0;
	sortLocations = sortLocationsIn;
	goToOverflow = false;
	if(sortLocations == null || source.getDistance((int) mc.player.posX, (int) mc.player.posY, (int) mc.player.posZ) > 250 ||
			(overflow == null ? false : overflow.getDistance((int) mc.player.posX, (int) mc.player.posY, (int) mc.player.posZ) > 250)) {
		Chung.sendMessage("AutoSort error: Chests too far away");
		Chung.runningActivities.remove(this);
		sourceFacing = null;
		overflowFacing = null;
		sourceDoubleChest = false;
		overflowDoubleChest = false;
		inventorySpace = 0;
		status = Status.SOURCE_EMPTY_TIMEOUT;
		return;
	}
	log.info("[AutoSort] Running AutoSort");
	EnumFacing getSourceFacing = null;
	boolean getSourceDoubleChest = false;
	for(IProperty<?> property:mc.world.getBlockState(source).getBlock().getBlockState().getProperties()) if(property.getName().equals("facing")) {
		getSourceFacing = (EnumFacing) mc.world.getBlockState(source).getValue(property);
		getSourceDoubleChest = (isChest(Block.getIdFromBlock(mc.world.getBlockState(source.offset(getSourceFacing.rotateY())).getBlock())) ||
			isChest(Block.getIdFromBlock(mc.world.getBlockState(source.offset(getSourceFacing.rotateYCCW())).getBlock())));
		break;
	}
	sourceFacing = getSourceFacing;
	sourceDoubleChest = getSourceDoubleChest;
	EnumFacing getOverflowFacing = null;
	boolean getOverflowDoubleChest = false;
	if(overflow != null) for(IProperty<?> property:mc.world.getBlockState(overflow).getBlock().getBlockState().getProperties()) if(property.getName().equals("facing")) {
		getOverflowFacing = (EnumFacing) mc.world.getBlockState(overflow).getValue(property);
		getOverflowDoubleChest = (isChest(Block.getIdFromBlock(mc.world.getBlockState(overflow.offset(getOverflowFacing.rotateY())).getBlock())) ||
			isChest(Block.getIdFromBlock(mc.world.getBlockState(overflow.offset(getOverflowFacing.rotateYCCW())).getBlock())));
		break;
	}
	overflowFacing = getOverflowFacing;
	overflowDoubleChest = getOverflowDoubleChest;
	
	byte getInventorySpace = 0;
	for(byte i = 9; i < 45; i++) {
		Slot thisSlot = mc.player.openContainer.getSlot(i);
		if(thisSlot.getHasStack()) {
			int itemID = Item.getIdFromItem(thisSlot.getStack().getItem());
			if(itemID > 218 && itemID < 235 && overflow != null) getInventorySpace++;
		} else getInventorySpace++;
	}
	inventorySpace = getInventorySpace;
	slot = 9;
	status = Status.SEARCHING_STORAGE;
	TickTimer.addListener(this);
	nextTick = nextTick(1);
}

private static LinkedList<StorageUnit> getStorageUnits(Minecraft mc, BlockPos pos1, BlockPos pos2, Logger log) {
	LinkedList<StorageUnit> sortLocations = new LinkedList<>();
	if(pos1.getDistance((int) mc.player.posX, (int) mc.player.posY, (int) mc.player.posZ) > 250 ||
			pos2.getDistance((int) mc.player.posX, (int) mc.player.posY, (int) mc.player.posZ) > 250) return null;
	int minX = Math.min(pos1.getX(), pos2.getX());
	int minY = Math.min(pos1.getY(), pos2.getY());
	int minZ = Math.min(pos1.getZ(), pos2.getZ());
	int maxX = Math.max(pos1.getX(), pos2.getX());
	int maxY = Math.max(pos1.getY(), pos2.getY());
	int maxZ = Math.max(pos1.getZ(), pos2.getZ());
	for(int x = minX; x <= maxX; x++) for(int y = minY; y <= maxY; y++) for(int z = minZ; z <= maxZ; z++) {
		if(Block.getIdFromBlock(mc.world.getBlockState(new BlockPos(x, y, z)).getBlock()) == 68) {
			BlockPos blockPos = new BlockPos(x, y, z);
			for(IProperty<?> property:mc.world.getBlockState(blockPos).getBlock().getBlockState().getProperties()) if(property.getName().equals("facing")) {
				String shulkerName = "";
				for(ITextComponent line:((TileEntitySign) mc.world.getChunkFromBlockCoords(blockPos).getTileEntity(blockPos, Chunk.EnumCreateEntityType.CHECK)).signText) if(!line.getFormattedText().isEmpty()) {
					char[] lineChars = line.getFormattedText().toCharArray();
					String actualLine = "";
					for(char character:lineChars) {
						if(character == '§') break;
						actualLine += character;
					}
					shulkerName += " " + actualLine;
				}
				if(!shulkerName.isEmpty()) {
				shulkerName = shulkerName.substring(1);
				EnumFacing facing = (EnumFacing) mc.world.getBlockState(blockPos).getValue(property);
				blockPos = blockPos.offset(facing, -1);
				int blockID = Block.getIdFromBlock(mc.world.getBlockState(blockPos).getBlock());
				if(isChest(blockID)) {
					while(Block.getIdFromBlock(mc.world.getBlockState(blockPos).getBlock()) == blockID) blockPos = blockPos.down();
					blockPos = blockPos.up();
					EnumFacing doubleChestFacing = null;
					if(Block.getIdFromBlock(mc.world.getBlockState(blockPos.offset(facing.rotateY())).getBlock()) == blockID) doubleChestFacing = facing.rotateY();
					else if(Block.getIdFromBlock(mc.world.getBlockState(blockPos.offset(facing.rotateYCCW())).getBlock()) == blockID) doubleChestFacing = facing.rotateYCCW();
					else if(Block.getIdFromBlock(mc.world.getBlockState(blockPos.offset(facing.getOpposite())).getBlock()) == blockID) doubleChestFacing = facing.getOpposite();
					byte height = 0;
					while(Block.getIdFromBlock(mc.world.getBlockState(blockPos.up(height)).getBlock()) == blockID &&
							(doubleChestFacing == null ? true : (Block.getIdFromBlock(mc.world.getBlockState(blockPos.up(height).offset(doubleChestFacing)).getBlock())) == blockID) && height < 6) height++;
					sortLocations.add(new StorageUnit(blockPos, shulkerName, height, facing, doubleChestFacing != null));
					log.debug("[AutoSort] Adding storage location of \"" + shulkerName + "\" with height " + height + " at " + blockPos);
				}
				break;
				}
			}
		}
	}
	return sortLocations;
}

@Override
public boolean onTick(int tick) {
if(tick == nextTick) {
	try {
	switch(status) {
	case GOING_TO_STORAGE:
	case GOING_TO_SOURCE:
	case GOING_TO_OVERFLOW:
		double distance = new BlockPos(mc.player.posX, mc.player.posY, mc.player.posZ).getDistance(destination.offset(destinationOffset).getX(), destination.getY(), destination.offset(destinationOffset).getZ());
		if(distance == 0D) {
			mc.player.sendChatMessage("#cancel");
			stuckCheck = 0;
			
			//open chest
			double dx = destination.getX() - mc.player.posX + 0.5;
			double dy = destination.getY() - mc.player.posY + 0.5 - mc.player.eyeHeight;
			if(status == Status.GOING_TO_STORAGE) {
				dy += unit.getFillHeight();
				if(Block.getIdFromBlock(mc.world.getBlockState(destination.up(unit.getFillHeight()).offset(unit.getFacing())).getBlock()) == 68) {
					if(unit.getFillHeight() == 0) dy += 0.375;
					else if(unit.getFillHeight() == 1 || unit.getFillHeight() == 2) dy -= 0.375;
					else if(unit.getFillHeight() == 3) dy -= 0.3125;
				}
			}
			double dz = destination.getZ() - mc.player.posZ + 0.5;
			
			double yawToPlayer = -Math.atan2(dx, dz) + Math.PI;
			double edgeOfBlockRadius = Math.min(Math.abs(1 / Math.cos(yawToPlayer)), Math.abs(1 / Math.sin(yawToPlayer))) / 2;
			dx += -Math.sin(yawToPlayer) * edgeOfBlockRadius;
			dz += Math.cos(yawToPlayer) * edgeOfBlockRadius;
			
			double r = Math.sqrt(dx * dx + dy * dy + dz * dz);
			double yaw = -Math.atan2(dx, dz) / Math.PI * 180;
			if(yaw < 0) yaw = 360 + yaw;
			double pitch = -Math.asin(dy / r) / Math.PI * 180;
			mc.player.connection.sendPacket(new CPacketPlayer.Rotation((float) yaw, (float) pitch, mc.player.onGround));
			
			Vec3d vec3d = mc.player.getPositionEyes(1.0F);
	        Vec3d vec3d1 = Chung.getVectorForRotation((float) pitch, (float) yaw);
	        Vec3d vec3d2 = vec3d.addVector(vec3d1.x * 4.5D, vec3d1.y * 4.5D, vec3d1.z * 4.5D);
	        RayTraceResult rayTrace = mc.world.rayTraceBlocks(vec3d, vec3d2, false, false, true);
			
			BlockPos pos1 = rayTrace.getBlockPos();
			Vec3d vec1 = rayTrace.hitVec;
			float f = (float)(vec1.x - (double)pos1.getX());
	        float f1 = (float)(vec1.y - (double)pos1.getY());
	        float f2 = (float)(vec1.z - (double)pos1.getZ());
			connection.sendPacket(new CPacketPlayerTryUseItemOnBlock(pos1, rayTrace.sideHit, EnumHand.MAIN_HAND, f, f1, f2));
			
			if(status == Status.GOING_TO_STORAGE) status = Status.WAITING_ON_STORAGE;
			else if(status == Status.GOING_TO_SOURCE) status = Status.WAITING_ON_SOURCE;
			else status = Status.WAITING_ON_OVERFLOW;
			
			prevDistance = 0D;
			nextTick = nextTick((Byte) Settings.get("tick_delay"));
		} else {
			if(distance == prevDistance) {
				if(stuckCheck + distance * 5 > 127) stuckCheck = 127;
				else stuckCheck += distance * 5;
			}
			prevDistance = distance;
			nextTick = nextTick((int) (distance * 5));
		}
		break;
	case WAITING_ON_SOURCE:
		if(mc.player.openContainer.windowId == 0) {
			stuckCheck++;
			nextTick = nextTick(1);
			break;
		} else {
			stuckCheck = 0;
			slot = -1;
			moveSlot = 0;
			status = Status.TAKING_SHULKERS;
			log.info("[AutoSort] Taking shulkers");
			if((short) Settings.get("autosort", "chest_open_tick_delay") > 0) {
				nextTick = nextTick((short) Settings.get("autosort", "chest_open_tick_delay"));
				break;
			}
		}
	case TAKING_SHULKERS:
		if(slot == (sourceDoubleChest ? 54 : 27)) {
			if(moveSlot == 0) {
				status = Status.SOURCE_EMPTY_TIMEOUT;
				if(sourceEmptyTimeout != 0) {
					nextTick = nextTick(sourceEmptyTimeout * 40);
					break;
				}
			} else {
				mc.player.closeScreen();
				slot = 9;
				status = Status.SEARCHING_STORAGE;
				log.info("[AutoSort] Going to storage");
			}
		} else {
			for(slot++; slot < (sourceDoubleChest ? 54 : 27); slot++) {
				Slot thisSlot = mc.player.openContainer.getSlot(slot);
				if(thisSlot.getHasStack()) {
					int itemID = Item.getIdFromItem(thisSlot.getStack().getItem());
					if(itemID > 218 && itemID < 235) {
						connection.sendPacket(new CPacketClickWindow(mc.player.openContainer.windowId, slot, 0, ClickType.QUICK_MOVE, thisSlot.getStack(), mc.player.openContainer.getNextTransactionID(mc.player.inventory)));
						moveSlot++;
						if(moveSlot == inventorySpace) slot = (byte) (sourceDoubleChest ? 54 : 27);
						break;
					}
				}
			}
		}
		nextTick = nextTick((Byte) Settings.get("tick_delay"));
		break;
	case SEARCHING_STORAGE:
		if(mc.player.openContainer.windowId == 0) {
			stuckCheck = 0;
			for(; slot < 45; slot++) {
				Slot thisSlot = mc.player.openContainer.getSlot(slot);
				if(thisSlot.getHasStack()) {
					int itemID = Item.getIdFromItem(thisSlot.getStack().getItem());
					if(itemID > 218 && itemID < 235) {
						for(StorageUnit unit:sortLocations) {
							if((unit.getName()).equals(thisSlot.getStack().getDisplayName())) {
								log.debug("[AutoSort] Going to storage for: " + thisSlot.getStack().getDisplayName());
								this.unit = unit;
								status = Status.GOING_TO_STORAGE;
								break;
							}
						}
						if(status == Status.GOING_TO_STORAGE) break;
						else goToOverflow = true;
					}
				}
			}
			if(status == Status.GOING_TO_STORAGE) {
				destination = unit.getBlockPos();
				destinationOffset = unit.getFacing();
			} else {
				if(goToOverflow && overflow != null) {
					status = Status.GOING_TO_OVERFLOW;
					log.info("[AutoSort] Going to overflow");
					destination = overflow;
					destinationOffset = overflowFacing;
					goToOverflow = false;
				} else {
					status = Status.GOING_TO_SOURCE;
					log.info("[AutoSort] Going to source");
					destination = source;
					destinationOffset = sourceFacing;
				}
			}
			mc.player.sendChatMessage("#goto ~" + (destination.offset(destinationOffset).getX() - (int) Math.floor(mc.player.posX)) + " ~" + (destination.getY() - (int) Math.floor(mc.player.posY)) + " ~" + (destination.offset(destinationOffset).getZ() - (int) Math.floor(mc.player.posZ)));
		} else stuckCheck++;
		nextTick = nextTick(1);
		break;
	case WAITING_ON_STORAGE:
		if(mc.player.openContainer.windowId == 0) {
			stuckCheck++;
			nextTick = nextTick(1);
			break;
		} else {
			stuckCheck = 0;
			if(mc.player.openContainer.getSlot(unit.isDoubleChest() ? 53 : 26).getHasStack() && mc.player.openContainer.getSlot(0).getHasStack()) {
				mc.player.closeScreen();
				log.debug("[AutoSort] Chest full");
				if(unit.nextUp()) {
					status = Status.GOING_TO_STORAGE;
				} else {
					sortLocations.remove(unit);
					if(sortLocations.size() == 0) {
						Chung.sendMessage("[AutoSort] Storage full");
						stop();
						Chung.runningActivities.remove(this);
						return true;
					}
					status = Status.SEARCHING_STORAGE;
				}
				nextTick = nextTick(1);
				break;
			} else {
				moveSlot = (byte) (slot + (unit.isDoubleChest() ? 44 : 17));
				status = Status.PUTTING_SHULKERS;
				log.debug("Putting shulkers: " + unit.getName());
				if((short) Settings.get("autosort", "chest_open_tick_delay") > 0) {
					nextTick = nextTick((short) Settings.get("autosort", "chest_open_tick_delay"));
					break;
				}
			}
		}
	case PUTTING_SHULKERS:
		for(; moveSlot < (unit.isDoubleChest() ? 90 : 63); moveSlot++) {
			Slot thisSlot = mc.player.openContainer.getSlot(moveSlot);
			if(thisSlot.getHasStack()) {
				int itemID = Item.getIdFromItem(thisSlot.getStack().getItem());
				if(itemID > 218 && itemID < 235 && thisSlot.getStack().getDisplayName().equals(unit.getName())) {
					connection.sendPacket(new CPacketClickWindow(mc.player.openContainer.windowId, moveSlot, 0, ClickType.QUICK_MOVE, thisSlot.getStack(), mc.player.openContainer.getNextTransactionID(mc.player.inventory)));
					break;
				}
			}
		}
		if(moveSlot == (unit.isDoubleChest() ? 90 : 63)) {
			mc.player.closeScreen();
			status = Status.SEARCHING_STORAGE;
		} else if(mc.player.openContainer.getSlot(unit.isDoubleChest() ? 53 : 26).getHasStack()) status = Status.WAITING_ON_STORAGE;
		nextTick = nextTick((Byte) Settings.get("tick_delay"));
		break;
	case WAITING_ON_OVERFLOW:
		if(mc.player.openContainer.windowId == 0) {
			stuckCheck++;
			nextTick = nextTick(1);
			break;
		} else {
			stuckCheck = 0;
			slot = (byte) (overflowDoubleChest ? 53 : 26);
			status = Status.DUMPING_SHULKERS;
			log.debug("[AutoSort] Dumping shulkers");
			if((short) Settings.get("autosort", "chest_open_tick_delay") > 0) {
				nextTick = nextTick((short) Settings.get("autosort", "chest_open_tick_delay"));
				break;
			}
		}
	case DUMPING_SHULKERS:
		for(slot++; slot < (overflowDoubleChest ? 90 : 63); slot++) {
			Slot thisSlot = mc.player.openContainer.getSlot(slot);
			if(thisSlot.getHasStack()) {
				int itemID = Item.getIdFromItem(thisSlot.getStack().getItem());
				if(itemID > 218 && itemID < 235) {
					connection.sendPacket(new CPacketClickWindow(mc.player.openContainer.windowId, slot, 0, ClickType.QUICK_MOVE, thisSlot.getStack(), mc.player.openContainer.getNextTransactionID(mc.player.inventory)));
					break;
				}
			}
		}
		if(slot == (overflowDoubleChest ? 90 : 63)) {
			mc.player.closeScreen();
			slot = 9;
			status = Status.SEARCHING_STORAGE;
		} else if(mc.player.openContainer.getSlot(overflowDoubleChest ? 53 : 26).getHasStack() && mc.player.openContainer.getSlot(0).getHasStack()) {
			Chung.sendMessage("[AutoSort] Overflow chest full");
			stop();
			Chung.runningActivities.remove(this);
			return true;
		}
		nextTick = nextTick((Byte) Settings.get("tick_delay"));
		break;
	case SOURCE_EMPTY_TIMEOUT:
		Slot thisSlot = mc.player.openContainer.getSlot(0);
		if(thisSlot.getHasStack()) {
			int itemID = Item.getIdFromItem(thisSlot.getStack().getItem());
			if(itemID > 218 && itemID < 235) {
				slot = -1;
				moveSlot = 0;
				status = Status.TAKING_SHULKERS;
				break;
			}
		}
		mc.player.closeScreen();
		Chung.sendMessage("[AutoSort] Finished sorting");
		stop();
		Chung.runningActivities.remove(this);
		return true;
	}
	} catch(Exception e) {
	log.catching(Level.ERROR, e);
	Chung.sendMessage("Exception in AutoSort: " + e.getClass().getName() + (e.getMessage() == null ? "" : ": " + e.getMessage()));
	stop();
	Chung.runningActivities.remove(this);
	return true;
	}
	if(stuckCheck == 127) {
		Chung.sendMessage("AutoSort stuck in status: " + status);
		log.warn("AutoSort stuck\nStatus: {}\nStorage chests: {}\nDestination: {}\nPlayer window ID: {}\nDistance to destination: {}\nSlot: {}\nMoveSlot: {}", status, sortLocations, destination, mc.player.openContainer.windowId, prevDistance, slot, moveSlot);
		stop();
		Chung.runningActivities.remove(this);
		Chung.runningActivities.add(new AutoSort(mc, sortLocations, source, sourceEmptyTimeout, overflow, log));
		return true;
	}
}
return false;
}

@Override
public void stop() {
	log.info("[AutoSort] Stopping AutoSort");
	if(status.equals(Status.GOING_TO_SOURCE) || status.equals(Status.GOING_TO_STORAGE) || status.equals(Status.GOING_TO_OVERFLOW)) mc.player.sendChatMessage("#cancel");
}

public static boolean isChest(int blockID) {
	return (blockID == 54 || blockID == 146);
}

private enum Status {
	GOING_TO_SOURCE, WAITING_ON_SOURCE, TAKING_SHULKERS, SEARCHING_STORAGE, GOING_TO_STORAGE, WAITING_ON_STORAGE, PUTTING_SHULKERS, GOING_TO_OVERFLOW, WAITING_ON_OVERFLOW, DUMPING_SHULKERS, SOURCE_EMPTY_TIMEOUT
}

}