package com.craisinlord.integrated_superheroes.mixin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class IntegratedSuperheroesMixinPlugin implements IMixinConfigPlugin {
    private static final Map<String, String> OPTIONAL_PREFIXES = Map.of(
            "AlexsCaves", "alexscaves",
            "Weather2", "weather2",
            "ScorchedGuns", "scguns",
            "IceAndFire", "iceandfire",
            "MutantMonsters", "mutantmonsters",
            "BetterCombat", "bettercombat",
            "Celestisynth", "celestisynth");

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String simpleName = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
        for (Map.Entry<String, String> entry : OPTIONAL_PREFIXES.entrySet()) {
            if (simpleName.startsWith(entry.getKey()) && !isLoaded(entry.getValue())) {
                return false;
            }
        }
        if (simpleName.equals("MarvelArmorCheckMixin")
                && getClass().getClassLoader().getResource("net/tintankgames/marvel/mixin/LivingEntityMixin.class") == null) {
            return false;
        }
        return getClass().getClassLoader().getResource(targetClassName.replace('.', '/') + ".class") != null;
    }

    private static boolean isLoaded(String modId) {
        LoadingModList mods = LoadingModList.get();
        return mods != null && mods.getModFileById(modId) != null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
