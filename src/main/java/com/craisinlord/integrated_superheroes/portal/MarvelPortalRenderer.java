package com.craisinlord.integrated_superheroes.portal;

import com.craisinlord.anthology.portal.client.PortalEntityRenderer;
import com.craisinlord.anthology.portal.client.PortalRenderState;
import com.craisinlord.anthology.portal.client.PortalViewRenderer;
import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.UUID;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class MarvelPortalRenderer extends PortalEntityRenderer<MarvelPortal> {
    private static Bridge bridge;
    private static boolean failed;
    private static boolean rimOnly;

    public MarvelPortalRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MarvelPortal.TYPE.get(), MarvelPortalRenderer::new);
    }

    public static boolean rimOnly() {
        return rimOnly;
    }

    @Override
    public ResourceLocation getTextureLocation(MarvelPortal portal) {
        return portal.appearance().surfaceTexture(false);
    }

    @Override
    public void render(MarvelPortal portal, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        if (!MarvelPortal.enabled() || portal.style() == MarvelPortal.HIDDEN || PortalRenderState.getRenderLevel() != 0
                || !portal.shouldRenderFront(net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition())) {
            return;
        }
        if (failed) {
            super.render(portal, yaw, partialTick, pose, buffers, light);
            return;
        }
        var previousShader = RenderSystem.getShader();
        pose.pushPose();
        try {
            if (bridge == null) {
                bridge = new Bridge();
            }
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            rimOnly = PortalViewRenderer.isEnabled() && portal.getLinkedPortal() != null;
            bridge.draw(portal, pose, partialTick);
        } catch (ReflectiveOperationException | LinkageError failure) {
            failed = true;
            IntegratedSuperheroes.LOGGER.warn("Could not render Marvel portal effects", failure);
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.setShader(() -> previousShader);
            rimOnly = false;
            pose.popPose();
        }
    }

    private static final class Bridge {
        private final Constructor<?> point;
        private final Constructor<?> endpoint;
        private final Constructor<?> view;
        private final Object spiderverse;
        private final Object strange;
        private final Method hexDraw;
        private final Method strangeDraw;

        private Bridge() throws ReflectiveOperationException {
            Class<?> pointType = Class.forName("dev.svportals.portal.Point3");
            Class<?> endpointType = Class.forName("dev.svportals.portal.PortalEndpoint");
            Class<?> styleType = Class.forName("dev.svportals.portal.PortalStyle");
            Class<?> viewType = Class.forName("dev.svportals.network.PortalSnapshot$View");
            point = pointType.getConstructor(double.class, double.class, double.class);
            endpoint = endpointType.getConstructor(UUID.class, UUID.class, String.class, pointType,
                    float.class, float.class, double.class);
            view = viewType.getConstructor(UUID.class, styleType, endpointType, long.class, long.class);
            spiderverse = styleType.getField("SPIDERVERSE").get(null);
            strange = styleType.getField("STRANGE").get(null);
            hexDraw = drawMethod("HexPortalRenderer", "renderPortal", viewType);
            strangeDraw = drawMethod("StrangePortalRenderer", "draw", viewType);
        }

        private static Method drawMethod(String className, String method, Class<?> viewType)
                throws ReflectiveOperationException {
            Method draw = Class.forName("dev.svportals.client." + className)
                    .getDeclaredMethod(method, PoseStack.class, Vec3.class, viewType, double.class);
            draw.setAccessible(true);
            return draw;
        }

        private void draw(MarvelPortal portal, PoseStack pose, float partialTick)
                throws ReflectiveOperationException {
            UUID id = portal.getUUID();
            boolean eldritch = portal.style() == MarvelPortal.STRANGE;
            UUID twin = portal.getLinkedPortalId();
            Object end = endpoint.newInstance(id, twin == null ? id : twin,
                    portal.level().dimension().location().toString(), point.newInstance(0.0, 0.0, 0.0),
                    portal.yaw(), 0.0F, MarvelPortal.RADIUS);
            Object snapshot = view.newInstance(id, eldritch ? strange : spiderverse, end,
                    portal.opened(), portal.closed() - 20);
            (eldritch ? strangeDraw : hexDraw).invoke(null, pose, Vec3.ZERO, snapshot,
                    portal.level().getGameTime() + (double) partialTick);
        }
    }
}
