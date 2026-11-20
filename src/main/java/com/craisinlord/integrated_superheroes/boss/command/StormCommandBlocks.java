package com.craisinlord.integrated_superheroes.boss.command;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.boss.command.client.StormCommandBlockClient;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class StormCommandBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(IntegratedSuperheroes.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(IntegratedSuperheroes.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IntegratedSuperheroes.MODID);

    public static final DeferredBlock<StormCommandBlock> BLOCK = BLOCKS.register("storm_command_block", () -> new StormCommandBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(5.0F, 1200.0F)
                    .sound(SoundType.METAL)
                    .pushReaction(PushReaction.BLOCK)
                    .noLootTable()));

    public static final DeferredItem<StormCommandBlockItem> ITEM = ITEMS.register("storm_command_block",
            () -> new StormCommandBlockItem(BLOCK.get(), new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StormCommandBlockEntity>> BLOCK_ENTITY = BLOCK_ENTITIES.register("storm_command_block",
            () -> BlockEntityType.Builder.of(StormCommandBlockEntity::new, BLOCK.get()).build(null));

    private StormCommandBlocks() {
    }

    public static void register(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, StormCommandBlockConfig.SPEC, "integrated_superheroes-storm-command-block.toml");
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        modEventBus.addListener(StormCommandBlocks::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToServer(StormCommandEditPayload.TYPE, StormCommandEditPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> {
                            if (context.player() instanceof ServerPlayer player) {
                                handleEdit(player, payload);
                            }
                        }))
                .playToClient(StormCommandOpenPayload.TYPE, StormCommandOpenPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> StormCommandBlockClient.open(payload)));
    }

    private static Component denial(ServerPlayer player, StormCommandBlockEntity entity) {
        if (!StormCommandBlockConfig.enabled() || entity.isUsed()) {
            return Component.translatable("message.integrated_superheroes.storm_command_block.disabled");
        }
        if (!StormCommandBlockConfig.allowSurvivalEditing() && !player.canUseGameMasterBlocks()) {
            return Component.translatable("advMode.notAllowed");
        }
        if (entity.isLockedFor(player)) {
            return Component.translatable("message.integrated_superheroes.storm_command_block.not_owner");
        }
        return null;
    }

    static void tryOpen(ServerPlayer player, StormCommandBlockEntity entity) {
        Component denied = denial(player, entity);
        if (denied != null) {
            player.displayClientMessage(denied, true);
            return;
        }
        String command = entity.getCommandBlock().getCommand();
        if (command.length() > StormCommandEditPayload.MAX_COMMAND_LENGTH) {
            command = command.substring(0, StormCommandEditPayload.MAX_COMMAND_LENGTH);
        }
        PacketDistributor.sendToPlayer(player, new StormCommandOpenPayload(
                entity.getBlockPos(),
                command,
                entity.getCommandBlock().getLastOutput(),
                entity.getCommandBlock().isTrackOutput(),
                entity.isConditional(),
                entity.isAutomatic()));
    }

    private static void handleEdit(ServerPlayer player, StormCommandEditPayload payload) {
        BlockPos pos = payload.pos();
        Level level = player.level();
        if (!level.isLoaded(pos) || !player.canInteractWithBlock(pos, 1.0)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!state.is(BLOCK.get()) || !(level.getBlockEntity(pos) instanceof StormCommandBlockEntity entity)) {
            return;
        }
        Component denied = denial(player, entity);
        if (denied != null) {
            player.displayClientMessage(denied, true);
            return;
        }
        if (state.getValue(StormCommandBlock.CONDITIONAL) != payload.conditional()) {
            BlockState updated = state.setValue(StormCommandBlock.CONDITIONAL, payload.conditional());
            level.setBlock(pos, updated, 2);
        }
        String command = payload.command();
        entity.applyEdit(command, payload.trackOutput(), payload.automatic());
        if (!StringUtil.isNullOrEmpty(command)) {
            player.sendSystemMessage(Component.translatable("advMode.setCommand.success", command));
        }
    }
}
