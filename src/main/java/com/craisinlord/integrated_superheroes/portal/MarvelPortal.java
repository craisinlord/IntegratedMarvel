package com.craisinlord.integrated_superheroes.portal;

import com.craisinlord.anthology.portal.PortalAppearance;
import com.craisinlord.anthology.portal.PortalEntity;
import com.craisinlord.anthology.portal.PortalShape;
import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MarvelPortal extends PortalEntity {
    private static final DeferredRegister<EntityType<?>> TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, IntegratedSuperheroes.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<MarvelPortal>> TYPE =
            TYPES.register("marvel_portal", () -> EntityType.Builder.of(MarvelPortal::new, MobCategory.MISC)
                    .sized(3.0F, 3.0F).clientTrackingRange(12).updateInterval(1).noSave().noSummon()
                    .build("integrated_superheroes:marvel_portal"));
    private static final EntityDataAccessor<Float> YAW =
            SynchedEntityData.defineId(MarvelPortal.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Long> OPENED =
            SynchedEntityData.defineId(MarvelPortal.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> CLOSED =
            SynchedEntityData.defineId(MarvelPortal.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> STYLE =
            SynchedEntityData.defineId(MarvelPortal.class, EntityDataSerializers.INT);
    private static final ResourceLocation FALLBACK =
            ResourceLocation.withDefaultNamespace("textures/block/nether_portal.png");
    private static final PortalAppearance APPEARANCE = new PortalAppearance() {
        public int color() { return 0xFFFFFF; }
        public ResourceLocation rimTexture() { return FALLBACK; }
        public ResourceLocation surfaceTexture(boolean fancy) { return FALLBACK; }
    };
    public static final int SPIDERVERSE = 0;
    public static final int STRANGE = 1;
    public static final int HIDDEN = 2;
    public static final double RADIUS = 1.25;
    public static final double APERTURE_RADIUS = RADIUS * 0.84;

    public MarvelPortal(EntityType<MarvelPortal> type, Level level) {
        super(type, level);
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }

    public static boolean enabled() {
        return ModList.get().isLoaded("sv_portals");
    }

    public static MarvelPortal create(ServerLevel level, Vec3 center, Vec3 normal, int style, int lifetime) {
        MarvelPortal portal = new MarvelPortal(TYPE.get(), level);
        portal.entityData.set(YAW, (float) Math.toDegrees(Math.atan2(-normal.x, normal.z)));
        portal.entityData.set(OPENED, level.getGameTime());
        portal.entityData.set(CLOSED, level.getGameTime() + lifetime);
        portal.entityData.set(STYLE, style);
        portal.setPos(center);
        return portal;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(YAW, 0.0F).define(OPENED, 0L).define(CLOSED, 0L).define(STYLE, HIDDEN);
    }

    public long opened() { return entityData.get(OPENED); }
    public long closed() { return entityData.get(CLOSED); }
    public int style() { return entityData.get(STYLE); }
    public float yaw() { return entityData.get(YAW); }

    @Override
    public Vec3 getNormalVec() {
        double angle = Math.toRadians(yaw());
        return new Vec3(-Math.sin(angle), 0, Math.cos(angle));
    }

    @Override
    public Vec3 getUpVec() {
        return new Vec3(0, 1, 0);
    }

    @Override
    public Vec3 getWidthVec() {
        return getUpVec().cross(getNormalVec());
    }

    @Override
    public PortalShape getWorldPortalShape() {
        Vec3 normal = getNormalVec();
        Vec3 up = getUpVec();
        return new PortalShape(position(), normal, up, up.cross(normal), RADIUS, RADIUS, 0.05);
    }

    @Override
    public boolean shouldRenderFront(Vec3 camera) {
        return style() != HIDDEN && super.shouldRenderFront(camera);
    }

    @Override
    public float getPortalVisualScale(float partialTick) {
        double now = level().getGameTime() + partialTick;
        return (float) Math.clamp(Math.min((now - opened()) / 20, (closed() - now) / 20), 0, 1);
    }

    @Override
    public PortalAppearance appearance() {
        return APPEARANCE;
    }

    @Override
    protected boolean isStillValid(ServerLevel level) {
        return enabled() && level.getGameTime() < closed();
    }

    @Override
    public boolean canTransit(Entity entity) {
        return false;
    }
}
