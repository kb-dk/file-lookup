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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Responsible for scanning the file system for new or deleted files.
 */
public class ScanBot {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public enum STATE {scanning, ready}

    private STATE state = STATE.ready;
    private static ScanBot instance;
    private List<String> activeRoots = null;
    private String activePath = null;
    private final Executor executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread t = new Thread(runnable, "ScanBot");
        t.setDaemon(true);
        return t;
    });

    public static ScanBot instance() {
        if (instance == null) {
            instance = new ScanBot();
        }
        return instance;
    }

    public ScanBot() {
        log.info("Creating instance");
    }

    /**
     * Starts a background scan from the given roots, feeding the given consumer with the encountered folders.
     * This call will return immediately.
     * @param roots where to scan from.
     * @param consumer handles callbacks.
     * @return true if the scan was started.
     */
    public synchronized boolean startScan(List<String> roots, Consumer<Folder> consumer) {
        if (state == STATE.scanning) {
            log.info("Attempted to start job with roots " + roots + " but a scan was already running");
            return false;
        }
        state = STATE.scanning;
        activeRoots = roots;
        activePath = null;
        executor.execute(() -> {
            try {
                performScan(activeRoots, consumer);
            } catch (Exception e) {
                log.error("Exception during scan of " + activeRoots, e);
            } finally {
                state = STATE.ready;
                activeRoots = null;
                activePath = null;
            }
        });
        return true;
    }

    /**
     * @return the current state of the ScanBot.
     */
    public STATE getState() {
        return state;
    }

    public boolean isReady() {
        return state == STATE.ready;
    }

    /**
     * @return the folder currently being scanned. Can be null.
     */
    public String getActivePath() {
        return activePath;
    }

    private void performScan(List<String> roots, Consumer<Folder> consumer) {
        for (int i = 1 ; i <= roots.size() ; i++ ) {
            String root = roots.get(i-1);
            log.info(String.format(Locale.ENGLISH, "Starting scan of root %d/%d '%s'", i, roots.size(), root));
            try {
                performScan(Paths.get(root), consumer);
                log.debug(String.format(Locale.ENGLISH, "Finished scan of root %d/%d '%s'", i, roots.size(), root));
            } catch (Exception e) {
                log.warn(String.format(Locale.ENGLISH, "Exception during scan %d/%d '%s'", i, roots.size(), root), e);
            }
        }
    }

    private void performScan(Path path, Consumer<Folder> consumer) {
        if (!Files.exists(path)) {
            log.debug("Path '" + path + "' could not be located");
            // Send empty folder for potential deletion of pre-existing files
            consumer.accept(new Folder(path.toString()));
            return;
        }

        log.debug("Scanning path " + path + "'");
        activePath = path.toString();
        Folder folder = new Folder(path.toString());
        List<Path> subFolders = new ArrayList<>();
        try (DirectoryStream<Path> pathEntries = Files.newDirectoryStream(path)) {
            pathEntries.forEach(pathEntry -> {
                if (Files.isDirectory(pathEntry)) {
                    subFolders.add(pathEntry);
                } else {
                    folder.add(new FileEntry(path.toString(), pathEntry.getFileName().toString()));
                }
            });
            folder.subFolderCount = subFolders.size();
            log.debug(String.format(Locale.ENGLISH, "Finished scan of path '%s' with %d files and %d sub-folders. " +
                                                    "Performing callback with result",
                                    path.toString(),  folder.size(), folder.subFolderCount));
            consumer.accept(folder);
        } catch (AccessDeniedException e) {
            log.debug("AccessDeniedException for path '" + path + "'");
        } catch (IOException e) {
            log.warn("Exception while streaming the content of folder '" + path + "'", e);
        }
        activePath = null;

        subFolders.forEach(subFolder -> performScan(subFolder, consumer));
    }

    /**
     * Holds non-recursive information for a folder, listing all file entries.
     * Note that this list does not contain sub-folders.
     */
    public static class Folder extends ArrayList<FileEntry> {
        public final String folder;
        public int subFolderCount = 0;

        public Folder(String folder) {
            this.folder = folder;
        }

        public String toString() {
            return "Folder(#files=" + size() + ", #subFolders=" + subFolderCount + ")";
        }
    }

}
