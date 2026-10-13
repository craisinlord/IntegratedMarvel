package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public class SwarmSquirrelModel extends GeoModel<SwarmSquirrel> {
    private static final ResourceLocation MODEL = IntegratedSuperheroes.id("geo/swarm_squirrel.geo.json");
    private static final ResourceLocation ANIMATIONS = IntegratedSuperheroes.id("animations/swarm_squirrel.animation.json");
    private static final ResourceLocation REGULAR_TEXTURE = IntegratedSuperheroes.id("textures/entity/swarm_squirrel/swarm_squirrel.png");
    private static final ResourceLocation BROWN_TEXTURE = IntegratedSuperheroes.id("textures/entity/swarm_squirrel/swarm_squirrel_brown.png");
    private static final ResourceLocation ALBINO_TEXTURE = IntegratedSuperheroes.id("textures/entity/swarm_squirrel/swarm_squirrel_albino.png");
    private static final float SHOULDER_BODY_ROT_X = 65.0F * Mth.DEG_TO_RAD;
    private static final float SHOULDER_TAIL_ROT_X = 42.5F * Mth.DEG_TO_RAD;
    private static final float SHOULDER_HEAD_ROT_X = -60.0F * Mth.DEG_TO_RAD;

    @Override
    public ResourceLocation getModelResource(SwarmSquirrel animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(SwarmSquirrel animatable) {
        return switch (animatable.getTextureVariantName()) {
            case "brown" -> BROWN_TEXTURE;
            case "albino" -> ALBINO_TEXTURE;
            default -> REGULAR_TEXTURE;
        };
    }

    @Override
    public ResourceLocation getAnimationResource(SwarmSquirrel animatable) {
        return ANIMATIONS;
    }

    @Override
    public void setCustomAnimations(SwarmSquirrel animatable, long instanceId, AnimationState<SwarmSquirrel> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        if (!animatable.isOnShoulder()) {
            return;
        }
        this.getBone("body").ifPresent(bone -> bone.setRotX(SHOULDER_BODY_ROT_X));
        this.getBone("left_arm").ifPresent(bone -> bone.setRotZ(0.0F));
        this.getBone("right_arm").ifPresent(bone -> bone.setRotZ(0.0F));
        this.getBone("tail").ifPresent(bone -> bone.setRotX(SHOULDER_TAIL_ROT_X));
        this.getBone("head").ifPresent(bone -> {
            bone.setRotX(SHOULDER_HEAD_ROT_X);
            bone.setPosY(-1.0425F);
            bone.setPosZ(-1.31366F);
        });
        this.getBone("left_flying_part").ifPresent(SwarmSquirrelModel::hide);
        this.getBone("right_flying_part").ifPresent(SwarmSquirrelModel::hide);
        this.getBone("left_cheeks").ifPresent(SwarmSquirrelModel::hide);
        this.getBone("right_cheeks").ifPresent(SwarmSquirrelModel::hide);
    }

    private static void hide(GeoBone bone) {
        bone.setScaleX(0.0F);
        bone.setScaleY(0.0F);
        bone.setScaleZ(0.0F);
    }
}
