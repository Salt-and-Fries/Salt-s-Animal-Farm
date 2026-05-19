package org.betterLostItems.salts_animal_farm.client.config;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;
import org.betterLostItems.salts_animal_farm.config.SaltsAnimalFarmConfig;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class SaltsAnimalFarmConfigScreen extends Screen {
    private static final int NORMAL_TEXT = 0xE0E0E0;
    private static final int ERROR_TEXT = 0xFF5555;
    private static final int LABEL_TEXT = 0xA0A0A0;

    private final Screen parent;
    private final MutableConfig values;
    private Page page = Page.GENERAL;
    private String status = "Changes apply immediately";

    public SaltsAnimalFarmConfigScreen(Screen parent) {
        super(Component.literal("Salt's Animal Farm Config"));
        this.parent = parent;
        this.values = MutableConfig.from(Salts_animal_farm.CONFIG);
    }

    @Override
    protected void init() {
        rebuildPage();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 12, 0xFFFFFF);
        graphics.centeredText(font, Component.literal(page.title), width / 2, 34, 0xFFD966);
        graphics.centeredText(font, Component.literal(status), width / 2, height - 50, status.startsWith("Invalid") ? ERROR_TEXT : LABEL_TEXT);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private void rebuildPage() {
        clearWidgets();

        int topButtonY = 28;
        addRenderableWidget(Button.builder(Component.literal("<"), button -> switchPage(-1))
                .bounds(20, topButtonY, 28, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> switchPage(1))
                .bounds(width - 48, topButtonY, 28, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Reset Page"), button -> resetPage())
                .bounds(width / 2 - 104, height - 28, 100, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> minecraft.setScreen(parent))
                .bounds(width / 2 + 4, height - 28, 100, 20)
                .build());

        switch (page) {
            case GENERAL -> buildGeneralPage();
            case FARM_ANIMALS -> buildListPage("Farm Animals", values.farmAnimals, list -> values.farmAnimals = list);
            case SCARY_MOBS -> buildListPage("Scary Mobs", values.scaryMobs, list -> values.scaryMobs = list);
            case SOFT_BLOCKS -> buildListPage("Soft Blocks", values.softBlocks, list -> values.softBlocks = list);
            case COMFORT_TIMING -> buildComfortTimingPage();
            case COMFORT_SEARCH -> buildComfortSearchPage();
            case HOSTILE_FEAR -> buildHostileFearPage();
            case FRANTIC_FEAR -> buildFranticFearPage();
            case DEBUG -> buildDebugPage();
        }
    }

    private void switchPage(int direction) {
        Page[] pages = Page.values();
        int next = (page.ordinal() + direction + pages.length) % pages.length;
        page = pages[next];
        status = "Changes apply immediately";
        rebuildPage();
    }

    private void resetPage() {
        SaltsAnimalFarmConfig defaults = SaltsAnimalFarmConfig.DEFAULT;

        switch (page) {
            case GENERAL -> {
                values.enableMod = defaults.modEnabled();
                values.enableRainBehavior = defaults.rainBehaviorEnabled();
                values.minimumWeight = defaults.minimumWeight();
                values.maximumWeight = defaults.maximumWeight();
            }
            case FARM_ANIMALS -> values.farmAnimals = defaults.farmAnimals();
            case SCARY_MOBS -> values.scaryMobs = defaults.scaryMobs();
            case SOFT_BLOCKS -> values.softBlocks = defaults.softBlocks();
            case COMFORT_TIMING -> {
                values.comfortTaskAverageDelayTicks = defaults.comfortTaskAverageDelayTicks();
                values.comfortTaskDelayJitterTicks = defaults.comfortTaskDelayJitterTicks();
                values.comfortLingerTicks = defaults.comfortLingerTicks();
                values.comfortMaxTaskTicks = defaults.comfortMaxTaskTicks();
                values.comfortMoveSpeed = defaults.comfortMoveSpeed();
            }
            case COMFORT_SEARCH -> {
                values.comfortSearchRadius = defaults.comfortSearchRadius();
                values.comfortVerticalSearch = defaults.comfortVerticalSearch();
                values.comfortSearchSamples = defaults.comfortSearchSamples();
            }
            case HOSTILE_FEAR -> {
                values.hostileScareRadius = defaults.hostileScareRadius();
                values.hostileScanIntervalTicks = defaults.hostileScanIntervalTicks();
                values.hostileScanRandomOffsetTicks = defaults.hostileScanRandomOffsetTicks();
                values.hostileScareCooldownTicks = defaults.hostileScareCooldownTicks();
                values.hostileFleeSpeed = defaults.hostileFleeSpeed();
            }
            case FRANTIC_FEAR -> {
                values.killWitnessRadius = defaults.killWitnessRadius();
                values.maxKillWitnesses = defaults.maxKillWitnesses();
                values.franticDurationTicks = defaults.franticDurationTicks();
                values.franticRepathTicks = defaults.franticRepathTicks();
                values.franticMoveSpeed = defaults.franticMoveSpeed();
            }
            case DEBUG -> values.enableDetailedDebugInformation = defaults.enableDetailedDebugInformation();
        }

        apply("Reset " + page.title.toLowerCase(Locale.ROOT));
        rebuildPage();
    }

    private void buildGeneralPage() {
        int y = contentTop();
        addBoolean("Enable Mod", values.enableMod, value -> values.enableMod = value, y);
        y += 26;
        addBoolean("Enable Rain Behavior", values.enableRainBehavior, value -> values.enableRainBehavior = value, y);
        y += 26;
        addInt("Minimum Weight", values.minimumWeight, value -> values.minimumWeight = value, y);
        y += 26;
        addInt("Maximum Weight", values.maximumWeight, value -> values.maximumWeight = value, y);
    }

    private void buildComfortTimingPage() {
        int y = contentTop();
        addInt("Average Task Delay Ticks", values.comfortTaskAverageDelayTicks, value -> values.comfortTaskAverageDelayTicks = value, y);
        y += 26;
        addInt("Delay Jitter Ticks", values.comfortTaskDelayJitterTicks, value -> values.comfortTaskDelayJitterTicks = value, y);
        y += 26;
        addInt("Linger Ticks", values.comfortLingerTicks, value -> values.comfortLingerTicks = value, y);
        y += 26;
        addInt("Maximum Task Ticks", values.comfortMaxTaskTicks, value -> values.comfortMaxTaskTicks = value, y);
        y += 26;
        addDouble("Comfort Move Speed", values.comfortMoveSpeed, value -> values.comfortMoveSpeed = value, y);
    }

    private void buildComfortSearchPage() {
        int y = contentTop();
        addInt("Search Radius", values.comfortSearchRadius, value -> values.comfortSearchRadius = value, y);
        y += 26;
        addInt("Vertical Search", values.comfortVerticalSearch, value -> values.comfortVerticalSearch = value, y);
        y += 26;
        addInt("Search Samples", values.comfortSearchSamples, value -> values.comfortSearchSamples = value, y);
    }

    private void buildHostileFearPage() {
        int y = contentTop();
        addInt("Hostile Scare Radius", values.hostileScareRadius, value -> values.hostileScareRadius = value, y);
        y += 26;
        addInt("Scan Interval Ticks", values.hostileScanIntervalTicks, value -> values.hostileScanIntervalTicks = value, y);
        y += 26;
        addInt("Scan Random Offset Ticks", values.hostileScanRandomOffsetTicks, value -> values.hostileScanRandomOffsetTicks = value, y);
        y += 26;
        addInt("Scare Cooldown Ticks", values.hostileScareCooldownTicks, value -> values.hostileScareCooldownTicks = value, y);
        y += 26;
        addDouble("Hostile Flee Speed", values.hostileFleeSpeed, value -> values.hostileFleeSpeed = value, y);
    }

    private void buildFranticFearPage() {
        int y = contentTop();
        addInt("Kill Witness Radius", values.killWitnessRadius, value -> values.killWitnessRadius = value, y);
        y += 26;
        addInt("Maximum Kill Witnesses", values.maxKillWitnesses, value -> values.maxKillWitnesses = value, y);
        y += 26;
        addInt("Frantic Duration Ticks", values.franticDurationTicks, value -> values.franticDurationTicks = value, y);
        y += 26;
        addInt("Frantic Repath Ticks", values.franticRepathTicks, value -> values.franticRepathTicks = value, y);
        y += 26;
        addDouble("Frantic Move Speed", values.franticMoveSpeed, value -> values.franticMoveSpeed = value, y);
    }

    private void buildDebugPage() {
        addBoolean("Detailed Debug Information", values.enableDetailedDebugInformation, value -> values.enableDetailedDebugInformation = value, contentTop());
    }

    private void buildListPage(String label, List<String> currentValues, Consumer<List<String>> setter) {
        int left = Math.max(20, width / 2 - 150);
        int top = contentTop();
        int boxWidth = Math.min(300, width - 40);
        int boxHeight = Math.max(80, height - top - 92);

        addRenderableOnly(new StringWidget(left, top, boxWidth, 18, Component.literal(label + " (one entry per line)"), font));

        MultiLineEditBox box = MultiLineEditBox.builder()
                .setX(left)
                .setY(top + 22)
                .setPlaceholder(Component.literal("minecraft:cow\n#minecraft:wool"))
                .build(font, boxWidth, boxHeight, Component.literal(label));
        box.setCharacterLimit(4096);
        box.setValue(String.join("\n", currentValues));
        box.setValueListener(value -> {
            setter.accept(parseList(value));
            apply("Updated " + label.toLowerCase(Locale.ROOT));
        });
        addRenderableWidget(box);
    }

    private void addBoolean(String label, boolean value, Consumer<Boolean> setter, int y) {
        addLabel(label, y);
        addRenderableWidget(CycleButton.onOffBuilder(value)
                .create(inputX(), y, inputWidth(), 20, Component.empty(), (button, selected) -> {
                    setter.accept(selected);
                    apply("Updated " + label.toLowerCase(Locale.ROOT));
                }));
    }

    private void addInt(String label, int value, IntConsumer setter, int y) {
        addLabel(label, y);
        EditBox box = new EditBox(font, inputX(), y, inputWidth(), 20, Component.literal(label));
        box.setMaxLength(12);
        box.setValue(Integer.toString(value));
        box.setResponder(text -> {
            try {
                setter.accept(Integer.parseInt(text.trim()));
                box.setTextColor(NORMAL_TEXT);
                apply("Updated " + label.toLowerCase(Locale.ROOT));
            } catch (NumberFormatException exception) {
                box.setTextColor(ERROR_TEXT);
                status = "Invalid number: " + label;
            }
        });
        addRenderableWidget(box);
    }

    private void addDouble(String label, double value, Consumer<Double> setter, int y) {
        addLabel(label, y);
        EditBox box = new EditBox(font, inputX(), y, inputWidth(), 20, Component.literal(label));
        box.setMaxLength(12);
        box.setValue(Double.toString(value));
        box.setResponder(text -> {
            try {
                setter.accept(Double.parseDouble(text.trim()));
                box.setTextColor(NORMAL_TEXT);
                apply("Updated " + label.toLowerCase(Locale.ROOT));
            } catch (NumberFormatException exception) {
                box.setTextColor(ERROR_TEXT);
                status = "Invalid number: " + label;
            }
        });
        addRenderableWidget(box);
    }

    private void addLabel(String label, int y) {
        addRenderableOnly(new StringWidget(labelX(), y + 5, labelWidth(), 10, Component.literal(label), font));
    }

    private void apply(String message) {
        Salts_animal_farm.updateConfig(values.toConfig());
        values.copyFrom(Salts_animal_farm.CONFIG);
        status = message;
    }

    private static List<String> parseList(String value) {
        return Arrays.stream(value.split("[\\r\\n,]+"))
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .toList();
    }

    private int contentTop() {
        return 58;
    }

    private int labelX() {
        return Math.max(20, width / 2 - 155);
    }

    private int labelWidth() {
        return Math.max(90, Math.min(160, width / 2 - 30));
    }

    private int inputX() {
        return Math.min(width - inputWidth() - 20, width / 2 + 25);
    }

    private int inputWidth() {
        return Math.min(150, Math.max(96, width / 2 - 35));
    }

    private enum Page {
        GENERAL("General"),
        FARM_ANIMALS("Farm Animals"),
        SCARY_MOBS("Scary Mobs"),
        SOFT_BLOCKS("Soft Blocks"),
        COMFORT_TIMING("Comfort Timing"),
        COMFORT_SEARCH("Comfort Search"),
        HOSTILE_FEAR("Hostile Fear"),
        FRANTIC_FEAR("Frantic Fear"),
        DEBUG("Debug");

        private final String title;

        Page(String title) {
            this.title = title;
        }
    }

    private static final class MutableConfig {
        private boolean enableMod;
        private List<String> farmAnimals;
        private List<String> scaryMobs;
        private List<String> softBlocks;
        private int minimumWeight;
        private int maximumWeight;
        private boolean enableRainBehavior;
        private int comfortTaskAverageDelayTicks;
        private int comfortTaskDelayJitterTicks;
        private int comfortSearchRadius;
        private int comfortVerticalSearch;
        private int comfortSearchSamples;
        private int comfortLingerTicks;
        private int comfortMaxTaskTicks;
        private double comfortMoveSpeed;
        private int hostileScareRadius;
        private int hostileScanIntervalTicks;
        private int hostileScanRandomOffsetTicks;
        private int hostileScareCooldownTicks;
        private double hostileFleeSpeed;
        private int killWitnessRadius;
        private int maxKillWitnesses;
        private int franticDurationTicks;
        private int franticRepathTicks;
        private double franticMoveSpeed;
        private boolean enableDetailedDebugInformation;

        private static MutableConfig from(SaltsAnimalFarmConfig config) {
            MutableConfig values = new MutableConfig();
            values.copyFrom(config);
            return values;
        }

        private void copyFrom(SaltsAnimalFarmConfig config) {
            enableMod = config.modEnabled();
            farmAnimals = config.farmAnimals();
            scaryMobs = config.scaryMobs();
            softBlocks = config.softBlocks();
            minimumWeight = config.minimumWeight();
            maximumWeight = config.maximumWeight();
            enableRainBehavior = config.rainBehaviorEnabled();
            comfortTaskAverageDelayTicks = config.comfortTaskAverageDelayTicks();
            comfortTaskDelayJitterTicks = config.comfortTaskDelayJitterTicks();
            comfortSearchRadius = config.comfortSearchRadius();
            comfortVerticalSearch = config.comfortVerticalSearch();
            comfortSearchSamples = config.comfortSearchSamples();
            comfortLingerTicks = config.comfortLingerTicks();
            comfortMaxTaskTicks = config.comfortMaxTaskTicks();
            comfortMoveSpeed = config.comfortMoveSpeed();
            hostileScareRadius = config.hostileScareRadius();
            hostileScanIntervalTicks = config.hostileScanIntervalTicks();
            hostileScanRandomOffsetTicks = config.hostileScanRandomOffsetTicks();
            hostileScareCooldownTicks = config.hostileScareCooldownTicks();
            hostileFleeSpeed = config.hostileFleeSpeed();
            killWitnessRadius = config.killWitnessRadius();
            maxKillWitnesses = config.maxKillWitnesses();
            franticDurationTicks = config.franticDurationTicks();
            franticRepathTicks = config.franticRepathTicks();
            franticMoveSpeed = config.franticMoveSpeed();
            enableDetailedDebugInformation = config.enableDetailedDebugInformation();
        }

        private SaltsAnimalFarmConfig toConfig() {
            return new SaltsAnimalFarmConfig(
                    enableMod,
                    farmAnimals,
                    scaryMobs,
                    softBlocks,
                    minimumWeight,
                    maximumWeight,
                    enableRainBehavior,
                    comfortTaskAverageDelayTicks,
                    comfortTaskDelayJitterTicks,
                    comfortSearchRadius,
                    comfortVerticalSearch,
                    comfortSearchSamples,
                    comfortLingerTicks,
                    comfortMaxTaskTicks,
                    comfortMoveSpeed,
                    hostileScareRadius,
                    hostileScanIntervalTicks,
                    hostileScanRandomOffsetTicks,
                    hostileScareCooldownTicks,
                    hostileFleeSpeed,
                    killWitnessRadius,
                    maxKillWitnesses,
                    franticDurationTicks,
                    franticRepathTicks,
                    franticMoveSpeed,
                    enableDetailedDebugInformation
            );
        }
    }
}
