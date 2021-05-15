package magnileve.chungamod.time;

@FunctionalInterface
public interface TickListener {

/**
 * Called on scheduled client ticks.
 * @param tick the number of client ticks since client startup
 * @return false unless the tick listener should be removed
 */
public boolean onTick(int tick);

/**
 * Schedules a client tick for method onTick to be called during.
 * @param tick the number of ticks in the future for the tick being scheduled
 * @return the number of client ticks since client startup when onTick will be called
 */
default int nextTick(int tick) {
	TickTimer.add(tick);
	return TickTimer.current() + tick;
}

}