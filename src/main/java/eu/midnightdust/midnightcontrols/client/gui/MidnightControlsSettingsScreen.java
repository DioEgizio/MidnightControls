/*
 * Copyright © 2021 LambdAurora <aurora42lambda@gmail.com>
 *
 * This file is part of midnightcontrols.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package eu.midnightdust.midnightcontrols.client.gui;

import eu.midnightdust.midnightcontrols.MidnightControls;
import eu.midnightdust.midnightcontrols.client.MidnightControlsClient;
import eu.midnightdust.midnightcontrols.client.MidnightControlsConfig;
import eu.midnightdust.midnightcontrols.client.controller.Controller;
import eu.midnightdust.midnightcontrols.client.gui.widget.ControllerControlsWidget;
import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.SpruceTexts;
import dev.lambdaurora.spruceui.option.*;
import dev.lambdaurora.spruceui.screen.SpruceScreen;
import dev.lambdaurora.spruceui.widget.AbstractSpruceWidget;
import dev.lambdaurora.spruceui.widget.SpruceLabelWidget;
import dev.lambdaurora.spruceui.widget.container.SpruceContainerWidget;
import dev.lambdaurora.spruceui.widget.container.SpruceOptionListWidget;
import dev.lambdaurora.spruceui.widget.container.tabbed.SpruceTabbedWidget;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

/**
 * Represents the midnightcontrols settings screen.
 */
public class MidnightControlsSettingsScreen extends SpruceScreen {
    private static final Text SDL2_GAMEPAD_TOOL = new LiteralText("SDL2 Gamepad Tool").formatted(Formatting.GREEN);
    public static final String GAMEPAD_TOOL_URL = "https://generalarcade.com/gamepadtool/";
    final MidnightControlsClient mod = MidnightControlsClient.get();
    private final Screen parent;
    // General options
    private final SpruceOption inputModeOption;
    private final SpruceOption autoSwitchModeOption;
    private final SpruceOption rotationSpeedOption;
    private final SpruceOption mouseSpeedOption;
    private final SpruceOption virtualMouseOption;
    private final SpruceOption resetOption;
    // Gameplay options
    private final SpruceOption analogMovementOption;
    private final SpruceOption autoJumpOption;
    private final SpruceOption fastBlockPlacingOption;
    private final SpruceOption frontBlockPlacingOption;
    private final SpruceOption verticalReacharoundOption;
    private final SpruceOption flyDriftingOption;
    private final SpruceOption flyVerticalDriftingOption;
    // Appearance options
    private final SpruceOption controllerTypeOption;
    private final SpruceOption virtualMouseSkinOption;
    private final SpruceOption hudEnableOption;
    private final SpruceOption hudSideOption;
    // Controller options
    private final SpruceOption controllerOption =
            new SpruceCyclingOption("midnightcontrols.menu.controller",
                    amount -> {
                        int id = MidnightControlsConfig.getController().id();
                        id += amount;
                        if (id > GLFW.GLFW_JOYSTICK_LAST)
                            id = GLFW.GLFW_JOYSTICK_1;
                        id = searchNextAvailableController(id, false);
                        MidnightControlsConfig.setController(Controller.byId(id));
                    },
                    option -> {
                        var controller = MidnightControlsConfig.getController();
                        var controllerName = controller.getName();
                        if (!controller.isConnected())
                            return option.getDisplayText(new LiteralText(controllerName).formatted(Formatting.RED));
                        else if (!controller.isGamepad())
                            return option.getDisplayText(new LiteralText(controllerName).formatted(Formatting.GOLD));
                        else
                            return option.getDisplayText(new LiteralText(controllerName));
                    }, null);
    private final SpruceOption secondControllerOption = new SpruceCyclingOption("midnightcontrols.menu.controller2",
            amount -> {
                int id = MidnightControlsConfig.getSecondController().map(Controller::id).orElse(-1);
                id += amount;
                if (id > GLFW.GLFW_JOYSTICK_LAST)
                    id = -1;
                id = searchNextAvailableController(id, true);
                MidnightControlsConfig.setSecondController(id == -1 ? null : Controller.byId(id));
            },
            option -> MidnightControlsConfig.getSecondController().map(controller -> {
                var controllerName = controller.getName();
                if (!controller.isConnected())
                    return option.getDisplayText(new LiteralText(controllerName).formatted(Formatting.RED));
                else if (!controller.isGamepad())
                    return option.getDisplayText(new LiteralText(controllerName).formatted(Formatting.GOLD));
                else
                    return option.getDisplayText(new LiteralText(controllerName));
            }).orElse(option.getDisplayText(SpruceTexts.OPTIONS_OFF.shallowCopy().formatted(Formatting.RED))),
            new TranslatableText("midnightcontrols.tooltip.controller2"));
    private final SpruceOption unfocusedInputOption;
    private final SpruceOption invertsRightXAxis;
    private final SpruceOption invertsRightYAxis;
    private final SpruceOption rightDeadZoneOption;
    private final SpruceOption leftDeadZoneOption;
    private final SpruceOption[] maxAnalogValueOptions = new SpruceOption[]{
            maxAnalogValueOption("midnightcontrols.menu.max_left_x_value", GLFW.GLFW_GAMEPAD_AXIS_LEFT_X),
            maxAnalogValueOption("midnightcontrols.menu.max_left_y_value", GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y),
            maxAnalogValueOption("midnightcontrols.menu.max_right_x_value", GLFW.GLFW_GAMEPAD_AXIS_RIGHT_X),
            maxAnalogValueOption("midnightcontrols.menu.max_right_y_value", GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y)
    };

    private static SpruceOption maxAnalogValueOption(String key, int axis) {
        return new SpruceDoubleOption(key, .25f, 1.f, 0.05f,
                () -> MidnightControlsConfig.getAxisMaxValue(axis),
                newValue -> MidnightControlsConfig.setAxisMaxValue(axis, newValue),
                option -> option.getDisplayText(new LiteralText(String.format("%.2f", option.get()))),
                new TranslatableText(key.replace("menu", "tooltip"))
        );
    }

    private final MutableText controllerMappingsUrlText = new LiteralText("(")
            .append(new LiteralText(GAMEPAD_TOOL_URL).formatted(Formatting.GOLD))
            .append("),");

    private static int searchNextAvailableController(int newId, boolean allowNone) {
        if ((allowNone && newId == -1) || newId == 0) return newId;

        boolean connected = Controller.byId(newId).isConnected();
        if (!connected) {
            newId++;
        }

        if (newId > GLFW.GLFW_JOYSTICK_LAST)
            newId = allowNone ? -1 : GLFW.GLFW_JOYSTICK_1;

        return connected ? newId : searchNextAvailableController(newId, allowNone);
    }

    public MidnightControlsSettingsScreen(Screen parent, boolean hideControls) {
        super(new TranslatableText("midnightcontrols.title.settings"));
        this.parent = parent;
        // General options
        this.inputModeOption = new SpruceCyclingOption("midnightcontrols.menu.controls_mode",
                amount -> {
                    var next = MidnightControlsConfig.controlsMode.next();
                    MidnightControlsConfig.controlsMode = next;
                    MidnightControlsConfig.save();

                    if (this.client.player != null) {
                        ClientPlayNetworking.getSender().sendPacket(MidnightControls.CONTROLS_MODE_CHANNEL, this.mod.makeControlsModeBuffer(next));
                    }
                }, option -> option.getDisplayText(new TranslatableText(MidnightControlsConfig.controlsMode.getTranslationKey())),
                new TranslatableText("midnightcontrols.tooltip.controls_mode"));
        this.autoSwitchModeOption = new SpruceToggleBooleanOption("midnightcontrols.menu.auto_switch_mode", () -> MidnightControlsConfig.autoSwitchMode,
                value -> MidnightControlsConfig.autoSwitchMode = value, new TranslatableText("midnightcontrols.tooltip.auto_switch_mode"));
        this.rotationSpeedOption = new SpruceDoubleOption("midnightcontrols.menu.rotation_speed", 0.0, 100.0, .5f,
                () -> MidnightControlsConfig.rotationSpeed,
                value -> MidnightControlsConfig.rotationSpeed = value, option -> option.getDisplayText(new LiteralText(String.valueOf(option.get()))),
                new TranslatableText("midnightcontrols.tooltip.rotation_speed"));
        this.mouseSpeedOption = new SpruceDoubleOption("midnightcontrols.menu.mouse_speed", 0.0, 150.0, .5f,
                () -> MidnightControlsConfig.mouseSpeed,
                value -> MidnightControlsConfig.mouseSpeed = value, option -> option.getDisplayText(new LiteralText(String.valueOf(option.get()))),
                new TranslatableText("midnightcontrols.tooltip.mouse_speed"));
        this.resetOption = SpruceSimpleActionOption.reset(btn -> {
            // TODO
            MidnightControlsConfig.init("midnightcontrols", MidnightControlsConfig.class);
            var client = MinecraftClient.getInstance();
            this.init(client, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
        });
        // Gameplay options
        this.analogMovementOption = new SpruceToggleBooleanOption("midnightcontrols.menu.analog_movement",
                () -> MidnightControlsConfig.analogMovement, value -> MidnightControlsConfig.analogMovement = value,
                new TranslatableText("midnightcontrols.tooltip.analog_movement"));
        this.autoJumpOption = new SpruceToggleBooleanOption("options.autoJump",
                () -> this.client.options.autoJump,
                newValue -> this.client.options.autoJump = newValue,
                null);
        this.fastBlockPlacingOption = new SpruceToggleBooleanOption("midnightcontrols.menu.fast_block_placing", () -> MidnightControlsConfig.fastBlockPlacing,
                value -> MidnightControlsConfig.fastBlockPlacing = value, new TranslatableText("midnightcontrols.tooltip.fast_block_placing"));
        this.frontBlockPlacingOption = new SpruceToggleBooleanOption("midnightcontrols.menu.reacharound.horizontal", () -> MidnightControlsConfig.horizontalReacharound,
                value -> MidnightControlsConfig.horizontalReacharound = value, new TranslatableText("midnightcontrols.tooltip.reacharound.horizontal"));
        this.verticalReacharoundOption = new SpruceToggleBooleanOption("midnightcontrols.menu.reacharound.vertical", () -> MidnightControlsConfig.verticalReacharound,
                value -> MidnightControlsConfig.verticalReacharound = value, new TranslatableText("midnightcontrols.tooltip.reacharound.vertical"));
        this.flyDriftingOption = new SpruceToggleBooleanOption("midnightcontrols.menu.fly_drifting", () -> MidnightControlsConfig.flyDrifting,
                value -> MidnightControlsConfig.flyDrifting = value, new TranslatableText("midnightcontrols.tooltip.fly_drifting"));
        this.flyVerticalDriftingOption = new SpruceToggleBooleanOption("midnightcontrols.menu.fly_drifting_vertical", () -> MidnightControlsConfig.verticalFlyDrifting,
                value -> MidnightControlsConfig.verticalFlyDrifting = value, new TranslatableText("midnightcontrols.tooltip.fly_drifting_vertical"));
        // Appearance options
        this.controllerTypeOption = new SpruceCyclingOption("midnightcontrols.menu.controller_type",
                amount -> MidnightControlsConfig.controllerType = MidnightControlsConfig.controllerType.next(),
                option -> option.getDisplayText(MidnightControlsConfig.controllerType.getTranslatedText()),
                new TranslatableText("midnightcontrols.tooltip.controller_type"));
        this.virtualMouseSkinOption = new SpruceCyclingOption("midnightcontrols.menu.virtual_mouse.skin",
                amount -> MidnightControlsConfig.virtualMouseSkin = MidnightControlsConfig.virtualMouseSkin.next(),
                option -> option.getDisplayText(MidnightControlsConfig.virtualMouseSkin.getTranslatedText()),
                null);
        this.hudEnableOption = new SpruceToggleBooleanOption("midnightcontrols.menu.hud_enable", () -> MidnightControlsConfig.hudEnable,
                this.mod::setHudEnabled, new TranslatableText("midnightcontrols.tooltip.hud_enable"));
        this.hudSideOption = new SpruceCyclingOption("midnightcontrols.menu.hud_side",
                amount -> MidnightControlsConfig.hudSide = MidnightControlsConfig.hudSide.next(),
                option -> option.getDisplayText(MidnightControlsConfig.hudSide.getTranslatedText()),
                new TranslatableText("midnightcontrols.tooltip.hud_side"));
        // Controller options
        this.rightDeadZoneOption = new SpruceDoubleOption("midnightcontrols.menu.right_dead_zone", 0.05, 1.0, .05f,
                () -> MidnightControlsConfig.rightDeadZone,
                value -> MidnightControlsConfig.rightDeadZone = value, option -> {
            var value = String.valueOf(option.get());
            return option.getDisplayText(new LiteralText(value.substring(0, Math.min(value.length(), 5))));
        }, new TranslatableText("midnightcontrols.tooltip.right_dead_zone"));
        this.leftDeadZoneOption = new SpruceDoubleOption("midnightcontrols.menu.left_dead_zone", 0.05, 1.0, .05f,
                () -> MidnightControlsConfig.leftDeadZone,
                value -> MidnightControlsConfig.leftDeadZone = value, option -> {
            var value = String.valueOf(option.get());
            return option.getDisplayText(new LiteralText(value.substring(0, Math.min(value.length(), 5))));
        }, new TranslatableText("midnightcontrols.tooltip.left_dead_zone"));
        this.invertsRightXAxis = new SpruceToggleBooleanOption("midnightcontrols.menu.invert_right_x_axis", () -> MidnightControlsConfig.invertRightXAxis,
                value -> MidnightControlsConfig.invertRightXAxis = value, null);
        this.invertsRightYAxis = new SpruceToggleBooleanOption("midnightcontrols.menu.invert_right_y_axis", () -> MidnightControlsConfig.invertRightYAxis,
                value -> MidnightControlsConfig.invertRightYAxis = value, null);
        this.unfocusedInputOption = new SpruceToggleBooleanOption("midnightcontrols.menu.unfocused_input", () -> MidnightControlsConfig.unfocusedInput,
                value -> MidnightControlsConfig.unfocusedInput = value, new TranslatableText("midnightcontrols.tooltip.unfocused_input"));
        this.virtualMouseOption = new SpruceToggleBooleanOption("midnightcontrols.menu.virtual_mouse", () -> MidnightControlsConfig.virtualMouse,
                value -> MidnightControlsConfig.virtualMouse = value, new TranslatableText("midnightcontrols.tooltip.virtual_mouse"));
    }

    @Override
    public void removed() {
        MidnightControlsConfig.save();
        super.removed();
    }

    @Override
    public void onClose() {
        MidnightControlsConfig.save();
        super.onClose();
    }

    private int getTextHeight() {
        return (5 + this.textRenderer.fontHeight) * 3 + 5;
    }

    @Override
    protected void init() {
        super.init();

        this.buildTabs();

        this.addDrawableChild(this.resetOption.createWidget(Position.of(this.width / 2 - 155, this.height - 29), 150));
        this.addDrawableChild(new ButtonWidget(this.width / 2 - 155 + 160, this.height - 29, 150, 20, SpruceTexts.GUI_DONE,
                btn -> this.client.setScreen(this.parent)));
    }

    public void buildTabs() {
        var tabs = new SpruceTabbedWidget(Position.of(0, 24), this.width, this.height - 32 - 24,
                null,
                Math.max(116, this.width / 8), 0);
        this.addDrawableChild(tabs);

        tabs.addSeparatorEntry(new TranslatableText("midnightcontrols.menu.separator.general"));
        tabs.addTabEntry(new TranslatableText("midnightcontrols.menu.title.general"), null,
                this::buildGeneralTab);
        tabs.addTabEntry(new TranslatableText("midnightcontrols.menu.title.gameplay"), null,
                this::buildGameplayTab);
        tabs.addTabEntry(new TranslatableText("midnightcontrols.menu.title.visual"), null,
                this::buildVisualTab);

        tabs.addSeparatorEntry(new TranslatableText("options.controls"));
        tabs.addTabEntry(new TranslatableText("midnightcontrols.menu.title.controller_controls"), null,
                this::buildControllerControlsTab);

        tabs.addSeparatorEntry(new TranslatableText("midnightcontrols.menu.separator.controller"));
        tabs.addTabEntry(new TranslatableText("midnightcontrols.menu.title.controller"), null,
                this::buildControllerTab);
        tabs.addTabEntry(new TranslatableText("midnightcontrols.menu.title.mappings.string"), null,
                this::buildMappingsStringEditorTab);
    }

    public SpruceOptionListWidget buildGeneralTab(int width, int height) {
        var list = new SpruceOptionListWidget(Position.origin(), width, height);
        list.addSingleOptionEntry(this.inputModeOption);
        list.addSingleOptionEntry(this.autoSwitchModeOption);
        list.addSingleOptionEntry(this.rotationSpeedOption);
        list.addSingleOptionEntry(this.mouseSpeedOption);
        list.addSingleOptionEntry(this.virtualMouseOption);
        return list;
    }

    public SpruceOptionListWidget buildGameplayTab(int width, int height) {
        var list = new SpruceOptionListWidget(Position.origin(), width, height);
        list.addSingleOptionEntry(this.analogMovementOption);
        list.addSingleOptionEntry(this.fastBlockPlacingOption);
        list.addSingleOptionEntry(this.frontBlockPlacingOption);
        list.addSingleOptionEntry(this.verticalReacharoundOption);
        list.addSingleOptionEntry(this.flyDriftingOption);
        list.addSingleOptionEntry(this.flyVerticalDriftingOption);
        list.addSingleOptionEntry(this.autoJumpOption);
        return list;
    }

    public SpruceOptionListWidget buildVisualTab(int width, int height) {
        var list = new SpruceOptionListWidget(Position.origin(), width, height);
        list.addSingleOptionEntry(this.controllerTypeOption);
        list.addSingleOptionEntry(this.virtualMouseSkinOption);
        list.addSingleOptionEntry(new SpruceSeparatorOption("midnightcontrols.menu.title.hud", true, null));
        list.addSingleOptionEntry(this.hudEnableOption);
        list.addSingleOptionEntry(this.hudSideOption);
        return list;
    }

    public ControllerControlsWidget buildControllerControlsTab(int width, int height) {
        return new ControllerControlsWidget(Position.origin(), width, height);
    }

    public AbstractSpruceWidget buildControllerTab(int width, int height) {
        var root = new SpruceContainerWidget(Position.origin(), width, height);

        var aboutMappings1 = new SpruceLabelWidget(Position.of(0, 2),
                new TranslatableText("midnightcontrols.controller.mappings.1", SDL2_GAMEPAD_TOOL),
                width, true);

        var gamepadToolUrlLabel = new SpruceLabelWidget(Position.of(0, aboutMappings1.getHeight() + 4),
                this.controllerMappingsUrlText, width,
                label -> Util.getOperatingSystem().open(GAMEPAD_TOOL_URL), true);
        gamepadToolUrlLabel.setTooltip(new TranslatableText("chat.link.open"));

        var aboutMappings3 = new SpruceLabelWidget(Position.of(0,
                aboutMappings1.getHeight() + gamepadToolUrlLabel.getHeight() + 6),
                new TranslatableText("midnightcontrols.controller.mappings.3", Formatting.GREEN.toString(), Formatting.RESET.toString()),
                width, true);

        int listHeight = height - 8 - aboutMappings1.getHeight() - aboutMappings3.getHeight() - gamepadToolUrlLabel.getHeight();
        var labels = new SpruceContainerWidget(Position.of(0,
                listHeight),
                width, height - listHeight);
        labels.addChild(aboutMappings1);
        labels.addChild(gamepadToolUrlLabel);
        labels.addChild(aboutMappings3);

        var list = new SpruceOptionListWidget(Position.origin(), width, listHeight);
        list.addSingleOptionEntry(this.controllerOption);
        list.addSingleOptionEntry(this.secondControllerOption);
        list.addSingleOptionEntry(this.unfocusedInputOption);
        list.addOptionEntry(this.invertsRightXAxis, this.invertsRightYAxis);
        list.addSingleOptionEntry(this.rightDeadZoneOption);
        list.addSingleOptionEntry(this.leftDeadZoneOption);
        for (var option : this.maxAnalogValueOptions) {
            list.addSingleOptionEntry(option);
        }

        root.addChild(list);
        root.addChild(labels);
        return root;
    }

    public SpruceContainerWidget buildMappingsStringEditorTab(int width, int height) {
        return new MappingsStringInputWidget(Position.origin(), width, height);
    }

    @Override
    public void renderTitle(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        drawCenteredText(matrices, this.textRenderer, I18n.translate("midnightcontrols.menu.title"), this.width / 2, 8, 16777215);
    }
}
