package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.antarchy.content.time.TimeDilationApi;
import com.craisinlord.integrated_superheroes.suit.SuitAbilities;
import com.craisinlord.integrated_superheroes.suit.SuitAbilityTracker;
import com.craisinlord.integrated_superheroes.suit.SuitPassives;
import com.craisinlord.integrated_superheroes.suit.SuitStates;
import com.stxllarplanes.marvellegacy.init.AttachmentsRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public final class HeroContent {
    private static final DeferredRegister.Items LEGACY_ITEMS = DeferredRegister.createItems(com.craisinlord.integrated_superheroes.IntegratedSuperheroes.MODID);
    private static final ResourceKey<CreativeModeTab> LEGACY_ITEM_TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath("marvellegacy", "item_tab"));

    public static final DeferredItem<Item> SUPER_SOLDIER_SYRINGE = syringe("syringe_super_soldier_steve_rogers", SuperSoldierPower.ID);
    public static final DeferredItem<Item> PANTHER_SYRINGE = syringe("syringe_heart_shaped_herb_tchalla", PantherPower.ID);
    public static final DeferredItem<Item> DEADPOOL_SYRINGE = syringe("syringe_weapon_x_wade_wilson", DeadpoolPower.ID);
    public static final DeferredItem<Item> QUICKSILVER_SYRINGE = syringe("syringe_super_speed_pietro_maximoff", QuicksilverPower.ID);
    public static final DeferredItem<Item> CYCLOPS_SYRINGE = syringe("syringe_optic_blasts_scott_summers", CyclopsPower.ID);
    public static final DeferredItem<Item> BINARY_SYRINGE = syringe("syringe_binary_carol_danvers", BinaryPower.ID);
    public static final DeferredItem<Item> HULK_SYRINGE = syringe("syringe_gamma_mutation_bruce_banner", HulkPower.ID);
    public static final DeferredItem<Item> MAGNETO_SYRINGE = ModCompat.ALEXS_CAVES ? syringe("syringe_magnetism_erik_lehnsherr", MagnetoPower.ID) : null;
    public static final DeferredItem<Item> STORM_SYRINGE = syringe("syringe_weather_manipulation_ororo_munroe", StormPower.ID);
    public static final DeferredItem<Item> ICEMAN_SYRINGE = syringe("syringe_cryokinesis_bobby_drake", IcemanPower.ID);
    public static final DeferredItem<Item> SCARLET_WITCH_SYRINGE = syringe("syringe_chaos_magic_wanda_maximoff", ScarletWitchPower.ID);
    public static final DeferredItem<Item> COLOSSUS_SYRINGE = syringe("syringe_organic_steel_piotr_rasputin", ColossusPower.ID);
    public static final DeferredItem<Item> DAREDEVIL_SYRINGE = syringe("syringe_radar_sense_matt_murdock", DaredevilPower.ID);
    public static final DeferredItem<Item> KITTY_SYRINGE = syringe("syringe_phasing_kitty_pryde", KittyPower.ID);
    public static final DeferredItem<Item> SQUIRREL_GIRL_SYRINGE = syringe("syringe_squirrel_powers_doreen_green", SquirrelGirlPower.ID);
    public static final DeferredItem<Item> TORCH_SYRINGE = syringe("syringe_flame_on_johnny_storm", HumanTorchPower.ID);
    public static final DeferredItem<Item> SUE_SYRINGE = syringe("syringe_invisibility_sue_storm", InvisibleWomanPower.ID);
    public static final DeferredItem<Item> REED_SYRINGE = syringe("syringe_elasticity_reed_richards", MrFantasticPower.ID);
    public static final DeferredItem<Item> THING_SYRINGE = syringe("syringe_rock_hide_ben_grimm", ThingPower.ID);
    private static final List<DeferredItem<Item>> SYRINGES = java.util.stream.Stream.of(
            SUPER_SOLDIER_SYRINGE, PANTHER_SYRINGE, DEADPOOL_SYRINGE, QUICKSILVER_SYRINGE, CYCLOPS_SYRINGE, BINARY_SYRINGE,
            HULK_SYRINGE, MAGNETO_SYRINGE, STORM_SYRINGE, ICEMAN_SYRINGE, SCARLET_WITCH_SYRINGE, COLOSSUS_SYRINGE,
            DAREDEVIL_SYRINGE, KITTY_SYRINGE, SQUIRREL_GIRL_SYRINGE, TORCH_SYRINGE, SUE_SYRINGE, REED_SYRINGE, THING_SYRINGE).filter(java.util.Objects::nonNull).toList();

    private HeroContent() {
    }

    private static DeferredItem<Item> syringe(String name, String heroId) {
        return LEGACY_ITEMS.register(name, () -> new HeroSyringeItem(new Item.Properties().stacksTo(1), heroId));
    }

    public static void register(IEventBus modEventBus) {
        HeroPowers.registerLegacyHero("spider_powers_peter_parker", MarvelBalanceConfig.SPIDER_POWERS);
        HeroPowers.registerLegacyHero("weapon_x_logan", MarvelBalanceConfig.WEAPON_X_LOGAN);
        HeroPowers.register(SuperSoldierPower.create(SUPER_SOLDIER_SYRINGE));
        HeroPowers.register(PantherPower.create(PANTHER_SYRINGE));
        HeroPowers.register(DeadpoolPower.create(DEADPOOL_SYRINGE));
        HeroPowers.register(QuicksilverPower.create(QUICKSILVER_SYRINGE));
        HeroPowers.register(CyclopsPower.create(CYCLOPS_SYRINGE));
        HeroPowers.register(BinaryPower.create(BINARY_SYRINGE));
        HeroPowers.register(HulkPower.create(HULK_SYRINGE));
        if (ModCompat.ALEXS_CAVES) {
            HeroPowers.register(MagnetoPower.create(MAGNETO_SYRINGE));
        }
        HeroPowers.register(StormPower.create(STORM_SYRINGE));
        HeroPowers.register(IcemanPower.create(ICEMAN_SYRINGE));
        HeroPowers.register(ScarletWitchPower.create(SCARLET_WITCH_SYRINGE));
        HeroPowers.register(ColossusPower.create(COLOSSUS_SYRINGE));
        HeroPowers.register(DaredevilPower.create(DAREDEVIL_SYRINGE));
        HeroPowers.register(KittyPower.create(KITTY_SYRINGE));
        HeroPowers.register(SquirrelGirlPower.create(SQUIRREL_GIRL_SYRINGE));
        HeroPowers.register(HumanTorchPower.create(TORCH_SYRINGE));
        HeroPowers.register(InvisibleWomanPower.create(SUE_SYRINGE));
        HeroPowers.register(MrFantasticPower.create(REED_SYRINGE));
        HeroPowers.register(ThingPower.create(THING_SYRINGE));

        LEGACY_ITEMS.register(modEventBus);
        modEventBus.addListener(HeroContent::commonSetup);
        NeoForge.EVENT_BUS.addListener(HeroContent::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(HeroContent::onAnyPlayerTick);
        NeoForge.EVENT_BUS.addListener(HeroContent::onServerTick);
        NeoForge.EVENT_BUS.addListener(HeroContent::onLevelTick);
        NeoForge.EVENT_BUS.addListener(HeroContent::onLogin);
        NeoForge.EVENT_BUS.addListener(HeroContent::onLogout);
        NeoForge.EVENT_BUS.addListener(HeroContent::onChangeDimension);
        NeoForge.EVENT_BUS.addListener(HeroContent::onRespawn);
        NeoForge.EVENT_BUS.addListener(HeroContent::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(HeroContent::onDeath);
        NeoForge.EVENT_BUS.addListener(HeroContent::onJump);
        NeoForge.EVENT_BUS.addListener(HeroContent::onJoinLevel);
        HealingFactor.register();
        DeadpoolPower.registerEvents();
        NeoForge.EVENT_BUS.addListener(TempBlocks::onBreak);
        NeoForge.EVENT_BUS.addListener(KittyPower::onAttack);
        NeoForge.EVENT_BUS.addListener(KittyPower::onUseBlock);
        NeoForge.EVENT_BUS.addListener(KittyPower::onUseEntity);
        NeoForge.EVENT_BUS.addListener(KittyPower::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(KittyPower::onBreak);
        NeoForge.EVENT_BUS.addListener(KittyPower::onPlace);
        NeoForge.EVENT_BUS.addListener(KittyPower::onTarget);
        NeoForge.EVENT_BUS.addListener(KittyPower::onProjectile);
        NeoForge.EVENT_BUS.addListener(HumanTorchPower::onImpact);
        NeoForge.EVENT_BUS.addListener(IcemanPower::onProjectile);
        NeoForge.EVENT_BUS.addListener(ReedShapes::onProjectile);
        HeroAdvancements.register();
        NeoForge.EVENT_BUS.addListener(InvisibleWomanPower::onAttack);
        NeoForge.EVENT_BUS.addListener(InvisibleWomanPower::onTarget);
        SuitStates.registerEvents();
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(HeroPowers::registerLegacyPowerEntries);
        event.enqueueWork(SuitAbilities::registerLegacyPowerEntries);
        try {
            TimeDilationApi.addHorizontalOnlyMovement(entity -> entity instanceof Player player
                    && player.hasData(AttachmentsRegistry.PLAYER_DATA.get())
                    && QuicksilverPower.ID.equals(player.getData(AttachmentsRegistry.PLAYER_DATA.get()).getPrimaryPower()));
        } catch (LinkageError ignored) {
        }
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(LEGACY_ITEM_TAB)) {
            SYRINGES.forEach(syringe -> event.accept(syringe.get()));
        }
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 20 == 0) {
            HeroPowers.syncStats(player);
        }
        if (player.tickCount % 100 == 0) {
            HeroPowers.pruneStale(player);
        }
        HeroAbilities.tickDash(player);
        HeroAnimations.tick(player);
        HeroGrapple.tick(player);
        ColossusPower.tickFastball(player);
        KittyPower.tickAny(player);
        SuitAbilityTracker.tick(player);
        SuitStates.tick(player);
        SuitPassives.tick(player);
        HeroBeams.tick(player);
        HealingFactor.tick(player);
        HeroPower hero = HeroPowers.activeHero(player);
        if (hero != null) {
            if (player.tickCount % 20 == 0) {
                HeroToggles.ensureWheel(player, hero);
            }
            hero.hooks().tick(player);
        }
    }

    private static void onAnyPlayerTick(PlayerTickEvent.Post event) {
        HulkPower.tickMovement(event.getEntity());
        ColossusPower.tickMovement(event.getEntity());
        KittyPower.tickMovement(event.getEntity());
        ThingPower.tickMovement(event.getEntity());
        MrFantasticPower.tickMovement(event.getEntity());
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        HeroShockwaves.tick();
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            TempBlocks.tick(level);
            HeroGrip.tickThrown(level);
            HeroBlockLift.tickThrown(level);
            ScarletWitchPower.tickLevel(level);
            StormZones.tick(level);
            HeroStun.tick(level);
        }
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StormZones.syncTo(player);
            HeroPowers.syncStats(player);
            SuitAbilityTracker.sync(player);
            KittyPower.onLogin(player);
            HeroPowers.pruneStale(player);
        }
    }

    private static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StormZones.syncTo(player);
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HeroGrip.clear(player);
            HeroBlockLift.drop(player);
            HeroGrapple.clear(player);
            DaredevilPower.dropClub(player);
        }
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HeroPowers.syncStats(player);
            SuitAbilityTracker.sync(player);
        }
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (HealingFactor.blocksDamage(event)) {
            event.setCanceled(true);
            return;
        }
        ScarletWitchPower.onAnyDamage(event);
        KittyPower.onAnyDamage(event);
        if (event.isCanceled()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer faller && event.getSource().is(DamageTypeTags.IS_FALL) && HeroGrapple.protectsFall(faller)) {
            event.setCanceled(true);
            return;
        }
        ColossusPower.onAnyDamage(event);
        if (event.isCanceled()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer target) {
            HeroPower hero = HeroPowers.activeHero(target);
            if (hero != null) {
                hero.hooks().incomingDamage(target, event);
                if (event.isCanceled()) {
                    return;
                }
            }
        }
        if (event.getSource().is(DamageTypes.PLAYER_ATTACK)
                && !HeroAbilities.isAbilityDamage()
                && event.getSource().getEntity() instanceof ServerPlayer player
                && event.getSource().getDirectEntity() == player
                && player.getMainHandItem().isEmpty()) {
            double bonus = HeroPowers.unarmedBonus(player);
            if (bonus > 0.0D) {
                event.setAmount(event.getAmount() + (float) bonus);
            }
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && attacker != event.getEntity()) {
            HeroPower hero = HeroPowers.activeHero(attacker);
            if (hero != null) {
                hero.hooks().outgoingDamage(attacker, event);
            }
        }
    }

    private static void onJoinLevel(EntityJoinLevelEvent event) {
        HeroStun.onJoin(event);
        DaredevilPower.onJoin(event);
    }

    private static void onJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof Player player) {
            HeroPower hero = HeroPowers.activeHero(player);
            if (hero != null) {
                hero.hooks().jump(player);
            }
        }
    }

    private static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HeroPower hero = HeroPowers.activeHero(player);
            if (hero != null) {
                hero.hooks().death(player, event);
            }
        }
        DeadpoolPower.onAnyDeath(event);
    }
}
