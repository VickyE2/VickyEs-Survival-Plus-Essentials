package org.vicky.vspe_forge.advancements;

import net.kyori.adventure.text.Component;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.vicky.forge.forgeplatform.adventure.AdventureComponentConverter;
import org.vicky.forge.forgeplatform.useables.ForgePlatformItem;
import org.vicky.forge.forgeplatform.useables.ForgePlatformPlayer;
import org.vicky.platform.PlatformItem;
import org.vicky.platform.PlatformPlayer;
import org.vicky.vspe.platform.features.advancement.PlatformAdvancement;

import java.util.UUID;

public class ForgeAdvancement implements PlatformAdvancement {

    private final AdvancementHolder handle;
    private final ResourceLocation id;
    private final UUID uuid;

    public ForgeAdvancement(AdvancementHolder advancement) {
        this.handle = advancement;
        this.id = advancement.id();
        this.uuid = UUID.nameUUIDFromBytes(id.toString().getBytes());
    }

    @Override
    public UUID getId() {
        return uuid;
    }

    @Override
    public String getTitle() {
        return handle.value().display().map(d -> d.getTitle().getString()).orElse("Unnamed Advancement");
    }

    @Override
    public Component getDescription() {
        return handle.value().display()
                .map(d -> AdventureComponentConverter.fromNative(d.getDescription()))
                .orElse(Component.text("No description"));
    }

    @Override
    public PlatformItem getIcon() {
        return handle.value().display()
                .map(d -> new ForgePlatformItem(d.getIcon()))
                .orElse(new ForgePlatformItem(ItemStack.EMPTY));
    }

    @Override
    public boolean isEligible(PlatformPlayer player) {
        if (!(player instanceof ForgePlatformPlayer sp)) return false;
        AdvancementProgress progress = sp.getHandle().getAdvancements().getOrStartProgress(handle);
        return progress.isDone();
    }

    @Override
    public void grant(PlatformPlayer player) {
        if (!(player instanceof ForgePlatformPlayer sp)) return;
        AdvancementProgress progress = sp.getHandle().getAdvancements().getOrStartProgress(handle);
        for (String remaining : progress.getRemainingCriteria()) {
            sp.getHandle().getAdvancements().award(handle, remaining);
        }
    }

    @Override
    public boolean isHasParent() {
        return handle.value().parent().isPresent();
    }

    @Override
    public String getIdentifier() {
        return id.toString(); // e.g., "my_mod:advancement_name"
    }

    public AdvancementHolder getHandle() {
        return handle;
    }
}
