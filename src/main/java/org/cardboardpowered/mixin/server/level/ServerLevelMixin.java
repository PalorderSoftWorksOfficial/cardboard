package org.cardboardpowered.mixin.server.level;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

import com.javazilla.bukkitfabric.Utils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.server.level.progress.LoggingLevelLoadListener;
import net.minecraft.util.ProgressListener;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import org.bukkit.craftbukkit.CraftServer;
import org.cardboardpowered.CardboardConfig;
import org.cardboardpowered.CardboardMod;
import org.cardboardpowered.bridge.server.level.ServerLevelBridge;
import org.cardboardpowered.bridge.world.level.storage.LevelData_RespawnDataBridge;
import org.cardboardpowered.bridge.world.level.storage.PrimaryLevelDataBridge;
import org.cardboardpowered.mixin.world.level.LevelMixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class ServerLevelMixin extends LevelMixin implements ServerLevelBridge {

    private LevelStorageSource.LevelStorageAccess cardboard$session;
    private UUID cardboard$uuid;
    private LevelLoadListener cardboard$levelLoadListener;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void banner$initWorldServer(MinecraftServer minecraftserver, Executor executor, LevelStorageSource.LevelStorageAccess convertable_conversionsession, ServerLevelData iworlddataserver, ResourceKey<Level> resourcekey, LevelStem worlddimension, boolean flag, long i2, List<CustomSpawner> list, boolean flag1, RandomSequences randomsequences, CallbackInfo ci) {
        if (CardboardConfig.DEBUG_OTHER) {
            CardboardMod.LOGGER.info("Debug: getting world uuid");
        }

        this.cardboard$session = convertable_conversionsession;
        this.cardboard$uuid = Utils.getWorldUUID(cardboard$session.getDimensionPath(((ServerLevel)(Object)this).dimension()).toFile());
        this.cardboard$levelLoadListener = new LoggingLevelLoadListener(false);
    }

    @Inject(method = "save", at = @At("HEAD"))
    public void doWorldSaveEvent(ProgressListener aa, boolean bb, boolean cc, CallbackInfo ci) {
        if (!cc) {
            org.bukkit.Bukkit.getPluginManager().callEvent(new org.bukkit.event.world.WorldSaveEvent(this.cardboard$getWorld()));
        }
    }

    @Shadow
    public ServerLevelData serverLevelData;

    @Override
    public ServerLevelData cardboard_worldProperties() {
        return serverLevelData;
    }

    @Override
    public CraftServer getCraftServer() {
        return CraftServer.INSTANCE;
    }

    @Shadow
    private PersistentEntitySectionManager<Entity> entityManager;

    @Shadow
    public LevelEntityGetter<Entity> getEntities() {
        return this.entityManager.getEntityGetter();
    }

    @Shadow
    @Final
    private MinecraftServer server;

    @Override
    public void cardboard$set_uuid(UUID id) {
        this.cardboard$uuid = id;
    }

    @Override
    public UUID cardboard$get_uuid() {
        return this.cardboard$uuid;
    }

    @Override
    public LevelLoadListener cardboard$levelLoadListener() {
        return cardboard$levelLoadListener;
    }

    @Inject(method = "setRespawnData", at = @At("HEAD"), cancellable = true)
    public void setRespawnDataPaper(LevelData.RespawnData respawnData, CallbackInfo ci) {
        if (!((LevelData_RespawnDataBridge)(Object)this.serverLevelData.getRespawnData()).cardboard$positionEquals(respawnData)) {
            org.bukkit.Location previousLocation = this.cardboard$getWorld().getSpawnLocation();
            this.serverLevelData.setSpawn(respawnData);
            this.server.getPlayerList().broadcastAll(new net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket(respawnData), this.dimension());
            this.server.updateEffectiveRespawnData();
            new org.bukkit.event.world.SpawnChangeEvent(this.cardboard$getWorld(), previousLocation).callEvent();
        }
        if (((PrimaryLevelDataBridge)this.server.overworld().serverLevelData).cardboard$getRespawnDimension() != this.dimension()) {
            ((PrimaryLevelDataBridge)this.server.overworld().serverLevelData).cardboard$setRespawnDimension(this.dimension());
            this.server.updateEffectiveRespawnData();
        }
        ci.cancel();
    }
}