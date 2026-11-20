package com.craisinlord.integrated_superheroes.compat.thematic;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;

final class ThematicLogging {
    private static boolean listening;

    private ThematicLogging() {
    }

    static void apply() {
        if (!ThematicOptions.on(ThematicOptions.QUIET_DEBUG_LOG)) {
            return;
        }
        Configurator.setLevel("thematic", Level.INFO);
        Configurator.setLevel("Thematic", Level.INFO);
        if (!listening && LogManager.getContext(false) instanceof LoggerContext context) {
            listening = true;
            context.addPropertyChangeListener(event -> {
                if (LoggerContext.PROPERTY_CONFIG.equals(event.getPropertyName())) {
                    Configurator.setLevel("thematic", Level.INFO);
                    Configurator.setLevel("Thematic", Level.INFO);
                }
            });
        }
    }
}
