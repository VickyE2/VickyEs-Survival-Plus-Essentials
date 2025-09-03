package org.vicky.vspe.forge.forgeplatform;

import net.minecraft.resources.ResourceLocation;
import org.vicky.platform.PlatformPlayer;
import org.vicky.utilities.Identifiable;
import org.vicky.vspe.forge.advancements.AdvancementHandler;
import org.vicky.vspe.forge.advancements.ForgeAdvancement;
import org.vicky.vspe.platform.features.advancement.AdvancementStorage;
import org.vicky.vspe.platform.features.advancement.PlatformAdvancementManager;
import org.vicky.vspe.platform.utilities.Manager.EntityNotFoundException;
import org.vicky.vspe.platform.utilities.Manager.ManagerRegistry;

import java.util.List;
import java.util.stream.Collectors;

public class ForgeAdvancementManager implements PlatformAdvancementManager<ForgeAdvancement> {

    private static ForgeAdvancementManager INSTANCE;

    private ForgeAdvancementManager() {
        ManagerRegistry.register(this);
    }

    public static ForgeAdvancementManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ForgeAdvancementManager();
        }
        return INSTANCE;
    }

    @Override
    public void processAdvancements() {

    }

    @Override
    public boolean grantAdvancement(Class<? extends ForgeAdvancement> advancementClass, PlatformPlayer player) {
        // Find by class type
        for (ForgeAdvancement adv : getAll()) {
            if (adv.getClass().equals(advancementClass)) {
                adv.grant(player);
                return true;
            }
        }
        return false;
    }

    @Override
    public void grantAdvancement(String advancementId, PlatformPlayer player) {
        ForgeAdvancement adv = getAll().stream()
                .filter(a -> a.getIdentifier().equals(advancementId))
                .findFirst()
                .orElse(null);
        if (adv != null) adv.grant(player);
    }

    @Override
    public void addAdvancement(ForgeAdvancement advancement) {
        AdvancementHandler.register(
                advancement.getHandle()
        );
    }

    @Override
    public ForgeAdvancement getAdvancement(Class<? extends ForgeAdvancement> advancementClass) {
        return getAll().stream()
                .filter(a -> a.getClass().equals(advancementClass))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void setStorage(AdvancementStorage storage) {

    }

    @Override
    public String getManagerId() {
        return "forge_advancements";
    }

    @Override
    public void removeEntity(String namespace) throws EntityNotFoundException {
        ResourceLocation rl = new ResourceLocation(namespace);
        if (!AdvancementHandler.REGISTERED.containsKey(rl)) {
            throw new EntityNotFoundException(namespace);
        }
        AdvancementHandler.unregister(rl);
    }

    @Override
    public void disableEntity(String namespace) throws EntityNotFoundException {
        removeEntity(namespace);
    }

    @Override
    public void enableEntity(String namespace) throws EntityNotFoundException {
        var id = new ResourceLocation(namespace);
        var builder = AdvancementHandler.UNREGISTERED.get(id);
        if (builder == null) throw new EntityNotFoundException(namespace);
        AdvancementHandler.register(builder);
    }

    @Override
    public List<Identifiable> getRegisteredEntities() {
        return AdvancementHandler.REGISTERED.values().stream()
                .map(ForgeAdvancement::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<Identifiable> getUnregisteredEntities() {
        return AdvancementHandler.REGISTERED.values().stream()
                .map(ForgeAdvancement::new)
                .map(Identifiable.class::cast)
                .toList();
    }

    private List<ForgeAdvancement> getAll() {
        return AdvancementHandler.REGISTERED.values().stream()
                .map(ForgeAdvancement::new)
                .collect(Collectors.toList());
    }
}
