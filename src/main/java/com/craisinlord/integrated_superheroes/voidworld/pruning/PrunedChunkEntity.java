package com.craisinlord.integrated_superheroes.voidworld.pruning;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

import java.util.Arrays;

public class PrunedChunkEntity extends Entity implements IEntityWithComplexSpawn {
    public static final double ACCEL = 0.05D;
    public static final double MAX_SPEED = 3.2D;
    private static final double TERMINAL_TICKS = MAX_SPEED / ACCEL;
    private static final double TERMINAL_DISTANCE = 0.5D * ACCEL * TERMINAL_TICKS * TERMINAL_TICKS;

    private long seed;
    private double startY;
    private double landY;
    private int age;
    private int sizeX = 1;
    private int sizeY = 1;
    private int sizeZ = 1;
    private int[] palette = new int[0];
    private int[] cells = new int[0];
    private VoidPruning.Drop drop;

    public PrunedChunkEntity(EntityType<? extends PrunedChunkEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setInvulnerable(true);
        this.setNoGravity(true);
    }

    static PrunedChunkEntity create(ServerLevel level, PrunedPiece piece, VoidPruning.Drop drop, double startY) {
        PrunedChunkEntity entity = new PrunedChunkEntity(PruningEntities.PRUNED_CHUNK.get(), level);
        entity.drop = drop;
        entity.seed = piece.seed();
        entity.landY = piece.box().minY();
        entity.startY = Math.max(startY, entity.landY + 1.0D);
        PrunedPiece.Grid grid = piece.grid();
        entity.sizeX = grid.sizeX();
        entity.sizeY = grid.sizeY();
        entity.sizeZ = grid.sizeZ();
        entity.palette = grid.palette();
        entity.cells = grid.cells();
        double x = piece.box().minX() + piece.box().getXSpan() / 2.0D;
        double z = piece.box().minZ() + piece.box().getZSpan() / 2.0D;
        entity.moveTo(x, entity.startY, z, 0.0F, 0.0F);
        return entity;
    }

    public static double fallen(double ticks) {
        if (ticks <= TERMINAL_TICKS) {
            return 0.5D * ACCEL * ticks * ticks;
        }
        return TERMINAL_DISTANCE + MAX_SPEED * (ticks - TERMINAL_TICKS);
    }

    public static int ticksToFall(double distance) {
        if (distance <= 0.0D) {
            return 0;
        }
        if (distance <= TERMINAL_DISTANCE) {
            return Mth.ceil(Math.sqrt(2.0D * distance / ACCEL));
        }
        return Mth.ceil(TERMINAL_TICKS + (distance - TERMINAL_DISTANCE) / MAX_SPEED);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        if (this.level().isClientSide) {
            this.age++;
            this.follow();
            this.trail();
        } else if (this.drop == null || this.drop.done) {
            this.discard();
        }
    }

    void setFallAge(int age) {
        this.age = Math.max(0, age);
        this.follow();
    }

    private void follow() {
        this.setPos(this.getX(), Math.max(this.landY, this.startY - fallen(this.age)), this.getZ());
    }

    private void trail() {
        if (this.getY() - this.landY < 2.0D) {
            return;
        }
        Level level = this.level();
        for (int i = 0; i < 3; i++) {
            double ox = (this.random.nextDouble() - 0.5D) * this.sizeX;
            double oz = (this.random.nextDouble() - 0.5D) * this.sizeZ;
            double oy = this.random.nextDouble() * this.sizeY;
            level.addParticle(i == 0 ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.LARGE_SMOKE,
                    this.getX() + ox, this.getY() + oy, this.getZ() + oz, 0.0D, 0.15D, 0.0D);
        }
    }

    private double left() {
        double distance = this.startY - this.landY;
        return distance <= 0.0D ? 0.0D : Mth.clamp((this.getY() - this.landY) / distance, 0.0D, 1.0D);
    }

    public float tumble(float partialTick) {
        return (float) (Math.sin((this.age + partialTick) * 0.09D + (this.seed & 63L)) * 9.0D * this.left());
    }

    public float tumbleAxis() {
        return (float) ((this.seed >>> 8 & 1023L) / 1024.0D * Math.PI * 2.0D);
    }

    public float spin() {
        double left = this.left();
        return (float) (((this.seed & 1L) == 0L ? 1.0D : -1.0D) * left * left * 25.0D);
    }

    public int sizeX() {
        return this.sizeX;
    }

    public int sizeY() {
        return this.sizeY;
    }

    public int sizeZ() {
        return this.sizeZ;
    }

    public int[] palette() {
        return this.palette;
    }

    public int[] cells() {
        return this.cells;
    }

    public double landY() {
        return this.landY;
    }

    @Override
    public Vec3 getLightProbePosition(float partialTick) {
        return this.getPosition(partialTick).add(0.0D, this.sizeY + 0.5D, 0.0D);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        double reach = Math.max(this.sizeX, this.sizeZ) * 0.75D + 1.0D;
        return new AABB(this.getX() - reach, this.getY() - 2.0D, this.getZ() - reach,
                this.getX() + reach, this.getY() + this.sizeY + 2.0D, this.getZ() + reach);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
    }

    @Override
    public void lerpMotion(double x, double y, double z) {
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 512.0D * 512.0D;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        buf.writeLong(this.seed);
        buf.writeDouble(this.startY);
        buf.writeDouble(this.landY);
        ByteBufCodecs.VAR_INT.encode(buf, this.age);
        buf.writeByte(this.sizeX);
        buf.writeByte(this.sizeY);
        buf.writeByte(this.sizeZ);
        ByteBufCodecs.VAR_INT.encode(buf, this.palette.length);
        for (int id : this.palette) {
            ByteBufCodecs.VAR_INT.encode(buf, id);
        }
        int i = 0;
        while (i < this.cells.length) {
            int value = this.cells[i];
            int run = 1;
            while (i + run < this.cells.length && this.cells[i + run] == value) {
                run++;
            }
            ByteBufCodecs.VAR_INT.encode(buf, value);
            ByteBufCodecs.VAR_INT.encode(buf, run);
            i += run;
        }
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        this.seed = buf.readLong();
        this.startY = buf.readDouble();
        this.landY = buf.readDouble();
        this.age = ByteBufCodecs.VAR_INT.decode(buf);
        this.sizeX = Mth.clamp(buf.readUnsignedByte(), 1, PrunedPiece.MAX_SPAN);
        this.sizeY = Mth.clamp(buf.readUnsignedByte(), 1, PrunedPiece.MAX_SPAN);
        this.sizeZ = Mth.clamp(buf.readUnsignedByte(), 1, PrunedPiece.MAX_SPAN);
        int paletteSize = ByteBufCodecs.VAR_INT.decode(buf);
        if (paletteSize < 0 || paletteSize > this.sizeX * this.sizeY * this.sizeZ) {
            throw new IllegalStateException("Bad pruned chunk palette size " + paletteSize);
        }
        this.palette = new int[paletteSize];
        for (int i = 0; i < paletteSize; i++) {
            this.palette[i] = ByteBufCodecs.VAR_INT.decode(buf);
        }
        this.cells = new int[this.sizeX * this.sizeY * this.sizeZ];
        int i = 0;
        while (i < this.cells.length) {
            int value = ByteBufCodecs.VAR_INT.decode(buf);
            int run = ByteBufCodecs.VAR_INT.decode(buf);
            if (run <= 0 || i + run > this.cells.length || value < 0 || value > paletteSize) {
                throw new IllegalStateException("Bad pruned chunk cell run");
            }
            Arrays.fill(this.cells, i, i + run, value);
            i += run;
        }
        this.follow();
        this.setOldPosAndRot();
    }
}
