package com.snc.energies.client.screen;

import com.snc.energies.registry.IndustryKind;
import com.snc.energies.registry.IndustryRecipes;
import com.snc.energies.registry.WorkshopRecipes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Recipe quantities come from the same records as the machine engine. */
public final class FieldGuideScreen extends Screen {
    private static final int PAGES = 9 + IndustryKind.values().length;
    /** Warm inks on parchment instead of near-black (readability fix). */
    private static final int INK_BODY = 0xff4d3a26;
    private static final int INK_HEADING = 0xff7a3a20;
    private static final int INK_MUTED = 0xff8a6a48;
    private static final int INK_ERROR = 0xffa04430;
    /** Sidebar index palette, kept below the body ink for a quiet look. */
    private static final int INDEX_COLOR = 0xffa45130;
    private static final int INDEX_CURRENT = 0xffd8e6d0;
    private static final int BOOK_PARCHMENT = 0xfff0e3c4;
    private static final int PARCHMENT_EDGE = 0xffd9c39a;
    private static final int INDEX_LINE = 11;

    private int page;
    private int scroll;
    private int indexScroll;
    private int left;
    private int right;
    private EditBox search;
    private String query = "";
    private List<Integer> matches = new ArrayList<>();

    public FieldGuideScreen() { super(Component.translatable("item.snc_energies.field_guide")); }

    @Override protected void init() {
        // Index behind a narrower book: search is created first so the query rebuild keeps its title.
        int center = width / 2;
        int indexWidth = Math.min(90, center - 130);
        left = Math.max(indexWidth + 26, center - 140);
        right = Math.min(width - 12, center + 140);
        int bottom = height - 26;
        addRenderableWidget(Button.builder(Component.literal("<"), button -> step(-1))
                .bounds(center - 110, bottom, 32, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> step(1))
                .bounds(center + 78, bottom, 32, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(center - 38, bottom, 76, 20).build());
        addRenderableWidget(Button.builder(Component.literal("↑"), button -> scroll = Math.max(0, scroll - 44)).bounds(center - 73, bottom, 28, 20).build());
        addRenderableWidget(Button.builder(Component.literal("↓"), button -> scroll += 44).bounds(center + 45, bottom, 28, 20).build());
        search = new EditBox(font, left + 10, 18, right - left - 20, 16, Component.translatable("gui.snc_energies.search"));
        search.setMaxLength(64);
        search.setValue(query);
        search.setResponder(value -> { query = value; rebuildMatches(); });
        addRenderableWidget(search);
        rebuildMatches();
    }

    /** Jump to a page with the book page-turn feedback; called from index clicks and search jumps. */
    private void goToPage(int candidate) {
        page = candidate;
        scroll = 0;
        indexScroll = 0;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }

    /** Navigation stays inside the pages that match the search box. */
    private void step(int direction) {
        if (matches.isEmpty()) return;
        int at = matches.indexOf(page);
        goToPage(matches.get(Math.floorMod((at < 0 ? 0 : at) + direction, matches.size())));
    }

    private void rebuildMatches() {
        String[] tokens = normalize(query).trim().split("\\s+");
        matches = new ArrayList<>();
        for (int candidate = 0; candidate < PAGES; candidate++) {
            if (query.isBlank()) { matches.add(candidate); continue; }
            String index = pageIndex(candidate);
            boolean hit = true;
            for (String token : tokens) if (!index.contains(token)) { hit = false; break; }
            if (hit) matches.add(candidate);
        }
        if (!query.isBlank() && !matches.contains(page) && !matches.isEmpty()) goToPage(matches.get(0));
    }

    /** Searchable text for one page: title plus every text that appears on it. */
    private String pageIndex(int candidatePage) {
        StringBuilder text = new StringBuilder();
        if (candidatePage < 8) {
            text.append(title(candidatePage)).append(' ')
                .append(trans("guide.snc_energies.body." + candidatePage));
        } else if (candidatePage < 8 + IndustryKind.values().length) {
            IndustryKind kind = IndustryKind.values()[candidatePage - 8];
            text.append(trans("block.snc_energies." + kind.id)).append(' ');
            for (var recipe : IndustryRecipes.all()) if (recipe.kind() == kind)
                text.append(recipe.input().getName(recipe.input().getDefaultInstance()).getString()).append(' ')
                    .append(recipe.product().getName(recipe.product().getDefaultInstance()).getString()).append(' ');
            text.append(trans("guide.snc_energies.craft." + kind.id)).append(' ');
            String special = kind == IndustryKind.BOILER ? "boiler"
                    : kind == IndustryKind.TURBINE ? "turbine"
                    : kind == IndustryKind.SYNTHESIZER ? "synthesis"
                    : kind == IndustryKind.COMPACTOR ? "compactor" : null;
            if (special != null) text.append(trans("guide.snc_energies." + special)).append(' ');
            text.append(trans("guide.snc_energies.industry_base"));
        } else {
            text.append(trans("block.snc_energies.silo")).append(' ');
            text.append(trans("guide.snc_energies.craft.silo")).append(' ');
            text.append(trans("guide.snc_energies.silo"));
        }
        return normalize(text.toString());
    }

    /** Full page title, also used as the heading on the page itself. */
    private Component heading(int candidatePage) {
        if (candidatePage < 8) return Component.translatable("guide.snc_energies.title." + candidatePage);
        if (candidatePage < 8 + IndustryKind.values().length)
            return Component.translatable("block.snc_energies." + IndustryKind.values()[candidatePage - 8].id);
        return Component.translatable("block.snc_energies.silo");
    }

    /** Short index label: numbered titles drop the number, machines use their block name. */
    private String title(int candidatePage) {
        String text = heading(candidatePage).getString();
        return candidatePage < 8 && text.length() > 4 && text.charAt(3) == '/' ? text.substring(4).trim() : text;
    }

    private static String trans(String key) { return Component.translatable(key).getString(); }

    /** Lowercase without diacritics so "ferro" also finds "Ferro" and "minério" matches "minerio". */
    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{Mn}+", "")
                .toLowerCase(Locale.ROOT);
    }

    @Override public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        // Background here, not in extractRenderState: it must never sit on top of the text.
        g.fill(0, 0, width, height, 0xff15201b);
        g.fill(6, 38, left - 12, height - 32, BOOK_PARCHMENT); // sidebar parchment
        g.fill(6, 38, 11, height - 32, 0xffa45130);            // its spine
        g.fill(6, height - 34, left - 12, height - 32, PARCHMENT_EDGE);
        // Search box zone sits on its own parchment strip so the index never overlaps the box.
        g.fill(left, 12, right, 38, BOOK_PARCHMENT);
        g.fill(left, 12, left + 5, 38, 0xffa45130);
        g.fill(left, 12, right, height - 32, BOOK_PARCHMENT);
        g.fill(left, 12, left + 5, height - 32, 0xffa45130);
        g.fill(left, height - 34, right, height - 32, PARCHMENT_EDGE);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick); // widgets above the parchment
        if (query.isEmpty()) {
            g.text(font, Component.translatable("gui.snc_energies.search_hint"), left + 14, 23, INK_MUTED);
        } else if (matches.isEmpty()) {
            g.text(font, Component.translatable("gui.snc_energies.no_results"), left + 14, 23, INK_ERROR);
        } else {
            g.text(font, Component.translatable("gui.snc_energies.matches", matches.indexOf(page) + 1, matches.size()),
                    left + 14, 23, INK_MUTED);
        }
        // Sidebar index entries: current page highlighted, non-matching pages dimmed when filtering.
        int columnWidth = left - 12 - 16;
        int indexY = 42 - indexScroll;
        for (int entry = 0; entry < PAGES; entry++) {
            boolean isMatch = matches.contains(entry);
            int ink = entry == page ? INDEX_CURRENT : isMatch ? INDEX_COLOR : 0x60a45130;
            String label = font.plainSubstrByWidth(title(entry), columnWidth);
            g.text(font, label, 16, indexY, ink);
            if (entry == page) g.fill(12, indexY + 9, 12 + columnWidth, indexY + 10, 0x80d8e6d0);
            indexY += INDEX_LINE;
        }
        int top = 42;
        for (var line : font.split(heading(page), right - left - 30)) { g.text(font, line, left + 15, top, INK_HEADING); top += 12; }
        top += 6;
        List<FormattedCharSequence> lines = new ArrayList<>();
        if (page >= 8) {
            if (page < 8 + IndustryKind.values().length) {
                IndustryKind kind = IndustryKind.values()[page - 8];
                for (var recipe : IndustryRecipes.all()) if (recipe.kind() == kind) {
                    var text = Component.literal(recipe.count() + " × ").append(recipe.input().getName(recipe.input().getDefaultInstance()));
                    if (recipe.reagentCount() > 0) text.append(" + " + recipe.reagentCount() + " × ").append(recipe.reagent().getName(recipe.reagent().getDefaultInstance()));
                    text.append(" → " + recipe.productCount() + " × ").append(recipe.product().getName(recipe.product().getDefaultInstance()));
                    if (recipe.residueCount() > 0) text.append(" + " + recipe.residueCount() + " × ").append(recipe.residue().getName(recipe.residue().getDefaultInstance()));
                    lines.addAll(font.split(text, right - left - 30));
                    lines.addAll(font.split(Component.literal(" "), right - left - 30));
                }
                lines.addAll(font.split(Component.translatable("guide.snc_energies.craft." + kind.id), right - left - 30));
                String special = kind == IndustryKind.BOILER ? "boiler"
                        : kind == IndustryKind.TURBINE ? "turbine"
                        : kind == IndustryKind.SYNTHESIZER ? "synthesis"
                        : kind == IndustryKind.COMPACTOR ? "compactor" : null;
                if (special != null) lines.addAll(font.split(Component.translatable("guide.snc_energies." + special), right - left - 30));
                lines.addAll(font.split(Component.translatable("guide.snc_energies.industry_base", kind.width + " × " + kind.depth + " × " + kind.height), right - left - 30));
            } else {
                lines.addAll(font.split(Component.translatable("guide.snc_energies.craft.silo"), right - left - 30));
                lines.addAll(font.split(Component.literal(" "), right - left - 30));
                lines.addAll(font.split(Component.translatable("guide.snc_energies.silo"), right - left - 30));
            }
        } else {
            if (page == 2 || page == 3) {
                for (var recipe : WorkshopRecipes.all()) {
                    if (recipe.press() != (page == 3)) continue;
                    Component text = Component.literal(recipe.count() + " × ").append(recipe.input().getName(recipe.input().getDefaultInstance()))
                            .append(" → " + recipe.outputCount() + " × ").append(recipe.output().getName(recipe.output().getDefaultInstance()));
                    if (recipe.residueCount() > 0) text = text.copy().append(" + " + recipe.residueCount() + " × ").append(recipe.residue().getName(recipe.residue().getDefaultInstance()));
                    lines.addAll(font.split(text, right - left - 30));
                    lines.addAll(font.split(Component.literal(" "), right - left - 30));
                }
            }
            lines.addAll(font.split(Component.translatable("guide.snc_energies.body." + page), right - left - 30));
        }
        scroll = Math.min(scroll, Math.max(0, lines.size() * 11 - (height - 44 - top)));
        int cursor = top;
        for (var line : lines) {
            int at = cursor - scroll;
            if (at >= top && at < height - 44) g.text(font, line, left + 15, at, INK_BODY);
            cursor += 11;
        }
    }

    /** Clicking an index entry jumps straight to that page; outside the current filter it clears the search. */
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        int mouseY = (int) event.y();
        if (event.button() == 0 && event.x() < left - 12 && mouseY >= 38 && mouseY < height - 32) {
            int entry = (mouseY - 42 + indexScroll) / INDEX_LINE;
            if (entry >= 0 && entry < PAGES) {
                if (!matches.contains(entry) && !query.isBlank()) search.setValue("");
                goToPage(entry);
                return true;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    /** The mouse wheel scrolls the page text inside the book, or the index above it. */
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX < left - 12) {
            indexScroll = Math.max(0, indexScroll - (int) Math.signum(scrollY) * INDEX_LINE);
            return true;
        }
        scroll = Math.max(0, scroll - (int) Math.signum(scrollY) * 22);
        return true;
    }
}
