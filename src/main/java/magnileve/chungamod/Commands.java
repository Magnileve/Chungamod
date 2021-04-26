package magnileve.chungamod;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collection;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import magnileve.chungamod.itemstorage.AutoSort;
import magnileve.chungamod.time.ClientTps;
import magnileve.chungamod.time.Activity;
import net.arikia.dev.drpc.DiscordEventHandlers;
import net.arikia.dev.drpc.DiscordRPC;
import net.arikia.dev.drpc.DiscordRichPresence;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.ClientChatEvent;

@Mod.EventBusSubscriber(modid=Ref.MODID)
public class Commands {

private static final long START_TIME = System.currentTimeMillis() / 1000;

private static Minecraft mc;
private static Logger log;
private static final String HELP_MESSAGE = "Chungamod \\version by Magnileve\nCommands:\n\\prefixcancel - cancel current activities\n\\prefixset <setting> <value> - set a setting\n\\prefixset <feature> <setting> <value> - set a setting of a feature\n\\prefixhelp - sends this message\n\\prefixautosort - automatically sort shulker boxes\n\\prefixblockdata - get block state of selected block\n\\prefixclienttps - measure and periodically send client TPS";

protected static void init(Minecraft minecraft, Logger logger) {
	mc = minecraft;
	log = logger;
}

public static void init2() {
	//initiate DiscordRPC
	if((Boolean) Settings.get("discordrpc")) {
		discordRPCstart();
	}
	java.lang.Runtime.getRuntime().addShutdownHook(new Thread() {
		@Override
		public void run() {
			if((Boolean) Settings.get("discordrpc")) DiscordRPC.discordShutdown();
		}
	});
}

@SubscribeEvent
@SideOnly(value = Side.CLIENT)
public static void onServerChatEvent(ClientChatEvent event) {
	if(event.getMessage().startsWith((String) Settings.get("prefix"))) {
		event.setCanceled(true);
		log.info("Chungamod command called: " + event.getMessage());
		String[] command = event.getMessage().split(" ");
		if (command[0].length() == ((String) Settings.get("prefix")).length()) mc.player.sendMessage(new TextComponentString(HELP_MESSAGE.replaceAll("\\\\prefix", (String) Settings.get("prefix")).replaceFirst("\\\\version", Ref.VERSION)));
		else switch (command[0].substring(((String) Settings.get("prefix")).length()).toLowerCase()) {
		case "help":
			mc.player.sendMessage(new TextComponentString(HELP_MESSAGE.replaceAll("\\n\\\\prefix", "\n" + (String) Settings.get("prefix")).replaceFirst("\\\\version", Ref.VERSION)));
			break;
		case "set":
			if(command.length > 1) {
			command[1] = command[1].toLowerCase();
			Object settingValue = Settings.getValue(command[1]);
			if(settingValue != null) {
			Settings.Type settingValueEnum = null;
			try {
				settingValueEnum = (Settings.Type) settingValue;
			} catch(ClassCastException e) {
				
			}
			if(settingValueEnum == null) {
				Object[] featureSettings = (Object[]) settingValue;
				if(command.length > 2) {
					byte i = 1;
					command[2] = command[2].toLowerCase();
					for(String setting:((String) featureSettings[0]).split(" ")) {
						if(setting.equals(command[2])) {
							switch((Settings.Type) featureSettings[i]) {
							case STRING:
								if(command.length > 3) {
									String newString = Ref.inverseSplit(command, " ").substring(command[0].length() + command[1].length() + 2);
									Settings.set(command[1], command[2], newString);
									Ref.sendMessage("Set " + command[1] + "." + command[2] + " to " + newString);
								} else Ref.sendMessage(Settings.get(command[1], command[2]).toString());
								break;
							case BOOLEAN:
								if(command.length > 3) {
									if(command[3].toLowerCase().equals("true") || command[3].toLowerCase().equals("false")) {
										Settings.set(command[1], command[2], Boolean.valueOf(command[3]));
										Ref.sendMessage("Set " + command[1] + "." + command[2] + " to " + command[3]);
									} Ref.sendMessage("Valid setting values: true, false");
								} else Ref.sendMessage(Settings.get(command[1], command[2]).toString());
								break;
							case BLOCKPOS:
								try {
									BlockPos newPos;
									if (command.length > 3) {
										newPos = new BlockPos(Integer.valueOf(command[3]), Integer.valueOf(command[4]), Integer.valueOf(command[5]));
									} else {
										newPos = mc.getRenderViewEntity().rayTrace(4.5D, 1.0F).getBlockPos();
										if(Block.getIdFromBlock(mc.world.getBlockState(newPos).getBlock()) == 0) {
											Ref.sendMessage(Settings.get(command[1], command[2]).toString());
											break;
										}
									}
									Settings.set(command[1], command[2], newPos);
									Ref.sendMessage("Set " + command[1] + "." + command[2] + " to " + newPos);
								} catch (IndexOutOfBoundsException e) {
									Ref.sendMessage("Look at a block while setting this value or supply coordinates: " + command[0] + " " + command[1] + " " + command[2] + " <x> <y> <z>");
								} catch (NumberFormatException e) {
									Ref.sendMessage("Look at a block while setting this value or supply coordinates: " + command[0] + " " + command[1] + " " + command[2] + " <x> <y> <z>");
								} catch (NullPointerException e) {
									Ref.sendMessage("NullPointerException while setting BlockPos");
									log.warn("NullPointerException while setting BlockPos");
									log.catching(Level.WARN, e);
								}
								break;
							case SHORT:
								if(command.length == 4) {
									try {
										Short newNumber = Short.valueOf(command[3]);
										if(newNumber < 0) newNumber = 0;
										Settings.set(command[1], command[2], newNumber);
										Ref.sendMessage("Set " + command[1] + "." + command[2] + " to " + newNumber);
									} catch(NumberFormatException e) {
										Ref.sendMessage("Enter a number value");
									}
								} else Ref.sendMessage(Settings.get(command[1], command[2]).toString());
								break;
							case STRING_ONE_WORD:
								if(command.length == 4) {
									Settings.set(command[1], command[2], command[3]);
									Ref.sendMessage("Set " + command[1] + "." + command[2] + " to " + command[3]);
								} else if(command.length == 3) Ref.sendMessage(Settings.get(command[1], command[2]).toString());
								else Ref.sendMessage("Value can not contain spaces");
								break;
							case OBJECT_ARRAY:
								log.error("Enum representing object array in setting values");
								break;
							}
							i = 0;
							break;
						}
						i++;
					}
					if(i != 0) {
						Ref.sendMessage("Settings for " + command[1] + ": " + ((String) featureSettings[0]).replaceAll(" ", ", "));
					}
				} else Ref.sendMessage("Settings for " + command[1] + ": " + ((String) featureSettings[0]).replaceAll(" ", ", "));
			} else {
				switch(settingValueEnum) {
				case STRING:
					if(command.length > 2) {
						String newString = Ref.inverseSplit(command, " ").substring(command[0].length() + command[1].length() + 2);
						Settings.set(command[1], newString);
						Ref.sendMessage("Set " + command[1] + " to " + newString);
					} else Ref.sendMessage(Settings.get(command[1]).toString());
					break;
				case BOOLEAN:
					if(command.length > 2) {
						if(command[2].toLowerCase().equals("true") || command[2].toLowerCase().equals("false")) {
							Settings.set(command[1], Boolean.valueOf(command[2]));
							Ref.sendMessage("Set " + command[1] + " to " + command[2]);
						} Ref.sendMessage("Valid setting values: true, false");
					} else Ref.sendMessage(Settings.get(command[1]).toString());
					break;
				case BLOCKPOS:
					try {
						BlockPos newPos;
						if (command.length > 2) {
							newPos = new BlockPos(Integer.valueOf(command[2]), Integer.valueOf(command[3]), Integer.valueOf(command[4]));
						} else {
							newPos = mc.getRenderViewEntity().rayTrace(4.5D, 1.0F).getBlockPos();
							if(Block.getIdFromBlock(mc.world.getBlockState(newPos).getBlock()) == 0) {
								Ref.sendMessage(Settings.get(command[1]).toString());
								break;
							}
						}
						Settings.set(command[1], newPos);
						Ref.sendMessage("Set " + command[1] + " to " + newPos);
					} catch (IndexOutOfBoundsException e) {
						Ref.sendMessage("Look at a block while setting this value or supply coordinates: " + command[0] + " " + command[1] + " <x> <y> <z>");
					} catch (NumberFormatException e) {
						Ref.sendMessage("Look at a block while setting this value or supply coordinates: " + command[0] + " " + command[1] + " <x> <y> <z>");
					} catch (NullPointerException e) {
						Ref.sendMessage("NullPointerException while setting BlockPos");
						log.warn("NullPointerException while setting BlockPos");
						log.catching(Level.WARN, e);
					}
					break;
				case SHORT:
					if(command.length == 3) {
						try {
							Short newNumber = Short.valueOf(command[2]);
							if(newNumber < 0) newNumber = 0;
							Settings.set(command[1], newNumber);
							Ref.sendMessage("Set " + command[1] + " to " + newNumber);
						} catch(NumberFormatException e) {
							Ref.sendMessage("Enter a number value");
						}
					} else Ref.sendMessage(Settings.get(command[1]).toString());
					break;
				case STRING_ONE_WORD:
					if(command.length == 3) {
						Settings.set(command[1], command[2]);
						Ref.sendMessage("Set " + command[1] + " to " + command[2]);
					} else if(command.length == 2) Ref.sendMessage(Settings.get(command[1]).toString());
					else Ref.sendMessage("Value can not contain spaces");
					break;
				case OBJECT_ARRAY:
					log.error("Enum representing object array in setting values");
					break;
				}
			}
			}
			} else Ref.sendMessage("Enter a setting and value following your command");
		case "autosort":
			if(Settings.get("autosort", "pos1") != null && Settings.get("autosort", "pos2") != null && Settings.get("autosort", "source") != null) {
				Ref.sendMessage("Running AutoSort");
				Ref.runningActivities.add(new AutoSort(mc, (BlockPos) Settings.get("autosort", "pos1"), (BlockPos) Settings.get("autosort", "pos2"), (BlockPos) Settings.get("autosort", "source"), (Short) Settings.get("autosort", "sourceemptytimeout"), (BlockPos) Settings.get("autosort", "overflow"), log));
			}
			else Ref.sendMessage("Make sure to set AutoSort settings pos1, pos2, and source before running (use " + (String) Settings.get("prefix") + "set autosort <setting> <value>");
			break;
		case "cancel":
			Ref.sendMessage("Cancelling running activities");
			for(Activity activity:Ref.runningActivities) activity.stop();
			Ref.runningActivities.clear();
			break;
		case "clienttps":
			Ref.runningActivities.add(new ClientTps());
			break;
		case "blockdata":
			BlockPos blockPos = mc.getRenderViewEntity().rayTrace(4.5D, 1.0F).getBlockPos();
			for(IProperty<?> property:mc.world.getBlockState(blockPos).getBlock().getBlockState().getProperties()) {
				Ref.sendMessage(property.toString());
				Ref.sendMessage(property.getName());
				Ref.sendMessage(property.getClass().toString());
				Ref.sendMessage("property value: " + mc.world.getBlockState(blockPos).getValue(property));
			}
			if(Block.getIdFromBlock(mc.world.getBlockState(blockPos).getBlock()) == 68) {
				TileEntitySign tileEntity = (TileEntitySign) mc.world.getChunkFromBlockCoords(blockPos).getTileEntity(blockPos, Chunk.EnumCreateEntityType.CHECK);
				for(ITextComponent text:tileEntity.signText) Ref.sendMessage("Sign text: " + text);
			}
			break;
		
		default:
			if((Boolean) Settings.get("debug")) {
			switch (command[0].substring(((String) Settings.get("prefix")).length()).toLowerCase()) {
			case "cameradirection":
				Ref.sendMessage("Camera pitch: " + mc.player.cameraPitch + "\nCamera yaw: " + mc.player.cameraYaw + "\nRotation pitch: " + mc.player.rotationPitch + "\nRotation yaw: " + mc.player.rotationYaw);
				break;
			case "entitylist":
				for(Collection<?> collection:mc.world.getChunkFromBlockCoords(mc.player.getPosition()).getEntityLists()) {
					log.debug("\nCollection " + collection.getClass());
					for(Object o:collection) {
						log.debug(o.getClass().toString());
						log.debug(o.toString());
					}
				}
				break;
			case "playerpos":
				Ref.sendMessage(Ref.playerPos().toString());
				break;
			
			default:
				mc.player.sendMessage(new TextComponentString("Unknown command.  Try " + (String) Settings.get("prefix") + "help for a list of commands"));
			}
			} else mc.player.sendMessage(new TextComponentString("Unknown command.  Try " + (String) Settings.get("prefix") + "help for a list of commands"));
		}
	}
}

//setup DiscordRPC
private static void discordRPCstart() {
	DiscordRPC.discordInitialize("832742372420091964", new DiscordEventHandlers.Builder().build(), true);
	DiscordRPC.discordUpdatePresence(new DiscordRichPresence.Builder("Mincerfat client").setBigImage("chungustransparentcroppedlarge", "All hail Big Chungus").setDetails("The funniest").setStartTimestamps(START_TIME).build());
}

}