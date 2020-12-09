/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.kb.lookup;

import dk.kb.lookup.api.MergedApi;
import dk.kb.lookup.config.ServiceConfig;
import dk.kb.util.yaml.YAML;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.Locale;

/**
 * Responsible for starting scans at scheduled times.
 */
public class ScanControl implements Runnable {
    private static Log log = LogFactory.getLog(ScanControl.class);

    private static ScanControl instance = null;
    private String rootPattern;
    private long scanIntervalMS;
    private long nextScan;
    private boolean active = true;

    public static void initControl() {
        String rootPattern = ServiceConfig.getConfig().getString(".lookup.autoscan.scanRootPattern", ".*");
        if (ServiceConfig.getConfig().getBoolean(".lookup.autoscan.scanOnStartup", true)) {
            log.info("Performing initial scan as .lookup.autoscan.scanOnStartup is true");
            startFullScan(rootPattern);
        }
        long scanInterval = ServiceConfig.getConfig().getLong(".lookup.autoscan.scanIntervalSeconds");
        if (scanInterval == -1) {
            log.info("No automatic rescanning as .lookup.autoscan.scanIntervalSeconds == -1");
        } else {
            log.info("Starting background ScanControl with scanIntervalSeconds=" + scanInterval);
            instance = new ScanControl(scanInterval, rootPattern);
            Thread controlThread = new Thread(instance, "ScanControl_thread");
            controlThread.setDaemon(true);
            controlThread.start();
        }
    }

    public ScanControl(long scanIntervalSeconds, String rootPattern) {
        this.scanIntervalMS = scanIntervalSeconds*1000;
        this.rootPattern = rootPattern;
        nextScan = System.currentTimeMillis() + scanIntervalMS;
    }

    /**
     * @return the ScanControl running in the background, if recurrent scanning has been specified, else null.
     */
    public static ScanControl getInstance() {
        return instance;
    }

    /**
     * Stop the running ScanControl. The ScanControl will finish a running scan, but will not start new ones.
     */
    public void stop() {
        active = false;
        this.notifyAll();
    }

    /**
     * Set the scan interval. Values < 1 means continuous scanning.
     * @param scanIntervalSeconds the number of seconds between scan initation.
     */
    public void setScanInterval(long scanIntervalSeconds) {
        scanIntervalMS = scanIntervalSeconds*1000;
        nextScan = System.currentTimeMillis() + scanIntervalMS;
        this.notifyAll();
    }

    /**
     * @return the number of seconds between scan initation.
     */
    public long getScanIntervalSeconds() {
        return scanIntervalMS/1000;
    }

    @SuppressWarnings("BusyWait")
    @Override
    public void run() {
        log.info(String.format(Locale.ENGLISH, "Recurrent scanner (scan interval %d seconds) activated with root pattern '%s'",
                               scanIntervalMS / 1000, rootPattern));
        while (active) {
            if (System.currentTimeMillis() < nextScan) {
                try {
                    Thread.sleep(nextScan - System.currentTimeMillis());
                } catch (InterruptedException e) {
                    log.debug("Interrupted from sleep. Re-evaluating conditions");
                }
                continue;
            }
            nextScan = System.currentTimeMillis() + scanIntervalMS;
            startFullScan(rootPattern);
        }
    }

    private static void startFullScan(String rootPattern) {
        ServiceConfig.getControl().startScan(rootPattern);
        log.info("Initiating scan with rootPattern='" + rootPattern + "'");
    }

}
