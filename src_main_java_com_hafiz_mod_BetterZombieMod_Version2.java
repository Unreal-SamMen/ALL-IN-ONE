package com.hafiz.mod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.Monster;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * Better Zombie - Fabric 1.20.1 mod entrypoint.
 *
 * Feature implemented: When it's night time (in-game ticks 13000..23000) all hostile mobs (instances of Monster)
 * have their movement speed doubled via an attribute modifier. The modifier is removed when daytime returns.
 *
 * Notes:
 * - This applies to all vanilla and mod hostile mobs that extend net.minecraft.entity.mob.Monster.
 * - The speed change is implemented using a MULTIPLY_TOTAL attribute modifier with value 1.0 (i.e. +100%).
 */
public class BetterZombieMod implements ModInitializer {
    public static final String MODID = "betterzombie";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    // Stable UUID so the same modifier can be found and removed later
    private static final UUID NIGHT_SPEED_MOD_UUID = UUID.fromString("6f2a3c04-8d2a-4b3c-9c3b-1a2f5e6d7c88");
    private static final String NIGHT_SPEED_MOD_NAME = "betterzombie:night_speed";

    // Night window (inclusive) in Minecraft day ticks (0..23999)
    private static final long NIGHT_START = 13000L;
    private static final long NIGHT_END = 23000L;

    @Override
    public void onInitialize() {
        LOGGER.info("Better Zombie (1.20.1) initializing — night speed feature registering.");

        // Run at end of each server tick: check worlds and apply/remove modifier to hostile mobs.
        ServerTickEvents.END_SERVER_TICK.register(this::onEndServerTick);
    }

    private void onEndServerTick(MinecraftServer server) {
        // Iterate worlds on the server
        for (ServerWorld world : server.getWorlds()) {
            long timeOfDay = world.getTimeOfDay() % 24000L;
            boolean isNight = (timeOfDay >= NIGHT_START && timeOfDay <= NIGHT_END);

            // Get all hostile mobs (classes that extend Monster)
            List<Monster> hostileMobs = world.getEntitiesByClass(Monster.class, mob -> true);

            for (Monster mob : hostileMobs) {
                EntityAttributeInstance inst = mob.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
                if (inst == null) continue;

                EntityAttributeModifier existing = inst.getModifier(NIGHT_SPEED_MOD_UUID);

                if (isNight) {
                    // If it's night and the modifier isn't present, add it to double speed
                    if (existing == null) {
                        EntityAttributeModifier mod = new EntityAttributeModifier(
                                NIGHT_SPEED_MOD_UUID,
                                NIGHT_SPEED_MOD_NAME,
                                1.0, // MULTIPLY_TOTAL by 1.0 -> +100% speed (2x)
                                EntityAttributeModifier.Operation.MULTIPLY_TOTAL
                        );
                        inst.addPersistentModifier(mod);
                    }
                } else {
                    // Daytime: remove modifier if present
                    if (existing != null) {
                        inst.removeModifier(NIGHT_SPEED_MOD_UUID);
                    }
                }
            }
        }
    }
}