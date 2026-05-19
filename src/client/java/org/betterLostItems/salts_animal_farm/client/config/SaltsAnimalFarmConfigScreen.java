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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class SaltsAnimalFarmConfigScreen extends Screen {
    private static final int NORMAL_TEXT = 0xFFE0E0E0;
    private static final int ERROR_TEXT = 0xFFFF5555;
    private static final int LABEL_TEXT = 0xFFA0A0A0;
    private static final int HELP_TEXT = 0xFFD7D7D7;
    private static final int HELP_BOX_BACKGROUND = 0xAA101010;
    private static final int HELP_BOX_BORDER = 0xFF4F4F4F;

    private final Screen parent;
    private final MutableConfig values;
    private final List<HoverRegion> hoverRegions = new ArrayList<>();
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
        drawHelpBox(graphics, mouseX, mouseY);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private void rebuildPage() {
        clearWidgets();
        hoverRegions.clear();

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
                values.comfortTaskReachTimeoutTicks = defaults.comfortTaskReachTimeoutTicks();
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
        addInt("Task Reach Timeout Ticks", values.comfortTaskReachTimeoutTicks, value -> values.comfortTaskReachTimeoutTicks = value, y);
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
        hoverRegions.add(new HoverRegion(left, top, boxWidth, boxHeight + 22, descriptionFor(label)));
    }

    private void addBoolean(String label, boolean value, Consumer<Boolean> setter, int y) {
        addLabel(label, y);
        addRenderableWidget(CycleButton.onOffBuilder(value)
                .create(inputX(), y, inputWidth(), 20, Component.empty(), (button, selected) -> {
                    setter.accept(selected);
                    apply("Updated " + label.toLowerCase(Locale.ROOT));
                }));
        addHoverRegion(label, y);
    }

    private void addInt(String label, int value, IntConsumer setter, int y) {
        addLabel(label, y);
        EditBox box = new EditBox(font, inputX(), y, inputWidth(), 20, Component.literal(label));
        box.setMaxLength(12);
        box.setTextColor(NORMAL_TEXT);
        box.setTextColorUneditable(NORMAL_TEXT);
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
        addHoverRegion(label, y);
    }

    private void addDouble(String label, double value, Consumer<Double> setter, int y) {
        addLabel(label, y);
        EditBox box = new EditBox(font, inputX(), y, inputWidth(), 20, Component.literal(label));
        box.setMaxLength(12);
        box.setTextColor(NORMAL_TEXT);
        box.setTextColorUneditable(NORMAL_TEXT);
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
        addHoverRegion(label, y);
    }

    private void addLabel(String label, int y) {
        addRenderableOnly(new StringWidget(labelX(), y + 5, labelWidth(), 10, Component.literal(label), font));
    }

    private void apply(String message) {
        Salts_animal_farm.updateConfig(values.toConfig());
        values.copyFrom(Salts_animal_farm.CONFIG);
        status = message;
    }

    private void addHoverRegion(String label, int y) {
        int left = labelX();
        int right = inputX() + inputWidth();
        hoverRegions.add(new HoverRegion(left, y, right - left, 20, descriptionFor(label)));
    }

    private void drawHelpBox(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        String text = hoverText(mouseX, mouseY);
        boolean error = status.startsWith("Invalid");
        int boxWidth = Math.min(520, width - 40);
        int textWidth = boxWidth - 12;
        int boxHeight = Math.max(30, font.wordWrapHeight(Component.literal(text), textWidth) + 12);
        int boxX = Math.max(20, width / 2 - boxWidth / 2);
        int boxY = Math.max(50, height - 28 - boxHeight - 8);

        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, HELP_BOX_BACKGROUND);
        graphics.outline(boxX, boxY, boxWidth, boxHeight, HELP_BOX_BORDER);
        graphics.textWithWordWrap(font, Component.literal(text), boxX + 6, boxY + 6, textWidth, error ? ERROR_TEXT : HELP_TEXT);
    }

    private String hoverText(int mouseX, int mouseY) {
        for (HoverRegion region : hoverRegions) {
            if (region.contains(mouseX, mouseY)) {
                return region.description();
            }
        }

        return status;
    }

    private static String descriptionFor(String label) {
        return switch (label) {
            case "Enable Mod" -> "Turns all Salt's Animal Farm behavior on or off. When disabled, animal goals, weighted loot, debug labels, fear, rain, and interaction features are inactive.";
            case "Enable Rain Behavior" -> "Controls whether configured farm animals seek cover in rain, build rain exposure, ignore space tasks while raining, and avoid wet paths while sheltered.";
            case "Minimum Weight" -> "Lowest weight value a farm animal can have after clamping. Weight controls how many normal loot rolls the animal drops.";
            case "Maximum Weight" -> "Highest weight value a farm animal can reach from comfort successes and other weight changes.";
            case "Average Task Delay Ticks" -> "Average delay between comfort task attempts. Twenty ticks is about one second.";
            case "Delay Jitter Ticks" -> "Random timing variation added around the average task delay so animals do not all act at the same moment.";
            case "Linger Ticks" -> "How long an animal should remain at a completed comfort condition before the task is considered stable.";
            case "Task Reach Timeout Ticks" -> "How long an animal has to reach or satisfy a comfort task target before the attempt fails. Twenty ticks is about one second.";
            case "Maximum Task Ticks" -> "Absolute safety cap for any comfort task attempt. The stricter of this and the reach timeout ends normal tasks.";
            case "Comfort Move Speed" -> "Movement speed used while an animal travels toward comfort task targets.";
            case "Search Radius" -> "Horizontal block radius used when finding comfort task targets.";
            case "Vertical Search" -> "Vertical block range above and below the animal used when finding comfort task targets.";
            case "Search Samples" -> "Number of extra random target samples checked after the deterministic search.";
            case "Hostile Scare Radius" -> "Distance around a farm animal scanned for configured scary mobs.";
            case "Scan Interval Ticks" -> "How often animals scan for scary mobs. Lower values react faster but do more work.";
            case "Scan Random Offset Ticks" -> "Random offset applied to scare scans so animals do not all scan on the same tick.";
            case "Scare Cooldown Ticks" -> "Minimum time before the same animal can lose weight again from hostile scare behavior.";
            case "Hostile Flee Speed" -> "Movement speed used when farm animals flee scary mobs.";
            case "Kill Witness Radius" -> "Distance from a player-caused animal death where other farm animals can witness it.";
            case "Maximum Kill Witnesses" -> "Maximum number of nearby farm animals processed as witnesses to a player-caused animal death.";
            case "Frantic Duration Ticks" -> "How long farm animals remain frantic after player damage, death witnessing, or similar fear events.";
            case "Frantic Repath Ticks" -> "How often frantic animals pick a new panic movement target.";
            case "Frantic Move Speed" -> "Movement speed used while animals are frantic.";
            case "Detailed Debug Information" -> "Enables verbose server log output for comfort task target searching, task ticking, and completion decisions.";
            case "Farm Animals" -> "Entity IDs or entity tags that receive this mod's farm-animal behavior, weight data, loot changes, and debug data.";
            case "Scary Mobs" -> "Entity IDs or entity tags that farm animals treat as scary when scanning for threats.";
            case "Soft Blocks" -> "Block IDs or block tags that count as soft nap spots for the night nap comfort task.";
            default -> "Changes apply immediately.";
        };
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

    private record HoverRegion(int x, int y, int width, int height, String description) {
        private boolean contains(int mouseX, int mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
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
        private int comfortTaskReachTimeoutTicks;
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
            comfortTaskReachTimeoutTicks = config.comfortTaskReachTimeoutTicks();
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
                    comfortTaskReachTimeoutTicks,
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
