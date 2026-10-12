package com.craisinlord.integrated_superheroes.suit;

import com.craisinlord.integrated_superheroes.hero.HeroPowers;
import com.craisinlord.integrated_superheroes.hero.HeroSlots;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import com.stxllarplanes.marvellegacy.power.Power;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class SuitAbilityTracker {
    private static final String COOLDOWNS_TAG = "integrated_superheroes_suit_cooldowns";

    private SuitAbilityTracker() {
    }

    public static void tick(ServerPlayer player) {
        if (player.tickCount % 10 == 0) {
            sync(player);
        }
    }

    public static void sync(ServerPlayer player) {
        SuitContext context = player.isSpectator() ? null : SuitAbilities.context(player);
        List<SuitAbility> wanted = SuitAbilities.loadout(context == null ? null : context.suitId());
        Set<String> wantedNames = new HashSet<>();
        wanted.forEach(ability -> wantedNames.add(ability.name()));

        PlayerData data = HeroPowers.data(player);
        data.retainActivePowerSize();
        List<Power> active = data.getActivePowers();
        boolean changed = false;

        for (int i = 0; i < active.size(); i++) {
            Power power = active.get(i);
            if (power != null && SuitAbilities.isSuitAbility(power.getName()) && !wantedNames.contains(power.getName())) {
                rememberCooldown(player, power);
                active.set(i, new Power());
                changed = true;
            }
        }
        Iterator<Power> available = data.getAvailablePowers().iterator();
        while (available.hasNext()) {
            Power power = available.next();
            if (power != null && SuitAbilities.isSuitAbility(power.getName()) && !wantedNames.contains(power.getName())) {
                rememberCooldown(player, power);
                available.remove();
                changed = true;
            }
        }

        for (SuitAbility ability : wanted) {
            if (data.hasActivePower(ability.name()) || data.hasAvailablePower(ability.name())) {
                continue;
            }
            data.addAvailablePowers(SuitAbilities.newPower(ability, remainingCooldown(player, ability.name())));
            changed = true;
        }
        changed |= HeroSlots.arrange(data);

        if (changed) {
            PlayerData.sync(player);
        }
    }

    private static void rememberCooldown(ServerPlayer player, Power power) {
        CompoundTag cooldowns = player.getPersistentData().getCompound(COOLDOWNS_TAG);
        if (power.getCooldown() > 0.0D) {
            cooldowns.putLong(power.getName(), player.level().getGameTime() + (long) Math.ceil(power.getCooldown() * 20.0D));
        } else {
            cooldowns.remove(power.getName());
        }
        player.getPersistentData().put(COOLDOWNS_TAG, cooldowns);
    }

    private static double remainingCooldown(ServerPlayer player, String name) {
        CompoundTag cooldowns = player.getPersistentData().getCompound(COOLDOWNS_TAG);
        long ticks = cooldowns.getLong(name) - player.level().getGameTime();
        return ticks > 0L ? ticks / 20.0D : 0.0D;
    }
}
