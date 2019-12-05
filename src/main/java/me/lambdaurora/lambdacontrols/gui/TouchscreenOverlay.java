/*
 * Copyright © 2019 LambdAurora <aurora42lambda@gmail.com>
 *
 * This file is part of LambdaControls.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package me.lambdaurora.lambdacontrols.gui;

import me.lambdaurora.lambdacontrols.HudSide;
import me.lambdaurora.lambdacontrols.LambdaControls;
import me.lambdaurora.lambdacontrols.util.KeyBindingAccessor;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.server.network.packet.PlayerActionC2SPacket;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the touchscreen overlay
 */
public class TouchscreenOverlay extends Screen
{
    public static final Identifier              WIDGETS_LOCATION        = new Identifier("lambdacontrols", "textures/gui/widgets.png");
    private             LambdaControls          mod;
    private             TouchscreenButtonWidget jump_button;
    private             TouchscreenButtonWidget fly_button;
    private             TouchscreenButtonWidget fly_up_button;
    private             TouchscreenButtonWidget fly_down_button;
    private             int                     fly_button_enable_ticks = 0;
    private             int                     forward_button_tick     = 0;
    private             TouchscreenButtonWidget forward_left_button;
    private             TouchscreenButtonWidget forward_right_button;
    private             TouchscreenButtonWidget start_sneak_button;
    private             TouchscreenButtonWidget end_sneak_button;

    public TouchscreenOverlay(@NotNull LambdaControls mod)
    {
        super(new LiteralText("Touchscreen overlay"));
        this.mod = mod;
        this.passEvents = true;
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        return false;
    }

    private void pause_game(boolean bl)
    {
        if (this.minecraft == null)
            return;
        boolean bl2 = this.minecraft.isIntegratedServerRunning() && !this.minecraft.getServer().isRemote();
        if (bl2) {
            this.minecraft.openScreen(new GameMenuScreen(!bl));
            this.minecraft.getSoundManager().pauseAll();
        } else {
            this.minecraft.openScreen(new GameMenuScreen(true));
        }
    }

    /**
     * Updates the forward button ticks cooldown.
     *
     * @param state The button state.
     */
    private void update_forward_buttons_state(boolean state)
    {
        if (state)
            this.forward_button_tick = -1;
        else
            this.forward_button_tick = 20;
    }

    /**
     * Updates the jump buttons.
     */
    private void update_jump_buttons()
    {
        if (this.minecraft == null)
            return;
        if (this.minecraft.player.abilities.allowFlying && this.minecraft.player.abilities.flying) {
            boolean old_state_fly = this.fly_button.visible;
            this.jump_button.visible = false;
            this.fly_button.visible = true;
            this.fly_up_button.visible = true;
            this.fly_down_button.visible = true;
            if (old_state_fly != this.fly_button.visible) {
                this.fly_button_enable_ticks = 5;
                this.handle_jump(false);
            } else if (this.fly_button_enable_ticks > 0)
                this.fly_button_enable_ticks--;
        } else {
            this.jump_button.visible = true;
            this.fly_button.visible = false;
            this.fly_up_button.visible = false;
            this.fly_down_button.visible = false;
        }
    }

    /**
     * Handles the jump button.
     *
     * @param state The state of the jump button.
     */
    private void handle_jump(boolean state)
    {
        ((KeyBindingAccessor) this.minecraft.options.keyJump).handle_press_state(state);
    }

    @Override
    public void tick()
    {
        if (this.forward_button_tick > 0) {
            this.forward_button_tick--;
        } else if (this.forward_button_tick == 0) {
            if (this.forward_left_button.visible)
                this.forward_left_button.visible = false;
            if (this.forward_right_button.visible)
                this.forward_right_button.visible = false;
        }
        this.update_jump_buttons();
    }

    protected void init()
    {
        super.init();
        int scaled_width = this.minecraft.window.getScaledWidth();
        int scaled_height = this.minecraft.window.getScaledHeight();
        this.addButton(new TexturedButtonWidget(scaled_width / 2 - 20, 0, 20, 20, 0, 106, 20, ButtonWidget.WIDGETS_LOCATION, 256, 256,
                btn -> this.minecraft.openScreen(new ChatScreen("")), ""));
        this.addButton(new TexturedButtonWidget(scaled_width / 2, 0, 20, 20, 0, 0, 20, WIDGETS_LOCATION, 256, 256,
                btn -> this.pause_game(false)));
        // Inventory buttons.
        int inventory_button_x = scaled_width / 2;
        int inventory_button_y = scaled_height - 16 - 5;
        if (this.minecraft.options.mainArm == Arm.LEFT) {
            inventory_button_x = inventory_button_x - 91 - 24;
        } else {
            inventory_button_x = inventory_button_x + 91 + 4;
        }
        this.addButton(new TexturedButtonWidget(inventory_button_x, inventory_button_y, 20, 20, 20, 0, 20, WIDGETS_LOCATION, 256, 256,
                btn -> {
                    if (this.minecraft.interactionManager.hasRidingInventory()) {
                        this.minecraft.player.openRidingInventory();
                    } else {
                        this.minecraft.getTutorialManager().onInventoryOpened();
                        this.minecraft.openScreen(new InventoryScreen(this.minecraft.player));
                    }
                }));
        int jump_button_x, swap_hands_x, sneak_button_x;
        int sneak_button_y = scaled_height - 10 - 40 - 5;
        if (this.mod.config.get_hud_side() == HudSide.LEFT) {
            jump_button_x = scaled_width - 20 - 20;
            swap_hands_x = jump_button_x - 5 - 40;
            sneak_button_x = 10 + 20 + 5;
        } else {
            jump_button_x = 20;
            swap_hands_x = jump_button_x + 5 + 40;
            sneak_button_x = scaled_width - 10 - 40 - 5;
        }
        // Swap items hand.
        this.addButton(new TouchscreenButtonWidget(swap_hands_x, sneak_button_y, 20, 20, 0, 160, 20, WIDGETS_LOCATION,
                state -> {
                    if (state) {
                        if (!this.minecraft.player.isSpectator()) {
                            this.minecraft.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_HELD_ITEMS, BlockPos.ORIGIN, Direction.DOWN));
                        }
                    }
                }));
        // Drop
        this.addButton(new TouchscreenButtonWidget(swap_hands_x, sneak_button_y + 5 + 20, 20, 20, 20, 160, 20, WIDGETS_LOCATION,
                state -> ((KeyBindingAccessor) this.minecraft.options.keyDrop).handle_press_state(state)));
        // Jump keys
        this.addButton(this.jump_button = new TouchscreenButtonWidget(jump_button_x, sneak_button_y, 20, 20, 0, 40, 20, WIDGETS_LOCATION,
                this::handle_jump));
        this.addButton(this.fly_button = new TouchscreenButtonWidget(jump_button_x, sneak_button_y, 20, 20, 20, 40, 20, WIDGETS_LOCATION,
                state -> {
                    if (this.fly_button_enable_ticks == 0) this.minecraft.player.abilities.flying = false;
                }));
        this.addButton(this.fly_up_button = new TouchscreenButtonWidget(jump_button_x, sneak_button_y - 5 - 20, 20, 20, 40, 40, 20, WIDGETS_LOCATION,
                this::handle_jump));
        this.addButton(this.fly_down_button = new TouchscreenButtonWidget(jump_button_x, sneak_button_y + 20 + 5, 20, 20, 60, 40, 20, WIDGETS_LOCATION,
                state -> ((KeyBindingAccessor) this.minecraft.options.keySneak).handle_press_state(state)));
        this.update_jump_buttons();
        // Movements keys
        this.addButton((this.start_sneak_button = new TouchscreenButtonWidget(sneak_button_x, sneak_button_y, 20, 20, 0, 120, 20, WIDGETS_LOCATION,
                state -> {
                    if (state) {
                        ((KeyBindingAccessor) this.minecraft.options.keySneak).handle_press_state(true);
                        this.start_sneak_button.visible = false;
                        this.end_sneak_button.visible = true;
                    }
                })));
        this.addButton((this.end_sneak_button = new TouchscreenButtonWidget(sneak_button_x, sneak_button_y, 20, 20, 20, 120, 20, WIDGETS_LOCATION,
                state -> {
                    if (state) {
                        ((KeyBindingAccessor) this.minecraft.options.keySneak).handle_press_state(false);
                        this.end_sneak_button.visible = false;
                        this.start_sneak_button.visible = true;
                    }
                })));
        this.end_sneak_button.visible = false;
        this.addButton(this.forward_left_button = new TouchscreenButtonWidget(sneak_button_x - 20 - 5, sneak_button_y - 5 - 20, 20, 20, 80, 80, 20, WIDGETS_LOCATION,
                state -> {
                    ((KeyBindingAccessor) this.minecraft.options.keyForward).handle_press_state(state);
                    ((KeyBindingAccessor) this.minecraft.options.keyLeft).handle_press_state(state);
                    this.update_forward_buttons_state(state);
                }));
        this.forward_left_button.visible = false;
        this.addButton(new TouchscreenButtonWidget(sneak_button_x, sneak_button_y - 5 - 20, 20, 20, 0, 80, 20, WIDGETS_LOCATION,
                state -> {
                    ((KeyBindingAccessor) this.minecraft.options.keyForward).handle_press_state(state);
                    this.update_forward_buttons_state(state);
                    this.forward_left_button.visible = true;
                    this.forward_right_button.visible = true;
                }));
        this.addButton(this.forward_right_button = new TouchscreenButtonWidget(sneak_button_x + 20 + 5, sneak_button_y - 5 - 20, 20, 20, 100, 80, 20, WIDGETS_LOCATION,
                state -> {
                    ((KeyBindingAccessor) this.minecraft.options.keyForward).handle_press_state(state);
                    ((KeyBindingAccessor) this.minecraft.options.keyRight).handle_press_state(state);
                    this.update_forward_buttons_state(state);
                }));
        this.forward_right_button.visible = true;
        this.addButton(new TouchscreenButtonWidget(sneak_button_x + 20 + 5, sneak_button_y, 20, 20, 20, 80, 20, WIDGETS_LOCATION,
                state -> ((KeyBindingAccessor) this.minecraft.options.keyRight).handle_press_state(state)));
        this.addButton(new TouchscreenButtonWidget(sneak_button_x, sneak_button_y + 20 + 5, 20, 20, 40, 80, 20, WIDGETS_LOCATION,
                state -> ((KeyBindingAccessor) this.minecraft.options.keyBack).handle_press_state(state)));
        this.addButton(new TouchscreenButtonWidget(sneak_button_x - 20 - 5, sneak_button_y, 20, 20, 60, 80, 20, WIDGETS_LOCATION,
                state -> ((KeyBindingAccessor) this.minecraft.options.keyLeft).handle_press_state(state)));
    }

    @Override
    public void render(int mouseX, int mouseY, float delta)
    {
        super.render(mouseX, mouseY, delta);
    }
}
