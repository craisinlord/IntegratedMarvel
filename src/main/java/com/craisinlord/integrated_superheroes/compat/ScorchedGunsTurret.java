package com.craisinlord.integrated_superheroes.compat;

import com.craisinlord.integrated_superheroes.hero.HeroAbilities;
import com.craisinlord.integrated_superheroes.hero.ServerState;
import com.craisinlord.integrated_superheroes.suit.SuitAbilityConfig;
import com.craisinlord.integrated_superheroes.suit.SuitFx;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingGetProjectileEvent;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.tintankgames.marvel.world.item.EnergySuitItem;
import net.tintankgames.marvel.world.item.ShoulderTurretItem;
import top.ribs.scguns.common.FireMode;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.item.AmmoBoxItem;
import top.ribs.scguns.item.GunItem;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ScorchedGunsTurret {
    private static final float ENERGY = 0.2F;
    private static final double AIM_RANGE = 64.0D;
    private static final double MIN_CONVERGENCE = 3.0D;
    private static final long MAX_DELAY = 200L;
    private static final String LOADED_PROJECTILE = "scguns:LoadedProjectile:";
    private static final ResourceLocation DEFAULT_AMMO = id("compact_advanced_round");
    private static final ResourceLocation VARIANT_GUN = id("gattaler");
    private static final Map<ResourceLocation, ResourceLocation> PREFERRED = Map.ofEntries(
            gun("powder_and_ball", "flintlock_pistol"),
            gun("grapeshot", "doublet"),
            gun("compact_copper_round", "rusty_gnat"),
            gun("standard_copper_round", "birdfeeder"),
            gun("compact_advanced_round", "gattaler"),
            gun("advanced_round", "cogloader"),
            gun("hog_round", "freyr"),
            gun("gibbs_round", "mas_55"),
            gun("krahg_round", "stiletto"),
            gun("ramrod_round", "auvtomag"),
            gun("beowulf_round", "osgood_50"),
            gun("osborne_slug", "big_bore"),
            gun("shotgun_shell", "jackhammer"),
            gun("bearpack_shell", "brawler"),
            gun("shatter_round", "thunderhead"),
            gun("frog_dart", "zilk_45"),
            gun("microjet", "cyclone"),
            gun("rocket", "dozier_rl"),
            gun("he_grenade_round", "hammer_gl"),
            gun("energy_cell", "plasgun"),
            gun("blaze_fuel", "spitfire"),
            gun("shock_cell", "pulsar"),
            gun("sculk_cell", "whispers"));
    private static final Map<UUID, Long> NEXT_SHOT = ServerState.map(new HashMap<>());
    private static volatile Map<Item, GunItem> guns;

    private ScorchedGunsTurret() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("scguns", path);
    }

    private static Map.Entry<ResourceLocation, ResourceLocation> gun(String ammo, String gun) {
        return Map.entry(id(ammo), id(gun));
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((TagsUpdatedEvent event) -> guns = null);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, ScorchedGunsTurret::onGetProjectile);
        NeoForge.EVENT_BUS.addListener(ScorchedGunsTurret::onUseTick);
        NeoForge.EVENT_BUS.addListener(ScorchedGunsTurret::onUseStop);
    }

    private static Map<Item, GunItem> guns() {
        Map<Item, GunItem> found = guns;
        if (found == null) {
            found = collect();
            guns = found;
        }
        return found;
    }

    private static Map<Item, GunItem> collect() {
        Map<Item, GunItem> best = new HashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof GunItem candidate) || candidate.getGun() == null) {
                continue;
            }
            Gun gun = candidate.getGun();
            FireMode mode = gun.getGeneral().getFireMode();
            if (FireMode.BEAM.equals(mode) || FireMode.SEMI_BEAM.equals(mode)) {
                continue;
            }
            for (Gun.Projectile projectile : gun.getAcceptedProjectiles()) {
                Item ammo = projectile.getItem();
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(ammo);
                if (projectile.firesArrows() || projectile.getDamage() <= 0.0F || key.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)) {
                    continue;
                }
                GunItem current = best.get(ammo);
                if (current == null || better(key, ammo, candidate, current)) {
                    best.put(ammo, candidate);
                }
            }
        }
        return Map.copyOf(best);
    }

    private static boolean better(ResourceLocation ammoId, Item ammo, GunItem candidate, GunItem current) {
        ResourceLocation preferred = PREFERRED.get(ammoId);
        if (BuiltInRegistries.ITEM.getKey(current).equals(preferred)) {
            return false;
        }
        return BuiltInRegistries.ITEM.getKey(candidate).equals(preferred) || damagePerTick(candidate, ammo) < damagePerTick(current, ammo);
    }

    private static double damagePerTick(GunItem item, Item ammo) {
        Gun gun = item.getGun();
        return gun.getProjectileForItem(ammo).getDamage() / Math.max(1.0D, gun.getGeneral().getRate());
    }

    private static boolean isAmmo(ItemStack stack) {
        return !stack.isEmpty() && guns().containsKey(stack.getItem());
    }

    private static List<ItemStack> ammoBoxes(Player player) {
        List<ItemStack> boxes = new ArrayList<>();
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof AmmoBoxItem) {
                boxes.add(stack);
            }
        }
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            IItemHandlerModifiable equipped = handler.getEquippedCurios();
            for (int i = 0; i < equipped.getSlots(); i++) {
                ItemStack stack = equipped.getStackInSlot(i);
                if (stack.getItem() instanceof AmmoBoxItem) {
                    boxes.add(stack);
                }
            }
        });
        return boxes;
    }

    private static ItemStack findAmmo(Player player) {
        if (isAmmo(player.getOffhandItem())) {
            return player.getOffhandItem();
        }
        for (ItemStack stack : player.getInventory().items) {
            if (isAmmo(stack)) {
                return stack;
            }
        }
        for (ItemStack box : ammoBoxes(player)) {
            ItemStack stored = AmmoBoxItem.getContents(box).filter(ScorchedGunsTurret::isAmmo).findFirst().orElse(ItemStack.EMPTY);
            if (!stored.isEmpty()) {
                return stored;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void consume(Player player, Item ammo) {
        if (player.getOffhandItem().is(ammo)) {
            player.getOffhandItem().shrink(1);
            return;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ammo)) {
                stack.shrink(1);
                return;
            }
        }
        for (ItemStack box : ammoBoxes(player)) {
            if (AmmoBoxItem.shrinkAmmo(box, ammo, 1, player.registryAccess()) > 0) {
                return;
            }
        }
    }

    private static void onGetProjectile(LivingGetProjectileEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(event.getProjectileWeaponItemStack().getItem() instanceof ShoulderTurretItem turret)) {
            return;
        }
        ItemStack ammo = findAmmo(player);
        if (!ammo.isEmpty()) {
            event.setProjectileItemStack(ammo);
            return;
        }
        if (!event.getProjectileItemStack().is(ItemTags.ARROWS)) {
            return;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.is(ItemTags.ARROWS) && turret.getAllSupportedProjectiles().test(stack)) {
                event.setProjectileItemStack(stack);
                return;
            }
        }
        event.setProjectileItemStack(player.isCreative() ? new ItemStack(BuiltInRegistries.ITEM.get(DEFAULT_AMMO)) : ItemStack.EMPTY);
    }

    private static void onUseTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getItem().getItem() instanceof ShoulderTurretItem)) {
            return;
        }
        ItemStack ammo = player.getProjectile(event.getItem());
        if (ammo.isEmpty() || isAmmo(ammo) && !fire(player, event.getItem(), ammo.getItem())) {
            player.stopUsingItem();
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS);
        }
    }

    private static void onUseStop(LivingEntityUseItemEvent.Stop event) {
        if (!(event.getEntity() instanceof Player player) || !(event.getItem().getItem() instanceof ShoulderTurretItem)) {
            return;
        }
        ItemStack ammo = player.getProjectile(event.getItem());
        if (!isAmmo(ammo)) {
            return;
        }
        if (player instanceof ServerPlayer server && player.getTicksUsingItem() <= 1) {
            fire(server, event.getItem(), ammo.getItem());
        }
        event.setCanceled(true);
    }

    private static boolean fire(ServerPlayer player, ItemStack turret, Item ammo) {
        if (!player.isCreative() && EnergySuitItem.getEnergy(player.getItemBySlot(EquipmentSlot.CHEST)) < ENERGY) {
            return false;
        }
        GunItem item = guns().get(ammo);
        long now = player.level().getGameTime();
        Long next = NEXT_SHOT.get(player.getUUID());
        if (item == null || next != null && now < next && next - now < MAX_DELAY) {
            return true;
        }
        ItemStack stack = loaded(item, ammo);
        Gun gun = item.getModifiedGun(stack);
        Gun.Projectile projectile = Gun.getLoadedProjectile(stack, gun);
        NEXT_SHOT.put(player.getUUID(), now + ScorchedGunsUnload.interval(stack, 1.0D));
        float extra = projectile.getDamage() * (SuitAbilityConfig.SHOULDER_TURRET_DAMAGE.get().floatValue() - 1.0F);
        Vec3 look = player.getLookAngle();
        Vec3 eye = player.getEyePosition();
        Vec3 barrel = barrel(player, eye);
        double distance = Math.max(MIN_CONVERGENCE, HeroAbilities.beam(player, look, AIM_RANGE, 1).end().distanceTo(eye));
        ScorchedGunsUnload.launch(player, stack, item, gun, projectile, look, extra, bullet -> {
            Vec3 motion = bullet.getDeltaMovement();
            double speed = motion.length();
            if (barrel != null && speed > 0.0D) {
                Vec3 aim = eye.add(motion.scale(distance / speed));
                bullet.setPos(barrel);
                bullet.setDeltaMovement(aim.subtract(barrel).normalize().scale(speed));
            }
        });
        ScorchedGunsUnload.sound(player, stack, gun, false);
        SuitFx.turretShot(player.serverLevel(), player, projectile.ejectsCasing());
        player.awardStat(Stats.ITEM_USED.get(turret.getItem()));
        if (!player.isCreative()) {
            consume(player, ammo);
            player.getInventory().armor.forEach(piece -> EnergySuitItem.removeEnergy(piece, ENERGY));
        }
        return true;
    }

    private static Vec3 barrel(LivingEntity shooter, Vec3 eye) {
        Vec3 barrel = SuitFx.turretBarrel(shooter);
        HitResult hit = shooter.level().clip(new ClipContext(eye, barrel, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
        return hit.getType() == HitResult.Type.MISS ? barrel : null;
    }

    private static ItemStack loaded(GunItem item, Item ammo) {
        ItemStack stack = new ItemStack(item);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(LOADED_PROJECTILE + BuiltInRegistries.ITEM.getKey(item),
                BuiltInRegistries.ITEM.getKey(ammo).toString()));
        return stack;
    }

    public static ItemStack variantGun() {
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(VARIANT_GUN));
        ScorchedGunsVillains.prepareGunner(stack, new CompoundTag(), 0);
        return stack;
    }

    public static void variantShot(ServerLevel level, Mob mob, ItemStack gun, LivingEntity target) {
        ScorchedGunsVillains.shoot(mob, gun, target.position().add(0.0D, target.getBbHeight() * 0.6D, 0.0D), 1.0F);
        SuitFx.turretShot(level, mob, true);
    }
}
