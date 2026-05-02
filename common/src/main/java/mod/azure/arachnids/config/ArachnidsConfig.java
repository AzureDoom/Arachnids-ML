package mod.azure.arachnids.config;

import mod.azure.azurelib.common.config.Config;
import mod.azure.azurelib.common.config.Configurable;

import mod.azure.arachnids.CommonMod;

@Config(id = CommonMod.MOD_ID)
public class ArachnidsConfig {

    @Configurable
    public boolean debugPathingParticlesEnabled = false;

    @Configurable
    @Configurable.Synchronized
    public EntityConfigs entityConfigs = new EntityConfigs();

    public static class EntityConfigs {

        @Configurable
        @Configurable.Synchronized
        public float brainBugHealth = 200F;

        @Configurable
        @Configurable.Synchronized
        public float brainBugAttackDamage = 3F;

        @Configurable
        @Configurable.Synchronized
        public float chariotBugHealth = 10F;

        @Configurable
        @Configurable.Synchronized
        public float chariotBugAttackDamage = 1F;

        @Configurable
        @Configurable.Synchronized
        public float hopperBugHealth = 40F;

        @Configurable
        @Configurable.Synchronized
        public float hopperBugAttackDamage = 9F;

        @Configurable
        @Configurable.Synchronized
        public double hopperBugKnockbackRes = 0.8D;

        @Configurable
        @Configurable.Synchronized
        public float warriorBugHealth = 60F;

        @Configurable
        @Configurable.Synchronized
        public float warriorBugAttackDamage = 10F;

        @Configurable
        @Configurable.Synchronized
        public float warriorBugArmor = 14F;

        @Configurable
        @Configurable.Synchronized
        public float warriorBugArmorToughness = 4F;

        @Configurable
        @Configurable.Synchronized
        public double warriorBugKnockbackRes = 0.8D;

        @Configurable
        @Configurable.Synchronized
        public float workerBugHealth = 20F;

        @Configurable
        @Configurable.Synchronized
        public float workerBugAttackDamage = 7F;
    }
}
