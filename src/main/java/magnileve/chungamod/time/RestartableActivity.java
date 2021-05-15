package magnileve.chungamod.time;

import net.minecraft.util.math.BlockPos;

public interface RestartableActivity extends Activity {

/**
 * Called when the client has reconnected to the world where the activity was running.
 */
public void restart();

/**
 * Gets a BlockPos having to do with the activity in order to determine if the player has logged on at the same place where the activity was running.
 * @return the BlockPos having to do with the activity
 */
public BlockPos getNearbyPosition();

}