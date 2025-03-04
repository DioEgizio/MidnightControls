/*
 * Copyright © 2021 LambdAurora <aurora42lambda@gmail.com>
 *
 * This file is part of midnightcontrols.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package eu.midnightdust.midnightcontrols.client.mixin;

import eu.midnightdust.midnightcontrols.MidnightControls;
import eu.midnightdust.midnightcontrols.MidnightControlsFeature;
import eu.midnightdust.midnightcontrols.client.MidnightControlsClient;
import eu.midnightdust.midnightcontrols.client.MidnightInput;
import eu.midnightdust.midnightcontrols.client.gui.MidnightControlsRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow
    @Nullable
    public HitResult crosshairTarget;

    @Shadow
    @Nullable
    public ClientPlayerEntity player;

    @Shadow
    @Nullable
    public ClientPlayerInteractionManager interactionManager;

    @Shadow
    @Nullable
    public ClientWorld world;

    @Shadow
    @Final
    public GameRenderer gameRenderer;

    @Shadow
    private int itemUseCooldown;

    private BlockPos midnightcontrols$lastTargetPos;
    private Vec3d midnightcontrols$lastPos;
    private Direction midnightcontrols$lastTargetSide;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        MidnightControlsClient.get().onMcInit((MinecraftClient) (Object) this);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onStartTick(CallbackInfo ci) {
        if (this.player == null)
            return;

        if (!MidnightControlsFeature.FAST_BLOCK_PLACING.isAvailable())
            return;
        if (this.midnightcontrols$lastPos == null)
            this.midnightcontrols$lastPos = this.player.getPos();

        int cooldown = this.itemUseCooldown;
        BlockHitResult hitResult;
        if (this.crosshairTarget != null && this.crosshairTarget.getType() == HitResult.Type.BLOCK && this.player.getAbilities().flying) {
            hitResult = (BlockHitResult) this.crosshairTarget;
            var targetPos = hitResult.getBlockPos();
            var side = hitResult.getSide();

            boolean sidewaysBlockPlacing = this.midnightcontrols$lastTargetPos == null || !targetPos.equals(this.midnightcontrols$lastTargetPos.offset(this.midnightcontrols$lastTargetSide));
            boolean backwardsBlockPlacing = this.player.input.movementForward < 0.0f && (this.midnightcontrols$lastTargetPos == null || targetPos.equals(this.midnightcontrols$lastTargetPos.offset(this.midnightcontrols$lastTargetSide)));

            if (cooldown > 1
                    && !targetPos.equals(this.midnightcontrols$lastTargetPos)
                    && (sidewaysBlockPlacing || backwardsBlockPlacing)) {
                this.itemUseCooldown = 1;
            }

            this.midnightcontrols$lastTargetPos = targetPos.toImmutable();
            this.midnightcontrols$lastTargetSide = side;
        }
        // Removed front placing sprinting as way too cheaty.
        /*else if (this.player.isSprinting()) {
            hitResult = midnightcontrolsClient.get().reacharound.getLastReacharoundResult();
            if (hitResult != null) {
                if (cooldown > 0)
                    this.itemUseCooldown = 0;
            }
        }*/
        this.midnightcontrols$lastPos = this.player.getPos();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(boolean fullRender, CallbackInfo ci) {
        MidnightControlsClient.get().onRender((MinecraftClient) (Object) (this));
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;render(FJZ)V", shift = At.Shift.AFTER))
    private void renderVirtualCursor(boolean fullRender, CallbackInfo ci) {
        MidnightControlsRenderer.renderVirtualCursor(new MatrixStack(), (MinecraftClient) (Object) this);
    }

    @Inject(method = "doItemUse()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/hit/HitResult;getType()Lnet/minecraft/util/hit/HitResult$Type;"), locals = LocalCapture.CAPTURE_FAILEXCEPTION, cancellable = true)
    private void onItemUse(CallbackInfo ci, Hand[] hands, int handCount, int handIndex, Hand hand, ItemStack stackInHand) {
        var mod = MidnightControlsClient.get();
        if (!stackInHand.isEmpty() && this.player.getPitch(0.f) > 35.0F && mod.reacharound.isReacharoundAvailable()) {
            if (this.crosshairTarget != null && this.crosshairTarget.getType() == HitResult.Type.MISS && this.player.isOnGround()) {
                if (!stackInHand.isEmpty() && stackInHand.getItem() instanceof BlockItem) {
                    var hitResult = mod.reacharound.getLastReacharoundResult();

                    if (hitResult == null)
                        return;

                    hitResult = mod.reacharound.withSideForReacharound(hitResult, stackInHand);

                    int previousStackCount = stackInHand.getCount();
                    var result = this.interactionManager.interactBlock(this.player, this.world, hand, hitResult);
                    if (result.isAccepted()) {
                        if (result.shouldSwingHand()) {
                            this.player.swingHand(hand);
                            if (!stackInHand.isEmpty() && (stackInHand.getCount() != previousStackCount || this.interactionManager.hasCreativeInventory())) {
                                this.gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
                            }
                        }

                        ci.cancel();
                    }

                    if (result == ActionResult.FAIL) {
                        ci.cancel();
                    }
                }
            }
        }
    }
}
