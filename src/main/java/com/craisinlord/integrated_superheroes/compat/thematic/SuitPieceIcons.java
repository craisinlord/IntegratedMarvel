package com.craisinlord.integrated_superheroes.compat.thematic;

import bond.thematic.api.bridge.BridgeBuiltinRenderer;
import bond.thematic.api.platform.client.BuiltinItemRendererRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SuitPieceIcons {
    private static final Map<Item, ItemStack> STACKS = new ConcurrentHashMap<>();
    private static EquipmentSlot drawing;

    private SuitPieceIcons() {
    }

    public static EquipmentSlot drawing() {
        return drawing;
    }

    static void onRegisterExtensions(RegisterClientExtensionsEvent event) {
        register(event, SuitPieces.HELMET.get(), EquipmentSlot.HEAD);
        register(event, SuitPieces.LEGGINGS.get(), EquipmentSlot.LEGS);
        register(event, SuitPieces.BOOTS.get(), EquipmentSlot.FEET);
    }

    private static void register(RegisterClientExtensionsEvent event, Item item, EquipmentSlot slot) {
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft minecraft = Minecraft.getInstance();
                    renderer = new BlockEntityWithoutLevelRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels()) {
                        @Override
                        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
                            draw(slot, stack, context, pose, buffers, light, overlay);
                        }
                    };
                }
                return renderer;
            }
        }, item);
    }

    private static void draw(EquipmentSlot slot, ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Item suit = SuitPieces.suit(stack);
        if (suit == null) {
            return;
        }
        ItemStack look = STACKS.computeIfAbsent(suit, ItemStack::new);
        EquipmentSlot previous = drawing;
        drawing = slot;
        try {
            BlockEntityWithoutLevelRenderer custom = IClientItemExtensions.of(suit).getCustomRenderer();
            if (custom instanceof BridgeBuiltinRenderer) {
                custom.renderByItem(look, context, pose, buffers, light, overlay);
                return;
            }
            BuiltinItemRendererRegistry.DynamicItemRenderer dynamic = BuiltinItemRendererRegistry.INSTANCE.get(suit);
            if (dynamic != null) {
                dynamic.render(look, context, pose, buffers, light, overlay);
            }
        } finally {
            drawing = previous;
        }
    }
}
