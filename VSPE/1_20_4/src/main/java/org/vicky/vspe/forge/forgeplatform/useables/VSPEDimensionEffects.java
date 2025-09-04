package org.vicky.vspe.forge.forgeplatform.useables;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import org.vicky.vspe.platform.systems.dimension.DimensionDescriptor;

import java.util.HashMap;
import java.util.Map;

public class VSPEDimensionEffects {
    private static final Map<ResourceLocation, ForgeDescriptorDispensableDimensionEffects> effects
            = new HashMap<>();

    public static void registerAll(RegisterDimensionSpecialEffectsEvent bootstrap) {
        effects.forEach(bootstrap::register);
    }

    public static ResourceLocation registerSpecialEffect(DimensionDescriptor descriptor) {
        var loc = ResourceLocation.parse(descriptor.identifier());
        effects.put(loc, new ForgeDescriptorDispensableDimensionEffects(descriptor));
        return loc;
    }
}
