package com.craisinlord.integrated_superheroes.boss;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class WitherstormModel extends GeoModel<WitherstormEntity> {
    private static final ResourceLocation SMALL_MODEL = IntegratedSuperheroes.id("geo/entity/the_storm_small.geo.json");
    private static final ResourceLocation SMALL_ANIMATIONS = IntegratedSuperheroes.id("animations/entity/the_storm_small.animation.json");
    private static final ResourceLocation SMALL_TEXTURE = IntegratedSuperheroes.id("textures/entity/witherstorm/the_storm_small.png");
    private static final ResourceLocation BIG_MODEL = IntegratedSuperheroes.id("geo/entity/the_storm.geo.json");
    private static final ResourceLocation BIG_ANIMATIONS = IntegratedSuperheroes.id("animations/entity/the_storm.animation.json");
    private static final ResourceLocation BIG_TEXTURE = IntegratedSuperheroes.id("textures/entity/witherstorm/the_storm.png");

    @Override
    public ResourceLocation getModelResource(WitherstormEntity animatable) {
        return animatable.isSmallForm() ? SMALL_MODEL : BIG_MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(WitherstormEntity animatable) {
        return animatable.isSmallForm() ? SMALL_TEXTURE : BIG_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(WitherstormEntity animatable) {
        return animatable.isSmallForm() ? SMALL_ANIMATIONS : BIG_ANIMATIONS;
    }
}
