package org.betterLostItems.salts_animal_farm.client.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;
import net.minecraft.client.renderer.entity.state.CowRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.PigRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.chicken.ChickenVariant;
import net.minecraft.world.entity.animal.cow.CowVariant;
import net.minecraft.world.entity.animal.pig.PigVariant;
import net.minecraft.world.entity.variant.ModelAndTexture;
import net.minecraft.world.entity.variant.SpawnPrioritySelectors;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;
import org.betterLostItems.salts_animal_farm.config.SaltsAnimalFarmConfig;
import org.betterLostItems.salts_animal_farm.mixin.client.EntityRenderDispatcherAccessor;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
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
    private static final Constructor<ItemStack> MENU_SAFE_ITEM_STACK_CONSTRUCTOR = menuSafeItemStackConstructor();
    private static final ChickenVariant DEFAULT_CHICKEN_VARIANT = new ChickenVariant(
            new ModelAndTexture<>(ChickenVariant.ModelType.NORMAL, textureAsset("entity/chicken/chicken_temperate")),
            textureAsset("entity/chicken/chicken_temperate_baby"),
            SpawnPrioritySelectors.EMPTY
    );
    private static final CowVariant DEFAULT_COW_VARIANT = new CowVariant(
            new ModelAndTexture<>(CowVariant.ModelType.NORMAL, textureAsset("entity/cow/cow_temperate")),
            textureAsset("entity/cow/cow_temperate_baby"),
            SpawnPrioritySelectors.EMPTY
    );
    private static final PigVariant DEFAULT_PIG_VARIANT = new PigVariant(
            new ModelAndTexture<>(PigVariant.ModelType.NORMAL, textureAsset("entity/pig/pig_temperate")),
            textureAsset("entity/pig/pig_temperate_baby"),
            SpawnPrioritySelectors.EMPTY
    );

    private final Screen parent;
    private final MutableConfig values;
    private SettingsList settingsList;
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);
        graphics.text(font, Component.literal(status), 20, height - 22, status.startsWith("Invalid") ? ERROR_TEXT : MUTED_TEXT);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private void rebuildMainScreen() {
        clearWidgets();

        int listTop = 32;
        int listHeight = Math.max(60, height - listTop - 38);
        settingsList = new SettingsList(minecraft, width, listHeight, listTop);
        buildSettingsEntries(settingsList);
        addRenderableWidget(settingsList);

        addRenderableWidget(Button.builder(Component.literal("Reset All"), button -> resetAll())
                .bounds(width / 2 - 104, height - 28, 100, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> minecraft.setScreen(parent))
                .bounds(width / 2 + 4, height - 28, 100, 20)
                .build());
    }

    private void buildSettingsEntries(SettingsList list) {
        list.addSection("General");
        list.addBoolean(
                "Enable Mod",
                "Master switch for animal goals, weighted loot, fear, rain behavior, debug labels, and commands.",
                values.enableMod,
                value -> values.enableMod = value
        );
        list.addBoolean(
                "Enable Rain Behavior",
                "Farm animals seek cover, track rain exposure, and avoid exposed rain paths while sheltered.",
                values.enableRainBehavior,
                value -> values.enableRainBehavior = value
        );
        list.addInt("Minimum Weight", "Lowest clamped weight value an animal can have.", values.minimumWeight, value -> values.minimumWeight = value);
        list.addInt("Maximum Weight", "Highest clamped weight value an animal can reach.", values.maximumWeight, value -> values.maximumWeight = value);

        list.addSection("Lists");
        list.addAction(
                "Farm Animals",
                () -> selectionSummary(values.farmAnimals, "entry", "entries"),
                "Choose which mobs receive farm-animal behavior and weight data.",
                () -> minecraft.setScreen(new MobPickerScreen(this, MobListKind.FARM_ANIMALS))
        );
        list.addAction(
                "Scary Mobs",
                () -> selectionSummary(values.scaryMobs, "entry", "entries"),
                "Choose which mobs farm animals treat as scary threats.",
                () -> minecraft.setScreen(new MobPickerScreen(this, MobListKind.SCARY_MOBS))
        );
        list.addAction(
                "Soft Blocks",
                () -> selectionSummary(values.softBlocks, "entry", "entries"),
                "Choose block tags and blocks that count as soft nap spots.",
                () -> minecraft.setScreen(new BlockPickerScreen(this))
        );

        list.addSection("Comfort Timing");
        list.addInt("Average Task Delay Ticks", "Average delay between comfort task attempts. Twenty ticks is about one second.", values.comfortTaskAverageDelayTicks, value -> values.comfortTaskAverageDelayTicks = value);
        list.addInt("Delay Jitter Ticks", "Random timing spread so animals do not all act on the same tick.", values.comfortTaskDelayJitterTicks, value -> values.comfortTaskDelayJitterTicks = value);
        list.addInt("Linger Ticks", "How long an animal remains at a completed comfort condition before it counts as stable.", values.comfortLingerTicks, value -> values.comfortLingerTicks = value);
        list.addInt("Task Reach Timeout Ticks", "How long an animal has to reach or satisfy a comfort target before the attempt fails.", values.comfortTaskReachTimeoutTicks, value -> values.comfortTaskReachTimeoutTicks = value);
        list.addInt("Maximum Task Ticks", "Absolute safety cap for any comfort task attempt.", values.comfortMaxTaskTicks, value -> values.comfortMaxTaskTicks = value);
        list.addDouble("Comfort Move Speed", "Movement speed used while traveling toward comfort task targets.", values.comfortMoveSpeed, value -> values.comfortMoveSpeed = value);

        list.addSection("Comfort Search");
        list.addInt("Search Radius", "Horizontal block radius used when finding comfort task targets.", values.comfortSearchRadius, value -> values.comfortSearchRadius = value);
        list.addInt("Vertical Search", "Vertical block range above and below the animal used during target search.", values.comfortVerticalSearch, value -> values.comfortVerticalSearch = value);
        list.addInt("Search Samples", "Extra random target samples checked after deterministic search.", values.comfortSearchSamples, value -> values.comfortSearchSamples = value);

        list.addSection("Hostile Fear");
        list.addInt("Hostile Scare Radius", "Distance around a farm animal scanned for scary mobs.", values.hostileScareRadius, value -> values.hostileScareRadius = value);
        list.addInt("Scan Interval Ticks", "How often animals scan for scary mobs. Lower values react faster but do more work.", values.hostileScanIntervalTicks, value -> values.hostileScanIntervalTicks = value);
        list.addInt("Scan Random Offset Ticks", "Random offset applied to scare scans so animals do not all scan together.", values.hostileScanRandomOffsetTicks, value -> values.hostileScanRandomOffsetTicks = value);
        list.addInt("Scare Cooldown Ticks", "Minimum time before the same animal can lose weight again from hostile scare behavior.", values.hostileScareCooldownTicks, value -> values.hostileScareCooldownTicks = value);
        list.addDouble("Hostile Flee Speed", "Movement speed used when farm animals flee scary mobs.", values.hostileFleeSpeed, value -> values.hostileFleeSpeed = value);

        list.addSection("Frantic Fear");
        list.addInt("Kill Witness Radius", "Distance from a player-caused animal death where other farm animals can witness it.", values.killWitnessRadius, value -> values.killWitnessRadius = value);
        list.addInt("Maximum Kill Witnesses", "Maximum number of nearby farm animals processed as witnesses.", values.maxKillWitnesses, value -> values.maxKillWitnesses = value);
        list.addInt("Frantic Duration Ticks", "How long farm animals remain frantic after player damage or death witnessing.", values.franticDurationTicks, value -> values.franticDurationTicks = value);
        list.addInt("Frantic Repath Ticks", "How often frantic animals pick a new panic movement target.", values.franticRepathTicks, value -> values.franticRepathTicks = value);
        list.addDouble("Frantic Move Speed", "Movement speed used while animals are frantic.", values.franticMoveSpeed, value -> values.franticMoveSpeed = value);

        list.addSection("Debug");
        list.addBoolean(
                "Detailed Debug Information",
                "Verbose server logs for comfort target searching, task ticking, and completion decisions.",
                values.enableDetailedDebugInformation,
                value -> values.enableDetailedDebugInformation = value
        );
    }

    private void resetAll() {
        values.copyFrom(SaltsAnimalFarmConfig.DEFAULT);
        apply("Reset all settings");
        rebuildMainScreen();
    }

    private void resetList(MobListKind kind) {
        switch (kind) {
            case FARM_ANIMALS -> values.farmAnimals = new ArrayList<>(SaltsAnimalFarmConfig.DEFAULT.farmAnimals());
            case SCARY_MOBS -> values.scaryMobs = new ArrayList<>(SaltsAnimalFarmConfig.DEFAULT.scaryMobs());
        }
        apply("Reset " + kind.title.toLowerCase(Locale.ROOT));
    }

    private void resetSoftBlocks() {
        values.softBlocks = new ArrayList<>(SaltsAnimalFarmConfig.DEFAULT.softBlocks());
        apply("Reset soft blocks");
    }

    private void apply(String message) {
        Salts_animal_farm.updateConfig(values.toConfig());
        values.copyFrom(Salts_animal_farm.CONFIG);
        status = message;
    }

    private List<String> currentEntries(MobListKind kind) {
        return switch (kind) {
            case FARM_ANIMALS -> values.farmAnimals;
            case SCARY_MOBS -> values.scaryMobs;
        };
    }

    private void setEntries(MobListKind kind, Collection<String> entries) {
        List<String> next = cleanEntries(entries);
        switch (kind) {
            case FARM_ANIMALS -> values.farmAnimals = next;
            case SCARY_MOBS -> values.scaryMobs = next;
        }
        apply("Updated " + kind.title.toLowerCase(Locale.ROOT));
    }

    private void setSoftBlockEntries(Collection<String> entries) {
        values.softBlocks = cleanEntries(entries);
        apply("Updated soft blocks");
    }

    private static List<String> cleanEntries(Collection<String> entries) {
        return entries.stream()
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .distinct()
                .toList();
    }

    private static String normalizeCustomEntry(String text, boolean allowTag) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

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

        return font.plainSubstrByWidth(text, Math.max(0, width - 9)) + "...";
    }

    private static String searchText(String... values) {
        return String.join(" ", values).toLowerCase(Locale.ROOT);
    }

    private static ClientAsset.ResourceTexture textureAsset(String path) {
        return new ClientAsset.ResourceTexture(Identifier.withDefaultNamespace(path));
    }

    private static Constructor<ItemStack> menuSafeItemStackConstructor() {
        try {
            Constructor<ItemStack> constructor = ItemStack.class.getDeclaredConstructor(Holder.class, int.class, PatchedDataComponentMap.class);
            return constructor.trySetAccessible() ? constructor : null;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
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
            if (!builder.isEmpty()) {
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
                .filter(SaltsAnimalFarmConfigScreen::isConfigurableMob)
                .filter(type -> type != excludedType)
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

    private final class SettingsList extends ContainerObjectSelectionList<SettingsEntry> {
        private SettingsList(Minecraft minecraft, int width, int height, int y) {
            super(minecraft, width, height, y, 34);
            centerListVertically = false;
        }

        @Override
        public int getRowWidth() {
            return Math.min(740, Math.max(260, getWidth() - 72));
        }

        private void addSection(String label) {
            addEntry(new SectionEntry(font, label), 24);
        }

        private void addBoolean(String label, String description, boolean value, Consumer<Boolean> setter) {
            CycleButton<Boolean> control = CycleButton.onOffBuilder(value)
                    .displayOnlyValue()
                    .create(0, 0, 124, 20, Component.empty(), (button, selected) -> {
                        setter.accept(selected);
                        apply("Updated " + label.toLowerCase(Locale.ROOT));
                    });
            addEntry(new WidgetEntry(font, label, description, control, 124), 38);
        }

        private void addInt(String label, String description, int value, IntConsumer setter) {
            EditBox control = new EditBox(font, 0, 0, 96, 20, Component.literal(label));
            control.setMaxLength(12);
            control.setTextColor(NORMAL_TEXT);
            control.setTextColorUneditable(NORMAL_TEXT);
            control.setValue(Integer.toString(value));
            control.setResponder(text -> {
                try {
                    setter.accept(Integer.parseInt(text.trim()));
                    control.setTextColor(NORMAL_TEXT);
                    apply("Updated " + label.toLowerCase(Locale.ROOT));
                } catch (NumberFormatException exception) {
                    control.setTextColor(ERROR_TEXT);
                    status = "Invalid number: " + label;
                }
            });
            addEntry(new WidgetEntry(font, label, description, control, 96), 38);
        }

        private void addDouble(String label, String description, double value, Consumer<Double> setter) {
            EditBox control = new EditBox(font, 0, 0, 96, 20, Component.literal(label));
            control.setMaxLength(12);
            control.setTextColor(NORMAL_TEXT);
            control.setTextColorUneditable(NORMAL_TEXT);
            control.setValue(Double.toString(value));
            control.setResponder(text -> {
                try {
                    setter.accept(Double.parseDouble(text.trim()));
                    control.setTextColor(NORMAL_TEXT);
                    apply("Updated " + label.toLowerCase(Locale.ROOT));
                } catch (NumberFormatException exception) {
                    control.setTextColor(ERROR_TEXT);
                    status = "Invalid number: " + label;
                }
            });
            addEntry(new WidgetEntry(font, label, description, control, 96), 38);
        }

        private void addAction(String label, SummarySupplier summarySupplier, String description, Runnable action) {
            Button control = Button.builder(Component.literal("Edit"), button -> action.run())
                    .bounds(0, 0, 124, 20)
                    .build();
            addEntry(new ActionEntry(font, label, summarySupplier, description, control, 124), 38);
        }
    }

    private abstract static class SettingsEntry extends ContainerObjectSelectionList.Entry<SettingsEntry> {
    }

    private static final class SectionEntry extends SettingsEntry {
        private final Font font;
        private final String label;

        private SectionEntry(Font font, String label) {
            this.font = font;
            this.label = label;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            graphics.text(font, Component.literal(label), getContentX(), getContentY() + 8, SECTION_TEXT);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of();
        }
    }

    private static class WidgetEntry extends SettingsEntry {
        protected final Font font;
        protected final String label;
        protected final String description;
        protected final AbstractWidget control;
        protected final int controlWidth;
        private final List<AbstractWidget> widgets;

        private WidgetEntry(Font font, String label, String description, AbstractWidget control, int controlWidth) {
            this.font = font;
            this.label = label;
            this.description = description;
            this.control = control;
            this.controlWidth = controlWidth;
            this.widgets = List.of(control);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            int x = getX();
            int y = getY();
            int width = getWidth();
            int height = getHeight() - 2;

            graphics.fill(x, y + 1, x + width, y + height, hovered ? ROW_HOVER_BACKGROUND : ROW_BACKGROUND);
            int controlX = getContentRight() - controlWidth;
            int controlY = getContentY() + 8;
            control.setX(controlX);
            control.setY(controlY);
            control.setWidth(controlWidth);

            int labelWidth = Math.max(70, controlX - getContentX() - 12);
            graphics.text(font, Component.literal(trimToWidth(font, label, labelWidth)), getContentX(), getContentY() + 5, NORMAL_TEXT);
            graphics.text(font, Component.literal(trimToWidth(font, description, labelWidth)), getContentX(), getContentY() + 18, MUTED_TEXT);
            control.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return widgets;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return widgets;
        }
    }

    private static final class ActionEntry extends WidgetEntry {
        private final SummarySupplier summarySupplier;

        private ActionEntry(Font font, String label, SummarySupplier summarySupplier, String description, Button control, int controlWidth) {
            super(font, label, description, control, controlWidth);
            this.summarySupplier = summarySupplier;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            control.setMessage(Component.literal("Edit (" + summarySupplier.summary() + ")"));
            super.extractContent(graphics, mouseX, mouseY, hovered, partialTick);
        }
    }

    @FunctionalInterface
    private interface SummarySupplier {
        String summary();
    }

    private static final class DialogBackdrop implements Renderable {
        private final Font font;
        private final String title;
        private final String message;
        private final int screenWidth;
        private final int screenHeight;
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        private DialogBackdrop(Font font, String title, String message, int screenWidth, int screenHeight, int x, int y, int width, int height) {
            this.font = font;
            this.title = title;
            this.message = message;
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, screenWidth, screenHeight, 0x99000000);
            graphics.fill(x, y, x + width, y + height, 0xEE101010);
            graphics.outline(x, y, width, height, 0xFF777777);
            graphics.centeredText(font, Component.literal(title), x + width / 2, y + 12, NORMAL_TEXT);
            if (!message.isEmpty()) {
                graphics.centeredText(font, Component.literal(trimToWidth(font, message, width - 24)), x + width / 2, y + 28, MUTED_TEXT);
            }
        }
    }

    private enum MobListKind {
        FARM_ANIMALS("Farm Animals", "Filter for passive mobs"),
        SCARY_MOBS("Scary Mobs", "Filter out passive mobs");

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
        private final Map<EntityType<?>, LivingEntity> previewEntities = new HashMap<>();
        private final LinkedHashSet<String> explicitCustomEntries = new LinkedHashSet<>();
        private MobSelectionList mobList;
        private EditBox searchBox;
        private EditBox customInput;
        private Button customPrimaryButton;
        private Button customCancelButton;
        private boolean filterEnabled = true;
        private boolean addingCustom;
        private String pendingCustomDelete;
        private String customDraft = "";
        private String query = "";

        private MobPickerScreen(SaltsAnimalFarmConfigScreen owner, MobListKind kind) {
            super(Component.literal(kind.title));
            this.owner = owner;
            this.kind = kind;
        }

        @Override
        protected void init() {
            clearWidgets();

            int controlY = 34;
            int left = Math.max(20, width / 2 - Math.min(360, width - 40) / 2);
            int filterWidth = Math.min(190, Math.max(130, width / 2 - 34));
            int searchWidth = Math.min(190, Math.max(120, width - left - filterWidth - 32));

            addRenderableWidget(CycleButton.onOffBuilder(filterEnabled)
                    .create(left, controlY, filterWidth, 20, Component.literal(kind.filterLabel), (button, selected) -> {
                        filterEnabled = selected;
                        rebuildMobList();
                    }));

            searchBox = new EditBox(font, left + filterWidth + 8, controlY, searchWidth, 20, Component.literal("Search mobs"));
            searchBox.setHint(Component.literal("Search mobs"));
            searchBox.setValue(query);
            searchBox.setResponder(value -> {
                query = value;
                rebuildMobList();
            });
            addRenderableWidget(searchBox);

            addRenderableWidget(Button.builder(Component.literal("Add Custom"), button -> openAddCustomDialog())
                    .bounds(Math.max(20, width - 116), controlY, 96, 20)
                    .build());

            int listTop = 62;
            int listHeight = Math.max(60, height - listTop - 38);
            mobList = new MobSelectionList(minecraft, width, listHeight, listTop, this);
            addRenderableWidget(mobList);
            rebuildMobList();

            addRenderableWidget(Button.builder(Component.literal("Reset List"), button -> {
                        owner.resetList(kind);
                        rebuildMobList();
                    })
                    .bounds(width / 2 - 104, height - 28, 100, 20)
                    .build());
            addRenderableWidget(Button.builder(Component.literal("Done"), button -> minecraft.setScreen(owner))
                    .bounds(width / 2 + 4, height - 28, 100, 20)
                    .build());

            addDialogWidgets();
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            graphics.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);
            graphics.centeredText(font, Component.literal(owner.status), width / 2, 22, owner.status.startsWith("Invalid") ? ERROR_TEXT : MUTED_TEXT);
        }

        @Override
        public void onClose() {
            minecraft.setScreen(owner);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (isDialogOpen()) {
                for (GuiEventListener widget : dialogWidgets()) {
                    if (widget.mouseClicked(event, doubleClick)) {
                        setFocused(widget);
                        return true;
                    }
                }
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (isDialogOpen()) {
                if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                    closeDialog();
                    return true;
                }
                if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
                    activateDialogPrimary();
                    return true;
                }
                return customInput != null && customInput.keyPressed(event);
            }
            return super.keyPressed(event);
        }

        @Override
        public boolean charTyped(CharacterEvent event) {
            if (isDialogOpen()) {
                return customInput != null && customInput.charTyped(event);
            }
            return super.charTyped(event);
        }

        private void rebuildMobList() {
            if (mobList == null) {
                return;
            }

            List<String> customEntries = customMobEntries().stream()
                    .filter(entry -> matchesQuery(searchText(entry, "custom"), query))
                    .toList();
            List<MobOption> options = allMobOptions().stream()
                    .filter(option -> switch (kind) {
                        case FARM_ANIMALS -> !filterEnabled || option.passive();
                        case SCARY_MOBS -> !filterEnabled || !option.passive();
                    })
                    .filter(option -> matchesQuery(option.searchable(), query))
                    .toList();
            mobList.replaceOptions(customEntries, options);
        }

        private List<MobOption> allMobOptions() {
            return BuiltInRegistries.ENTITY_TYPE.entrySet()
                    .stream()
                    .map(entry -> entry.getValue())
                    .filter(SaltsAnimalFarmConfigScreen::isConfigurableMob)
                    .map(type -> new MobOption(
                            type,
                            type.getDescription().getString(),
                            idString(type),
                            isPassiveMob(type),
                            searchText(type.getDescription().getString(), idString(type))
                    ))
                    .sorted(Comparator.comparing(option -> option.name().toLowerCase(Locale.ROOT)))
                    .toList();
        }

        private List<String> customMobEntries() {
            return owner.currentEntries(kind).stream()
                    .filter(entry -> explicitCustomEntries.contains(entry) || !isKnownMobEntry(entry))
                    .toList();
        }

        private boolean isKnownMobEntry(String entry) {
            if (entry.startsWith("#")) {
                return false;
            }

            Identifier id = Identifier.tryParse(entry);
            if (id == null) {
                return false;
            }

            return BuiltInRegistries.ENTITY_TYPE.getOptional(id)
                    .filter(SaltsAnimalFarmConfigScreen::isConfigurableMob)
                    .isPresent();
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
                List<String> matchingTags = entries.stream()
                        .filter(entry -> entityMatchesTag(type, entry))
                        .toList();
                matchingTags.forEach(entries::remove);
                for (String tag : matchingTags) {
                    entries.addAll(expandEntityTag(tag, type));
                }
            } else {
                entries.add(id);
            }

            owner.setEntries(kind, entries);
            rebuildMobList();
        }

        private void openAddCustomDialog() {
            addingCustom = true;
            pendingCustomDelete = null;
            customDraft = "";
            init();
        }

        private void requestDeleteCustom(String entry) {
            pendingCustomDelete = entry;
            addingCustom = false;
            customDraft = "";
            init();
        }

        private void closeDialog() {
            addingCustom = false;
            pendingCustomDelete = null;
            customDraft = customInput == null ? "" : customInput.getValue();
            init();
        }

        private boolean isDialogOpen() {
            return addingCustom || pendingCustomDelete != null;
        }

        private List<GuiEventListener> dialogWidgets() {
            List<GuiEventListener> widgets = new ArrayList<>();
            if (customInput != null) {
                widgets.add(customInput);
            }
            if (customPrimaryButton != null) {
                widgets.add(customPrimaryButton);
            }
            if (customCancelButton != null) {
                widgets.add(customCancelButton);
            }
            return widgets;
        }

        private void addDialogWidgets() {
            customInput = null;
            customPrimaryButton = null;
            customCancelButton = null;
            if (!isDialogOpen()) {
                return;
            }

            int dialogWidth = Math.min(320, Math.max(240, width - 48));
            int dialogHeight = addingCustom ? 116 : 104;
            int dialogX = width / 2 - dialogWidth / 2;
            int dialogY = height / 2 - dialogHeight / 2;

            if (addingCustom) {
                addRenderableOnly(new DialogBackdrop(font, "Add Custom", "Enter an entity id, like modname:entity", width, height, dialogX, dialogY, dialogWidth, dialogHeight));
                customInput = new EditBox(font, dialogX + 16, dialogY + 44, dialogWidth - 32, 20, Component.literal("Custom entity"));
                customInput.setHint(Component.literal("modname:entity"));
                customInput.setValue(customDraft);
                customInput.setResponder(value -> customDraft = value);
                addRenderableWidget(customInput);
                customPrimaryButton = Button.builder(Component.literal("Add"), button -> submitCustomEntry())
                        .bounds(dialogX + dialogWidth / 2 - 104, dialogY + 80, 100, 20)
                        .build();
                customCancelButton = Button.builder(Component.literal("Cancel"), button -> closeDialog())
                        .bounds(dialogX + dialogWidth / 2 + 4, dialogY + 80, 100, 20)
                        .build();
                addRenderableWidget(customPrimaryButton);
                addRenderableWidget(customCancelButton);
                setInitialFocus(customInput);
            } else {
                addRenderableOnly(new DialogBackdrop(font, "Delete Custom Entry?", pendingCustomDelete, width, height, dialogX, dialogY, dialogWidth, dialogHeight));
                customPrimaryButton = Button.builder(Component.literal("Delete"), button -> confirmDeleteCustom())
                        .bounds(dialogX + dialogWidth / 2 - 104, dialogY + 68, 100, 20)
                        .build();
                customCancelButton = Button.builder(Component.literal("Cancel"), button -> closeDialog())
                        .bounds(dialogX + dialogWidth / 2 + 4, dialogY + 68, 100, 20)
                        .build();
                addRenderableWidget(customPrimaryButton);
                addRenderableWidget(customCancelButton);
            }
        }

        private void activateDialogPrimary() {
            if (addingCustom) {
                submitCustomEntry();
            } else if (pendingCustomDelete != null) {
                confirmDeleteCustom();
            }
        }

        private void submitCustomEntry() {
            String entry = normalizeCustomEntry(customInput == null ? customDraft : customInput.getValue(), false);
            if (entry == null) {
                owner.status = "Invalid custom entity id";
                return;
            }

            LinkedHashSet<String> entries = new LinkedHashSet<>(owner.currentEntries(kind));
            entries.add(entry);
            explicitCustomEntries.add(entry);
            owner.setEntries(kind, entries);
            addingCustom = false;
            customDraft = "";
            init();
        }

        private void confirmDeleteCustom() {
            if (pendingCustomDelete == null) {
                return;
            }

            LinkedHashSet<String> entries = new LinkedHashSet<>(owner.currentEntries(kind));
            entries.remove(pendingCustomDelete);
            explicitCustomEntries.remove(pendingCustomDelete);
            owner.setEntries(kind, entries);
            pendingCustomDelete = null;
            init();
        }

        private LivingEntity previewEntity(EntityType<?> type) {
            Level level = minecraft.player == null ? minecraft.level : minecraft.player.level();
            if (level == null) {
                return null;
            }

            LivingEntity cached = previewEntities.get(type);
            if (cached != null && cached.level() == level) {
                return cached;
            }

            try {
                Entity created = type.create(level, EntitySpawnReason.LOAD);
                if (created instanceof LivingEntity living) {
                    living.setYRot(25.0F);
                    living.setYHeadRot(25.0F);
                    living.yBodyRot = 25.0F;
                    living.yBodyRotO = 25.0F;
                    living.yHeadRotO = 25.0F;
                    living.setNoGravity(true);
                    if (living instanceof Mob mob) {
                        mob.setNoAi(true);
                    }
                    previewEntities.put(type, living);
                    return living;
                }
            } catch (RuntimeException ignored) {
                return null;
            }

            return null;
        }

        private EntityRenderState previewRenderState(EntityType<?> type) {
            EntityRenderer<?, ?> renderer = ((EntityRenderDispatcherAccessor) minecraft.getEntityRenderDispatcher())
                    .salts_animal_farm$getRenderers()
                    .get(type);
            if (renderer == null) {
                return null;
            }

            try {
                EntityRenderState state = renderer.createRenderState();
                preparePreviewState(type, state);
                return state;
            } catch (RuntimeException exception) {
                return null;
            }
        }

        private void preparePreviewState(EntityType<?> type, EntityRenderState state) {
            state.entityType = type;
            state.boundingBoxWidth = type.getWidth();
            state.boundingBoxHeight = type.getHeight();
            state.eyeHeight = type.getHeight() * 0.85F;
            state.ageInTicks = minecraft.gui.getGuiTicks();
            state.lightCoords = 0x00F000F0;
            state.outlineColor = EntityRenderState.NO_OUTLINE;
            state.nameTag = null;
            state.scoreText = null;
            state.shadowPieces.clear();

            if (state instanceof LivingEntityRenderState living) {
                living.scale = 1.0F;
                living.ageScale = 1.0F;
                living.walkAnimationSpeed = 0.0F;
                living.walkAnimationPos = 0.0F;
            }
            if (state instanceof ChickenRenderState chicken) {
                chicken.variant = DEFAULT_CHICKEN_VARIANT;
            } else if (state instanceof CowRenderState cow) {
                cow.variant = DEFAULT_COW_VARIANT;
            } else if (state instanceof PigRenderState pig) {
                pig.variant = DEFAULT_PIG_VARIANT;
            }

            fillNullRenderStateDefaults(state);
        }

        private void fillNullRenderStateDefaults(EntityRenderState state) {
            for (Class<?> type = state.getClass(); type != null && EntityRenderState.class.isAssignableFrom(type); type = type.getSuperclass()) {
                for (Field field : type.getDeclaredFields()) {
                    if (!field.trySetAccessible() || field.getType().isPrimitive()) {
                        continue;
                    }

                    try {
                        if (field.get(state) != null) {
                            continue;
                        }
                        String fieldName = field.getName().toLowerCase(Locale.ROOT);
                        boolean previewIdentityField = fieldName.equals("variant") || fieldName.equals("pattern") || fieldName.equals("markings");
                        if (field.getType().isEnum() && previewIdentityField) {
                            Object preferred = preferredEnumDefault(field.getType());
                            if (preferred != null) {
                                field.set(state, preferred);
                            }
                        } else if (field.getType() == ItemStack.class) {
                            field.set(state, ItemStack.EMPTY);
                        }
                    } catch (IllegalAccessException ignored) {
                    }
                }
            }
        }

        private Object preferredEnumDefault(Class<?> enumType) {
            try {
                Field defaultField = enumType.getField("DEFAULT");
                return defaultField.get(null);
            } catch (ReflectiveOperationException ignored) {
            }

            Object[] constants = enumType.getEnumConstants();
            return constants == null || constants.length == 0 ? null : constants[0];
        }

        private void renderPreviewState(GuiGraphicsExtractor graphics, EntityRenderState state, int left, int top, int right, int bottom, int scale, float yOffset, float mouseX, float mouseY) {
            float centerX = (left + right) / 2.0F;
            float centerY = (top + bottom) / 2.0F;
            float yRot = (float) Math.atan((centerX - mouseX) / 40.0F);
            float xRot = (float) Math.atan((centerY - mouseY) / 40.0F);
            Quaternionf bodyRotation = new Quaternionf().rotateZ((float) Math.PI);
            Quaternionf cameraRotation = new Quaternionf().rotateX(xRot * 20.0F * ((float) Math.PI / 180.0F));
            bodyRotation.mul(cameraRotation);

            if (state instanceof LivingEntityRenderState living) {
                living.bodyRot = 180.0F + yRot * 20.0F;
                living.yRot = yRot * 20.0F;
                living.xRot = -xRot * 20.0F;
                if (living.scale == 0.0F) {
                    living.scale = 1.0F;
                }
                living.boundingBoxWidth = state.boundingBoxWidth / living.scale;
                living.boundingBoxHeight = state.boundingBoxHeight / living.scale;
                living.scale = 1.0F;
            }

            graphics.entity(
                    state,
                    scale,
                    new Vector3f(0.0F, state.boundingBoxHeight / 2.0F + yOffset, 0.0F),
                    bodyRotation,
                    cameraRotation,
                    left,
                    top,
                    right,
                    bottom
            );
        }
    }

    private record MobOption(EntityType<?> type, String name, String id, boolean passive, String searchable) {
    }

    private abstract static class MobListEntry extends ObjectSelectionList.Entry<MobListEntry> {
    }

    private static final class MobSelectionList extends ObjectSelectionList<MobListEntry> {
        private final MobPickerScreen screen;

        private MobSelectionList(Minecraft minecraft, int width, int height, int y, MobPickerScreen screen) {
            super(minecraft, width, height, y, 48);
            this.screen = screen;
            centerListVertically = false;
        }

        @Override
        public int getRowWidth() {
            return Math.min(680, Math.max(280, getWidth() - 64));
        }

        @Override
        protected boolean entriesCanBeSelected() {
            return false;
        }

        private void replaceOptions(List<String> customEntries, List<MobOption> options) {
            clearEntries();
            for (String entry : customEntries) {
                addEntry(new CustomMobEntry(screen, entry));
            }
            for (MobOption option : options) {
                addEntry(new MobEntry(screen, option));
            }
        }
    }

    private static final class CustomMobEntry extends MobListEntry {
        private final MobPickerScreen screen;
        private final String entry;

        private CustomMobEntry(MobPickerScreen screen, String entry) {
            this.screen = screen;
            this.entry = entry;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            Font font = screen.getFont();
            int x = getX();
            int y = getY();
            int width = getWidth();
            int height = getHeight() - 2;

            graphics.fill(x, y + 1, x + width, y + height, hovered ? ROW_HOVER_BACKGROUND : ROW_BACKGROUND);

            int boxX = getContentX();
            int boxY = getContentY() + 14;
            graphics.outline(boxX, boxY, 14, 14, SELECTED_GREEN);
            graphics.fill(boxX + 2, boxY + 2, boxX + 12, boxY + 12, 0x6633AA55);
            String checkmark = "\u2713";
            graphics.text(font, Component.literal(checkmark), boxX + 7 - font.width(checkmark) / 2, boxY + 3, SELECTED_GREEN);

            int deleteX = boxX + 20;
            graphics.outline(deleteX, boxY, 14, 14, 0xFFAA5555);
            graphics.centeredText(font, Component.literal("x"), deleteX + 7, boxY + 3, 0xFFFF7777);

            int labelX = boxX + 44;
            int labelWidth = Math.max(80, getContentRight() - labelX - 10);
            graphics.text(font, Component.literal(trimToWidth(font, entry, labelWidth)), labelX, getContentY() + 8, NORMAL_TEXT);
            graphics.text(font, Component.literal("Custom entry"), labelX, getContentY() + 22, MUTED_TEXT);

            if (hovered) {
                graphics.setComponentTooltipForNextFrame(
                        screen.getFont(),
                        List.of(Component.literal(entry), Component.literal("Custom entry")),
                        mouseX,
                        mouseY
                );
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            int deleteX = getContentX() + 20;
            int deleteY = getContentY() + 14;
            if (event.button() == 0
                    && event.x() >= deleteX
                    && event.x() < deleteX + 14
                    && event.y() >= deleteY
                    && event.y() < deleteY + 14) {
                screen.requestDeleteCustom(entry);
                return true;
            }
            return event.button() == 0 && isMouseOver(event.x(), event.y());
        }

        @Override
        public Component getNarration() {
            return Component.literal(entry);
        }
    }

    private static final class MobEntry extends MobListEntry {
        private final MobPickerScreen screen;
        private final MobOption option;

        private MobEntry(MobPickerScreen screen, MobOption option) {
            this.screen = screen;
            this.option = option;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            Font font = screen.getFont();
            int x = getX();
            int y = getY();
            int width = getWidth();
            int height = getHeight() - 2;
            boolean selected = screen.isSelected(option.type());

            graphics.fill(x, y + 1, x + width, y + height, hovered ? ROW_HOVER_BACKGROUND : ROW_BACKGROUND);

            int boxX = getContentX();
            int boxY = getContentY() + 14;
            graphics.outline(boxX, boxY, 14, 14, selected ? SELECTED_GREEN : DARK_OUTLINE);
            if (selected) {
                graphics.fill(boxX + 2, boxY + 2, boxX + 12, boxY + 12, 0x6633AA55);
                String checkmark = "\u2713";
                graphics.text(font, Component.literal(checkmark), boxX + 7 - font.width(checkmark) / 2, boxY + 3, SELECTED_GREEN);
            }

            int previewWidth = 58;
            int previewX = getContentRight() - previewWidth;
            int labelX = boxX + 24;
            int labelWidth = Math.max(80, previewX - labelX - 10);
            graphics.text(font, Component.literal(trimToWidth(font, option.name(), labelWidth)), labelX, getContentY() + 8, NORMAL_TEXT);
            graphics.text(font, Component.literal(trimToWidth(font, option.id(), labelWidth)), labelX, getContentY() + 22, MUTED_TEXT);

            int previewY = getContentY() + 3;
            graphics.outline(previewX, previewY, previewWidth, 38, 0x664D4D4D);
            LivingEntity entity = screen.previewEntity(option.type());
            if (entity != null) {
                int scale = Math.max(8, Math.min(24, (int) (26.0F / Math.max(option.type().getWidth(), option.type().getHeight()))));
                InventoryScreen.extractEntityInInventoryFollowsMouse(
                        graphics,
                        previewX + 2,
                        previewY + 2,
                        previewX + previewWidth - 2,
                        previewY + 36,
                        scale,
                        0.0F,
                        mouseX,
                        mouseY,
                        entity
                );
            } else {
                EntityRenderState state = screen.previewRenderState(option.type());
                if (state != null) {
                    int scale = Math.max(8, Math.min(24, (int) (26.0F / Math.max(option.type().getWidth(), option.type().getHeight()))));
                    screen.renderPreviewState(
                            graphics,
                            state,
                            previewX + 2,
                            previewY + 2,
                            previewX + previewWidth - 2,
                            previewY + 36,
                            scale,
                            0.0F,
                            mouseX,
                            mouseY
                    );
                } else {
                    graphics.centeredText(font, Component.literal("3D"), previewX + previewWidth / 2, previewY + 15, MUTED_TEXT);
                }
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (event.button() == 0 && isMouseOver(event.x(), event.y())) {
                screen.toggle(option.type());
                return true;
            }
            return false;
        }

        @Override
        public Component getNarration() {
            return Component.literal(option.name());
        }
    }

    private final class BlockPickerScreen extends Screen {
        private final SaltsAnimalFarmConfigScreen owner;
        private final LinkedHashSet<String> explicitCustomEntries = new LinkedHashSet<>();
        private BlockGridList blockList;
        private EditBox searchBox;
        private EditBox customInput;
        private Button customPrimaryButton;
        private Button customCancelButton;
        private boolean addingCustom;
        private String pendingCustomDelete;
        private String customDraft = "";
        private String query = "";

        private BlockPickerScreen(SaltsAnimalFarmConfigScreen owner) {
            super(Component.literal("Soft Blocks"));
            this.owner = owner;
        }

        @Override
        protected void init() {
            clearWidgets();

            int searchWidth = Math.min(320, Math.max(120, width - 80));
            int searchX = Math.max(20, width / 2 - searchWidth / 2);
            searchBox = new EditBox(font, searchX, 34, searchWidth, 20, Component.literal("Search blocks"));
            searchBox.setHint(Component.literal("Search blocks and tags"));
            searchBox.setValue(query);
            searchBox.setResponder(value -> {
                query = value;
                rebuildBlockList();
            });
            addRenderableWidget(searchBox);

            addRenderableWidget(Button.builder(Component.literal("Add Custom"), button -> openAddCustomDialog())
                    .bounds(Math.max(20, width - 116), 34, 96, 20)
                    .build());

            int listTop = 62;
            int listHeight = Math.max(60, height - listTop - 38);
            blockList = new BlockGridList(minecraft, width, listHeight, listTop, this);
            addRenderableWidget(blockList);
            rebuildBlockList();

            addRenderableWidget(Button.builder(Component.literal("Reset List"), button -> {
                        owner.resetSoftBlocks();
                        rebuildBlockList();
                    })
                    .bounds(width / 2 - 104, height - 28, 100, 20)
                    .build());
            addRenderableWidget(Button.builder(Component.literal("Done"), button -> minecraft.setScreen(owner))
                    .bounds(width / 2 + 4, height - 28, 100, 20)
                    .build());

            addDialogWidgets();
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            graphics.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);
            graphics.centeredText(font, Component.literal(owner.status), width / 2, 22, owner.status.startsWith("Invalid") ? ERROR_TEXT : MUTED_TEXT);
        }

        @Override
        public void onClose() {
            minecraft.setScreen(owner);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (isDialogOpen()) {
                for (GuiEventListener widget : dialogWidgets()) {
                    if (widget.mouseClicked(event, doubleClick)) {
                        setFocused(widget);
                        return true;
                    }
                }
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (isDialogOpen()) {
                if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                    closeDialog();
                    return true;
                }
                if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
                    activateDialogPrimary();
                    return true;
                }
                return customInput != null && customInput.keyPressed(event);
            }
            return super.keyPressed(event);
        }

        @Override
        public boolean charTyped(CharacterEvent event) {
            if (isDialogOpen()) {
                return customInput != null && customInput.charTyped(event);
            }
            return super.charTyped(event);
        }

        private void rebuildBlockList() {
            if (blockList == null) {
                return;
            }

            List<BlockChoice> allTagChoices = allBlockTagChoices();
            List<BlockChoice> allBlockChoices = allBlockChoices();
            LinkedHashSet<String> knownEntries = new LinkedHashSet<>();
            allTagChoices.forEach(choice -> knownEntries.add(choice.entry()));
            allBlockChoices.forEach(choice -> knownEntries.add(choice.entry()));

            List<BlockChoice> customChoices = customBlockChoices(knownEntries).stream()
                    .filter(choice -> matchesQuery(choice.searchable(), query))
                    .toList();
            List<BlockChoice> tagChoices = allTagChoices.stream()
                    .filter(choice -> matchesQuery(choice.searchable(), query))
                    .toList();
            List<BlockChoice> blockChoices = allBlockChoices.stream()
                    .filter(choice -> matchesQuery(choice.searchable(), query))
                    .toList();
            blockList.replaceChoices(customChoices, tagChoices, blockChoices);
        }

        private boolean isChoiceSelected(BlockChoice choice) {
            if (choice.custom()) {
                return owner.values.softBlocks.contains(choice.entry());
            }
            if (choice.tag()) {
                return owner.values.softBlocks.contains(choice.entry());
            }
            return isBlockSelected(choice.block());
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
            if (choice.custom()) {
                return;
            }

            LinkedHashSet<String> entries = new LinkedHashSet<>(owner.values.softBlocks);
            if (choice.tag()) {
                if (!entries.remove(choice.entry())) {
                    entries.add(choice.entry());
                }
            } else if (isBlockSelected(choice.block())) {
                entries.remove(choice.entry());
                List<String> matchingTags = entries.stream()
                        .filter(entry -> blockMatchesTag(choice.block(), entry))
                        .toList();
                matchingTags.forEach(entries::remove);
                for (String tag : matchingTags) {
                    entries.addAll(expandBlockTag(tag, choice.block()));
                }
            } else {
                entries.add(choice.entry());
            }

            owner.setSoftBlockEntries(entries);
            rebuildBlockList();
        }

        private void openAddCustomDialog() {
            addingCustom = true;
            pendingCustomDelete = null;
            customDraft = "";
            init();
        }

        private void requestDeleteCustom(String entry) {
            pendingCustomDelete = entry;
            addingCustom = false;
            customDraft = "";
            init();
        }

        private void closeDialog() {
            addingCustom = false;
            pendingCustomDelete = null;
            customDraft = customInput == null ? "" : customInput.getValue();
            init();
        }

        private boolean isDialogOpen() {
            return addingCustom || pendingCustomDelete != null;
        }

        private List<GuiEventListener> dialogWidgets() {
            List<GuiEventListener> widgets = new ArrayList<>();
            if (customInput != null) {
                widgets.add(customInput);
            }
            if (customPrimaryButton != null) {
                widgets.add(customPrimaryButton);
            }
            if (customCancelButton != null) {
                widgets.add(customCancelButton);
            }
            return widgets;
        }

        private void addDialogWidgets() {
            customInput = null;
            customPrimaryButton = null;
            customCancelButton = null;
            if (!isDialogOpen()) {
                return;
            }

            int dialogWidth = Math.min(340, Math.max(250, width - 48));
            int dialogHeight = addingCustom ? 116 : 104;
            int dialogX = width / 2 - dialogWidth / 2;
            int dialogY = height / 2 - dialogHeight / 2;

            if (addingCustom) {
                addRenderableOnly(new DialogBackdrop(font, "Add Custom", "Use modname:block or #modname:tag", width, height, dialogX, dialogY, dialogWidth, dialogHeight));
                customInput = new EditBox(font, dialogX + 16, dialogY + 44, dialogWidth - 32, 20, Component.literal("Custom block or tag"));
                customInput.setHint(Component.literal("modname:block or #modname:tag"));
                customInput.setValue(customDraft);
                customInput.setResponder(value -> customDraft = value);
                addRenderableWidget(customInput);
                customPrimaryButton = Button.builder(Component.literal("Add"), button -> submitCustomEntry())
                        .bounds(dialogX + dialogWidth / 2 - 104, dialogY + 80, 100, 20)
                        .build();
                customCancelButton = Button.builder(Component.literal("Cancel"), button -> closeDialog())
                        .bounds(dialogX + dialogWidth / 2 + 4, dialogY + 80, 100, 20)
                        .build();
                addRenderableWidget(customPrimaryButton);
                addRenderableWidget(customCancelButton);
                setInitialFocus(customInput);
            } else {
                addRenderableOnly(new DialogBackdrop(font, "Delete Custom Entry?", pendingCustomDelete, width, height, dialogX, dialogY, dialogWidth, dialogHeight));
                customPrimaryButton = Button.builder(Component.literal("Delete"), button -> confirmDeleteCustom())
                        .bounds(dialogX + dialogWidth / 2 - 104, dialogY + 68, 100, 20)
                        .build();
                customCancelButton = Button.builder(Component.literal("Cancel"), button -> closeDialog())
                        .bounds(dialogX + dialogWidth / 2 + 4, dialogY + 68, 100, 20)
                        .build();
                addRenderableWidget(customPrimaryButton);
                addRenderableWidget(customCancelButton);
            }
        }

        private void activateDialogPrimary() {
            if (addingCustom) {
                submitCustomEntry();
            } else if (pendingCustomDelete != null) {
                confirmDeleteCustom();
            }
        }

        private void submitCustomEntry() {
            String entry = normalizeCustomEntry(customInput == null ? customDraft : customInput.getValue(), true);
            if (entry == null) {
                owner.status = "Invalid custom block or tag id";
                return;
            }

            LinkedHashSet<String> entries = new LinkedHashSet<>(owner.values.softBlocks);
            entries.add(entry);
            explicitCustomEntries.add(entry);
            owner.setSoftBlockEntries(entries);
            addingCustom = false;
            customDraft = "";
            init();
        }

        private void confirmDeleteCustom() {
            if (pendingCustomDelete == null) {
                return;
            }

            LinkedHashSet<String> entries = new LinkedHashSet<>(owner.values.softBlocks);
            entries.remove(pendingCustomDelete);
            explicitCustomEntries.remove(pendingCustomDelete);
            owner.setSoftBlockEntries(entries);
            pendingCustomDelete = null;
            init();
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

            return tags.values()
                    .stream()
                    .sorted(Comparator.comparing(tag -> tag.location().toString()))
                    .map(tag -> {
                        String entry = "#" + displayIdentifier(tag.location());
                        String shortName = "#" + shortIdentifier(tag.location());
                        String label = titleFromPath(tag.location().getPath());
                        return new BlockChoice(
                                true,
                                entry,
                                shortName,
                                label,
                                searchText(entry, shortName, label),
                                tagPreviewStacks(tag),
                                null,
                                false
                        );
                    })
                    .toList();
        }

        private List<BlockChoice> customBlockChoices(Set<String> knownEntries) {
            return owner.values.softBlocks.stream()
                    .filter(entry -> explicitCustomEntries.contains(entry) || !knownEntries.contains(entry))
                    .map(this::customBlockChoice)
                    .toList();
        }

        private BlockChoice customBlockChoice(String entry) {
            List<ItemStack> previewStacks = List.of();
            if (entry.startsWith("#")) {
                TagKey<Block> tag = blockTag(entry);
                if (tag != null) {
                    previewStacks = tagPreviewStacks(tag);
                }
            } else {
                Identifier id = Identifier.tryParse(entry);
                if (id != null) {
                    Block block = BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
                    if (block != null && block.asItem() != Items.AIR) {
                        previewStacks = List.of(safeBlockStack(block));
                    }
                }
            }

            return new BlockChoice(
                    entry.startsWith("#"),
                    entry,
                    entry,
                    "Custom entry",
                    searchText(entry, "custom"),
                    previewStacks,
                    null,
                    true
            );
        }

        private List<BlockChoice> allBlockChoices() {
            return BuiltInRegistries.BLOCK.entrySet()
                    .stream()
                    .map(entry -> entry.getValue())
                    .filter(block -> block.asItem() != Items.AIR)
                    .map(this::blockChoice)
                    .sorted(Comparator.comparing(choice -> choice.label().toLowerCase(Locale.ROOT)))
                    .toList();
        }

        private BlockChoice blockChoice(Block block) {
            String name = block.getName().getString();
            String id = idString(block);
            return new BlockChoice(
                    false,
                    id,
                    name,
                    id,
                    searchText(name, id),
                    List.of(safeBlockStack(block)),
                    block,
                    false
            );
        }

        private ItemStack safeBlockStack(Block block) {
            try {
                return new ItemStack(block);
            } catch (RuntimeException exception) {
                return safeItemStack(block.asItem());
            }
        }

        private ItemStack safeItemStack(Item item) {
            if (MENU_SAFE_ITEM_STACK_CONSTRUCTOR == null || item == Items.AIR) {
                return ItemStack.EMPTY;
            }

            try {
                return MENU_SAFE_ITEM_STACK_CONSTRUCTOR.newInstance(
                        item.builtInRegistryHolder(),
                        1,
                        new PatchedDataComponentMap(menuSafeItemComponents(item))
                );
            } catch (ReflectiveOperationException exception) {
                return ItemStack.EMPTY;
            }
        }

        private DataComponentMap menuSafeItemComponents(Item item) {
            Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
            return DataComponentMap.builder()
                    .addAll(DataComponents.COMMON_ITEM_COMPONENTS)
                    .set(DataComponents.ITEM_NAME, Component.translatable(item.getDescriptionId()))
                    .set(DataComponents.ITEM_MODEL, itemId)
                    .build();
        }

        private List<ItemStack> tagPreviewStacks(TagKey<Block> tag) {
            LinkedHashSet<Block> blocks = new LinkedHashSet<>();
            StreamSupport.stream(BuiltInRegistries.BLOCK.getTagOrEmpty(tag).spliterator(), false)
                    .map(Holder::value)
                    .forEach(blocks::add);

            if (blocks.isEmpty()) {
                collectBlocksFromTagResources(tag.location(), blocks, new LinkedHashSet<>());
            }

            return blocks.stream()
                    .filter(block -> block.asItem() != Items.AIR)
                    .sorted(Comparator.comparing(block -> block.getName().getString().toLowerCase(Locale.ROOT)))
                    .map(this::safeBlockStack)
                    .filter(stack -> !stack.isEmpty())
                    .toList();
        }

        private boolean collectBlocksFromTagResources(Identifier tagId, LinkedHashSet<Block> blocks, Set<Identifier> visiting) {
            if (!visiting.add(tagId)) {
                return false;
            }

            boolean foundResource = false;
            Identifier resourceId = Identifier.fromNamespaceAndPath(tagId.getNamespace(), "tags/block/" + tagId.getPath() + ".json");
            for (Resource resource : minecraft.getResourceManager().getResourceStack(resourceId)) {
                foundResource = true;
                try (BufferedReader reader = resource.openAsReader()) {
                    collectBlocksFromTagJson(reader, blocks, visiting);
                } catch (IOException | RuntimeException ignored) {
                }
            }

            if (!foundResource) {
                foundResource = collectBlocksFromClasspathTag(tagId, blocks, visiting);
            }

            visiting.remove(tagId);
            return foundResource;
        }

        private boolean collectBlocksFromClasspathTag(Identifier tagId, LinkedHashSet<Block> blocks, Set<Identifier> visiting) {
            boolean foundResource = false;
            String resourcePath = "data/" + tagId.getNamespace() + "/tags/block/" + tagId.getPath() + ".json";
            LinkedHashSet<ClassLoader> classLoaders = new LinkedHashSet<>();
            classLoaders.add(SaltsAnimalFarmConfigScreen.class.getClassLoader());
            classLoaders.add(BlockTags.class.getClassLoader());
            classLoaders.add(Thread.currentThread().getContextClassLoader());

            try {
                for (ClassLoader classLoader : classLoaders) {
                    if (classLoader == null) {
                        continue;
                    }

                    Enumeration<URL> resources = classLoader.getResources(resourcePath);
                    while (resources.hasMoreElements()) {
                        foundResource = true;
                        URL resourceUrl = resources.nextElement();
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceUrl.openStream()))) {
                            collectBlocksFromTagJson(reader, blocks, visiting);
                        } catch (IOException | RuntimeException ignored) {
                        }
                    }
                }
            } catch (IOException ignored) {
            }
            return foundResource;
        }

        private void collectBlocksFromTagJson(BufferedReader reader, LinkedHashSet<Block> blocks, Set<Identifier> visiting) {
            JsonObject tagJson = JsonParser.parseReader(reader).getAsJsonObject();
            if (tagJson.has("replace") && tagJson.get("replace").getAsBoolean()) {
                blocks.clear();
            }

            JsonArray values = tagJson.getAsJsonArray("values");
            if (values == null) {
                return;
            }

            for (JsonElement value : values) {
                collectBlockTagValue(value, blocks, visiting);
            }
        }

        private void collectBlockTagValue(JsonElement value, LinkedHashSet<Block> blocks, Set<Identifier> visiting) {
            String id = null;
            boolean required = true;
            if (value.isJsonPrimitive()) {
                id = value.getAsString();
            } else if (value.isJsonObject()) {
                JsonObject valueObject = value.getAsJsonObject();
                JsonElement idElement = valueObject.get("id");
                if (idElement != null) {
                    id = idElement.getAsString();
                }
                JsonElement requiredElement = valueObject.get("required");
                if (requiredElement != null) {
                    required = requiredElement.getAsBoolean();
                }
            }

            if (id == null || id.isBlank()) {
                return;
            }

            if (id.startsWith("#")) {
                Identifier nestedTag = Identifier.tryParse(id.substring(1));
                if (nestedTag != null) {
                    collectBlocksFromTagResources(nestedTag, blocks, visiting);
                }
                return;
            }

            Identifier blockId = Identifier.tryParse(id);
            if (blockId == null) {
                return;
            }

            Block block = BuiltInRegistries.BLOCK.getOptional(blockId).orElse(null);
            if (block != null) {
                blocks.add(block);
            } else if (required) {
                // Vanilla and datapack tags can reference optional content. Required misses are ignored here
                // so a broken external tag cannot blank the config picker.
            }
        }
    }

    private record BlockChoice(
            boolean tag,
            String entry,
            String label,
            String detail,
            String searchable,
            List<ItemStack> previewStacks,
            Block block,
            boolean custom
    ) {
        private ItemStack previewStack(int guiTicks) {
            if (previewStacks.isEmpty()) {
                return ItemStack.EMPTY;
            }

            return previewStacks.get(Math.floorMod(guiTicks / 40, previewStacks.size()));
        }
    }

    private static final class BlockGridList extends ObjectSelectionList<BlockGridEntry> {
        private static final int CARD_WIDTH = 76;
        private static final int BLOCK_CARD_HEIGHT = 54;
        private static final int TAG_CARD_HEIGHT = 54;
        private static final int GAP = 6;

        private final BlockPickerScreen screen;

        private BlockGridList(Minecraft minecraft, int width, int height, int y, BlockPickerScreen screen) {
            super(minecraft, width, height, y, BLOCK_CARD_HEIGHT + GAP);
            this.screen = screen;
            centerListVertically = false;
        }

        @Override
        public int getRowWidth() {
            return Math.min(780, Math.max(240, getWidth() - 64));
        }

        @Override
        protected boolean entriesCanBeSelected() {
            return false;
        }

        private void replaceChoices(List<BlockChoice> customChoices, List<BlockChoice> tags, List<BlockChoice> blocks) {
            clearEntries();
            if (!customChoices.isEmpty()) {
                addEntry(new BlockGridSectionEntry(screen.getFont(), "Custom Entries"), 24);
                addRows(customChoices, BLOCK_CARD_HEIGHT);
            }
            addEntry(new BlockGridSectionEntry(screen.getFont(), "Block Tags"), 24);
            addRows(tags, TAG_CARD_HEIGHT);
            addEntry(new BlockGridSectionEntry(screen.getFont(), "Blocks"), 24);
            addRows(blocks, BLOCK_CARD_HEIGHT);
        }

        private void addRows(List<BlockChoice> choices, int cardHeight) {
            int columns = columns();
            for (int i = 0; i < choices.size(); i += columns) {
                addEntry(
                        new BlockChoiceRowEntry(screen, choices.subList(i, Math.min(i + columns, choices.size())), cardHeight, columns),
                        cardHeight + GAP
                );
            }
        }

        private int columns() {
            return Math.max(2, (getRowWidth() + GAP) / (CARD_WIDTH + GAP));
        }
    }

    private abstract static class BlockGridEntry extends ObjectSelectionList.Entry<BlockGridEntry> {
    }

    private static final class BlockGridSectionEntry extends BlockGridEntry {
        private final Font font;
        private final String label;

        private BlockGridSectionEntry(Font font, String label) {
            this.font = font;
            this.label = label;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            graphics.text(font, Component.literal(label), getContentX(), getContentY() + 8, SECTION_TEXT);
        }

        @Override
        public Component getNarration() {
            return Component.literal(label);
        }
    }

    private static final class BlockChoiceRowEntry extends BlockGridEntry {
        private final BlockPickerScreen screen;
        private final List<BlockChoice> choices;
        private final int cardHeight;
        private final int columns;

        private BlockChoiceRowEntry(BlockPickerScreen screen, List<BlockChoice> choices, int cardHeight, int columns) {
            this.screen = screen;
            this.choices = List.copyOf(choices);
            this.cardHeight = cardHeight;
            this.columns = columns;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            for (int i = 0; i < choices.size(); i++) {
                BlockChoice choice = choices.get(i);
                int x = cardX(i);
                int y = getContentY();
                boolean cardHovered = mouseX >= x && mouseX < x + BlockGridList.CARD_WIDTH && mouseY >= y && mouseY < y + cardHeight;
                boolean selected = screen.isChoiceSelected(choice);
                Font font = screen.getFont();

                graphics.fill(x, y, x + BlockGridList.CARD_WIDTH, y + cardHeight, cardHovered ? CARD_HOVER_BACKGROUND : CARD_BACKGROUND);
                graphics.outline(x, y, BlockGridList.CARD_WIDTH, cardHeight, selected ? SELECTED_GREEN : DARK_OUTLINE);

                if (choice.custom()) {
                    int deleteX = x + BlockGridList.CARD_WIDTH - 14;
                    int deleteY = y + 3;
                    graphics.outline(deleteX, deleteY, 10, 10, 0xFFAA5555);
                    graphics.centeredText(font, Component.literal("x"), deleteX + 5, deleteY - 1, 0xFFFF7777);
                }

                ItemStack previewStack = choice.previewStack(Minecraft.getInstance().gui.getGuiTicks());
                if (!previewStack.isEmpty()) {
                    graphics.item(previewStack, x + BlockGridList.CARD_WIDTH / 2 - 8, y + 7);
                }

                if (choice.tag()) {
                    graphics.centeredText(font, Component.literal(trimToWidth(font, choice.label(), BlockGridList.CARD_WIDTH - 8)), x + BlockGridList.CARD_WIDTH / 2, y + 31, selected ? SELECTED_GREEN : NORMAL_TEXT);
                } else {
                    graphics.centeredText(font, Component.literal(trimToWidth(font, choice.label(), BlockGridList.CARD_WIDTH - 8)), x + BlockGridList.CARD_WIDTH / 2, y + 31, NORMAL_TEXT);
                }

                if (cardHovered) {
                    graphics.setComponentTooltipForNextFrame(
                            screen.getFont(),
                            List.of(Component.literal(choice.entry()), Component.literal(selected ? "Selected" : "Not selected")),
                            mouseX,
                            mouseY
                    );
                }
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (event.button() != 0 || !isMouseOver(event.x(), event.y())) {
                return false;
            }

            int index = cardIndex(event.x(), event.y());
            if (index >= 0 && index < choices.size()) {
                BlockChoice choice = choices.get(index);
                if (choice.custom()) {
                    int x = cardX(index);
                    int y = getContentY();
                    int deleteX = x + BlockGridList.CARD_WIDTH - 14;
                    int deleteY = y + 3;
                    if (event.x() >= deleteX && event.x() < deleteX + 10 && event.y() >= deleteY && event.y() < deleteY + 10) {
                        screen.requestDeleteCustom(choice.entry());
                    }
                } else {
                    screen.toggle(choice);
                }
                return true;
            }
            return false;
        }

        @Override
        public Component getNarration() {
            return Component.literal("Soft block choices");
        }

        private int cardIndex(double mouseX, double mouseY) {
            if (mouseY < getContentY() || mouseY >= getContentY() + cardHeight) {
                return -1;
            }

            for (int i = 0; i < choices.size(); i++) {
                int x = cardX(i);
                if (mouseX >= x && mouseX < x + BlockGridList.CARD_WIDTH) {
                    return i;
                }
            }
            return -1;
        }

        private int cardX(int index) {
            int totalWidth = columns * BlockGridList.CARD_WIDTH + (columns - 1) * BlockGridList.GAP;
            int startX = getContentX() + Math.max(0, (getContentWidth() - totalWidth) / 2);
            return startX + index * (BlockGridList.CARD_WIDTH + BlockGridList.GAP);
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
            farmAnimals = new ArrayList<>(config.farmAnimals());
            scaryMobs = new ArrayList<>(config.scaryMobs());
            softBlocks = new ArrayList<>(config.softBlocks());
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
