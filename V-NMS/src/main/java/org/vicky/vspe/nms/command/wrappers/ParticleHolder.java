package org.vicky.vspe.nms.command.wrappers;

import org.bukkit.Particle;

public record ParticleHolder(Particle particle, Object nmsParticle, String asString) {
}
