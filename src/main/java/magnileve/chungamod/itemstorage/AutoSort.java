package magnileve.chungamod.itemstorage;

import java.util.LinkedList;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import magnileve.chungamod.Ref;
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

private Minecraft mc;
private Logger log;
private NetHandlerPlayClient connection;
private LinkedList<StorageUnit> sortLocations;
private Status status;
private BlockPos source;
private BlockPos overflow;
private BlockPos destination;
private StorageUnit unit;
private int nextTick;
private short sourceEmptyTimeout;
private byte slot;
private byte moveSlot;
private byte inventorySpace;
private EnumFacing sourceFacing;
private EnumFacing overflowFacing;
private EnumFacing destinationOffset;
private boolean sourceDoubleChest;
private boolean overflowDoubleChest;
private boolean goToOverflow;

public AutoSort(Minecraft minecraft, BlockPos pos1, BlockPos pos2, BlockPos shulkerSource, short sourceEmptyTimeout, BlockPos shulkerOverflow, Logger logger) {
	mc = minecraft;
	connection = mc.player.connection;
	log = logger;
	source = shulkerSource;
	this.sourceEmptyTimeout = sourceEmptyTimeout;
	overflow = shulkerOverflow;
	if(source.getDistance((int) mc.player.posX, (int) mc.player.posY, (int) mc.player.posZ) > 250 ||
			(overflow == null ? false : overflow.getDistance((int) mc.player.posX, (int) mc.player.posY, (int) mc.player.posZ) > 250) ||
			pos1.getDistance((int) mc.player.posX, (int) mc.player.posY, (int) mc.player.posZ) > 250 ||
			pos2.getDistance((int) mc.player.posX, (int) mc.player.posY, (int) mc.player.posZ) > 250) {
		Ref.sendMessage("AutoSort error: Chests too far away");
		Ref.runningActivities.remove(this);
		return;
	}
	sortLocations = new LinkedList<>();
	goToOverflow = false;
	log.info("[AutoSort] Running AutoSort");
	for(IProperty<?> property:mc.world.getBlockState(source).getBlock().getBlockState().getProperties()) if(property.getName().equals("facing")) {
		sourceFacing = (EnumFacing) mc.world.getBlockState(source).getValue(property);
		sourceDoubleChest = (isChest(Block.getIdFromBlock(mc.world.getBlockState(source.offset(sourceFacing.rotateY())).getBlock())) ||
			isChest(Block.getIdFromBlock(mc.world.getBlockState(source.offset(sourceFacing.rotateYCCW())).getBlock())));
		break;
	}
	if(overflow != null) for(IProperty<?> property:mc.world.getBlockState(overflow).getBlock().getBlockState().getProperties()) if(property.getName().equals("facing")) {
		overflowFacing = (EnumFacing) mc.world.getBlockState(overflow).getValue(property);
		overflowDoubleChest = (isChest(Block.getIdFromBlock(mc.world.getBlockState(overflow.offset(overflowFacing.rotateY())).getBlock())) ||
			isChest(Block.getIdFromBlock(mc.world.getBlockState(overflow.offset(overflowFacing.rotateYCCW())).getBlock())));
		break;
	}
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
					for(char character:shulkerName.toCharArray()) log.debug(character);
				}
				break;
				}
			}
		}
	}
	for(byte i = 9; i < 45; i++) {
		Slot thisSlot = mc.player.openContainer.getSlot(i);
		if(thisSlot.getHasStack()) {
			int itemID = Item.getIdFromItem(thisSlot.getStack().getItem());
			if(itemID > 218 && itemID < 235 && overflow != null) inventorySpace++;
		} else inventorySpace++;
	}
	slot = 8;
	status = Status.SEARCHING_STORAGE;
	TickTimer.addListener(this);
	nextTick(1);
}

@Override
public void onTick(int tick) {
if(tick == nextTick) {
	try {
	switch(status) {
	case GOING_TO_STORAGE:
	case GOING_TO_SOURCE:
	case GOING_TO_OVERFLOW:
		double distance = new BlockPos(mc.player.posX, mc.player.posY, mc.player.posZ).getDistance(destination.offset(destinationOffset).getX(), destination.getY(), destination.offset(destinationOffset).getZ());
		if(distance == 0D) {
			mc.player.sendChatMessage("#cancel");
			
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
			log.debug("Start chest coords: " + (dx + mc.player.posX) + ", " + (dy + mc.player.posY) + ", " + (dz + mc.player.posZ));
			
			double yawToPlayer = -Math.atan2(dx, dz) + Math.PI;
			double edgeOfBlockRadius = Math.min(Math.abs(1 / Math.cos(yawToPlayer)), Math.abs(1 / Math.sin(yawToPlayer))) / 2;
			dx += -Math.sin(yawToPlayer) * edgeOfBlockRadius;
			dz += Math.cos(yawToPlayer) * edgeOfBlockRadius;
			log.debug("Shifted chest coords: " + (dx + mc.player.posX) + ", " + (dy + mc.player.posY) + ", " + (dz + mc.player.posZ));
			
			double r = Math.sqrt(dx * dx + dy * dy + dz * dz);
			double yaw = -Math.atan2(dx, dz) / Math.PI * 180;
			if(yaw < 0) yaw = 360 + yaw;
			double pitch = -Math.asin(dy / r) / Math.PI * 180;
			mc.player.connection.sendPacket(new CPacketPlayer.Rotation((float) yaw, (float) pitch, mc.player.onGround));
			
			Vec3d vec3d = mc.player.getPositionEyes(1.0F);
	        Vec3d vec3d1 = Ref.getVectorForRotation((float) pitch, (float) yaw);
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
			nextTick(1);
		} else nextTick((int) (distance * 5));
		break;
	case WAITING_ON_SOURCE:
		if(mc.player.openContainer.windowId == 0) {
			nextTick(1);
			break;
		} else {
			slot = -1;
			moveSlot = 0;
			status = Status.TAKING_SHULKERS;
			log.debug("[AutoSort] Taking shulkers");
		}
	case TAKING_SHULKERS:
		if(slot == (sourceDoubleChest ? 54 : 27)) {
			if(moveSlot == 0) {
				status = Status.SOURCE_EMPTY_TIMEOUT;
				if(sourceEmptyTimeout != 0) {
					nextTick(sourceEmptyTimeout * 40);
					break;
				}
			} else {
				//connection.sendPacket(new CPacketCloseWindow(mc.player.openContainer.windowId));
				mc.player.closeScreen();
				slot = 8;
				status = Status.SEARCHING_STORAGE;
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
		nextTick(2);
		break;
	case SEARCHING_STORAGE:
		if(mc.player.openContainer.windowId == 0) {
			for(slot++; slot < 45; slot++) {
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
					log.debug("[AutoSort] Going to overflow");
					destination = overflow;
					destinationOffset = overflowFacing;
					goToOverflow = false;
				} else {
					status = Status.GOING_TO_SOURCE;
					log.debug("[AutoSort] Going to source");
					destination = source;
					destinationOffset = sourceFacing;
				}
			}
			mc.player.sendChatMessage("#goto ~" + (destination.offset(destinationOffset).getX() - (int) Math.floor(mc.player.posX)) + " ~" + (destination.getY() - (int) Math.floor(mc.player.posY)) + " ~" + (destination.offset(destinationOffset).getZ() - (int) Math.floor(mc.player.posZ)));
		}
		nextTick(1);
		break;
	case WAITING_ON_STORAGE:
		if(mc.player.openContainer.windowId == 0) {
			nextTick(1);
			break;
		} else {
			if(mc.player.openContainer.getSlot(unit.doubleChest() ? 53 : 26).getHasStack()) {
				mc.player.closeScreen();
				log.debug("[AutoSort] Chest full");
				if(unit.nextUp()) {
					status = Status.GOING_TO_STORAGE;
				} else {
					sortLocations.remove(unit);
					if(sortLocations.size() == 0) {
						Ref.sendMessage("[AutoSort] Storage full");
						stop();
						Ref.runningActivities.remove(this);
						break;
					}
					status = Status.SEARCHING_STORAGE;
				}
				nextTick(1);
				break;
			} else {
				log.debug("Slot: " + slot + (unit.doubleChest() ? 44 : 17));
				moveSlot = (byte) (slot + (unit.doubleChest() ? 44 : 17));
				status = Status.PUTTING_SHULKERS;
				log.debug("Putting shulkers: " + unit.getName());
			}
		}
	case PUTTING_SHULKERS:
		for(moveSlot++; moveSlot < (unit.doubleChest() ? 90 : 63); moveSlot++) {
			Slot thisSlot = mc.player.openContainer.getSlot(moveSlot);
			if(thisSlot.getHasStack()) {
				int itemID = Item.getIdFromItem(thisSlot.getStack().getItem());
				log.debug("MoveSlot: " + moveSlot);
				log.debug(thisSlot.getStack().getDisplayName() + unit.getName());
				log.debug(thisSlot.getStack().getDisplayName().equals(unit.getName()));
				if(itemID > 218 && itemID < 235 && thisSlot.getStack().getDisplayName().equals(unit.getName())) {
					connection.sendPacket(new CPacketClickWindow(mc.player.openContainer.windowId, moveSlot, 0, ClickType.QUICK_MOVE, thisSlot.getStack(), mc.player.openContainer.getNextTransactionID(mc.player.inventory)));
					break;
				}
			}
		}
		if(moveSlot == (unit.doubleChest() ? 90 : 63)) {
			mc.player.closeScreen();
			status = Status.SEARCHING_STORAGE;
		} else if(mc.player.openContainer.getSlot(unit.doubleChest() ? 53 : 26).getHasStack()) status = Status.WAITING_ON_STORAGE;
		nextTick(2);
		break;
	case WAITING_ON_OVERFLOW:
		if(mc.player.openContainer.windowId == 0) {
			nextTick(1);
			break;
		} else {
			slot = (byte) (overflowDoubleChest ? 53 : 26);
			status = Status.DUMPING_SHULKERS;
			log.debug("[AutoSort] Dumping shulkers");
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
			slot = 8;
			status = Status.SEARCHING_STORAGE;
		} else if(mc.player.openContainer.getSlot(overflowDoubleChest ? 53 : 26).getHasStack()) {
			Ref.sendMessage("[AutoSort] Overflow chest full");
			stop();
			Ref.runningActivities.remove(this);
			break;
		}
		nextTick(2);
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
		Ref.sendMessage("[AutoSort] Finished sorting");
		stop();
		Ref.runningActivities.remove(this);
		break;
	}
	} catch(Exception e) {
	Ref.sendMessage("Exception in AutoSort: " + e.getClass().getName() + (e.getMessage() == null ? "" : ": " + e.getMessage()));
	log.catching(Level.ERROR, e);
	stop();
	Ref.runningActivities.remove(this);
	}
}
}

@Override
public void stop() {
	log.info("[AutoSort] Stopping AutoSort");
	if(status.equals(Status.GOING_TO_SOURCE) || status.equals(Status.GOING_TO_STORAGE)) mc.player.sendChatMessage("#cancel");
	TickTimer.removeListener(this);
}

private enum Status {
	GOING_TO_SOURCE, WAITING_ON_SOURCE, TAKING_SHULKERS, SEARCHING_STORAGE, GOING_TO_STORAGE, WAITING_ON_STORAGE, PUTTING_SHULKERS, GOING_TO_OVERFLOW, WAITING_ON_OVERFLOW, DUMPING_SHULKERS, SOURCE_EMPTY_TIMEOUT
}

private void nextTick(int tick) {
	nextTick = TickTimer.current() + tick;
	TickTimer.add(tick);
}

private boolean isChest(int blockID) {
	return (blockID == 54 || blockID == 146);
}

}