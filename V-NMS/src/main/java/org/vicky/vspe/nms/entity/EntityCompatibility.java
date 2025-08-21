package org.vicky.vspe.nms.entity;

import org.vicky.vspe.nms.equipevent.TriIntConsumer;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vicky.vspe.nms.HitBox;
import org.vicky.vspe.nms.utils.MinecraftVersions;

import java.util.List;

public interface EntityCompatibility {

    EntityType ITEM_ENTITY = MinecraftVersions.TRAILS_AND_TAILS.get(5).isAtLeast() ? EntityType.DROPPED_ITEM : EntityType.valueOf("DROPPED_ITEM");

    /**
     * Null in cases where entity is invulnerable or dead.
     *
     * @param entity the entity
     * @return the living entity's hit box or null
     */
    default HitBox getHitBox(Entity entity) {
        if (entity.isInvulnerable() || !entity.getType().isAlive() || entity.isDead())
            return null;

        // This default should only be used after 1.13 R2

        HitBox hitBox = new HitBox(entity.getLocation().toVector(), getLastLocation(entity))
                .grow(entity.getWidth(), entity.getHeight());
        hitBox.setLivingEntity((LivingEntity) entity);

        if (entity instanceof ComplexLivingEntity) {
            for (ComplexEntityPart entityPart : ((ComplexLivingEntity) entity).getParts()) {
                BoundingBox boxPart = entityPart.getBoundingBox();
                hitBox.addVoxelShapePart(new HitBox(boxPart.getMinX(), boxPart.getMinY(), boxPart.getMinZ(), boxPart.getMaxX(), boxPart.getMaxY(), boxPart.getMaxZ()));
            }
        }

        return hitBox;
    }

    /**
     * @param entity the entity whose last location to get
     * @return the vector of entity's last location
     */
    Vector getLastLocation(Entity entity);

    /**
     * Returns the amount of absorption hearts that the entity currently has.
     *
     * @param entity The non-null bukkit entity who has absorption hearts.
     * @return The amount of absorption hearts.
     */
    default double getAbsorption(@NotNull LivingEntity entity) {
        return entity.getAbsorptionAmount();
    }

    /**
     * Sets the amount of absorption hearts for a given <code>entity</code>.
     *
     * @param entity     The non-null bukkit entity to set the hearts of.
     * @param absorption The amount of absorption hearts.
     */
    default void setAbsorption(@NotNull LivingEntity entity, double absorption) {
        entity.setAbsorptionAmount(absorption);
    }

    /**
     * Generates an NMS non-null-list (used by the player's inventory). This is used internally for the
     * {@link me.deecaad.core.events.EntityEquipmentEvent}, and should probably not be used by anything
     * else.
     *
     * @param size     The size of the list.
     * @param consumer The action to execute every item add.
     * @return The fixed size list.
     */
    List<Object> generateNonNullList(int size, TriIntConsumer<ItemStack, ItemStack> consumer);

    /**
     * Generates a {@link FakeEntity} with the given entity type as a disguise.
     *
     * @param location The non-null starting location of the entity.
     * @param type     The non-null type of the entity.
     * @param data     The nullable extra data for item/fallingblock/armorstand.
     * @return The fake entity.
     */
    FakeEntity generateFakeEntity(Location location, EntityType type, Object data);

    /**
     * Shorthand for {@link #generateFakeEntity(Location, EntityType, Object)}. Generates a
     * {@link org.bukkit.entity.Item}.
     *
     * @param location The non-null starting location of the entity.
     * @param item     The non-null item to show.
     * @return The fake entity.
     */
    default FakeEntity generateFakeEntity(Location location, ItemStack item) {
        return generateFakeEntity(location, ITEM_ENTITY, item);
    }

    /**
     * Shorthand for {@link #generateFakeEntity(Location, EntityType, Object)}. Generates a
     * {@link org.bukkit.entity.FallingBlock}.
     *
     * @param location The non-null starting location of the entity.
     * @param block    The non-null block state to show.
     * @return The fake entity.
     */
    default FakeEntity generateFakeEntity(Location location, BlockState block) {
        return generateFakeEntity(location, EntityType.FALLING_BLOCK, block);
    }

    /**
     * Attempts to use a totem of undying on the entity. This will return false if the entity does not
     * have a totem of undying, or if the {@link EntityResurrectEvent} is cancelled. This method will
     * return true if the totem affect is used.
     *
     * @param entity The non-null entity to use the totem on.
     * @return true if the totem was used.
     */
    default boolean tryUseTotemOfUndying(@NotNull LivingEntity entity) {

        // Check if the entity has a totem of undying.
        EntityEquipment equipment = entity.getEquipment();
        ItemStack mainHand = equipment == null ? null : equipment.getItemInMainHand();
        ItemStack offHand = equipment == null ? null : equipment.getItemInOffHand();
        EquipmentSlot hand = null;
        if (mainHand != null && mainHand.getType() == Material.TOTEM_OF_UNDYING) {
            hand = EquipmentSlot.HAND;
        } else if (offHand != null && offHand.getType() == Material.TOTEM_OF_UNDYING) {
            hand = EquipmentSlot.OFF_HAND;
        }

        // This is how Spigot handles resurrection. They always call the event,
        // and cancel the event if there is no totem.
        ItemStack totem = hand == null ? null : (hand == EquipmentSlot.HAND ? mainHand : offHand);
        EntityResurrectEvent event = MinecraftVersions.WILD_UPDATE.isAtLeast() ? new EntityResurrectEvent(entity, hand) : new EntityResurrectEvent(entity);
        event.setCancelled(hand == null);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled())
            return false;

        // 'totem' can still be null here, since plugins may modify the cancellation state/inventory
        if (totem != null && totem.getType() == Material.TOTEM_OF_UNDYING) {
            totem.setAmount(totem.getAmount() - 1);
        }

        // Attempt to award stats
        if (entity instanceof Player player) {
            player.incrementStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING);
            // TODO convert this to Bukkit Code
            // CriteriaTriggers.USED_TOTEM.trigger(entityplayer, itemstack);
        }

        // Not quite ideal, as this fires 1 event PER potion effect, but otherwise 1to1 copy from vanilla
        // code
        entity.setHealth(1.0D);
        for (PotionEffect potion : entity.getActivePotionEffects())
            entity.removePotionEffect(potion.getType());
        entity.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 900, 1));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 800, 0));
        entity.playEffect(EntityEffect.TOTEM_RESURRECT);
        return true;
    }

    /**
     * Gets the id of the entity from the metadata packet. This can be used to get the bukkit
     * {@link Entity} using
     * {@link me.deecaad.core.compatibility.ICompatibility#getEntityById(World, int)}.
     *
     * @param obj The entity metadata packet.
     * @return The entity's id.
     */
    int getId(Object obj);

    /**
     * Uses a packet to spawn a fake item in the player's inventory. If the player is in
     * {@link org.bukkit.GameMode#CREATIVE}, then they will be able to grab the item, regardless. This
     * issue isn't present with standard players.
     *
     * @param player The non-null player to see the change
     * @param slot   The slot number to set.
     * @param item   Which item to replace, or null.
     */
    void setSlot(Player player, EquipmentSlot slot, @Nullable ItemStack item);

    /**
     * Creates a metadata packet for the entity, force updating all metadata. This can be modified by
     * {@link #modifyMetaPacket(Object, EntityMeta, boolean)} to make the entity invisible, glow, etc.
     *
     * @param entity The non-null entity.
     * @return The non-null packet.
     */
    Object generateMetaPacket(Entity entity);

    /**
     * Sets the given entity metadata flag to true/false.
     *
     * @param obj     The metadata packet from {@link #generateMetaPacket(Entity)}/
     * @param meta    The meta flag you want to change.
     * @param enabled true/false.
     */
    void modifyMetaPacket(Object obj, EntityMeta meta, boolean enabled);

    /**
     * This enum outlines the different flags and their byte location for
     * <a href="https://wiki.vg/Entity_metadata#Entity">EntityMetaData</a>.
     */
    enum EntityMeta {

        FIRE(0), // If the entity is on fire
        SNEAKING(1), // If the entity is sneaking
        UNUSED(2), // If the entity is mounted (no longer used in recent versions)
        SPRINTING(3), // If the entity is running
        SWIMMING(4), // If the entity is swimming
        INVISIBLE(5), // If the entity is invisible
        GLOWING(6), // If the entity is glowing
        GLIDING(7); // If the entity is gliding using an elytra

        private final byte mask;

        EntityMeta(int location) {
            this.mask = (byte) (1 << location);
        }

        public byte getMask() {
            return mask;
        }

        public byte set(byte data, boolean is) {
            return (byte) (is ? data | mask : data & ~(mask));
        }
    }
}
