package org.vicky.vspe.features.AdvancementPlus.Advancements;

import eu.endercentral.crazy_advancements.advancement.AdvancementDisplay.AdvancementFrame;
import eu.endercentral.crazy_advancements.advancement.AdvancementFlag;
import eu.endercentral.crazy_advancements.advancement.AdvancementReward;
import eu.endercentral.crazy_advancements.advancement.AdvancementVisibility;
import eu.endercentral.crazy_advancements.advancement.criteria.Criteria;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.vicky.vspe.features.AdvancementPlus.AdvancementType;
import org.vicky.vspe.features.AdvancementPlus.BaseAdvancement;
import org.vicky.vspe.systems.BroadcastSystem.ToastType;

import java.util.ArrayList;

public class DimensionParentAdvancement extends BaseAdvancement {
    public DimensionParentAdvancement() {
        super(
                new ItemStack(Material.PAPER),
                new NamespacedKey("vspe", "dimension_parent"),
                AdvancementFrame.CHALLENGE,
                "Dimension Parent Advancement",
                "A Hidden Base",
                new ArrayList<>(),
                AdvancementType.CHALLENGE,
                AdvancementVisibility.HIDDEN,
                ToastType.POPUP_TOAST,
                ArrangementType.CIRCULAR
        );
        addOverride("advancement_flag", new AllowedAdvancementFlag<>(AdvancementFlag.SEND_WITH_HIDDEN_BOOLEAN));
    }

    @Override
    protected Criteria advancementCriteria() {
        return new Criteria(new String[]{"trigger: minecraft:shot_crossbow"}, new String[][]{{"minecraft:crossbow"}});
    }

    @Override
    protected AdvancementReward advancementReward() {
        return new AdvancementReward() {
            public void onGrant(Player player) {
                player.getWorld().spawnEntity(player.getLocation().add(1.0, 0.0, 1.0), EntityType.FIREWORK);
                player.getWorld().spawnEntity(player.getLocation().add(-1.0, 0.0, 1.0), EntityType.FIREWORK);
                player.getWorld().spawnEntity(player.getLocation().add(-1.0, 0.0, -1.0), EntityType.FIREWORK);
                player.getWorld().spawnEntity(player.getLocation().add(1.0, 0.0, -1.0), EntityType.FIREWORK);
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 20, 1));
            }
        };
    }

    @Override
    protected void performGrantAdvancement(OfflinePlayer player) {
    }
}
