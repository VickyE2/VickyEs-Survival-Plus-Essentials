package org.vicky.vspe.features.AdvancementPlus.Advancements;

import eu.endercentral.crazy_advancements.advancement.AdvancementDisplay.AdvancementFrame;
import eu.endercentral.crazy_advancements.advancement.AdvancementReward;
import eu.endercentral.crazy_advancements.advancement.AdvancementVisibility;
import eu.endercentral.crazy_advancements.advancement.criteria.Criteria;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.vicky.vspe.features.AdvancementPlus.AdvancementType;
import org.vicky.vspe.features.AdvancementPlus.BukkitAdvancement;
import org.vicky.vspe.systems.BroadcastSystem.ToastType;

import java.util.ArrayList;

public class TestAdvancementChild extends BukkitAdvancement {
    public TestAdvancementChild() {
        super(
                new ItemStack(Material.FILLED_MAP),
                AdvancementFrame.GOAL,
                "This is a simple Text for the advancement System D:",
                "A simple test child",
                new NamespacedKey("vspe", "a_simple_test_child"),
                new ArrayList<>(),
                AdvancementType.PROGRESSION,
                AdvancementVisibility.ALWAYS,
                ToastType.POPUP_TOAST,
                TestAdvancement.class
        );
    }

    @Override
    protected Criteria advancementCriteria() {
        return null;
    }

    @Override
    protected AdvancementReward advancementReward() {
        return null;
    }

    @Override
    protected void performGrantAdvancement(OfflinePlayer player) {
    }
}
