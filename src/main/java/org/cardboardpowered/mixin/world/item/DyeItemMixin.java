package org.cardboardpowered.mixin.world.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.event.entity.SheepDyeWoolEvent;
import org.cardboardpowered.bridge.world.entity.EntityBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DyeItem.class, priority = 5000)
public class DyeItemMixin {

    @Shadow
    public DyeColor dyeColor;

    @Inject(method = "interactLivingEntity", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings("deprecation")
    public void cardboard$interactLivingEntity(ItemStack itemstack, Player entityhuman, LivingEntity entityliving, InteractionHand enumhand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(entityliving instanceof Sheep entitysheep)) {
            return;
        }

        if (!entitysheep.isAlive() || entitysheep.isSheared() || entitysheep.getColor() == this.dyeColor) {
            return;
        }

        if (!entityhuman.level().isClientSide()) {
            byte bColor = (byte) this.dyeColor.getId();
            SheepDyeWoolEvent event = new SheepDyeWoolEvent((org.bukkit.entity.Sheep) ((EntityBridge) entitysheep).getBukkitEntity(), org.bukkit.DyeColor.getByWoolData(bColor));
            Bukkit.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                cir.setReturnValue(InteractionResult.PASS);
                return;
            }

            entitysheep.setColor(DyeColor.byId((byte) event.getColor().getWoolData()));
            itemstack.shrink(1);
        }

        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}