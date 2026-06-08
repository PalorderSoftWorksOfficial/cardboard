package org.cardboardpowered.mixin.world.entity;

import java.util.ArrayList;

import me.isaiah.common.entity.IRemoveReason;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Pose;
import org.bukkit.event.entity.EntityAirChangeEvent;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPoseChangeEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.cardboardpowered.bridge.commands.CommandSourceBridge;
import org.cardboardpowered.bridge.world.entity.EntityBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin implements CommandSourceBridge, EntityBridge {

    public CraftEntity bukkitEntity;
    public ProjectileSource projectileSource;
    private ArrayList<org.bukkit.inventory.ItemStack> drops = new ArrayList<>();
    private boolean forceDrops;
    public boolean valid = false;
    public boolean cardboard$inWorld = false;
    public Location origin_bukkit;

    @Shadow
    private Level level;

    @Shadow
    private EntityDimensions dimensions;

    @Override
    public ArrayList<org.bukkit.inventory.ItemStack> cardboard_getDrops() {
        return drops;
    }

    @Override
    public void cardboard_setDrops(ArrayList<org.bukkit.inventory.ItemStack> drops) {
        this.drops = drops;
    }

    @Override
    public AABB cardboad_getBoundingBoxAt(double x2, double y2, double z2) {
        return this.dimensions.makeBoundingBox(x2, y2, z2);
    }

    @Override
    public boolean cardboard_getForceDrops() {
        return forceDrops;
    }

    @Override
    public void cardboard_setForceDrops(boolean forceDrops) {
        this.forceDrops = forceDrops;
    }

    @Override
    public Level mc_world() {
        return level;
    }

    @Override
    public Location getOriginBF() {
        return origin_bukkit;
    }

    @Override
    public void setOriginBF(Location loc) {
        this.origin_bukkit = loc;
    }

    @Override
    public boolean isValidBF() {
        return valid;
    }

    @Override
    public void setValid(boolean b) {
        this.valid = b;
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"), method = "spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/entity/item/ItemEntity;")
    public boolean cardboard$mixinEntity_dropStack_EntityDropItemEvent(ServerLevel world, Entity entity, ServerLevel sworld, ItemStack itemstack, Vec3 offset) {
        if (itemstack.isEmpty()) {
            return false;
        }

        boolean chick = ((Entity) (Object) this) instanceof Chicken && itemstack.getItem() == Items.EGG;
        if (((Entity) (Object) this) instanceof net.minecraft.world.entity.LivingEntity && !this.forceDrops && !chick) {
            this.drops.add(org.bukkit.craftbukkit.inventory.CraftItemStack.asBukkitCopy(itemstack));
            return false;
        }

        ItemEntity entityitem = new ItemEntity(this.level, ((Entity) (Object) this).getX() + offset.x, ((Entity) (Object) this).getY() + offset.y, ((Entity) (Object) this).getZ() + offset.z, itemstack);
        entityitem.setDefaultPickUpDelay();

        EntityDropItemEvent event = new EntityDropItemEvent(this.getBukkitEntity(), (org.bukkit.entity.Item) ((EntityBridge) entityitem).getBukkitEntity());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        return this.level.addFreshEntity(entityitem);
    }

    @Override
    public CommandSender getBukkitSender(CommandSourceStack serverCommandSource) {
        return bukkitEntity;
    }

    @Override
    public CraftEntity getBukkitEntityRaw() {
        return bukkitEntity;
    }

    @Override
    public CraftEntity getBukkitEntity() {
        if (this.bukkitEntity == null) {
            synchronized (this) {
                if (this.bukkitEntity == null) {
                    return this.bukkitEntity = CraftEntity.getEntity(CraftServer.INSTANCE, (Entity) (Object) this);
                }
            }
        }
        return this.bukkitEntity;
    }

    @Inject(at = @At("HEAD"), method = "restoreFrom(Lnet/minecraft/world/entity/Entity;)V")
    public void cardboard$setBukkitHandleForCopy(Entity entity, CallbackInfo ci) {
        CraftEntity bukkitEntity = ((EntityBridge) entity).getBukkitEntityRaw();
        if (bukkitEntity != null) {
            bukkitEntity.setHandle((Entity) (Object) this);
            this.bukkitEntity = bukkitEntity;
        }
    }

    @Override
    public void setProjectileSourceBukkit(ProjectileSource source) {
        this.projectileSource = source;
    }

    @Override
    public ProjectileSource getProjectileSourceBukkit() {
        return projectileSource;
    }

    @Inject(at = @At("HEAD"), method = "setPose(Lnet/minecraft/world/entity/Pose;)V", cancellable = true)
    public void setPoseBF(net.minecraft.world.entity.Pose entitypose, CallbackInfo ci) {
        if (entitypose == ((Entity) (Object) this).getPose()) {
            ci.cancel();
            return;
        }

        Pose b = Pose.STANDING;
        switch (entitypose) {
            case CROUCHING:
                b = Pose.SNEAKING;
                break;
            case DYING:
                b = Pose.DYING;
                break;
            case FALL_FLYING:
                b = Pose.FALL_FLYING;
                break;
            case LONG_JUMPING:
                b = Pose.LONG_JUMPING;
                break;
            case SLEEPING:
                b = Pose.SLEEPING;
                break;
            case SPIN_ATTACK:
                b = Pose.SPIN_ATTACK;
                break;
            case STANDING:
                b = Pose.STANDING;
                break;
            case SWIMMING:
                b = Pose.SWIMMING;
                break;
            default:
                break;
        }
        Bukkit.getPluginManager().callEvent(new EntityPoseChangeEvent(this.getBukkitEntity(), b));
    }

    @ModifyVariable(method = "setAirSupply", at = @At("HEAD"), argsOnly = true)
    public int setAirBF(int i) {
        if (!valid) {
            return i;
        }

        EntityAirChangeEvent event = new EntityAirChangeEvent(this.getBukkitEntity(), i);
        event.getEntity().getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return i;
        }
        return event.getAmount();
    }

    public void removeBF() {
        ((me.isaiah.common.cmixin.IMixinEntity) this).Iremove(IRemoveReason.DISCARDED);
    }

    @Shadow
    public void move(MoverType moveType, Vec3 vec3d) {
    }

    @Shadow
    public boolean isPushable() {
        return false;
    }

    @Shadow
    public Level level() {
        return null;
    }

    @Shadow
    public float yRot;

    @Shadow
    public float getYRot() {
        return 0;
    }

    @Redirect(method = "lavaIgnite", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;igniteForSeconds(F)V"))
    public void cardboard$mixinEntity_igniteByLava_EntityCombustByBlockEvent(Entity entity, float seconds) {
        if ((Object) this instanceof LivingEntity && ((Entity) (Object) this).remainingFireTicks <= 0) {
            org.bukkit.block.Block damager = null;
            org.bukkit.entity.Entity damagee = this.getBukkitEntity();
            EntityCombustEvent combustEvent = new EntityCombustByBlockEvent(damager, damagee, 15);
            Bukkit.getPluginManager().callEvent(combustEvent);
            if (!combustEvent.isCancelled()) {
                ((Entity) (Object) this).igniteForSeconds(combustEvent.getDuration());
            }
        } else {
            ((Entity) (Object) this).igniteForSeconds(15);
        }
    }
}