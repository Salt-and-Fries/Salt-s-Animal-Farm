package org.betterLostItems.salts_animal_farm.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;
import org.betterLostItems.salts_animal_farm.config.SaltsAnimalFarmConfig;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

public class SaltsAnimalFarmConfigScreen extends Screen {
    private static final int NORMAL_TEXT = 0xFFE8E8E8;
    private static final int MUTED_TEXT = 0xFFA9A9A9;
    private static final int ERROR_TEXT = 0xFFFF5555;
    private static final int SECTION_TEXT = 0xFFFFD966;
    private static final int ROW_BACKGROUND = 0x44101010;
    private static final int ROW_HOVER_BACKGROUND = 0x66303030;
    private static final int SELECTED_GREEN = 0xFF55DD77;
    private static final int DARK_OUTLINE = 0xFF4D4D4D;
    private static final int CARD_BACKGROUND = 0x77151515;
    private static final int CARD_HOVER_BACKGROUND = 0x99282828;
    private static final int SCROLLBAR_TRACK = 0x55303030;
    private static final int SCROLLBAR_THUMB = 0xFF9B9B9B;
    private static final int SCROLLBAR_THUMB_HOVER = 0xFFC8C8C8;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int MIN_SCROLLBAR_THUMB_HEIGHT = 24;

    private final Screen parent;
    private final MutableConfig values;
    private int mainScroll;
    private boolean mainScrollbarDragging;
    private int mainScrollbarDragOffset;
    private String status = "Changes apply immediately";

    public SaltsAnimalFarmConfigScreen(Screen parent) {
        super(Component.literal("Salt's Animal Farm Config"));
        this.parent = parent;
        this.values = MutableConfig.from(Salts_animal_farm.CONFIG);
    }

    @Override
    protected void init() {
        rebuildMainScreen();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xC0101010);
        renderMainRows(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFFFF);
        graphics.drawString(font, Component.literal(status), 20, height - 22, status.startsWith("Invalid") ? ERROR_TEXT : MUTED_TEXT);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollMain(scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && beginMainScrollbarDrag(event.x(), event.y())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == 0 && mainScrollbarDragging) {
            updateMainScrollbarDrag(event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && mainScrollbarDragging) {
            mainScrollbarDragging = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    private void rebuildMainScreen() {
        clearWidgets();
        List<SettingRow> rows = buildSettingsRows();
        mainScroll = clamp(mainScroll, 0, maxMainScroll(rows));

        int listTop = 32;
        int listBottom = height - 38;
        int rowWidth = rowWidth(740);
        int rowX = (width - rowWidth) / 2;
        int controlWidth = 124;
        int y = listTop;

        for (int index = mainScroll; index < rows.size() && y < listBottom; index++) {
            SettingRow row = rows.get(index);
            if (y + row.height() > listBottom) {
                break;
            }
            row.addControl(rowX, y, rowWidth, controlWidth);
            y += row.height();
        }

        addRenderableWidget(Button.builder(Component.literal("Reset All"), button -> resetAll())
                .bounds(width / 2 - 104, height - 28, 100, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> minecraft.setScreen(parent))
                .bounds(width / 2 + 4, height - 28, 100, 20)
                .build());
    }

    private void renderMainRows(GuiGraphics graphics, int mouseX, int mouseY) {
        List<SettingRow> rows = buildSettingsRows();
        int listTop = 32;
        int listBottom = height - 38;
        int rowWidth = rowWidth(740);
        int rowX = (width - rowWidth) / 2;
        int y = listTop;

        for (int index = mainScroll; index < rows.size() && y < listBottom; index++) {
            SettingRow row = rows.get(index);
            if (y + row.height() > listBottom) {
                break;
            }
            row.render(graphics, rowX, y, rowWidth, mouseX, mouseY);
            y += row.height();
        }
        renderScrollbar(
                graphics,
                scrollbarX(rowX, rowWidth),
                listTop,
                listBottom - listTop,
                totalSettingRowsHeight(rows),
                maxMainScroll(rows),
                mainScroll,
                mouseX,
                mouseY,
                mainScrollbarDragging
        );
    }

    private List<SettingRow> buildSettingsRows() {
        List<SettingRow> rows = new ArrayList<>();
        SaltsAnimalFarmConfig screenConfig = values.toConfig().sanitized();
        SaltsAnimalFarmConfig.Preset selectedPreset = screenConfig.selectedPreset();
        SaltsAnimalFarmConfig.EffectiveValues effectiveValues = screenConfig.effectiveValues(previewDifficulty());
        boolean customPreset = selectedPreset == SaltsAnimalFarmConfig.Preset.CUSTOM;

        section(rows, "General");
        rows.add(new PresetRow("Preset", "Controls difficulty-tuned values. Dynamic follows the current world difficulty; Custom unlocks preset-controlled fields.", selectedPreset));
        bool(rows, "Enable Mod", "Master switch for animal goals, weighted loot, fear, rain behavior, debug labels, and commands.", () -> values.enableMod, value -> values.enableMod = value);
        bool(rows, "Enable Rain Behavior", "Farm animals seek cover, track rain exposure, and avoid exposed rain paths while sheltered.", () -> values.enableRainBehavior, value -> values.enableRainBehavior = value);
        intRow(rows, "Minimum Weight", presetDescription("Lowest clamped weight value an animal can have.", customPreset), () -> customPreset ? values.minimumWeight : effectiveValues.minimumWeight(), value -> values.minimumWeight = value, () -> customPreset);
        intRow(rows, "Maximum Weight", presetDescription("Highest clamped weight value an animal can reach.", customPreset), () -> customPreset ? values.maximumWeight : effectiveValues.maximumWeight(), value -> values.maximumWeight = value, () -> customPreset);
        intRow(rows, "Positive Streak Threshold", presetDescription("Comfort successes in a row needed before each successful task starts adding weight.", customPreset), () -> customPreset ? values.positiveTaskStreakThreshold : effectiveValues.positiveTaskStreakThreshold(), value -> values.positiveTaskStreakThreshold = value, () -> customPreset);
        intRow(rows, "Negative Streak Threshold", presetDescription("Comfort failures in a row needed before each failed task starts removing weight.", customPreset), () -> customPreset ? values.negativeTaskStreakThreshold : effectiveValues.negativeTaskStreakThreshold(), value -> values.negativeTaskStreakThreshold = value, () -> customPreset);
        doubleRow(rows, "Sick Movement Speed", "Movement speed multiplier used while an animal is sick at weight 0.", () -> values.sickMovementSpeedMultiplier, value -> values.sickMovementSpeedMultiplier = value);
        bool(rows, "Allow Non-Bred Sickness", "When enabled, spawn eggs, commands, natural spawns, and bred animals can all fall to weight 0.", () -> values.allowNonBredAnimalsToBecomeSick, value -> values.allowNonBredAnimalsToBecomeSick = value);

        section(rows, "Lists");
        rows.add(new ActionRow("Farm Animals", () -> selectionSummary(values.farmAnimals, "entry", "entries"), "Choose which mobs receive farm-animal behavior and weight data.", () -> minecraft.setScreen(new MobPickerScreen(this, MobListKind.FARM_ANIMALS))));
        rows.add(new ActionRow("Scary Mobs", () -> selectionSummary(values.scaryMobs, "entry", "entries"), "Choose which mobs farm animals treat as scary threats.", () -> minecraft.setScreen(new MobPickerScreen(this, MobListKind.SCARY_MOBS))));
        rows.add(new ActionRow("Soft Blocks", () -> selectionSummary(values.softBlocks, "entry", "entries"), "Choose block tags and blocks that count as soft nap spots.", () -> minecraft.setScreen(new BlockPickerScreen(this))));

        section(rows, "Comfort Timing");
        intRow(rows, "Average Task Delay Ticks", "Average delay between comfort task attempts. Twenty ticks is about one second.", () -> values.comfortTaskAverageDelayTicks, value -> values.comfortTaskAverageDelayTicks = value);
        intRow(rows, "Delay Jitter Ticks", "Random timing spread so animals do not all act on the same tick.", () -> values.comfortTaskDelayJitterTicks, value -> values.comfortTaskDelayJitterTicks = value);
        intRow(rows, "Linger Ticks", "How long an animal remains at a completed comfort condition before it counts as stable.", () -> values.comfortLingerTicks, value -> values.comfortLingerTicks = value);
        intRow(rows, "Task Reach Timeout Ticks", "How long an animal has to reach or satisfy a comfort target before the attempt fails.", () -> values.comfortTaskReachTimeoutTicks, value -> values.comfortTaskReachTimeoutTicks = value);
        intRow(rows, "Maximum Task Ticks", "Absolute safety cap for any comfort task attempt.", () -> values.comfortMaxTaskTicks, value -> values.comfortMaxTaskTicks = value);
        doubleRow(rows, "Comfort Move Speed", "Movement speed used while traveling toward comfort task targets.", () -> values.comfortMoveSpeed, value -> values.comfortMoveSpeed = value);

        section(rows, "Comfort Search");
        intRow(rows, "Search Radius", "Horizontal block search radius for soft blocks and shelter.", () -> values.comfortSearchRadius, value -> values.comfortSearchRadius = value);
        intRow(rows, "Vertical Search", "Vertical block search range around the animal.", () -> values.comfortVerticalSearch, value -> values.comfortVerticalSearch = value);
        intRow(rows, "Search Samples", "Number of nearby candidates checked per task attempt.", () -> values.comfortSearchSamples, value -> values.comfortSearchSamples = value);

        section(rows, "Hostile Avoidance");
        intRow(rows, "Hostile Scare Radius", "Distance used to find scary mobs around farm animals.", () -> values.hostileScareRadius, value -> values.hostileScareRadius = value);
        intRow(rows, "Hostile Scan Interval", "Ticks between hostile scans.", () -> values.hostileScanIntervalTicks, value -> values.hostileScanIntervalTicks = value);
        intRow(rows, "Hostile Scan Random Offset", "Extra randomized scan delay.", () -> values.hostileScanRandomOffsetTicks, value -> values.hostileScanRandomOffsetTicks = value);
        intRow(rows, "Hostile Scare Cooldown", "Minimum ticks between new scare reactions.", () -> values.hostileScareCooldownTicks, value -> values.hostileScareCooldownTicks = value);
        doubleRow(rows, "Hostile Flee Speed", "Movement speed while fleeing scary mobs.", () -> values.hostileFleeSpeed, value -> values.hostileFleeSpeed = value);

        section(rows, "Death Witnesses");
        bool(rows, "Lose Weight When Hit", "Player damage can remove weight from farm animals.", () -> values.loseWeightWhenHitByPlayer, value -> values.loseWeightWhenHitByPlayer = value);
        bool(rows, "Lose Weight On Witnessed Death", "Nearby farm animals can lose weight when another animal dies.", () -> values.loseWeightWhenWitnessingAnimalDeath, value -> values.loseWeightWhenWitnessingAnimalDeath = value);
        intRow(rows, "Kill Witness Radius", "Distance used to find animals that witnessed a death.", () -> values.killWitnessRadius, value -> values.killWitnessRadius = value);
        intRow(rows, "Maximum Witnesses", "Maximum nearby animals affected by one death.", () -> values.maxKillWitnesses, value -> values.maxKillWitnesses = value);
        intRow(rows, "Frantic Duration Ticks", "How long witnesses run around after seeing a death.", () -> values.franticDurationTicks, value -> values.franticDurationTicks = value);
        intRow(rows, "Frantic Repath Ticks", "How often frantic animals pick a new panic target.", () -> values.franticRepathTicks, value -> values.franticRepathTicks = value);
        doubleRow(rows, "Frantic Move Speed", "Movement speed used while frantic.", () -> values.franticMoveSpeed, value -> values.franticMoveSpeed = value);

        section(rows, "Debug");
        bool(rows, "Detailed Debug Information", "Enables extra debug log messages and makes debug label tools more useful.", () -> values.enableDetailedDebugInformation, value -> values.enableDetailedDebugInformation = value);
        return rows;
    }

    private void section(List<SettingRow> rows, String label) {
        rows.add(new SectionRow(label));
    }

    private void bool(List<SettingRow> rows, String label, String description, BooleanSupplier getter, Consumer<Boolean> setter) {
        rows.add(new BooleanRow(label, description, getter, setter));
    }

    private void intRow(List<SettingRow> rows, String label, String description, IntSupplier getter, IntConsumer setter) {
        intRow(rows, label, description, getter, setter, () -> true);
    }

    private void intRow(List<SettingRow> rows, String label, String description, IntSupplier getter, IntConsumer setter, BooleanSupplier active) {
        rows.add(new IntRow(label, description, getter, setter, active));
    }

    private void doubleRow(List<SettingRow> rows, String label, String description, DoubleSupplier getter, DoubleConsumer setter) {
        rows.add(new DoubleRow(label, description, getter, setter));
    }

    private void resetAll() {
        values.copyFrom(SaltsAnimalFarmConfig.DEFAULT);
        apply("Reset all settings", true);
    }

    private void resetList(MobListKind kind) {
        setEntries(kind, kind == MobListKind.FARM_ANIMALS ? SaltsAnimalFarmConfig.DEFAULT.farmAnimals() : SaltsAnimalFarmConfig.DEFAULT.scaryMobs());
    }

    private void resetSoftBlocks() {
        setSoftBlockEntries(SaltsAnimalFarmConfig.DEFAULT.softBlocks());
    }

    private void setPreset(SaltsAnimalFarmConfig.Preset preset) {
        values.preset = preset.serializedName();
        if (preset != SaltsAnimalFarmConfig.Preset.CUSTOM) {
            copyEffectivePresetValues(values.toConfig().sanitized().effectiveValues(previewDifficulty()));
        }
        apply("Updated preset", true);
    }

    private void copyEffectivePresetValues(SaltsAnimalFarmConfig.EffectiveValues effectiveValues) {
        values.minimumWeight = effectiveValues.minimumWeight();
        values.maximumWeight = effectiveValues.maximumWeight();
        values.positiveTaskStreakThreshold = effectiveValues.positiveTaskStreakThreshold();
        values.negativeTaskStreakThreshold = effectiveValues.negativeTaskStreakThreshold();
    }

    private Difficulty previewDifficulty() {
        return minecraft == null || minecraft.level == null ? Difficulty.NORMAL : minecraft.level.getDifficulty();
    }

    private static String presetDescription(String description, boolean editable) {
        return editable ? description : description + " Select Custom to edit this value.";
    }

    private void apply(String message, boolean rebuild) {
        Salts_animal_farm.updateConfig(values.toConfig());
        status = message;
        if (rebuild) {
            init();
        }
    }

    private List<String> currentEntries(MobListKind kind) {
        return kind == MobListKind.FARM_ANIMALS ? values.farmAnimals : values.scaryMobs;
    }

    private void setEntries(MobListKind kind, Collection<String> entries) {
        if (kind == MobListKind.FARM_ANIMALS) {
            values.farmAnimals = cleanEntries(entries);
        } else {
            values.scaryMobs = cleanEntries(entries);
        }
        apply("Updated " + kind.title.toLowerCase(Locale.ROOT), false);
    }

    private void setSoftBlockEntries(Collection<String> entries) {
        values.softBlocks = cleanEntries(entries);
        apply("Updated soft blocks", false);
    }

    private static List<String> cleanEntries(Collection<String> entries) {
        LinkedHashSet<String> cleaned = new LinkedHashSet<>();
        for (String entry : entries) {
            if (entry != null && !entry.isBlank()) {
                cleaned.add(entry.trim());
            }
        }
        return new ArrayList<>(cleaned);
    }

    private static String normalizeCustomEntry(String text, boolean allowTag) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        boolean tag = trimmed.startsWith("#");
        if (tag && !allowTag) {
            return null;
        }
        String idText = tag ? trimmed.substring(1) : trimmed;
        Identifier id = Identifier.tryParse(idText);
        if (id == null) {
            return null;
        }
        return tag ? "#" + id : id.toString();
    }

    private static String selectionSummary(List<String> values, String singular, String plural) {
        int count = values == null ? 0 : values.size();
        return count + " " + (count == 1 ? singular : plural);
    }

    private static String trimToWidth(Font font, String text, int width) {
        if (font.width(text) <= width) {
            return text;
        }
        String ellipsis = "...";
        int allowed = Math.max(0, width - font.width(ellipsis));
        return font.plainSubstrByWidth(text, allowed) + ellipsis;
    }

    private static String searchText(String... values) {
        return String.join(" ", values).toLowerCase(Locale.ROOT);
    }

    private static boolean matchesQuery(String searchable, String query) {
        return query == null || query.isBlank() || searchable.contains(query.trim().toLowerCase(Locale.ROOT));
    }

    private static String idString(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
    }

    private static String idString(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    private static String shortIdentifier(Identifier id) {
        return "minecraft".equals(id.getNamespace()) ? id.getPath() : id.toString();
    }

    private static String displayIdentifier(Identifier id) {
        return id.toString();
    }

    private static String titleFromPath(String path) {
        String[] words = path.split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
        }
        return builder.toString();
    }

    private static boolean isConfigurableMob(EntityType<?> type) {
        return type.canSummon() && type.getCategory() != MobCategory.MISC;
    }

    private static boolean isPassiveMob(EntityType<?> type) {
        MobCategory category = type.getCategory();
        return category.isFriendly() && category != MobCategory.MONSTER;
    }

    private static boolean entityMatchesTag(EntityType<?> type, String entry) {
        TagKey<EntityType<?>> tag = entityTag(entry);
        return tag != null && BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).is(tag);
    }

    private static TagKey<EntityType<?>> entityTag(String entry) {
        if (!entry.startsWith("#")) {
            return null;
        }
        Identifier id = Identifier.tryParse(entry.substring(1));
        return id == null ? null : TagKey.create(Registries.ENTITY_TYPE, id);
    }

    private static List<String> expandEntityTag(String entry, EntityType<?> excludedType) {
        TagKey<EntityType<?>> tag = entityTag(entry);
        if (tag == null) {
            return List.of();
        }
        return StreamSupport.stream(BuiltInRegistries.ENTITY_TYPE.getTagOrEmpty(tag).spliterator(), false)
                .map(Holder::value)
                .filter(type -> type != excludedType)
                .filter(SaltsAnimalFarmConfigScreen::isConfigurableMob)
                .sorted(Comparator.comparing(type -> type.getDescription().getString()))
                .map(SaltsAnimalFarmConfigScreen::idString)
                .toList();
    }

    private static boolean blockMatchesTag(Block block, String entry) {
        TagKey<Block> tag = blockTag(entry);
        return tag != null && BuiltInRegistries.BLOCK.wrapAsHolder(block).is(tag);
    }

    private static TagKey<Block> blockTag(String entry) {
        if (!entry.startsWith("#")) {
            return null;
        }
        Identifier id = Identifier.tryParse(entry.substring(1));
        return id == null ? null : TagKey.create(Registries.BLOCK, id);
    }

    private static List<String> expandBlockTag(String entry, Block excludedBlock) {
        TagKey<Block> tag = blockTag(entry);
        if (tag == null) {
            return List.of();
        }
        return StreamSupport.stream(BuiltInRegistries.BLOCK.getTagOrEmpty(tag).spliterator(), false)
                .map(Holder::value)
                .filter(block -> block != excludedBlock)
                .filter(block -> block.asItem() != Items.AIR)
                .sorted(Comparator.comparing(block -> new ItemStack(block).getHoverName().getString()))
                .map(SaltsAnimalFarmConfigScreen::idString)
                .toList();
    }

    private static void drawOutline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y + 1, x + 1, y + height - 1, color);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    private int rowWidth(int max) {
        return Math.min(max, Math.max(260, width - 72));
    }

    private int scrollbarX(int rowX, int rowWidth) {
        return Math.min(width - SCROLLBAR_WIDTH - 8, rowX + rowWidth + 8);
    }

    private boolean scrollMain(double delta) {
        List<SettingRow> rows = buildSettingsRows();
        return setMainScroll(mainScroll + scrollDirection(delta), rows);
    }

    private boolean setMainScroll(int scroll, List<SettingRow> rows) {
        int next = clamp(scroll, 0, maxMainScroll(rows));
        if (next == mainScroll) {
            return false;
        }
        mainScroll = next;
        init();
        return true;
    }

    private int maxMainScroll(List<SettingRow> rows) {
        int viewportHeight = Math.max(0, height - 38 - 32);
        int trailingHeight = 0;
        for (int index = rows.size() - 1; index >= 0; index--) {
            trailingHeight += rows.get(index).height();
            if (trailingHeight >= viewportHeight) {
                return index;
            }
        }
        return 0;
    }

    private int totalSettingRowsHeight(List<SettingRow> rows) {
        int total = 0;
        for (SettingRow row : rows) {
            total += row.height();
        }
        return total;
    }

    private boolean beginMainScrollbarDrag(double mouseX, double mouseY) {
        List<SettingRow> rows = buildSettingsRows();
        int listTop = 32;
        int viewportHeight = Math.max(0, height - 38 - listTop);
        int rowWidth = rowWidth(740);
        int rowX = (width - rowWidth) / 2;
        int maxScroll = maxMainScroll(rows);
        int contentHeight = totalSettingRowsHeight(rows);
        int scrollbarX = scrollbarX(rowX, rowWidth);
        if (!isInScrollbar(mouseX, mouseY, scrollbarX, listTop, viewportHeight, contentHeight, maxScroll)) {
            return false;
        }
        int thumbHeight = scrollbarThumbHeight(viewportHeight, contentHeight);
        int thumbY = scrollbarThumbY(listTop, viewportHeight, thumbHeight, mainScroll, maxScroll);
        mainScrollbarDragOffset = mouseY >= thumbY && mouseY <= thumbY + thumbHeight ? (int) mouseY - thumbY : thumbHeight / 2;
        mainScrollbarDragging = true;
        updateMainScrollbarDrag(mouseY);
        return true;
    }

    private void updateMainScrollbarDrag(double mouseY) {
        List<SettingRow> rows = buildSettingsRows();
        int listTop = 32;
        int viewportHeight = Math.max(0, height - 38 - listTop);
        int maxScroll = maxMainScroll(rows);
        int thumbHeight = scrollbarThumbHeight(viewportHeight, totalSettingRowsHeight(rows));
        setMainScroll(scrollForScrollbarMouse(mouseY, listTop, viewportHeight, thumbHeight, mainScrollbarDragOffset, maxScroll), rows);
    }

    private void renderScrollbar(GuiGraphics graphics, int x, int y, int viewportHeight, int contentHeight, int maxScroll, int scroll, int mouseX, int mouseY, boolean dragging) {
        if (!shouldShowScrollbar(viewportHeight, contentHeight, maxScroll)) {
            return;
        }
        int thumbHeight = scrollbarThumbHeight(viewportHeight, contentHeight);
        int thumbY = scrollbarThumbY(y, viewportHeight, thumbHeight, scroll, maxScroll);
        boolean hovered = dragging || (mouseX >= x - 2 && mouseX < x + SCROLLBAR_WIDTH + 2 && mouseY >= thumbY && mouseY < thumbY + thumbHeight);
        graphics.fill(x, y, x + SCROLLBAR_WIDTH, y + viewportHeight, SCROLLBAR_TRACK);
        graphics.fill(x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumbHeight, hovered ? SCROLLBAR_THUMB_HOVER : SCROLLBAR_THUMB);
    }

    private static boolean isInScrollbar(double mouseX, double mouseY, int x, int y, int viewportHeight, int contentHeight, int maxScroll) {
        return shouldShowScrollbar(viewportHeight, contentHeight, maxScroll)
                && mouseX >= x - 3
                && mouseX < x + SCROLLBAR_WIDTH + 3
                && mouseY >= y
                && mouseY < y + viewportHeight;
    }

    private static boolean shouldShowScrollbar(int viewportHeight, int contentHeight, int maxScroll) {
        return viewportHeight > 0 && contentHeight > viewportHeight && maxScroll > 0;
    }

    private static int scrollbarThumbHeight(int viewportHeight, int contentHeight) {
        if (viewportHeight <= 0 || contentHeight <= 0) {
            return 0;
        }
        return clamp((int) Math.round((double) viewportHeight * viewportHeight / contentHeight), MIN_SCROLLBAR_THUMB_HEIGHT, viewportHeight);
    }

    private static int scrollbarThumbY(int y, int viewportHeight, int thumbHeight, int scroll, int maxScroll) {
        if (maxScroll <= 0 || viewportHeight <= thumbHeight) {
            return y;
        }
        return y + (int) Math.round((viewportHeight - thumbHeight) * (scroll / (double) maxScroll));
    }

    private static int scrollForScrollbarMouse(double mouseY, int y, int viewportHeight, int thumbHeight, int dragOffset, int maxScroll) {
        int travel = viewportHeight - thumbHeight;
        if (travel <= 0 || maxScroll <= 0) {
            return 0;
        }
        double ratio = (mouseY - dragOffset - y) / travel;
        return clamp((int) Math.round(ratio * maxScroll), 0, maxScroll);
    }

    private static int scrollDirection(double delta) {
        return delta > 0.0D ? -1 : 1;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void showTooltip(GuiGraphics graphics, List<Component> lines, int mouseX, int mouseY) {
        graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

    private abstract class SettingRow {
        final String label;
        final String description;

        SettingRow(String label, String description) {
            this.label = label;
            this.description = description;
        }

        int height() {
            return 38;
        }

        void render(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY) {
            boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height() - 2;
            graphics.fill(x, y + 1, x + width, y + height() - 2, hovered ? ROW_HOVER_BACKGROUND : ROW_BACKGROUND);
            int controlX = x + width - 124;
            int labelWidth = Math.max(70, controlX - x - 20);
            graphics.drawString(font, Component.literal(trimToWidth(font, label, labelWidth)), x + 8, y + 6, NORMAL_TEXT);
            graphics.drawString(font, Component.literal(trimToWidth(font, description, labelWidth)), x + 8, y + 19, MUTED_TEXT);
        }

        abstract void addControl(int x, int y, int width, int controlWidth);
    }

    private final class SectionRow extends SettingRow {
        SectionRow(String label) {
            super(label, "");
        }

        @Override
        int height() {
            return 24;
        }

        @Override
        void render(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY) {
            graphics.drawString(font, Component.literal(label), x + 8, y + 8, SECTION_TEXT);
        }

        @Override
        void addControl(int x, int y, int width, int controlWidth) {
        }
    }

    private final class BooleanRow extends SettingRow {
        private final BooleanSupplier getter;
        private final Consumer<Boolean> setter;

        BooleanRow(String label, String description, BooleanSupplier getter, Consumer<Boolean> setter) {
            super(label, description);
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        void addControl(int x, int y, int width, int controlWidth) {
            addRenderableWidget(Button.builder(Component.literal(getter.getAsBoolean() ? "On" : "Off"), button -> {
                        setter.accept(!getter.getAsBoolean());
                        apply("Updated " + label.toLowerCase(Locale.ROOT), true);
                    })
                    .bounds(x + width - controlWidth, y + 9, controlWidth, 20)
                    .build());
        }
    }

    private final class PresetRow extends SettingRow {
        private final SaltsAnimalFarmConfig.Preset preset;

        PresetRow(String label, String description, SaltsAnimalFarmConfig.Preset preset) {
            super(label, description);
            this.preset = preset;
        }

        @Override
        void addControl(int x, int y, int width, int controlWidth) {
            addRenderableWidget(Button.builder(Component.literal(preset.displayName()), button -> setPreset(preset.next()))
                    .bounds(x + width - controlWidth, y + 9, controlWidth, 20)
                    .build());
        }
    }

    private final class IntRow extends SettingRow {
        private final IntSupplier getter;
        private final IntConsumer setter;
        private final BooleanSupplier active;

        IntRow(String label, String description, IntSupplier getter, IntConsumer setter, BooleanSupplier active) {
            super(label, description);
            this.getter = getter;
            this.setter = setter;
            this.active = active;
        }

        @Override
        void addControl(int x, int y, int width, int controlWidth) {
            EditBox box = new EditBox(font, x + width - 96, y + 9, 96, 20, Component.literal(label));
            box.setMaxLength(12);
            box.setTextColor(NORMAL_TEXT);
            box.setTextColorUneditable(MUTED_TEXT);
            box.setEditable(active.getAsBoolean());
            box.active = active.getAsBoolean();
            box.setValue(Integer.toString(getter.getAsInt()));
            box.setResponder(text -> {
                if (!box.isActive()) {
                    return;
                }
                try {
                    setter.accept(Integer.parseInt(text.trim()));
                    box.setTextColor(NORMAL_TEXT);
                    apply("Updated " + label.toLowerCase(Locale.ROOT), false);
                } catch (NumberFormatException exception) {
                    box.setTextColor(ERROR_TEXT);
                    status = "Invalid number: " + label;
                }
            });
            addRenderableWidget(box);
        }
    }

    private final class DoubleRow extends SettingRow {
        private final DoubleSupplier getter;
        private final DoubleConsumer setter;

        DoubleRow(String label, String description, DoubleSupplier getter, DoubleConsumer setter) {
            super(label, description);
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        void addControl(int x, int y, int width, int controlWidth) {
            EditBox box = new EditBox(font, x + width - 96, y + 9, 96, 20, Component.literal(label));
            box.setMaxLength(12);
            box.setTextColor(NORMAL_TEXT);
            box.setTextColorUneditable(NORMAL_TEXT);
            box.setValue(Double.toString(getter.getAsDouble()));
            box.setResponder(text -> {
                try {
                    setter.accept(Double.parseDouble(text.trim()));
                    box.setTextColor(NORMAL_TEXT);
                    apply("Updated " + label.toLowerCase(Locale.ROOT), false);
                } catch (NumberFormatException exception) {
                    box.setTextColor(ERROR_TEXT);
                    status = "Invalid number: " + label;
                }
            });
            addRenderableWidget(box);
        }
    }

    private final class ActionRow extends SettingRow {
        private final Supplier<String> summary;
        private final Runnable action;

        ActionRow(String label, Supplier<String> summary, String description, Runnable action) {
            super(label, description);
            this.summary = summary;
            this.action = action;
        }

        @Override
        void addControl(int x, int y, int width, int controlWidth) {
            addRenderableWidget(Button.builder(Component.literal("Edit (" + summary.get() + ")"), button -> action.run())
                    .bounds(x + width - controlWidth, y + 9, controlWidth, 20)
                    .build());
        }
    }

    private enum MobListKind {
        FARM_ANIMALS("Farm Animals", "Passive Only"),
        SCARY_MOBS("Scary Mobs", "Hostile Only");

        private final String title;
        private final String filterLabel;

        MobListKind(String title, String filterLabel) {
            this.title = title;
            this.filterLabel = filterLabel;
        }
    }

    private final class MobPickerScreen extends Screen {
        private final SaltsAnimalFarmConfigScreen owner;
        private final MobListKind kind;
        private int scroll;
        private boolean scrollbarDragging;
        private int scrollbarDragOffset;
        private boolean filterEnabled = true;
        private boolean addingCustom;
        private String customDraft = "";
        private String query = "";
        private EditBox searchBox;
        private EditBox customInput;

        MobPickerScreen(SaltsAnimalFarmConfigScreen owner, MobListKind kind) {
            super(Component.literal(kind.title));
            this.owner = owner;
            this.kind = kind;
        }

        @Override
        protected void init() {
            clearWidgets();
            int left = Math.max(20, width / 2 - Math.min(420, width - 40) / 2);
            int filterWidth = Math.min(130, Math.max(100, width / 3));
            addRenderableWidget(Button.builder(Component.literal(kind.filterLabel + ": " + (filterEnabled ? "On" : "Off")), button -> {
                        filterEnabled = !filterEnabled;
                        scroll = 0;
                        init();
                    })
                    .bounds(left, 34, filterWidth, 20)
                    .build());

            int searchWidth = Math.min(210, Math.max(120, width - left - filterWidth - 140));
            searchBox = new EditBox(font, left + filterWidth + 8, 34, searchWidth, 20, Component.literal("Search mobs"));
            searchBox.setHint(Component.literal("Search mobs"));
            searchBox.setValue(query);
            searchBox.setResponder(value -> {
                query = value;
                scroll = 0;
                init();
            });
            addRenderableWidget(searchBox);

            addRenderableWidget(Button.builder(Component.literal(addingCustom ? "Close" : "Add Custom"), button -> {
                        addingCustom = !addingCustom;
                        init();
                    })
                    .bounds(Math.max(20, width - 116), 34, 96, 20)
                    .build());

            int listTop = addingCustom ? 90 : 62;
            if (addingCustom) {
                customInput = new EditBox(font, left, 62, Math.min(260, width - left - 140), 20, Component.literal("Custom entity"));
                customInput.setHint(Component.literal("modname:entity"));
                customInput.setValue(customDraft);
                customInput.setResponder(value -> customDraft = value);
                addRenderableWidget(customInput);
                addRenderableWidget(Button.builder(Component.literal("Add"), button -> submitCustomEntry())
                        .bounds(customInput.getX() + customInput.getWidth() + 8, 62, 54, 20)
                        .build());
            }

            List<MobRow> rows = mobRows();
            scroll = clamp(scroll, 0, Math.max(0, rows.size() - visibleMobRows(listTop)));
            addMobRowButtons(rows, listTop);

            addRenderableWidget(Button.builder(Component.literal("Reset List"), button -> {
                        owner.resetList(kind);
                        scroll = 0;
                        init();
                    })
                    .bounds(width / 2 - 104, height - 28, 100, 20)
                    .build());
            addRenderableWidget(Button.builder(Component.literal("Done"), button -> minecraft.setScreen(owner))
                    .bounds(width / 2 + 4, height - 28, 100, 20)
                    .build());
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, width, height, 0xC0101010);
            int listTop = addingCustom ? 90 : 62;
            renderMobRows(graphics, mouseX, mouseY, listTop);
            super.render(graphics, mouseX, mouseY, partialTick);
            renderMobTooltip(graphics, mouseX, mouseY, listTop);
            graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFFFF);
            graphics.drawCenteredString(font, Component.literal(owner.status), width / 2, 22, owner.status.startsWith("Invalid") ? ERROR_TEXT : MUTED_TEXT);
        }

        @Override
        public void onClose() {
            minecraft.setScreen(owner);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            if (scrollMobRows(scrollY)) {
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (event.button() == 0 && beginScrollbarDrag(event.x(), event.y())) {
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
            if (event.button() == 0 && scrollbarDragging) {
                updateScrollbarDrag(event.y());
                return true;
            }
            return super.mouseDragged(event, dragX, dragY);
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            if (event.button() == 0 && scrollbarDragging) {
                scrollbarDragging = false;
                return true;
            }
            return super.mouseReleased(event);
        }

        private List<MobRow> mobRows() {
            List<MobRow> rows = new ArrayList<>();
            for (String entry : customMobEntries()) {
                if (matchesQuery(searchText(entry, "custom"), query)) {
                    rows.add(MobRow.custom(entry));
                }
            }
            allMobOptions().stream()
                    .filter(option -> switch (kind) {
                        case FARM_ANIMALS -> !filterEnabled || option.passive();
                        case SCARY_MOBS -> !filterEnabled || !option.passive();
                    })
                    .filter(option -> matchesQuery(option.searchable(), query))
                    .forEach(option -> rows.add(MobRow.option(option)));
            return rows;
        }

        private List<MobOption> allMobOptions() {
            return BuiltInRegistries.ENTITY_TYPE.entrySet()
                    .stream()
                    .map(Map.Entry::getValue)
                    .filter(SaltsAnimalFarmConfigScreen::isConfigurableMob)
                    .map(type -> new MobOption(type, type.getDescription().getString(), idString(type), isPassiveMob(type), searchText(type.getDescription().getString(), idString(type))))
                    .sorted(Comparator.comparing(option -> option.name.toLowerCase(Locale.ROOT)))
                    .toList();
        }

        private List<String> customMobEntries() {
            return owner.currentEntries(kind).stream()
                    .filter(entry -> !isKnownMobEntry(entry))
                    .toList();
        }

        private boolean isKnownMobEntry(String entry) {
            if (entry.startsWith("#")) {
                return false;
            }
            Identifier id = Identifier.tryParse(entry);
            return id != null && BuiltInRegistries.ENTITY_TYPE.getOptional(id).filter(SaltsAnimalFarmConfigScreen::isConfigurableMob).isPresent();
        }

        private boolean isSelected(EntityType<?> type) {
            String id = idString(type);
            for (String entry : owner.currentEntries(kind)) {
                if (entry.equals(id) || entityMatchesTag(type, entry)) {
                    return true;
                }
            }
            return false;
        }

        private void toggle(EntityType<?> type) {
            LinkedHashSet<String> entries = new LinkedHashSet<>(owner.currentEntries(kind));
            String id = idString(type);
            if (isSelected(type)) {
                entries.remove(id);
                List<String> matchingTags = entries.stream().filter(entry -> entityMatchesTag(type, entry)).toList();
                matchingTags.forEach(entries::remove);
                for (String tag : matchingTags) {
                    entries.addAll(expandEntityTag(tag, type));
                }
            } else {
                entries.add(id);
            }
            owner.setEntries(kind, entries);
            init();
        }

        private void submitCustomEntry() {
            String entry = normalizeCustomEntry(customInput == null ? customDraft : customInput.getValue(), false);
            if (entry == null) {
                owner.status = "Invalid custom entity id";
                return;
            }
            LinkedHashSet<String> entries = new LinkedHashSet<>(owner.currentEntries(kind));
            entries.add(entry);
            owner.setEntries(kind, entries);
            customDraft = "";
            addingCustom = false;
            init();
        }

        private void deleteCustom(String entry) {
            LinkedHashSet<String> entries = new LinkedHashSet<>(owner.currentEntries(kind));
            entries.remove(entry);
            owner.setEntries(kind, entries);
            init();
        }

        private int visibleMobRows(int listTop) {
            return Math.max(1, (height - listTop - 42) / 48);
        }

        private boolean scrollMobRows(double delta) {
            int listTop = addingCustom ? 90 : 62;
            List<MobRow> rows = mobRows();
            return setMobScroll(scroll + scrollDirection(delta), rows, listTop);
        }

        private boolean setMobScroll(int newScroll, List<MobRow> rows, int listTop) {
            int next = clamp(newScroll, 0, maxMobScroll(rows, listTop));
            if (next == scroll) {
                return false;
            }
            scroll = next;
            init();
            return true;
        }

        private int maxMobScroll(List<MobRow> rows, int listTop) {
            return Math.max(0, rows.size() - visibleMobRows(listTop));
        }

        private int mobViewportHeight(int listTop) {
            return visibleMobRows(listTop) * 48;
        }

        private boolean beginScrollbarDrag(double mouseX, double mouseY) {
            int listTop = addingCustom ? 90 : 62;
            List<MobRow> rows = mobRows();
            int rowWidth = rowWidth(680);
            int rowX = (width - rowWidth) / 2;
            int viewportHeight = mobViewportHeight(listTop);
            int contentHeight = rows.size() * 48;
            int maxScroll = maxMobScroll(rows, listTop);
            int scrollbarX = scrollbarX(rowX, rowWidth);
            if (!isInScrollbar(mouseX, mouseY, scrollbarX, listTop, viewportHeight, contentHeight, maxScroll)) {
                return false;
            }
            int thumbHeight = scrollbarThumbHeight(viewportHeight, contentHeight);
            int thumbY = scrollbarThumbY(listTop, viewportHeight, thumbHeight, scroll, maxScroll);
            scrollbarDragOffset = mouseY >= thumbY && mouseY <= thumbY + thumbHeight ? (int) mouseY - thumbY : thumbHeight / 2;
            scrollbarDragging = true;
            updateScrollbarDrag(mouseY);
            return true;
        }

        private void updateScrollbarDrag(double mouseY) {
            int listTop = addingCustom ? 90 : 62;
            List<MobRow> rows = mobRows();
            int viewportHeight = mobViewportHeight(listTop);
            int maxScroll = maxMobScroll(rows, listTop);
            int thumbHeight = scrollbarThumbHeight(viewportHeight, rows.size() * 48);
            setMobScroll(scrollForScrollbarMouse(mouseY, listTop, viewportHeight, thumbHeight, scrollbarDragOffset, maxScroll), rows, listTop);
        }

        private void addMobRowButtons(List<MobRow> rows, int listTop) {
            int rowWidth = rowWidth(680);
            int rowX = (width - rowWidth) / 2;
            int y = listTop;
            int visible = visibleMobRows(listTop);
            for (int index = scroll; index < Math.min(rows.size(), scroll + visible); index++) {
                MobRow row = rows.get(index);
                if (row.custom) {
                    addRenderableWidget(Button.builder(Component.literal("Delete"), button -> deleteCustom(row.entry))
                            .bounds(rowX + rowWidth - 84, y + 14, 76, 20)
                            .build());
                } else {
                    boolean selected = isSelected(row.option.type);
                    addRenderableWidget(Button.builder(Component.literal(selected ? "Remove" : "Add"), button -> toggle(row.option.type))
                            .bounds(rowX + rowWidth - 84, y + 14, 76, 20)
                            .build());
                }
                y += 48;
            }
        }

        private void renderMobTooltip(GuiGraphics graphics, int mouseX, int mouseY, int listTop) {
            List<MobRow> rows = mobRows();
            int rowWidth = rowWidth(680);
            int rowX = (width - rowWidth) / 2;
            int y = listTop;
            int visible = visibleMobRows(listTop);
            for (int index = scroll; index < Math.min(rows.size(), scroll + visible); index++) {
                MobRow row = rows.get(index);
                if (mouseX >= rowX && mouseX < rowX + rowWidth && mouseY >= y && mouseY < y + 46) {
                    if (row.custom) {
                        showTooltip(graphics, List.of(
                                Component.literal(row.entry),
                                Component.literal("Custom entry")
                        ), mouseX, mouseY);
                    } else {
                        boolean selected = isSelected(row.option.type);
                        showTooltip(graphics, List.of(
                                Component.literal(row.option.name),
                                Component.literal(row.option.id),
                                Component.literal(selected ? "Selected" : "Not selected")
                        ), mouseX, mouseY);
                    }
                    return;
                }
                y += 48;
            }
        }

        private void renderMobRows(GuiGraphics graphics, int mouseX, int mouseY, int listTop) {
            List<MobRow> rows = mobRows();
            int rowWidth = rowWidth(680);
            int rowX = (width - rowWidth) / 2;
            int y = listTop;
            int visible = visibleMobRows(listTop);
            for (int index = scroll; index < Math.min(rows.size(), scroll + visible); index++) {
                MobRow row = rows.get(index);
                boolean hovered = mouseX >= rowX && mouseX < rowX + rowWidth && mouseY >= y && mouseY < y + 46;
                graphics.fill(rowX, y + 1, rowX + rowWidth, y + 46, hovered ? ROW_HOVER_BACKGROUND : ROW_BACKGROUND);
                int boxX = rowX + 8;
                int boxY = y + 16;
                boolean selected = row.custom || isSelected(row.option.type);
                drawOutline(graphics, boxX, boxY, 14, 14, selected ? SELECTED_GREEN : DARK_OUTLINE);
                if (selected) {
                    graphics.fill(boxX + 2, boxY + 2, boxX + 12, boxY + 12, 0x6633AA55);
                }
                String title = row.custom ? row.entry : row.option.name;
                String detail = row.custom ? "Custom entry" : row.option.id;
                int labelWidth = Math.max(80, rowWidth - 124);
                graphics.drawString(font, Component.literal(trimToWidth(font, title, labelWidth)), rowX + 32, y + 9, NORMAL_TEXT);
                graphics.drawString(font, Component.literal(trimToWidth(font, detail, labelWidth)), rowX + 32, y + 24, MUTED_TEXT);
                y += 48;
            }
            renderScrollbar(
                    graphics,
                    scrollbarX(rowX, rowWidth),
                    listTop,
                    mobViewportHeight(listTop),
                    rows.size() * 48,
                    maxMobScroll(rows, listTop),
                    scroll,
                    mouseX,
                    mouseY,
                    scrollbarDragging
            );
        }
    }

    private record MobOption(EntityType<?> type, String name, String id, boolean passive, String searchable) {
    }

    private static final class MobRow {
        private final boolean custom;
        private final String entry;
        private final MobOption option;

        private MobRow(boolean custom, String entry, MobOption option) {
            this.custom = custom;
            this.entry = entry;
            this.option = option;
        }

        private static MobRow custom(String entry) {
            return new MobRow(true, entry, null);
        }

        private static MobRow option(MobOption option) {
            return new MobRow(false, null, option);
        }
    }

    private final class BlockPickerScreen extends Screen {
        private final SaltsAnimalFarmConfigScreen owner;
        private int scroll;
        private boolean scrollbarDragging;
        private int scrollbarDragOffset;
        private boolean addingCustom;
        private String customDraft = "";
        private String query = "";
        private EditBox searchBox;
        private EditBox customInput;

        BlockPickerScreen(SaltsAnimalFarmConfigScreen owner) {
            super(Component.literal("Soft Blocks"));
            this.owner = owner;
        }

        @Override
        protected void init() {
            clearWidgets();
            int searchWidth = Math.min(320, Math.max(120, width - 160));
            int searchX = Math.max(20, width / 2 - searchWidth / 2);
            searchBox = new EditBox(font, searchX, 34, searchWidth, 20, Component.literal("Search blocks"));
            searchBox.setHint(Component.literal("Search blocks and tags"));
            searchBox.setValue(query);
            searchBox.setResponder(value -> {
                query = value;
                scroll = 0;
                init();
            });
            addRenderableWidget(searchBox);
            addRenderableWidget(Button.builder(Component.literal(addingCustom ? "Close" : "Add Custom"), button -> {
                        addingCustom = !addingCustom;
                        init();
                    })
                    .bounds(Math.max(20, width - 116), 34, 96, 20)
                    .build());

            int listTop = addingCustom ? 90 : 62;
            if (addingCustom) {
                customInput = new EditBox(font, searchX, 62, Math.min(260, width - searchX - 140), 20, Component.literal("Custom block or tag"));
                customInput.setHint(Component.literal("modname:block or #modname:tag"));
                customInput.setValue(customDraft);
                customInput.setResponder(value -> customDraft = value);
                addRenderableWidget(customInput);
                addRenderableWidget(Button.builder(Component.literal("Add"), button -> submitCustomEntry())
                        .bounds(customInput.getX() + customInput.getWidth() + 8, 62, 54, 20)
                        .build());
            }

            List<BlockGridRow> rows = blockRows();
            scroll = clamp(scroll, 0, maxBlockScroll(rows, listTop));
            addBlockButtons(rows, listTop);

            addRenderableWidget(Button.builder(Component.literal("Reset List"), button -> {
                        owner.resetSoftBlocks();
                        scroll = 0;
                        init();
                    })
                    .bounds(width / 2 - 104, height - 28, 100, 20)
                    .build());
            addRenderableWidget(Button.builder(Component.literal("Done"), button -> minecraft.setScreen(owner))
                    .bounds(width / 2 + 4, height - 28, 100, 20)
                    .build());
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, width, height, 0xC0101010);
            int listTop = addingCustom ? 90 : 62;
            renderBlockRows(graphics, mouseX, mouseY, listTop);
            super.render(graphics, mouseX, mouseY, partialTick);
            renderBlockTooltip(graphics, mouseX, mouseY, listTop);
            graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFFFF);
            graphics.drawCenteredString(font, Component.literal(owner.status), width / 2, 22, owner.status.startsWith("Invalid") ? ERROR_TEXT : MUTED_TEXT);
        }

        @Override
        public void onClose() {
            minecraft.setScreen(owner);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            if (scrollBlockRows(scrollY)) {
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (event.button() == 0 && beginScrollbarDrag(event.x(), event.y())) {
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
            if (event.button() == 0 && scrollbarDragging) {
                updateScrollbarDrag(event.y());
                return true;
            }
            return super.mouseDragged(event, dragX, dragY);
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            if (event.button() == 0 && scrollbarDragging) {
                scrollbarDragging = false;
                return true;
            }
            return super.mouseReleased(event);
        }

        private List<BlockGridRow> blockRows() {
            List<BlockGridRow> rows = new ArrayList<>();
            List<BlockChoice> custom = customBlockChoices().stream().filter(choice -> matchesQuery(choice.searchable, query)).toList();
            List<BlockChoice> tags = allBlockTagChoices().stream().filter(choice -> matchesQuery(choice.searchable, query)).toList();
            List<BlockChoice> blocks = allBlockChoices().stream().filter(choice -> matchesQuery(choice.searchable, query)).toList();
            addBlockSection(rows, "Custom Entries", custom);
            addBlockSection(rows, "Block Tags", tags);
            addBlockSection(rows, "Blocks", blocks);
            return rows;
        }

        private void addBlockSection(List<BlockGridRow> rows, String title, List<BlockChoice> choices) {
            if (choices.isEmpty() && title.equals("Custom Entries")) {
                return;
            }
            rows.add(BlockGridRow.section(title));
            int columns = blockColumns();
            for (int index = 0; index < choices.size(); index += columns) {
                rows.add(BlockGridRow.choices(choices.subList(index, Math.min(index + columns, choices.size()))));
            }
        }

        private List<BlockChoice> customBlockChoices() {
            Set<String> knownEntries = new LinkedHashSet<>();
            allBlockTagChoices().forEach(choice -> knownEntries.add(choice.entry));
            allBlockChoices().forEach(choice -> knownEntries.add(choice.entry));
            return owner.values.softBlocks.stream()
                    .filter(entry -> !knownEntries.contains(entry))
                    .map(entry -> blockChoice(null, entry, entry, "Custom entry", true, entry.startsWith("#")))
                    .toList();
        }

        @SuppressWarnings("unchecked")
        private List<BlockChoice> allBlockTagChoices() {
            Map<Identifier, TagKey<Block>> tags = new LinkedHashMap<>();
            for (Field field : BlockTags.class.getFields()) {
                if (!TagKey.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                try {
                    TagKey<Block> tag = (TagKey<Block>) field.get(null);
                    tags.put(tag.location(), tag);
                } catch (IllegalAccessException ignored) {
                }
            }
            return tags.values().stream()
                    .sorted(Comparator.comparing(tag -> tag.location().toString()))
                    .map(tag -> {
                        String entry = "#" + displayIdentifier(tag.location());
                        String shortName = "#" + shortIdentifier(tag.location());
                        String label = titleFromPath(tag.location().getPath());
                        return blockChoice(tag, entry, shortName, label, false, true);
                    })
                    .toList();
        }

        private List<BlockChoice> allBlockChoices() {
            return BuiltInRegistries.BLOCK.entrySet()
                    .stream()
                    .map(Map.Entry::getValue)
                    .filter(block -> block.asItem() != Items.AIR)
                    .map(block -> blockChoice(block, idString(block), block.getName().getString(), idString(block), false, false))
                    .sorted(Comparator.comparing(choice -> choice.label.toLowerCase(Locale.ROOT)))
                    .toList();
        }

        private BlockChoice blockChoice(Object source, String entry, String label, String detail, boolean custom, boolean tag) {
            return new BlockChoice(source, entry, label, detail, searchText(entry, label, detail, custom ? "custom" : ""), custom, tag);
        }

        private boolean isChoiceSelected(BlockChoice choice) {
            if (choice.custom || choice.tag) {
                return owner.values.softBlocks.contains(choice.entry);
            }
            return choice.source instanceof Block block && isBlockSelected(block);
        }

        private boolean isBlockSelected(Block block) {
            String id = idString(block);
            for (String entry : owner.values.softBlocks) {
                if (entry.equals(id) || blockMatchesTag(block, entry)) {
                    return true;
                }
            }
            return false;
        }

        private void toggle(BlockChoice choice) {
            if (choice.custom) {
                deleteCustom(choice.entry);
                return;
            }
            LinkedHashSet<String> entries = new LinkedHashSet<>(owner.values.softBlocks);
            if (choice.tag) {
                if (!entries.remove(choice.entry)) {
                    entries.add(choice.entry);
                }
            } else if (choice.source instanceof Block block && isBlockSelected(block)) {
                entries.remove(choice.entry);
                List<String> matchingTags = entries.stream().filter(entry -> blockMatchesTag(block, entry)).toList();
                matchingTags.forEach(entries::remove);
                for (String tag : matchingTags) {
                    entries.addAll(expandBlockTag(tag, block));
                }
            } else {
                entries.add(choice.entry);
            }
            owner.setSoftBlockEntries(entries);
            init();
        }

        private void submitCustomEntry() {
            String entry = normalizeCustomEntry(customInput == null ? customDraft : customInput.getValue(), true);
            if (entry == null) {
                owner.status = "Invalid custom block or tag id";
                return;
            }
            LinkedHashSet<String> entries = new LinkedHashSet<>(owner.values.softBlocks);
            entries.add(entry);
            owner.setSoftBlockEntries(entries);
            customDraft = "";
            addingCustom = false;
            init();
        }

        private void deleteCustom(String entry) {
            LinkedHashSet<String> entries = new LinkedHashSet<>(owner.values.softBlocks);
            entries.remove(entry);
            owner.setSoftBlockEntries(entries);
            init();
        }

        private int blockColumns() {
            return Math.max(2, rowWidth(780) / 84);
        }

        private int visibleBlockRows(int listTop) {
            return Math.max(1, (height - listTop - 42) / 60);
        }

        private boolean scrollBlockRows(double delta) {
            int listTop = addingCustom ? 90 : 62;
            List<BlockGridRow> rows = blockRows();
            return setBlockScroll(scroll + scrollDirection(delta), rows, listTop);
        }

        private boolean setBlockScroll(int newScroll, List<BlockGridRow> rows, int listTop) {
            int next = clamp(newScroll, 0, maxBlockScroll(rows, listTop));
            if (next == scroll) {
                return false;
            }
            scroll = next;
            init();
            return true;
        }

        private int maxBlockScroll(List<BlockGridRow> rows, int listTop) {
            int viewportHeight = blockViewportHeight(listTop);
            int trailingHeight = 0;
            for (int index = rows.size() - 1; index >= 0; index--) {
                trailingHeight += blockRowHeight(rows.get(index));
                if (trailingHeight >= viewportHeight) {
                    return index;
                }
            }
            return 0;
        }

        private int blockViewportHeight(int listTop) {
            return Math.max(0, height - listTop - 42);
        }

        private int totalBlockRowsHeight(List<BlockGridRow> rows) {
            int total = 0;
            for (BlockGridRow row : rows) {
                total += blockRowHeight(row);
            }
            return total;
        }

        private int blockRowHeight(BlockGridRow row) {
            return row.section ? 24 : 60;
        }

        private boolean beginScrollbarDrag(double mouseX, double mouseY) {
            int listTop = addingCustom ? 90 : 62;
            List<BlockGridRow> rows = blockRows();
            int rowWidth = rowWidth(780);
            int rowX = (width - rowWidth) / 2;
            int viewportHeight = blockViewportHeight(listTop);
            int contentHeight = totalBlockRowsHeight(rows);
            int maxScroll = maxBlockScroll(rows, listTop);
            int scrollbarX = scrollbarX(rowX, rowWidth);
            if (!isInScrollbar(mouseX, mouseY, scrollbarX, listTop, viewportHeight, contentHeight, maxScroll)) {
                return false;
            }
            int thumbHeight = scrollbarThumbHeight(viewportHeight, contentHeight);
            int thumbY = scrollbarThumbY(listTop, viewportHeight, thumbHeight, scroll, maxScroll);
            scrollbarDragOffset = mouseY >= thumbY && mouseY <= thumbY + thumbHeight ? (int) mouseY - thumbY : thumbHeight / 2;
            scrollbarDragging = true;
            updateScrollbarDrag(mouseY);
            return true;
        }

        private void updateScrollbarDrag(double mouseY) {
            int listTop = addingCustom ? 90 : 62;
            List<BlockGridRow> rows = blockRows();
            int viewportHeight = blockViewportHeight(listTop);
            int maxScroll = maxBlockScroll(rows, listTop);
            int thumbHeight = scrollbarThumbHeight(viewportHeight, totalBlockRowsHeight(rows));
            setBlockScroll(scrollForScrollbarMouse(mouseY, listTop, viewportHeight, thumbHeight, scrollbarDragOffset, maxScroll), rows, listTop);
        }

        private void addBlockButtons(List<BlockGridRow> rows, int listTop) {
            int rowWidth = rowWidth(780);
            int rowX = (width - rowWidth) / 2;
            int y = listTop;
            int listBottom = height - 42;
            for (int rowIndex = scroll; rowIndex < rows.size() && y < listBottom; rowIndex++) {
                BlockGridRow row = rows.get(rowIndex);
                int rowHeight = blockRowHeight(row);
                if (y + rowHeight > listBottom) {
                    break;
                }
                if (!row.section) {
                    for (int index = 0; index < row.choices.size(); index++) {
                        BlockChoice choice = row.choices.get(index);
                        int x = cardX(rowX, rowWidth, index);
                        String text = choice.custom ? "Delete" : (isChoiceSelected(choice) ? "On" : "Off");
                        addRenderableWidget(Button.builder(Component.literal(text), button -> toggle(choice))
                                .bounds(x + 8, y + 32, 60, 18)
                                .build());
                    }
                }
                y += rowHeight;
            }
        }

        private void renderBlockTooltip(GuiGraphics graphics, int mouseX, int mouseY, int listTop) {
            List<BlockGridRow> rows = blockRows();
            int rowWidth = rowWidth(780);
            int rowX = (width - rowWidth) / 2;
            int y = listTop;
            int listBottom = height - 42;
            for (int rowIndex = scroll; rowIndex < rows.size() && y < listBottom; rowIndex++) {
                BlockGridRow row = rows.get(rowIndex);
                int rowHeight = blockRowHeight(row);
                if (y + rowHeight > listBottom) {
                    break;
                }
                if (!row.section) {
                    for (int index = 0; index < row.choices.size(); index++) {
                        BlockChoice choice = row.choices.get(index);
                        int x = cardX(rowX, rowWidth, index);
                        if (mouseX >= x && mouseX < x + 76 && mouseY >= y && mouseY < y + 54) {
                            showTooltip(graphics, List.of(
                                    Component.literal(choice.entry),
                                    Component.literal(choice.detail),
                                    Component.literal(isChoiceSelected(choice) ? "Selected" : "Not selected")
                            ), mouseX, mouseY);
                            return;
                        }
                    }
                }
                y += rowHeight;
            }
        }

        private void renderBlockRows(GuiGraphics graphics, int mouseX, int mouseY, int listTop) {
            List<BlockGridRow> rows = blockRows();
            int rowWidth = rowWidth(780);
            int rowX = (width - rowWidth) / 2;
            int y = listTop;
            int listBottom = height - 42;
            for (int rowIndex = scroll; rowIndex < rows.size() && y < listBottom; rowIndex++) {
                BlockGridRow row = rows.get(rowIndex);
                int rowHeight = blockRowHeight(row);
                if (y + rowHeight > listBottom) {
                    break;
                }
                if (row.section) {
                    graphics.drawString(font, Component.literal(row.title), rowX + 8, y + 8, SECTION_TEXT);
                    y += rowHeight;
                    continue;
                }
                for (int index = 0; index < row.choices.size(); index++) {
                    BlockChoice choice = row.choices.get(index);
                    int x = cardX(rowX, rowWidth, index);
                    boolean hovered = mouseX >= x && mouseX < x + 76 && mouseY >= y && mouseY < y + 54;
                    boolean selected = isChoiceSelected(choice);
                    graphics.fill(x, y, x + 76, y + 54, hovered ? CARD_HOVER_BACKGROUND : CARD_BACKGROUND);
                    drawOutline(graphics, x, y, 76, 54, selected ? SELECTED_GREEN : DARK_OUTLINE);
                    ItemStack preview = previewStack(choice);
                    if (!preview.isEmpty()) {
                        graphics.renderItem(preview, x + 30, y + 7);
                    }
                    int color = selected ? SELECTED_GREEN : NORMAL_TEXT;
                    graphics.drawCenteredString(font, Component.literal(trimToWidth(font, choice.label, 68)), x + 38, y + 22, color);
                }
                y += rowHeight;
            }
            renderScrollbar(
                    graphics,
                    scrollbarX(rowX, rowWidth),
                    listTop,
                    blockViewportHeight(listTop),
                    totalBlockRowsHeight(rows),
                    maxBlockScroll(rows, listTop),
                    scroll,
                    mouseX,
                    mouseY,
                    scrollbarDragging
            );
        }

        private ItemStack previewStack(BlockChoice choice) {
            if (choice.source instanceof Block block) {
                return safeBlockStack(block);
            }
            if (choice.source instanceof TagKey<?> tagKey) {
                @SuppressWarnings("unchecked")
                TagKey<Block> tag = (TagKey<Block>) tagKey;
                return StreamSupport.stream(BuiltInRegistries.BLOCK.getTagOrEmpty(tag).spliterator(), false)
                        .map(Holder::value)
                        .filter(block -> block.asItem() != Items.AIR)
                        .findFirst()
                        .map(this::safeBlockStack)
                        .orElse(ItemStack.EMPTY);
            }
            if (!choice.entry.startsWith("#")) {
                Identifier id = Identifier.tryParse(choice.entry);
                if (id != null) {
                    return BuiltInRegistries.BLOCK.getOptional(id).filter(block -> block.asItem() != Items.AIR).map(this::safeBlockStack).orElse(ItemStack.EMPTY);
                }
            }
            return ItemStack.EMPTY;
        }

        private ItemStack safeBlockStack(Block block) {
            try {
                return new ItemStack(block);
            } catch (RuntimeException exception) {
                return ItemStack.EMPTY;
            }
        }

        private int cardX(int rowX, int rowWidth, int index) {
            int columns = blockColumns();
            int totalWidth = columns * 76 + (columns - 1) * 8;
            int startX = rowX + Math.max(0, (rowWidth - totalWidth) / 2);
            return startX + index * 84;
        }
    }

    private static final class BlockGridRow {
        private final boolean section;
        private final String title;
        private final List<BlockChoice> choices;

        private BlockGridRow(boolean section, String title, List<BlockChoice> choices) {
            this.section = section;
            this.title = title;
            this.choices = choices;
        }

        private static BlockGridRow section(String title) {
            return new BlockGridRow(true, title, List.of());
        }

        private static BlockGridRow choices(List<BlockChoice> choices) {
            return new BlockGridRow(false, "", List.copyOf(choices));
        }
    }

    private static final class BlockChoice {
        private final Object source;
        private final String entry;
        private final String label;
        private final String detail;
        private final String searchable;
        private final boolean custom;
        private final boolean tag;

        private BlockChoice(Object source, String entry, String label, String detail, String searchable, boolean custom, boolean tag) {
            this.source = source;
            this.entry = entry;
            this.label = label;
            this.detail = detail;
            this.searchable = searchable;
            this.custom = custom;
            this.tag = tag;
        }
    }

    private static final class MutableConfig {
        private boolean enableMod;
        private List<String> farmAnimals;
        private List<String> scaryMobs;
        private List<String> softBlocks;
        private String preset;
        private int minimumWeight;
        private int maximumWeight;
        private int positiveTaskStreakThreshold;
        private int negativeTaskStreakThreshold;
        private boolean loseWeightWhenHitByPlayer;
        private boolean loseWeightWhenWitnessingAnimalDeath;
        private double sickMovementSpeedMultiplier;
        private boolean allowNonBredAnimalsToBecomeSick;
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
            farmAnimals = new ArrayList<>(config.farmAnimals());
            scaryMobs = new ArrayList<>(config.scaryMobs());
            softBlocks = new ArrayList<>(config.softBlocks());
            preset = config.selectedPreset().serializedName();
            minimumWeight = config.minimumWeight();
            maximumWeight = config.maximumWeight();
            positiveTaskStreakThreshold = config.positiveTaskStreakThreshold();
            negativeTaskStreakThreshold = config.negativeTaskStreakThreshold();
            loseWeightWhenHitByPlayer = config.loseWeightWhenHitByPlayer();
            loseWeightWhenWitnessingAnimalDeath = config.loseWeightWhenWitnessingAnimalDeath();
            sickMovementSpeedMultiplier = config.sanitizedSickMovementSpeedMultiplier();
            allowNonBredAnimalsToBecomeSick = config.nonBredAnimalsCanBecomeSick();
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
                    preset,
                    minimumWeight,
                    maximumWeight,
                    positiveTaskStreakThreshold,
                    negativeTaskStreakThreshold,
                    loseWeightWhenHitByPlayer,
                    loseWeightWhenWitnessingAnimalDeath,
                    sickMovementSpeedMultiplier,
                    allowNonBredAnimalsToBecomeSick,
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
