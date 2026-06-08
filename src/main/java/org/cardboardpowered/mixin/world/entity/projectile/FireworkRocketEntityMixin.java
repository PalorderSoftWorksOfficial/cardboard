package org.cardboardpowered.mixin.world.entity.projectile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.cardboardpowered.api.event.CardboardFireworkExplodeEvent;
import org.cardboardpowered.mixin.world.entity.EntityMixin;
import org.cardboardpowered.util.MixinInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@MixinInfo(events = {"FireworkExplodeEvent", "CardboradFireworkExplodeEvent"})
@Mixin(FireworkRocketEntity.class)
public class FireworkRocketEntityMixin extends EntityMixin {

    @Inject(method = "tick", cancellable = true, at = @At("HEAD"))
    private void bukkitFireworksExplode(CallbackInfo ci) {
        InteractionResult result = CardboardFireworkExplodeEvent.EVENT.invoker().interact((FireworkRocketEntity) (Object) this);

        if (result == InteractionResult.FAIL) {
            ci.cancel();
        }
    }

    @Inject(method = "dealExplosionDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private void bukkitDamageSource(ServerLevel world, CallbackInfo ci) {
        CraftEventFactory.entityDamage = (FireworkRocketEntity) (Object) this;
    }

    @Inject(method = "dealExplosionDamage", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/LivingEntity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private void bukkitDamageSourceReset(ServerLevel world, CallbackInfo ci) {
        CraftEventFactory.entityDamage = null;
    }

    /**
     * @param b
     */
    @Override
    public void cb$setInWorld(boolean b) {

    }

    /**
     * @return
     */
    @Override
    public boolean cb$getInWorld() {
        return false;
    }

    /**
     * @param ignoreClimbing
     * @return
     */
    @Override
    public boolean cardboard$isCollidable(boolean ignoreClimbing) {
        return false;
    }

    /**
     * @param entity
     * @return
     */
    @Override
    public boolean cardboard$canCollideWithBukkit(Entity entity) {
        return false;
    }

    /**
     * @return
     */
    @Override
    public float cardboard$getBukkitYaw() {
        return 0;
    }
}
