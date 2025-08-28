package org.vicky.vspe.ecosystem;

import org.vicky.ecosystem.plugin.Communicateable;
import org.vicky.utilities.ContextLogger.ContextLogger;

public class VSPECommunicateableImpl extends Communicateable {
    @Override
    protected void onRegister() {
        getLogger().print("VSPE IS COMING INTO YOUR ECO-SYSTEM", ContextLogger.LogType.AMBIENCE);
    }
}
