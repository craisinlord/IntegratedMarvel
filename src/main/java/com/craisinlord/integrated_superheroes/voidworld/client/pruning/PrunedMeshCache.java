package com.craisinlord.integrated_superheroes.voidworld.client.pruning;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.voidworld.client.sky.ShaderPacks;
import com.craisinlord.integrated_superheroes.voidworld.pruning.PrunedChunkEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PrunedMeshCache {
    private static final int CAPACITY = 16;
    private static final long IDLE_MILLIS = 5000L;
    private static final int UV2 = DefaultVertexFormat.BLOCK.getElementAttributeNames().indexOf("UV2");
    private static final Map<UUID, Mesh> CACHE = new LinkedHashMap<>(CAPACITY, 0.75F, true);

    private PrunedMeshCache() {
    }

    public record Animated(BlockPos pos, BlockState state) {
    }

    private record Built(Map<RenderType, Layer> layers, List<Animated> animated) {
    }

    static final class Layer implements VertexConsumer {
        private static final int STRIDE = 7;
        private int[] data = new int[STRIDE * 256];
        private int vertices;
        private int at = -STRIDE;

        int vertices() {
            return this.vertices;
        }

        void truncate(int count) {
            this.vertices = Math.min(this.vertices, count);
            this.at = (this.vertices - 1) * STRIDE;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            if ((this.vertices + 1) * STRIDE > this.data.length) {
                this.data = Arrays.copyOf(this.data, this.data.length * 2);
            }
            this.at = this.vertices * STRIDE;
            this.vertices++;
            this.data[this.at] = Float.floatToRawIntBits(x);
            this.data[this.at + 1] = Float.floatToRawIntBits(y);
            this.data[this.at + 2] = Float.floatToRawIntBits(z);
            this.data[this.at + 3] = -1;
            this.data[this.at + 4] = 0;
            this.data[this.at + 5] = 0;
            this.data[this.at + 6] = 0x007F00;
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            this.data[this.at + 3] = (alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            this.data[this.at + 4] = Float.floatToRawIntBits(u);
            this.data[this.at + 5] = Float.floatToRawIntBits(v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            this.data[this.at + 6] = pack(x) | pack(y) << 8 | pack(z) << 16;
            return this;
        }

        private static int pack(float value) {
            return (int) (Math.max(-1.0F, Math.min(1.0F, value)) * 127.0F) & 0xFF;
        }

        private static float unpack(int packed, int shift) {
            return (byte) (packed >> shift & 0xFF) / 127.0F;
        }

        void emit(VertexConsumer out, PoseStack.Pose pose, int light) {
            int[] d = this.data;
            for (int i = 0, end = this.vertices * STRIDE; i < end; i += STRIDE) {
                int normal = d[i + 6];
                VertexConsumer vertex = pose == null
                        ? out.addVertex(Float.intBitsToFloat(d[i]), Float.intBitsToFloat(d[i + 1]), Float.intBitsToFloat(d[i + 2]))
                        : out.addVertex(pose, Float.intBitsToFloat(d[i]), Float.intBitsToFloat(d[i + 1]), Float.intBitsToFloat(d[i + 2]));
                vertex.setColor(d[i + 3])
                        .setUv(Float.intBitsToFloat(d[i + 4]), Float.intBitsToFloat(d[i + 5]))
                        .setLight(light);
                if (pose == null) {
                    vertex.setNormal(unpack(normal, 0), unpack(normal, 8), unpack(normal, 16));
                } else {
                    vertex.setNormal(pose, unpack(normal, 0), unpack(normal, 8), unpack(normal, 16));
                }
            }
        }
    }

    public static final class Mesh {
        private CompletableFuture<Built> pending;
        private Map<RenderType, Layer> layers;
        private Map<RenderType, VertexBuffer> buffers;
        private List<Animated> animated = List.of();
        private long used = Util.getMillis();
        private boolean closed;

        public boolean ready() {
            if (this.layers != null) {
                return true;
            }
            if (this.closed || this.pending == null || !this.pending.isDone()) {
                return false;
            }
            Built built = this.pending.getNow(null);
            this.pending = null;
            if (built == null) {
                this.layers = Map.of();
                return true;
            }
            this.layers = built.layers();
            this.animated = built.animated();
            return true;
        }

        public List<Animated> animated() {
            return this.animated;
        }

        public void draw(PoseStack poseStack, MultiBufferSource source, int packedLight) {
            if (this.layers == null || this.layers.isEmpty()) {
                return;
            }
            if (ShaderPacks.inUse()) {
                this.drawBatched(poseStack.last(), source, packedLight);
            } else {
                this.drawBuffers(poseStack.last().pose(), packedLight);
            }
        }

        private void drawBatched(PoseStack.Pose pose, MultiBufferSource source, int packedLight) {
            for (RenderType type : RenderType.chunkBufferLayers()) {
                Layer layer = this.layers.get(type);
                if (layer != null) {
                    layer.emit(source.getBuffer(type == RenderType.translucent() ? RenderType.translucentMovingBlock() : type), pose, packedLight);
                }
            }
        }

        private void upload() {
            this.buffers = new IdentityHashMap<>();
            try {
                for (Map.Entry<RenderType, Layer> entry : this.layers.entrySet()) {
                    Layer layer = entry.getValue();
                    if (layer.vertices() == 0) {
                        continue;
                    }
                    try (ByteBufferBuilder bytes = new ByteBufferBuilder(layer.vertices() * DefaultVertexFormat.BLOCK.getVertexSize())) {
                        BufferBuilder builder = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
                        layer.emit(builder, null, 0);
                        MeshData mesh = builder.build();
                        if (mesh == null) {
                            continue;
                        }
                        if (mesh.drawState().format() != DefaultVertexFormat.BLOCK) {
                            mesh.close();
                            continue;
                        }
                        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
                        this.buffers.put(entry.getKey(), buffer);
                        buffer.bind();
                        buffer.upload(mesh);
                    }
                }
            } catch (RuntimeException e) {
                IntegratedSuperheroes.LOGGER.warn("Pruned chunk mesh upload failed: {}", e.getMessage());
                this.releaseBuffers();
                this.buffers = new IdentityHashMap<>();
            } finally {
                VertexBuffer.unbind();
            }
        }

        private void drawBuffers(Matrix4f pose, int packedLight) {
            if (this.buffers == null) {
                this.upload();
            }
            if (this.buffers.isEmpty()) {
                return;
            }
            Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose);
            Matrix4f projection = RenderSystem.getProjectionMatrix();
            int u = packedLight & 0xFFFF;
            int v = packedLight >>> 16 & 0xFFFF;
            for (RenderType type : RenderType.chunkBufferLayers()) {
                VertexBuffer buffer = this.buffers.get(type);
                if (buffer == null) {
                    continue;
                }
                type.setupRenderState();
                ShaderInstance shader = RenderSystem.getShader();
                if (shader != null) {
                    if (shader.CHUNK_OFFSET != null) {
                        shader.CHUNK_OFFSET.set(0.0F, 0.0F, 0.0F);
                    }
                    buffer.bind();
                    if (UV2 >= 0) {
                        GL20.glDisableVertexAttribArray(UV2);
                        GL30.glVertexAttribI2i(UV2, u, v);
                    }
                    buffer.drawWithShader(modelView, projection, shader);
                    if (UV2 >= 0) {
                        GL20.glEnableVertexAttribArray(UV2);
                    }
                }
                type.clearRenderState();
            }
            VertexBuffer.unbind();
        }

        private void releaseBuffers() {
            if (this.buffers != null) {
                for (VertexBuffer buffer : this.buffers.values()) {
                    buffer.close();
                }
                this.buffers = null;
            }
        }

        void close() {
            this.closed = true;
            if (this.pending != null) {
                this.pending.cancel(false);
                this.pending = null;
            }
            this.releaseBuffers();
            this.layers = null;
            this.animated = List.of();
        }
    }

    public static Mesh get(PrunedChunkEntity entity) {
        Mesh mesh = CACHE.get(entity.getUUID());
        if (mesh == null) {
            mesh = new Mesh();
            Source source = new Source(entity, Minecraft.getInstance().level);
            mesh.pending = CompletableFuture.supplyAsync(() -> build(source), Util.backgroundExecutor())
                    .exceptionally(error -> {
                        IntegratedSuperheroes.LOGGER.warn("Pruned chunk mesh failed: {}", error.getMessage());
                        return null;
                    });
            CACHE.put(entity.getUUID(), mesh);
            while (CACHE.size() > CAPACITY) {
                Iterator<Mesh> eldest = CACHE.values().iterator();
                eldest.next().close();
                eldest.remove();
            }
        }
        mesh.used = Util.getMillis();
        return mesh;
    }

    public static void sweep() {
        if (CACHE.isEmpty()) {
            return;
        }
        long now = Util.getMillis();
        Iterator<Mesh> iterator = CACHE.values().iterator();
        while (iterator.hasNext()) {
            Mesh mesh = iterator.next();
            if (now - mesh.used > IDLE_MILLIS) {
                mesh.close();
                iterator.remove();
            }
        }
    }

    public static void clear() {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(PrunedMeshCache::clear);
            return;
        }
        for (Mesh mesh : CACHE.values()) {
            mesh.close();
        }
        CACHE.clear();
    }

    private static final class Source implements BlockAndTintGetter {
        private static final LevelLightEngine DARK = new LevelLightEngine(new LightChunkGetter() {
            @Override
            public LightChunk getChunkForLighting(int x, int z) {
                return null;
            }

            @Override
            public BlockGetter getLevel() {
                return EmptyBlockGetter.INSTANCE;
            }
        }, false, false);
        final int sizeX;
        final int sizeY;
        final int sizeZ;
        final BlockState[] grid;
        final BlockPos origin;
        final ClientLevel level;

        Source(PrunedChunkEntity entity, ClientLevel level) {
            this.sizeX = entity.sizeX();
            this.sizeY = entity.sizeY();
            this.sizeZ = entity.sizeZ();
            this.grid = new BlockState[this.sizeX * this.sizeY * this.sizeZ];
            int[] palette = entity.palette();
            BlockState[] states = new BlockState[palette.length];
            for (int i = 0; i < palette.length; i++) {
                states[i] = Block.stateById(palette[i]);
            }
            int[] cells = entity.cells();
            for (int i = 0; i < this.grid.length && i < cells.length; i++) {
                int value = cells[i];
                if (value > 0 && value <= states.length) {
                    this.grid[i] = states[value - 1];
                }
            }
            this.origin = BlockPos.containing(entity.getX() - this.sizeX * 0.5D, entity.landY(), entity.getZ() - this.sizeZ * 0.5D);
            this.level = level;
        }

        int index(int x, int y, int z) {
            return (y * this.sizeZ + z) * this.sizeX + x;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();
            if (x < 0 || y < 0 || z < 0 || x >= this.sizeX || y >= this.sizeY || z >= this.sizeZ) {
                return Blocks.AIR.defaultBlockState();
            }
            BlockState state = this.grid[this.index(x, y, z)];
            return state == null ? Blocks.AIR.defaultBlockState() : state;
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return this.getBlockState(pos).getFluidState();
        }

        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public float getShade(Direction direction, boolean shade) {
            if (!shade) {
                return 1.0F;
            }
            return switch (direction) {
                case DOWN -> 0.5F;
                case UP -> 1.0F;
                case NORTH, SOUTH -> 0.8F;
                case WEST, EAST -> 0.6F;
            };
        }

        @Override
        public LevelLightEngine getLightEngine() {
            return DARK;
        }

        @Override
        public int getBrightness(LightLayer layer, BlockPos pos) {
            return layer == LightLayer.SKY ? 15 : 0;
        }

        @Override
        public int getRawBrightness(BlockPos pos, int darkening) {
            return Math.max(0, 15 - darkening);
        }

        @Override
        public boolean canSeeSky(BlockPos pos) {
            return true;
        }

        @Override
        public int getBlockTint(BlockPos pos, ColorResolver resolver) {
            return this.level == null ? -1 : this.level.getBlockTint(this.origin.offset(pos), resolver);
        }

        @Override
        public int getHeight() {
            return this.sizeY;
        }

        @Override
        public int getMinBuildHeight() {
            return 0;
        }
    }

    private static Built build(Source source) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        Map<RenderType, Layer> layers = new IdentityHashMap<>();
        List<Animated> animated = new ArrayList<>();
        PoseStack pose = new PoseStack();
        RandomSource random = RandomSource.create();
        for (int y = 0; y < source.sizeY; y++) {
            for (int z = 0; z < source.sizeZ; z++) {
                for (int x = 0; x < source.sizeX; x++) {
                    BlockState state = source.grid[source.index(x, y, z)];
                    if (state == null) {
                        continue;
                    }
                    BlockPos pos = new BlockPos(x, y, z);
                    RenderShape shape = state.getRenderShape();
                    if (shape == RenderShape.ENTITYBLOCK_ANIMATED && state.getBlock() instanceof AbstractChestBlock<?>) {
                        animated.add(new Animated(pos, state));
                    }
                    if (shape != RenderShape.MODEL) {
                        continue;
                    }
                    BakedModel model = dispatcher.getBlockModel(state);
                    long seed = state.getSeed(pos);
                    random.setSeed(seed);
                    for (RenderType type : model.getRenderTypes(state, random, ModelData.EMPTY)) {
                        Layer layer = layers.computeIfAbsent(type, key -> new Layer());
                        pose.pushPose();
                        pose.translate(x, y, z);
                        int before = layer.vertices();
                        try {
                            random.setSeed(seed);
                            dispatcher.renderBatched(state, pos, source, pose, layer, true, random, ModelData.EMPTY, type);
                        } catch (RuntimeException ignored) {
                            layer.truncate(before);
                        }
                        if (layer.vertices() % 4 != 0) {
                            layer.truncate(before);
                        }
                        pose.popPose();
                    }
                }
            }
        }
        layers.values().removeIf(layer -> layer.vertices() == 0);
        return new Built(layers, List.copyOf(animated));
    }
}
