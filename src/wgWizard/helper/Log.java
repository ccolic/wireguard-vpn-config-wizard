/*
 * This software is GPL2 licensed, find further license information
 * in the LICENSE file located in the root directory
 *
 * Created on : 22-12-17
 * Authors    : Christian Colic, Marc Werenfels
 *
 */

package wgWizard.helper;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple singleton logging class
 * Logging to console
 */
public class Log {
    private static Logger LOGGER = null;

    public static Logger getInstance() {
        if (LOGGER == null) {
            LOGGER = Logger.getLogger(Log.class.getName());
            LOGGER.setLevel(Level.ALL);
        }

        return LOGGER;
    }
}
