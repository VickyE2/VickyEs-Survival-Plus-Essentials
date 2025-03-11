package org.vicky.vspe.features.CharmsAndTrinkets.Trinkets;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.vicky.guiparent.GuiCreator;
import org.vicky.vspe.VSPE;
import org.vicky.vspe.features.CharmsAndTrinkets.BaseTrinket;
import org.vicky.vspe.features.CharmsAndTrinkets.TrinketAbilityType;
import org.vicky.vspe.features.CharmsAndTrinkets.TrinketSlot;

import java.util.List;

public class RingOfAcceleration extends BaseTrinket {
    public RingOfAcceleration() {
        super(
                "This is a ring that grants the user procedural acceleration at cost of hunger",
                TrinketSlot.RING,
                List.of(TrinketAbilityType.MOVEMENT_SPEED),
                GuiCreator.ItemConfigFactory.fromComponents(
                        null,
                        "RING OF ACCELERATION",
                        "",
                        true,
                        null,
                        "vspe_trinkets:ring_o_acceleration",
                        List.of(
                                Component.text("An ancient ring thought to have been from the world of ", NamedTextColor.GOLD).append(Component.text("hedgehogs,", NamedTextColor.DARK_BLUE, TextDecoration.ITALIC, TextDecoration.BOLD)),
                                Component.text("It grants the user immense acceleration gain at the cost of hunger.", NamedTextColor.GOLD)
                        ),
                        null
                )
        );
        setTrinketListener(new RingOAccelerationListener(this));
    }

    /**
     * Executes the specific trinket ability logic.
     *
     * @param activeUser the player using the trinket.
     */
    @Override
    protected void performTrinketAbility(Player activeUser) {

    }

    /**
     * Removes the trinket ability from the player.
     *
     * @param disabler the player disabling the trinket.
     */
    @Override
    protected void removeTrinketAbility(Player disabler) {

    }

    /**
     * Shared listener for RingOfAcceleration that processes events for all active players.
     */
    public static class RingOAccelerationListener extends TrinketEvent {

        public RingOAccelerationListener(BaseTrinket trinket) {
            super(trinket);
        }

        @EventHandler
        public void onPlayerMove(PlayerMoveEvent event) {
            Player player = event.getPlayer();
            if (!trinket.isPlayerUsing(player)) return;

            if (!player.isSprinting() && !player.isSwimming()) {
                player.setWalkSpeed(0.2f);
                player.setFlySpeed(0.2f);
                return;
            }

            if (player.getFoodLevel() < 5 && player.getGameMode() != GameMode.CREATIVE) {
                player.setWalkSpeed(0.2f);
                player.setFlySpeed(0.2f);
                return;
            }

            float totalDistance = (float) event.getFrom().distance(event.getTo()) / 10;
            float k = 0.3f;
            float speedIncrease = 5f * (1 - (float) Math.exp(-k * totalDistance));
            float newSpeed = Math.min(player.getWalkSpeed() + speedIncrease, 1.0f); // Cap speed at 1.0
            player.setWalkSpeed(newSpeed);
            player.setFlySpeed(newSpeed);

            // Reduce food level
            float reductionFactor = 0.0000000025f * speedIncrease;
            int newFoodLevel = (int) Math.max(0, player.getFoodLevel() - (player.getFoodLevel() * reductionFactor));
            player.setFoodLevel(newFoodLevel);

            // Apply Jump Boost
            int jumpBoostLevel = (int) ((newSpeed - 0.2f) * 10); // Maps 0.2f speed -> 0 boost, 1.0f speed -> level 8 boost
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20, jumpBoostLevel, true, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 80, 1, true, false));

            // Calculate color transition (Red -> #FF00AA)
            float progress = (newSpeed - 0.2f) / 0.8f; // Normalize speed between 0.2 and 1.0
            Color particleColor = blendColors(Color.fromRGB(255, 0, 0), Color.fromRGB(255, 0, 170), progress);
            Particle.DustOptions particleOptions = new Particle.DustOptions(particleColor, 1);

            Location base = player.getLocation();
            World world = player.getWorld();

            // Spawn particles from bottom to top with color transition
            for (double y = 0; y <= 2; y += 0.5) {
                Location particleLoc = base.clone().add(0, y, 0);
                if (y >= 0.5 && y <= 1.5)
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 5, 0.1, 0.1, 0.1);
                else
                    world.spawnParticle(Particle.REDSTONE, particleLoc, 5, 0.1, 0.1, 0.1, 0, particleOptions);
            }

            // Red-Purple trail behind player (2 blocks behind)
            Location trailBase = base.clone().add(base.getDirection().multiply(-2));
            world.spawnParticle(Particle.REDSTONE, trailBase, 5, 0.2, 0.2, 0.2, 0, particleOptions);
        }

        /**
         * Blends two colors based on the provided ratio.
         *
         * @param c1    The start Color.
         * @param c2    The end Color.
         * @param ratio A value between 0.0 (c1) and 1.0 (c2).
         * @return The blended Color.
         */
        private Color blendColors(Color c1, Color c2, float ratio) {
            int red = (int) (c1.getRed() * (1 - ratio) + c2.getRed() * ratio);
            int green = (int) (c1.getGreen() * (1 - ratio) + c2.getGreen() * ratio);
            int blue = (int) (c1.getBlue() * (1 - ratio) + c2.getBlue() * ratio);
            return Color.fromRGB(red, green, blue);
        }
    }
}
