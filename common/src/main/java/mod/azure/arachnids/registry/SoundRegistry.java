package mod.azure.arachnids.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

import java.util.function.Supplier;

import mod.azure.arachnids.CommonMod;
import mod.azure.arachnids.services.ArachnidsServices;

public class SoundRegistry {

    public static Supplier<SoundEvent> CHARIOT_DEATH = registerSound("arachnids.chariot_death");

    public static Supplier<SoundEvent> CHARIOT_HURT = registerSound("arachnids.chariot_hurt");

    public static Supplier<SoundEvent> CHARIOT_IDLE = registerSound("arachnids.chariot_idle");

    public static Supplier<SoundEvent> CHARIOT_MOVING = registerSound("arachnids.chariot_moving");

    public static Supplier<SoundEvent> BRAINBUG_DEATH = registerSound("arachnids.brainbug_death");

    public static Supplier<SoundEvent> BRAINBUG_HURT = registerSound("arachnids.brainbug_hurt");

    public static Supplier<SoundEvent> BRAINBUG_IDLE = registerSound("arachnids.brainbug_idle");

    public static Supplier<SoundEvent> HOPPER_DEATH = registerSound("arachnids.hopper_death");

    public static Supplier<SoundEvent> HOPPER_HURT = registerSound("arachnids.hopper_hurt");

    public static Supplier<SoundEvent> HOPPER_IDLE = registerSound("arachnids.hopper_idle");

    public static Supplier<SoundEvent> HOPPER_MOVING = registerSound("arachnids.hopper_moving");

    public static Supplier<SoundEvent> WARRIOR_ATTACK = registerSound("arachnids.warrior_attack");

    public static Supplier<SoundEvent> WARRIOR_DEATH = registerSound("arachnids.warrior_death");

    public static Supplier<SoundEvent> WARRIOR_HURT = registerSound("arachnids.warrior_hurt");

    public static Supplier<SoundEvent> WARRIOR_IDLE = registerSound("arachnids.warrior_idle");

    public static Supplier<SoundEvent> WARRIOR_MOVING = registerSound("arachnids.warrior_moving");

    public static Supplier<SoundEvent> WORKER_ATTACK = registerSound("arachnids.worker_attack");

    public static Supplier<SoundEvent> WORKER_DEATH = registerSound("arachnids.worker_death");

    public static Supplier<SoundEvent> WORKER_HURT = registerSound("arachnids.worker_hurt");

    public static Supplier<SoundEvent> WORKER_IDLE = registerSound("arachnids.worker_idle");

    static Supplier<SoundEvent> registerSound(String soundName) {
        return ArachnidsServices.COMMON_REGISTRY.register(
            BuiltInRegistries.SOUND_EVENT,
            soundName,
            () -> SoundEvent.createVariableRangeEvent(CommonMod.modResource(soundName))
        );
    }

    public static void initialize() {}
}
