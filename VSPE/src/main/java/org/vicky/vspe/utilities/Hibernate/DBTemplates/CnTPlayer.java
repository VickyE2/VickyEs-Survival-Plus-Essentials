package org.vicky.vspe.utilities.Hibernate.DBTemplates;

import jakarta.persistence.*;
import org.vicky.utilities.DatabaseManager.templates.ExtendedPlayerBase;
import org.vicky.utilities.DatabaseTemplate;
import org.vicky.vspe.features.CharmsAndTrinkets.gui.CharnsNTrinkets.EquippedRawTrinket;
import org.vicky.vspe.features.CharmsAndTrinkets.gui.CharnsNTrinkets.EquippedTrinket;
import org.vicky.vspe.platform.features.CharmsAndTrinkets.exceptions.NullManagerTrinket;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@DiscriminatorValue("CnTPlayer")
public class CnTPlayer extends ExtendedPlayerBase implements DatabaseTemplate {

    @JoinColumn(name = "neck_slot")
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private AvailableTrinket neckTrinket;

    // Ring Slots (Finger; these might also allow a “dual ring” occupying both)
    @JoinColumn(name = "ring_slot_1")
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private AvailableTrinket ringSlot1;

    @JoinColumn(name = "ring_slot_2")
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private AvailableTrinket ringSlot2;

    // Wrist Slots (Bracelets)
    @JoinColumn(name = "wrist_slot_1")
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private AvailableTrinket wristSlot1;

    @JoinColumn(name = "wrist_slot_2")
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private AvailableTrinket wristSlot2;

    // Ear Slots (Earrings)
    @JoinColumn(name = "ear_slot_1")
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private AvailableTrinket earSlot1;

    @JoinColumn(name = "ear_slot_2")
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private AvailableTrinket earSlot2;

    // Charm Slots (These could be multi-slot items that occupy more than one position)
    @JoinColumn(name = "charm_slot_1")
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private AvailableTrinket charmSlot1;

    @JoinColumn(name = "charm_slot_2")
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private AvailableTrinket charmSlot2;

    @JoinColumn(name = "anklet_slot_1")
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private AvailableTrinket ankletSlot1;
    @JoinColumn(name = "anklet_slot_2")
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private AvailableTrinket ankletSlot2;

    @JoinColumn(name = "belt_slot")
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private AvailableTrinket beltSlot;

    @JoinColumn(name = "head_slot")
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private AvailableTrinket headSlot;

    public CnTPlayer() {
    }

    public AvailableTrinket getCharmSlot1() {
        return charmSlot1;
    }

    public AvailableTrinket getCharmSlot2() {
        return charmSlot2;
    }

    public AvailableTrinket getEarSlot1() {
        return earSlot1;
    }

    public AvailableTrinket getNeckTrinket() {
        return neckTrinket;
    }

    public AvailableTrinket getEarSlot2() {
        return earSlot2;
    }

    public AvailableTrinket getRingSlot1() {
        return ringSlot1;
    }

    public AvailableTrinket getRingSlot2() {
        return ringSlot2;
    }

    public AvailableTrinket getWristSlot1() {
        return wristSlot1;
    }

    public AvailableTrinket getWristSlot2() {
        return wristSlot2;
    }

    public AvailableTrinket getAnkletSlot1() {
        return ankletSlot1;
    }

    public AvailableTrinket getAnkletSlot2() {
        return ankletSlot2;
    }

    public AvailableTrinket getBeltSlot() {
        return beltSlot;
    }

    public AvailableTrinket getHeadSlot() {
        return headSlot;
    }

    public void setNeckTrinket(AvailableTrinket neckTrinket) {
        this.neckTrinket = neckTrinket;
    }

    public void setRingSlot1(AvailableTrinket ringSlot1) {
        this.ringSlot1 = ringSlot1;
    }

    public void setRingSlot2(AvailableTrinket ringSlot2) {
        this.ringSlot2 = ringSlot2;
    }

    public void setWristSlot1(AvailableTrinket wristSlot1) {
        this.wristSlot1 = wristSlot1;
    }

    public void setWristSlot2(AvailableTrinket wristSlot2) {
        this.wristSlot2 = wristSlot2;
    }

    public void setEarSlot1(AvailableTrinket earSlot1) {
        this.earSlot1 = earSlot1;
    }

    public void setEarSlot2(AvailableTrinket earSlot2) {
        this.earSlot2 = earSlot2;
    }

    public void setCharmSlot1(AvailableTrinket charmSlot1) {
        this.charmSlot1 = charmSlot1;
    }

    public void setCharmSlot2(AvailableTrinket charmSlot2) {
        this.charmSlot2 = charmSlot2;
    }

    public void setAnkletSlot1(AvailableTrinket ankletSlot1) {
        this.ankletSlot1 = ankletSlot1;
    }

    public void setAnkletSlot2(AvailableTrinket ankletSlot2) {
        this.ankletSlot2 = ankletSlot2;
    }

    public void setBeltSlot(AvailableTrinket beltSlot) {
        this.beltSlot = beltSlot;
    }

    public void setHeadSlot(AvailableTrinket headSlot) {
        this.headSlot = headSlot;
    }

    @Transient
    public List<EquippedTrinket> getWornTrinkets() throws NullManagerTrinket {
        List<EquippedTrinket> trinkets = new ArrayList<>();

        if (getHeadSlot() != null) {
            trinkets.add(new EquippedTrinket(headSlot.getItem(), 1));
        }
        if (getEarSlot1() != null) {
            trinkets.add(new EquippedTrinket(earSlot1.getItem(), 9));
        }
        if (getEarSlot2() != null) {
            trinkets.add(new EquippedTrinket(earSlot2.getItem(), 11));
        }
        if (getWristSlot1() != null) {
            trinkets.add(new EquippedTrinket(wristSlot1.getItem(), 3));
        }
        if (getWristSlot2() != null) {
            trinkets.add(new EquippedTrinket(wristSlot2.getItem(), 5));
        }
        if (getRingSlot1() != null) {
            trinkets.add(new EquippedTrinket(ringSlot1.getItem(), 12));
        }
        if (getRingSlot2() != null) {
            trinkets.add(new EquippedTrinket(ringSlot2.getItem(), 14));
        }
        if (getBeltSlot() != null) {
            trinkets.add(new EquippedTrinket(beltSlot.getItem(), 7));
        }
        if (getAnkletSlot1() != null) {
            trinkets.add(new EquippedTrinket(ankletSlot1.getItem(), 12));
        }
        if (getAnkletSlot2() != null) {
            trinkets.add(new EquippedTrinket(ankletSlot2.getItem(), 14));
        }
        if (getNeckTrinket() != null) {
            trinkets.add(new EquippedTrinket(neckTrinket.getItem(), 22));
        }
        if (getCharmSlot1() != null) {
            trinkets.add(new EquippedTrinket(charmSlot1.getItem(), 30));
        }
        if (getCharmSlot2() != null) {
            trinkets.add(new EquippedTrinket(charmSlot2.getItem(), 32));
        }

        return trinkets;
    }

    @Transient
    public List<UUID> getTrinketUUIDs() throws NullManagerTrinket {
        List<UUID> trinkets = new ArrayList<>();

        if (getHeadSlot() != null) {
            trinkets.add(headSlot.getIdAsUUID());
        }
        if (getEarSlot1() != null) {
            trinkets.add(earSlot1.getIdAsUUID());
        }
        if (getEarSlot2() != null) {
            trinkets.add(earSlot2.getIdAsUUID());
        }
        if (getWristSlot1() != null) {
            trinkets.add(wristSlot1.getIdAsUUID());
        }
        if (getWristSlot2() != null) {
            trinkets.add(wristSlot2.getIdAsUUID());
        }
        if (getRingSlot1() != null) {
            trinkets.add(ringSlot1.getIdAsUUID());
        }
        if (getRingSlot2() != null) {
            trinkets.add(ringSlot2.getIdAsUUID());
        }
        if (getBeltSlot() != null) {
            trinkets.add(beltSlot.getIdAsUUID());
        }
        if (getAnkletSlot1() != null) {
            trinkets.add(ankletSlot1.getIdAsUUID());
        }
        if (getAnkletSlot2() != null) {
            trinkets.add(ankletSlot2.getIdAsUUID());
        }
        if (getNeckTrinket() != null) {
            trinkets.add(neckTrinket.getIdAsUUID());
        }
        if (getCharmSlot1() != null) {
            trinkets.add(charmSlot1.getIdAsUUID());
        }
        if (getCharmSlot2() != null) {
            trinkets.add(charmSlot2.getIdAsUUID());
        }

        return trinkets;
    }

    @Transient
    public List<EquippedRawTrinket> getRawWornTrinkets() throws NullManagerTrinket {
        List<EquippedRawTrinket> trinkets = new ArrayList<>();

        if (getHeadSlot() != null) {
            trinkets.add(new EquippedRawTrinket(headSlot.getRawItem(), 1));
        }
        if (getEarSlot1() != null) {
            trinkets.add(new EquippedRawTrinket(earSlot1.getRawItem(), 9));
        }
        if (getEarSlot2() != null) {
            trinkets.add(new EquippedRawTrinket(earSlot2.getRawItem(), 11));
        }
        if (getWristSlot1() != null) {
            trinkets.add(new EquippedRawTrinket(wristSlot1.getRawItem(), 3));
        }
        if (getWristSlot2() != null) {
            trinkets.add(new EquippedRawTrinket(wristSlot2.getRawItem(), 5));
        }
        if (getRingSlot1() != null) {
            trinkets.add(new EquippedRawTrinket(ringSlot1.getRawItem(), 12));
        }
        if (getRingSlot2() != null) {
            trinkets.add(new EquippedRawTrinket(ringSlot2.getRawItem(), 14));
        }
        if (getBeltSlot() != null) {
            trinkets.add(new EquippedRawTrinket(beltSlot.getRawItem(), 7));
        }
        if (getAnkletSlot1() != null) {
            trinkets.add(new EquippedRawTrinket(ankletSlot1.getRawItem(), 12));
        }
        if (getAnkletSlot2() != null) {
            trinkets.add(new EquippedRawTrinket(ankletSlot2.getRawItem(), 14));
        }
        if (getNeckTrinket() != null) {
            trinkets.add(new EquippedRawTrinket(neckTrinket.getRawItem(), 22));
        }
        if (getCharmSlot1() != null) {
            trinkets.add(new EquippedRawTrinket(charmSlot1.getRawItem(), 30));
        }
        if (getCharmSlot2() != null) {
            trinkets.add(new EquippedRawTrinket(charmSlot2.getRawItem(), 32));
        }

        return trinkets;
    }
}
