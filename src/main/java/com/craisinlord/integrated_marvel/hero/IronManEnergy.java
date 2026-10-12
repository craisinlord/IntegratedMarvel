package com.craisinlord.integrated_marvel.hero;

import com.craisinlord.integrated_marvel.IntegratedMarvel;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tintankgames.marvel.core.components.MarvelDataComponents;
import net.tintankgames.marvel.world.item.EnergySuitItem;
import net.tintankgames.marvel.world.level.block.MarvelBlocks;
import net.tintankgames.marvel.world.level.block.entity.Mark38ChargerBlockEntity;
import net.tintankgames.marvel.world.level.block.entity.Mark38ChargerMultiBlockEntity;
import net.tintankgames.marvel.world.level.block.entity.MarvelBlockEntityTypes;
import net.tintankgames.marvel.world.level.block.entity.SuitChargerBlockEntity;

import java.util.HashMap;
import java.util.Map;

public final class IronManEnergy {
    private static final double PERCENT_EPSILON = 1.0E-4D;
    private static final String[] SUFFIXES = {"_helmet", "_chestplate", "_leggings", "_boots"};

    public static final String CHARGER_ENERGY_TAG = "integrated_marvel_energy";

    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, IntegratedMarvel.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> SUIT_FE = COMPONENTS.register("suit_fe",
            () -> DataComponentType.<Double>builder().persistent(Codec.DOUBLE).networkSynchronized(ByteBufCodecs.DOUBLE).build());

    private static final Map<String, ModConfigSpec.IntValue> CAPACITY = new HashMap<>();
    public static ModConfigSpec.IntValue BASE_CAPACITY;
    public static ModConfigSpec.IntValue CHARGER_FULL_CHARGE_SECONDS;
    public static ModConfigSpec.IntValue SUIT_CHARGER_BUFFER;
    public static ModConfigSpec.IntValue SUIT_CHARGER_MAX_INPUT;
    public static ModConfigSpec.IntValue LARGE_CHARGER_BUFFER;
    public static ModConfigSpec.IntValue LARGE_CHARGER_MAX_INPUT;
    public static ModConfigSpec.IntValue VERONICA_FE_PER_TICK;

    private IronManEnergy() {
    }

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
        modEventBus.addListener(IronManEnergy::registerCapabilities);
    }

    static void define(ModConfigSpec.Builder builder) {
        builder.comment("Suit energy in FE. Every Marvel energy cost is a percent of baseCapacity, so a suit with a",
                        "bigger battery pays the same FE per shot but a smaller share of its bar.",
                        "Suits charge in Marvel's Suit Charger / Large Suit Charger (fed FE by cables), the",
                        "Oritech Charger or any FE item charger, and slowly from Veronica.")
                .push("energy");
        BASE_CAPACITY = builder.comment("FE that Marvel's 100% means for costs (one Mk1-sized battery). A unibeam costs 7.5% of this.")
                .defineInRange("baseCapacity", 250_000, 1, Integer.MAX_VALUE);
        CHARGER_FULL_CHARGE_SECONDS = builder.comment("Seconds for a Marvel charger to fill any suit from empty, if its power keeps up.")
                .defineInRange("chargerFullChargeSeconds", 30, 1, 36_000);
        SUIT_CHARGER_BUFFER = builder.comment("FE the Suit Charger can hold.")
                .defineInRange("suitChargerBuffer", 4_000_000, 1, Integer.MAX_VALUE);
        SUIT_CHARGER_MAX_INPUT = builder.comment("Max FE per tick the Suit Charger accepts from cables.")
                .defineInRange("suitChargerMaxInput", 100_000, 1, Integer.MAX_VALUE);
        LARGE_CHARGER_BUFFER = builder.comment("FE the Large Suit Charger (Mk38 dock) can hold.")
                .defineInRange("largeChargerBuffer", 10_000_000, 1, Integer.MAX_VALUE);
        LARGE_CHARGER_MAX_INPUT = builder.comment("Max FE per tick the Large Suit Charger accepts from cables.")
                .defineInRange("largeChargerMaxInput", 250_000, 1, Integer.MAX_VALUE);
        VERONICA_FE_PER_TICK = builder.comment("FE per tick Veronica adds to each piece of every suit she stores.")
                .defineInRange("veronicaFePerTick", 100, 0, Integer.MAX_VALUE);

        builder.comment("FE capacity per armor piece, per suit. Suits not listed use baseCapacity.").push("capacity");
        capacity(builder, "iron_man_mark_2", 290_000);
        capacity(builder, "iron_man_mark_3", 340_000);
        capacity(builder, "iron_man_mark_5", 400_000);
        capacity(builder, "iron_man_mark_6", 460_000);
        capacity(builder, "iron_man_mark_7", 540_000);
        capacity(builder, "iron_man_mark_11", 630_000);
        capacity(builder, "iron_man_mark_15", 730_000);
        capacity(builder, "iron_man_mark_17", 850_000);
        capacity(builder, "iron_man_mark_19", 1_000_000);
        capacity(builder, "iron_man_mark_20", 1_160_000);
        capacity(builder, "iron_man_mark_21", 1_360_000);
        capacity(builder, "iron_man_mark_22", 1_580_000);
        capacity(builder, "iron_man_mark_23", 1_840_000);
        capacity(builder, "iron_man_mark_24", 2_150_000);
        capacity(builder, "iron_man_mark_25", 2_500_000);
        capacity(builder, "iron_man_mark_28", 2_900_000);
        capacity(builder, "iron_man_mark_30", 3_400_000);
        capacity(builder, "iron_man_mark_33", 4_000_000);
        capacity(builder, "iron_man_mark_35", 4_600_000);
        capacity(builder, "iron_man_mark_37", 5_400_000);
        capacity(builder, "iron_man_mark_38", 6_300_000);
        capacity(builder, "iron_man_mark_39", 7_350_000);
        capacity(builder, "iron_man_mark_42", 8_600_000);
        capacity(builder, "iron_man_mark_43", 10_000_000);
        capacity(builder, "war_machine_mark_1", 540_000);
        capacity(builder, "war_machine_mark_2", 1_840_000);
        capacity(builder, "iron_patriot", 1_840_000);
        builder.pop();
        builder.pop();
    }

    private static void capacity(ModConfigSpec.Builder builder, String suitId, int fe) {
        CAPACITY.put(suitId, builder.defineInRange(suitId, fe, 1, Integer.MAX_VALUE));
    }

    private static boolean loaded() {
        return MarvelBalanceConfig.SPEC.isLoaded();
    }

    public static boolean hasEnergy(ItemStack stack) {
        return !stack.isEmpty() && stack.has(MarvelDataComponents.ENERGY);
    }

    public static long capacity(ItemStack stack) {
        if (!loaded()) {
            return 250_000L;
        }
        ModConfigSpec.IntValue value = CAPACITY.get(suitId(stack.getItem()));
        return value != null ? value.get() : BASE_CAPACITY.get();
    }

    private static String suitId(Item item) {
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        for (String suffix : SUFFIXES) {
            if (path.endsWith(suffix)) {
                return path.substring(0, path.length() - suffix.length());
            }
        }
        return path;
    }

    public static double getFE(ItemStack stack) {
        double capacity = capacity(stack);
        double percent = EnergySuitItem.getEnergy(stack);
        Double fe = stack.get(SUIT_FE.get());
        if (fe == null || Math.abs(fe / capacity * 100.0D - percent) > PERCENT_EPSILON) {
            return percent / 100.0D * capacity;
        }
        return fe;
    }

    public static void setFE(ItemStack stack, double fe) {
        double capacity = capacity(stack);
        double clamped = Math.max(0.0D, Math.min(capacity, fe));
        stack.set(SUIT_FE.get(), clamped);
        stack.set(MarvelDataComponents.ENERGY, (float) (clamped / capacity * 100.0D));
    }

    public static double addFE(ItemStack stack, double fe) {
        if (fe <= 0.0D || !hasEnergy(stack)) {
            return 0.0D;
        }
        double current = getFE(stack);
        double added = Math.min(fe, capacity(stack) - current);
        if (added > 0.0D) {
            setFE(stack, current + added);
        }
        return Math.max(0.0D, added);
    }

    public static void drainPercent(ItemStack stack, float percent) {
        if (!hasEnergy(stack)) {
            return;
        }
        double base = loaded() ? BASE_CAPACITY.get() : 250_000.0D;
        setFE(stack, getFE(stack) - percent / 100.0D * base);
    }

    public static float scaledPercent(ItemStack stack, float percent) {
        double base = loaded() ? BASE_CAPACITY.get() : 250_000.0D;
        return (float) (percent * base / capacity(stack));
    }

    public static double chargerRate(ItemStack stack) {
        int seconds = loaded() ? CHARGER_FULL_CHARGE_SECONDS.get() : 30;
        return (double) capacity(stack) / (seconds * 20.0D);
    }

    public static long chargeFromBuffer(Iterable<ItemStack> pieces, long buffer) {
        long drawn = 0L;
        for (ItemStack piece : pieces) {
            if (buffer - drawn <= 0L) {
                break;
            }
            if (!hasEnergy(piece)) {
                continue;
            }
            double want = Math.min(chargerRate(piece), buffer - drawn);
            drawn += (long) Math.ceil(addFE(piece, want));
        }
        return Math.min(drawn, buffer);
    }

    public static int veronicaRate() {
        return loaded() ? VERONICA_FE_PER_TICK.get() : 0;
    }

    public static String format(double fe) {
        return String.format("%,d", (long) Math.floor(fe));
    }

    public interface ChargerBuffer {
        long integratedMarvel$getEnergy();

        void integratedMarvel$setEnergy(long energy);

        long integratedMarvel$getCapacity();

        long integratedMarvel$getMaxInput();
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof EnergySuitItem) {
                event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, context) -> new SuitStorage(stack), item);
            }
        }
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, MarvelBlockEntityTypes.SUIT_CHARGER.get(),
                (be, side) -> bufferStorage(be));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, MarvelBlockEntityTypes.MARK_38_CHARGER.get(),
                (be, side) -> bufferStorage(be));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, MarvelBlockEntityTypes.MARK_38_CHARGER_PART.get(),
                (be, side) -> mainChargerStorage(be));
        event.registerBlock(Capabilities.EnergyStorage.BLOCK, (level, pos, state, be, side) -> {
            BlockEntity below = level.getBlockEntity(pos.below());
            return below instanceof SuitChargerBlockEntity ? bufferStorage(below) : null;
        }, MarvelBlocks.SUIT_CHARGER_UPPER.get());
    }

    private static IEnergyStorage mainChargerStorage(Mark38ChargerMultiBlockEntity part) {
        BlockPos main = part.mainBlock;
        if (part.getLevel() == null || main == null || main.equals(BlockPos.ZERO)) {
            return null;
        }
        BlockEntity be = part.getLevel().getBlockEntity(main);
        return be instanceof Mark38ChargerBlockEntity ? bufferStorage(be) : null;
    }

    private static IEnergyStorage bufferStorage(BlockEntity be) {
        return be instanceof ChargerBuffer buffer ? new BufferStorage(be, buffer) : null;
    }

    private record SuitStorage(ItemStack stack) implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (maxReceive <= 0 || !hasEnergy(stack)) {
                return 0;
            }
            double space = capacity(stack) - getFE(stack);
            int accepted = (int) Math.max(0L, Math.min(maxReceive, (long) Math.floor(space)));
            if (!simulate && accepted > 0) {
                addFE(stack, accepted);
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, (long) getFE(stack));
        }

        @Override
        public int getMaxEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, capacity(stack));
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return hasEnergy(stack);
        }
    }

    private record BufferStorage(BlockEntity be, ChargerBuffer buffer) implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            long space = buffer.integratedMarvel$getCapacity() - buffer.integratedMarvel$getEnergy();
            int accepted = (int) Math.max(0L, Math.min(Math.min(maxReceive, buffer.integratedMarvel$getMaxInput()), space));
            if (!simulate && accepted > 0) {
                buffer.integratedMarvel$setEnergy(buffer.integratedMarvel$getEnergy() + accepted);
                be.setChanged();
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, buffer.integratedMarvel$getEnergy());
        }

        @Override
        public int getMaxEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, buffer.integratedMarvel$getCapacity());
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}
