/*
 * Copyright © 2020 LambdAurora <aurora42lambda@gmail.com>
 *
 * This file is part of LambdaControls.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package me.lambdaurora.lambdacontrols.client.compat;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * This plugin is only present for the conditional mixins.
 *
 * @author LambdAurora
 * @version 1.2.0
 * @since 1.2.0
 */
public class LambdaControlsMixinPlugin implements IMixinConfigPlugin
{
    private final HashMap<String, Boolean> conditionalMixins = new HashMap<>();

    public LambdaControlsMixinPlugin()
    {
        this.conditionalMixins.put("me.lambdaurora.lambdacontrols.client.compat.mixin.RecipeViewingScreenAccessor", LambdaControlsCompat.isReiPresent());
        this.conditionalMixins.put("me.lambdaurora.lambdacontrols.client.compat.mixin.VillagerRecipeViewingScreenAccessor", LambdaControlsCompat.isReiPresent());
    }

    @Override
    public void onLoad(String mixinPackage)
    {
    }

    @Override
    public String getRefMapperConfig()
    {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName)
    {
        return this.conditionalMixins.getOrDefault(mixinClassName, Boolean.TRUE);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets)
    {
    }

    @Override
    public List<String> getMixins()
    {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo)
    {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo)
    {
    }
}
