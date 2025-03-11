package org.vicky.vspe.features.CharmsAndTrinkets.gui.CharnsNTrinkets;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.vicky.guiparent.GuiCreator;
import org.vicky.listeners.BaseGuiListener;
import org.vicky.utilities.ContextLogger.ContextLogger;
import org.vicky.vspe.features.CharmsAndTrinkets.BaseTrinket;
import org.vicky.vspe.features.CharmsAndTrinkets.TrinketSlot;
import org.vicky.vspe.features.CharmsAndTrinkets.exceptions.NullDatabaseTrinket;
import org.vicky.vspe.features.CharmsAndTrinkets.exceptions.NullManagerTrinket;
import org.vicky.vspe.features.CharmsAndTrinkets.exceptions.NullTrinketUser;
import org.vicky.vspe.utilities.Hibernate.DBTemplates.AvailableTrinket;
import org.vicky.vspe.utilities.Hibernate.DBTemplates.CnTPlayer;
import org.vicky.vspe.utilities.Hibernate.dao_s.AvailableTrinketDAO;
import org.vicky.vspe.utilities.Hibernate.dao_s.CnTPlayerDAO;
import org.vicky.vspe.utilities.global.Events.TrinketEquippedEvent;
import org.vicky.vspe.utilities.global.Events.TrinketUnEquippedEvent;

import java.util.*;

import static org.vicky.vspe.utilities.global.GlobalResources.trinketManager;

public class PlayerEquippedTrinketScreenListener extends BaseGuiListener {
    private final Map<Player, Map<TrinketSlot, List<EquippedTrinket>>> equippedItems = new HashMap<>();
    private final Map<Player, Map<TrinketSlot, List<EquippedTrinket>>> unEquippedItems = new HashMap<>();


    public PlayerEquippedTrinketScreenListener(JavaPlugin plugin) {
        super(plugin);
        var dao = new AvailableTrinketDAO();
        addInventoryOpenedHandler(event -> {
            this.equippedItems.computeIfPresent((Player) event.getPlayer(), (player, trinketMap) -> {
                trinketMap.clear();
                return trinketMap;
            });
            this.unEquippedItems.computeIfPresent((Player) event.getPlayer(), (player, trinketMap) -> {
                trinketMap.clear();
                return trinketMap;
            });
        });
        addInventoryClickHandler(event -> {
            if (!(event.getWhoClicked() instanceof Player player)) {
                logger.printBukkit("Cancelled not player");
                event.setCancelled(true);
                return;
            }

            if (event.isShiftClick()) {
                logger.printBukkit("Cancelled used shift");
                event.setCancelled(true);
                return;
            }
            boolean isPlacing = event.getAction() == InventoryAction.PLACE_ONE ||
                    event.getAction() == InventoryAction.PLACE_SOME ||
                    event.getAction() == InventoryAction.PLACE_ALL ||
                    event.getAction() == InventoryAction.SWAP_WITH_CURSOR;

            if (isPlacing) {
                // For drop operations, the cursor should not be air and must have NBT data.
                if (event.getCursor().getType().isAir() || !GuiCreator.hasNBTData(event.getCursor(), "vspe_trinket_slot")) {
                    logger.printBukkit("Cancelled drop: invalid cursor item or missing NBT");
                    event.setCancelled(true);
                    return;
                }
            } else {
                // For pick-up operations, the current item must be non-null and have NBT data.
                if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir() ||
                        !GuiCreator.hasNBTData(event.getCurrentItem(), "vspe_trinket_slot")) {
                    logger.printBukkit("Cancelled pick-up: invalid clicked item or missing NBT");
                    event.setCancelled(true);
                    return;
                }
            }

            ItemStack contextItem = isPlacing ? event.getCursor() : event.getCurrentItem();

            TrinketSlot slot = GuiCreator.getNBTData(contextItem, "vspe_trinket_slot", TrinketSlot.class);
            int accSlot = event.getSlot();
            accSlot += 1;

            logger.printBukkit(
                    "TrinketSlot: " + slot +
                    " TrinketSlotNumber: " + Arrays.toString(slot.getSlots()) +
                    " SlotClicked: " + accSlot +
                    " Action: " + event.getAction(),
                    ContextLogger.LogType.BASIC
            );

            switch (slot) {
                case HEAD -> {
                    boolean validSlot = false;
                    for (int y : TrinketSlot.HEAD.getSlots()) {
                        if (accSlot == y) {
                            validSlot = true;
                            break;
                        }
                    }
                    if (!validSlot) {
                        logger.printBukkit("Cancelled not valid slot");
                        event.setCancelled(true);
                        break;
                    }
                    int inventorySlot = event.getSlot();
                    EquippedTrinket equippedTrinket = new EquippedTrinket(contextItem.clone(), inventorySlot);
                    switch (event.getAction()) {
                        case PICKUP_ONE, PICKUP_ALL -> {
                            unEquippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .add(equippedTrinket);
                            equippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .remove(equippedTrinket);
                            event.setCancelled(false);
                        }
                         case PLACE_ONE, PLACE_ALL -> {
                            equippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .add(equippedTrinket);
                            unEquippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .remove(equippedTrinket);
                        }
                        default -> {
                             logger.printBukkit("Cancelled non allowed action type");
                            event.setCancelled(true);
                        }
                    }
                }
                case EARRING -> {
                    boolean validSlot = false;
                    for (int y : TrinketSlot.EARRING.getSlots()) {
                        if (accSlot == y) {
                            validSlot = true;
                            break;
                        }
                    }
                    if (!validSlot) {
                        logger.printBukkit("Cancelled not valid slot");
                        event.setCancelled(true);
                        break;
                    }
                    int inventorySlot = event.getSlot();
                    EquippedTrinket equippedTrinket = new EquippedTrinket(contextItem.clone(), inventorySlot);
                    switch (event.getAction()) {
                        case PICKUP_ONE, PICKUP_ALL -> {
                            unEquippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .add(equippedTrinket);
                            equippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .remove(equippedTrinket);
event.setCancelled(false);
                        }
                         case PLACE_ONE, PLACE_ALL -> {
                            equippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .add(equippedTrinket);
                            unEquippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .remove(equippedTrinket);
                        }
                        default -> {
                             logger.printBukkit("Cancelled non allowed action type");
                            event.setCancelled(true);
                        }
                    }
                }
                case AMULET -> {
                    boolean validSlot = false;
                    for (int y : TrinketSlot.AMULET.getSlots()) {
                        if (accSlot == y) {
                            validSlot = true;
                            break;
                        }
                    }
                    if (!validSlot) {
                        logger.printBukkit("Cancelled not valid slot");
                        event.setCancelled(true);
                        break;
                    }
                    int inventorySlot = event.getSlot();
                    EquippedTrinket equippedTrinket = new EquippedTrinket(contextItem.clone(), inventorySlot);
                    switch (event.getAction()) {
                        case PICKUP_ONE, PICKUP_ALL -> {
                            unEquippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .add(equippedTrinket);
                            equippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .remove(equippedTrinket);
                            event.setCancelled(false);
                        }
                         case PLACE_ONE, PLACE_ALL -> {
                            equippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .add(equippedTrinket);
                            unEquippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .remove(equippedTrinket);
                        }
                        default -> {
                             logger.printBukkit("Cancelled non allowed action type");
                            event.setCancelled(true);
                        }
                    }
                }
                case RING -> {
                    boolean validSlot = false;
                    for (int y : TrinketSlot.RING.getSlots()) {
                        if (accSlot == y) {
                            validSlot = true;
                            break;
                        }
                    }
                    if (!validSlot) {
                        logger.printBukkit("Cancelled not valid slot");
                        event.setCancelled(true);
                        break;
                    }
                    int inventorySlot = event.getSlot();
                    EquippedTrinket equippedTrinket = new EquippedTrinket(contextItem.clone(), inventorySlot);
                    switch (event.getAction()) {
                        case PICKUP_ONE, PICKUP_ALL -> {
                            unEquippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .add(equippedTrinket);
                            equippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .remove(equippedTrinket);
                            event.setCancelled(false);
                        }
                         case PLACE_ONE, PLACE_ALL -> {
                            equippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .add(equippedTrinket);
                            unEquippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .remove(equippedTrinket);
                        }
                        default -> {
                             logger.printBukkit("Cancelled non allowed action type");
                            event.setCancelled(true);
                        }
                    }
                }
                case BRACELET -> {
                    boolean validSlot = false;
                    for (int y : TrinketSlot.BRACELET.getSlots()) {
                        if (accSlot == y) {
                            validSlot = true;
                            break;
                        }
                    }
                    if (!validSlot) {
                        logger.printBukkit("Cancelled not valid slot");
                        event.setCancelled(true);
                        break;
                    }
                    int inventorySlot = event.getSlot();
                    EquippedTrinket equippedTrinket = new EquippedTrinket(contextItem.clone(), inventorySlot);
                    switch (event.getAction()) {
                        case PICKUP_ONE, PICKUP_ALL -> {
                            unEquippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .add(equippedTrinket);
                            equippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .remove(equippedTrinket);
                            event.setCancelled(false);
                        }
                         case PLACE_ONE, PLACE_ALL -> {
                            equippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .add(equippedTrinket);
                            unEquippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .remove(equippedTrinket);
                        }
                        default -> {
                             logger.printBukkit("Cancelled non allowed action type");
                            event.setCancelled(true);
                        }
                    }
                }
                case BELT -> {
                    boolean validSlot = false;
                    for (int y : TrinketSlot.BELT.getSlots()) {
                        if (accSlot == y) {
                            validSlot = true;
                            break;
                        }
                    }
                    if (!validSlot) {
                        logger.printBukkit("Cancelled not valid slot");
                        event.setCancelled(true);
                        break;
                    }
                    int inventorySlot = event.getSlot();
                    EquippedTrinket equippedTrinket = new EquippedTrinket(contextItem.clone(), inventorySlot);
                    switch (event.getAction()) {
                        case PICKUP_ONE, PICKUP_ALL -> {
                            unEquippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .add(equippedTrinket);
                            equippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .remove(equippedTrinket);
                            event.setCancelled(false);
                        }
                         case PLACE_ONE, PLACE_ALL -> {
                            equippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .add(equippedTrinket);
                            unEquippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .remove(equippedTrinket);
                        }
                        default -> {
                             logger.printBukkit("Cancelled non allowed action type");
                            event.setCancelled(true);
                        }
                    }
                }
                case ANKLET -> {
                    boolean validSlot = false;
                    for (int y : TrinketSlot.ANKLET.getSlots()) {
                        if (accSlot == y) {
                            validSlot = true;
                            break;
                        }
                    }
                    if (!validSlot) {
                        logger.printBukkit("Cancelled not valid slot");
                        event.setCancelled(true);
                        break;
                    }
                    int inventorySlot = event.getSlot();
                    EquippedTrinket equippedTrinket = new EquippedTrinket(contextItem.clone(), inventorySlot);
                    switch (event.getAction()) {
                        case PICKUP_ONE, PICKUP_ALL -> {
                            unEquippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .add(equippedTrinket);
                            equippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .remove(equippedTrinket);
                            event.setCancelled(false);
                        }
                         case PLACE_ONE, PLACE_ALL -> {
                            equippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .add(equippedTrinket);
                            unEquippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .remove(equippedTrinket);
                        }
                        default -> {
                             logger.printBukkit("Cancelled non allowed action type");
                            event.setCancelled(true);
                        }
                    }
                }
                case CHARM -> {
                    boolean validSlot = false;
                    for (int y : TrinketSlot.CHARM.getSlots()) {
                        if (accSlot == y) {
                            validSlot = true;
                            break;
                        }
                    }
                    if (!validSlot) {
                        logger.printBukkit("Cancelled not valid slot");
                        event.setCancelled(true);
                        break;
                    }
                    int inventorySlot = event.getSlot();
                    EquippedTrinket equippedTrinket = new EquippedTrinket(contextItem.clone(), inventorySlot);
                    switch (event.getAction()) {
                        case PICKUP_ONE, PICKUP_ALL -> {
                            unEquippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .add(equippedTrinket);
                            equippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .remove(equippedTrinket);
                            event.setCancelled(false);
                        }
                         case PLACE_ONE, PLACE_ALL -> {
                            equippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .add(equippedTrinket);
                            unEquippedItems.computeIfAbsent(player, p -> new HashMap<>())
                                    .computeIfAbsent(slot, s -> new ArrayList<>())
                                    .remove(equippedTrinket);
                        }
                        default -> {
                             logger.printBukkit("Cancelled non allowed action type");
                            event.setCancelled(true);
                        }
                    }
                }
                default -> {
                    logger.printBukkit("Cancelled none slot type");
                    event.setCancelled(true);
                }
            }
        });
        addInventoryCloseHandler(event -> {
            if (!(event.getPlayer() instanceof Player)) return;
            unEquippedItems.forEach((p, slotMap) -> {
                CnTPlayerDAO cntDao = new CnTPlayerDAO();
                Optional<CnTPlayer> optionalCnTPlayer = cntDao.findById(p.getUniqueId());
                if (optionalCnTPlayer.isPresent()) {
                    CnTPlayer dbPlayer = optionalCnTPlayer.get();
                    for (Map.Entry<TrinketSlot, List<EquippedTrinket>> slotItems : slotMap.entrySet()) {
                        for (EquippedTrinket equippedTrinket : slotItems.getValue()) {
                            // Look up the corresponding trinket in the database.
                            Optional<BaseTrinket> trinketOpt = trinketManager.getTrinketById(
                                    GuiCreator.getNBTData(equippedTrinket.getItem(), "vspe_trinket_id", UUID.class).toString());
                            // Call the unequipped event regardless (even if the trinket isn’t found in DB)
                            if (trinketOpt.isEmpty()) {
                                try {
                                    throw new NullManagerTrinket("Trinket was found in database but failed to be found in the manager", equippedTrinket.getItem(), p);
                                }
                                catch (NullManagerTrinket e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            TrinketUnEquippedEvent unequippedEvent =
                                    new TrinketUnEquippedEvent(p, trinketOpt.get());
                            Bukkit.getPluginManager().callEvent(unequippedEvent);

                            // Update dbPlayer: clear the appropriate slot.
                            switch (slotItems.getKey()) {
                                case HEAD -> dbPlayer.setHeadSlot(null);
                                case BRACELET -> {
                                    if (equippedTrinket.getSlot() == 3) {
                                        dbPlayer.setWristSlot1(null);
                                    } else {
                                        dbPlayer.setWristSlot2(null);
                                    }
                                }
                                case RING -> {
                                    if (equippedTrinket.getSlot() == 12) {
                                        dbPlayer.setRingSlot1(null);
                                    } else {
                                        dbPlayer.setRingSlot2(null);
                                    }
                                }
                                case EARRING -> {
                                    if (equippedTrinket.getSlot() == 9) {
                                        dbPlayer.setEarSlot1(null);
                                    } else {
                                        dbPlayer.setEarSlot2(null);
                                    }
                                }
                                case AMULET -> dbPlayer.setNeckTrinket(null);
                                case BELT -> dbPlayer.setBeltSlot(null);
                                case ANKLET -> {
                                    if (equippedTrinket.getSlot() == 12) {
                                        dbPlayer.setAnkletSlot1(null);
                                    } else {
                                        dbPlayer.setAnkletSlot2(null);
                                    }
                                }
                                case CHARM -> {
                                    if (equippedTrinket.getSlot() == 30) {
                                        dbPlayer.setCharmSlot1(null);
                                    } else {
                                        dbPlayer.setCharmSlot2(null);
                                    }
                                }
                                default -> {
                                }
                            }
                        }
                    }
                    cntDao.update(dbPlayer);
                }
                else {
                    try {
                        throw new NullTrinketUser("An error occurred while trying to unequip a player trinket. The player is not in the database...", p);
                    } catch (NullTrinketUser e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            equippedItems.forEach((p, slotMap) -> {
                CnTPlayerDAO cntDao = new CnTPlayerDAO();
                Optional<CnTPlayer> optionalCnTPlayer = cntDao.findById(p.getUniqueId());
                if (optionalCnTPlayer.isPresent()) {
                    CnTPlayer dbPlayer = optionalCnTPlayer.get();
                    for (Map.Entry<TrinketSlot, List<EquippedTrinket>> slotItems : slotMap.entrySet()) {
                        for (EquippedTrinket equippedTrinket : slotItems.getValue()) {
                            Optional<AvailableTrinket> trinketOpt = dao.findById(
                                    GuiCreator.getNBTData(equippedTrinket.getItem(), "vspe_trinket_id", UUID.class).toString());
                            if (trinketOpt.isPresent()) {
                                    // Look up the corresponding trinket in the database.
                                    Optional<BaseTrinket> tOpt = trinketManager.getTrinketById(
                                            GuiCreator.getNBTData(equippedTrinket.getItem(), "vspe_trinket_id", UUID.class).toString());
                                    // Call the unequipped event regardless (even if the trinket isn’t found in DB)
                                    if (tOpt.isEmpty()) {
                                        try {
                                            throw new NullManagerTrinket("Trinket was found in database but failed to be found in the manager", equippedTrinket.getItem(), p);
                                        } catch (NullManagerTrinket e) {
                                            throw new RuntimeException(e);
                                        }
                                    }// Call the equipped event before updating the DB.
                                    TrinketEquippedEvent equippedEvent =
                                            new TrinketEquippedEvent(p, tOpt.get());
                                    Bukkit.getPluginManager().callEvent(equippedEvent);

                                    switch (slotItems.getKey()) {
                                        case HEAD -> dbPlayer.setHeadSlot(trinketOpt.get());
                                        case BRACELET -> {
                                            if (equippedTrinket.getSlot() == 3) {
                                                dbPlayer.setWristSlot1(trinketOpt.get());
                                            } else {
                                                dbPlayer.setWristSlot2(trinketOpt.get());
                                            }
                                        }
                                        case RING -> {
                                            if (equippedTrinket.getSlot() == 12) {
                                                dbPlayer.setRingSlot1(trinketOpt.get());
                                            } else {
                                                dbPlayer.setRingSlot2(trinketOpt.get());
                                            }
                                        }
                                        case EARRING -> {
                                            if (equippedTrinket.getSlot() == 9) {
                                                dbPlayer.setEarSlot1(trinketOpt.get());
                                            } else {
                                                dbPlayer.setEarSlot2(trinketOpt.get());
                                            }
                                        }
                                        case AMULET -> dbPlayer.setNeckTrinket(trinketOpt.get());
                                        case BELT -> dbPlayer.setBeltSlot(trinketOpt.get());
                                        case ANKLET -> {
                                            if (equippedTrinket.getSlot() == 12) {
                                                dbPlayer.setAnkletSlot1(trinketOpt.get());
                                            } else {
                                                dbPlayer.setAnkletSlot2(trinketOpt.get());
                                            }
                                        }
                                        case CHARM -> {
                                            if (equippedTrinket.getSlot() == 30) {
                                                dbPlayer.setCharmSlot1(trinketOpt.get());
                                            } else {
                                                dbPlayer.setCharmSlot2(trinketOpt.get());
                                            }
                                        }
                                        default -> {
                                        }
                                    }
                                }
                            else {
                                    try {
                                        throw new NullDatabaseTrinket("A trinket from itemStack was passed but was not found in database.", equippedTrinket.getItem(), p);
                                    } catch (NullDatabaseTrinket e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                        }
                    }
                    cntDao.update(dbPlayer);
                }
                else {
                    try {
                        throw new NullTrinketUser("An error occurred while trying to equip a player trinket. The player seems to not be in the database...", p);
                    } catch (NullTrinketUser e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        });
    }
}
