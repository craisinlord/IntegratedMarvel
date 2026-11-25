package com.craisinlord.integrated_superheroes.boss.command;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

public class StormCommandBlockEntity extends BlockEntity {
    private final Runner commandBlock = new Runner();
    private boolean powered;
    private boolean automatic;
    private boolean conditionMet;
    private boolean used;
    private boolean running;
    private boolean lockedOnClient;
    private long lastRun = -1L;
    @Nullable
    private UUID owner;

    public StormCommandBlockEntity(BlockPos pos, BlockState state) {
        super(StormCommandBlocks.BLOCK_ENTITY.get(), pos, state);
    }

    public BaseCommandBlock getCommandBlock() {
        return this.commandBlock;
    }

    public boolean isPowered() {
        return this.powered;
    }

    public void setPowered(boolean powered) {
        this.powered = powered;
    }

    public boolean isAutomatic() {
        return this.automatic;
    }

    public boolean isUsed() {
        return this.used;
    }

    public boolean isConditional() {
        BlockState state = this.getBlockState();
        return state.hasProperty(StormCommandBlock.CONDITIONAL) && state.getValue(StormCommandBlock.CONDITIONAL);
    }

    @Nullable
    public UUID getOwner() {
        return this.owner;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
        this.setChanged();
    }

    public boolean canDrop() {
        return !this.used && !this.running;
    }

    public boolean isLockedFor(Player player) {
        boolean locked = this.level != null && this.level.isClientSide ? this.lockedOnClient : StormCommandBlockConfig.ownerOnly();
        return locked && this.owner != null && !this.owner.equals(player.getUUID()) && !player.canUseGameMasterBlocks();
    }

    public void setAutomatic(boolean automatic) {
        boolean was = this.automatic;
        this.automatic = automatic;
        if (!was && automatic && !this.powered && this.level != null && !this.level.isClientSide) {
            this.scheduleRun();
        }
    }

    public void scheduleRun() {
        if (this.level == null) {
            return;
        }
        this.conditionMet = this.checkCondition();
        this.level.scheduleTick(this.worldPosition, this.getBlockState().getBlock(), 1);
    }

    private boolean checkCondition() {
        if (!this.isConditional() || this.level == null) {
            return true;
        }
        Direction facing = this.getBlockState().getValue(StormCommandBlock.FACING);
        BlockPos behind = this.worldPosition.relative(facing.getOpposite());
        if (this.level.getBlockState(behind).getBlock() instanceof CommandBlock
                && this.level.getBlockEntity(behind) instanceof CommandBlockEntity vanilla) {
            return vanilla.getCommandBlock().getSuccessCount() > 0;
        }
        return false;
    }

    @Nullable
    public Component blockedCommand() {
        if (!(this.level instanceof ServerLevel level) || StringUtil.isNullOrEmpty(this.commandBlock.getCommand())) {
            return null;
        }
        return StormCommandFilter.blocked(level.getServer().getCommands().getDispatcher(), this.commandBlock.createCommandSourceStack(), this.commandBlock.getCommand());
    }

    public void applyEdit(String command, boolean trackOutput, boolean automatic) {
        this.commandBlock.setCommand(command);
        this.commandBlock.setTrackOutput(trackOutput);
        if (!trackOutput) {
            this.commandBlock.setLastOutput(null);
        }
        this.setAutomatic(automatic);
        this.commandBlock.onUpdated();
        this.setChanged();
    }

    public boolean fire(ServerLevel level) {
        if (this.running || this.isRemoved()) {
            return false;
        }
        if (this.used) {
            return true;
        }
        if (!StormCommandBlockConfig.enabled() || level.getGameTime() == this.lastRun) {
            return false;
        }
        if (!this.conditionMet) {
            if (this.isConditional()) {
                this.commandBlock.setSuccessCount(0);
            }
            return false;
        }
        if (StringUtil.isNullOrEmpty(this.commandBlock.getCommand())) {
            this.commandBlock.setSuccessCount(0);
            return false;
        }
        this.lastRun = level.getGameTime();
        this.used = true;
        this.running = true;
        this.setChanged();
        int successes;
        try {
            successes = this.commandBlock.run(level);
        } finally {
            this.running = false;
        }
        if (successes > 0) {
            return true;
        }
        if (!this.isRemoved()) {
            this.used = false;
            this.setChanged();
        }
        return false;
    }

    public CompoundTag itemData(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        this.commandBlock.save(tag, registries);
        tag.remove("CustomName");
        tag.remove("SuccessCount");
        tag.remove("LastOutput");
        tag.remove("LastExecution");
        tag.remove("UpdateLastExecution");
        tag.putBoolean("auto", this.automatic);
        return tag;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        this.commandBlock.save(tag, registries);
        tag.putBoolean("powered", this.powered);
        tag.putBoolean("conditionMet", this.conditionMet);
        tag.putBoolean("auto", this.automatic);
        tag.putBoolean("Used", this.used);
        if (this.owner != null) {
            tag.putUUID("Owner", this.owner);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.commandBlock.load(tag, registries);
        this.powered = tag.getBoolean("powered");
        this.conditionMet = tag.getBoolean("conditionMet");
        this.automatic = tag.getBoolean("auto");
        this.used = tag.getBoolean("Used");
        this.owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        if (tag.contains("Locked")) {
            this.lockedOnClient = tag.getBoolean("Locked");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (this.owner != null) {
            tag.putUUID("Owner", this.owner);
        }
        tag.putBoolean("Locked", StormCommandBlockConfig.ownerOnly());
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        this.commandBlock.setCustomName(input.get(DataComponents.CUSTOM_NAME));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(DataComponents.CUSTOM_NAME, this.commandBlock.getCustomName());
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("CustomName");
    }

    private final class Runner extends BaseCommandBlock {
        @Override
        public void setCommand(String command) {
            super.setCommand(command);
            StormCommandBlockEntity.this.setChanged();
        }

        @Override
        public ServerLevel getLevel() {
            return (ServerLevel) StormCommandBlockEntity.this.level;
        }

        @Override
        public void onUpdated() {
            if (StormCommandBlockEntity.this.level instanceof ServerLevel level) {
                BlockState state = level.getBlockState(StormCommandBlockEntity.this.worldPosition);
                level.sendBlockUpdated(StormCommandBlockEntity.this.worldPosition, state, state, 3);
            }
        }

        @Override
        public Vec3 getPosition() {
            return Vec3.atCenterOf(StormCommandBlockEntity.this.worldPosition);
        }

        @Override
        public CommandSourceStack createCommandSourceStack() {
            BlockState state = StormCommandBlockEntity.this.getBlockState();
            Direction direction = state.hasProperty(StormCommandBlock.FACING) ? state.getValue(StormCommandBlock.FACING) : Direction.NORTH;
            ServerLevel level = this.getLevel();
            return new CommandSourceStack(
                    this,
                    Vec3.atCenterOf(StormCommandBlockEntity.this.worldPosition),
                    new Vec2(0.0F, direction.toYRot()),
                    level,
                    StormCommandBlockConfig.permissionLevel(),
                    this.getName().getString(),
                    this.getName(),
                    level.getServer(),
                    null);
        }

        @Override
        public boolean performCommand(Level level) {
            return false;
        }

        @Override
        public boolean isValid() {
            return !StormCommandBlockEntity.this.isRemoved();
        }

        int run(ServerLevel level) {
            MinecraftServer server = level.getServer();
            int[] count = new int[1];
            this.setSuccessCount(0);
            this.setLastOutput(null);
            try {
                CommandSourceStack source = this.createCommandSourceStack().withCallback((success, result) -> {
                    if (success) {
                        count[0]++;
                    }
                });
                Component blocked = StormCommandFilter.blocked(server.getCommands().getDispatcher(), source, this.getCommand());
                if (blocked != null) {
                    this.setLastOutput(blocked);
                    this.setSuccessCount(0);
                    return 0;
                }
                server.getCommands().performPrefixedCommand(source, this.getCommand());
            } catch (Exception exception) {
                IntegratedSuperheroes.LOGGER.error("Storm Command Block at {} failed to run '{}'", StormCommandBlockEntity.this.worldPosition, this.getCommand(), exception);
                this.setLastOutput(Component.literal(String.valueOf(exception.getMessage())));
                count[0] = 0;
            }
            this.setSuccessCount(count[0]);
            return count[0];
        }
    }
}
