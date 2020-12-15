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

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * File system watcher that tracks multiple roots and automatically watches sub-folders.
 * Non-existing roots are checked for existence at regular intervals and will be added to the watched list if created.
 */
public class WatchBot implements Closeable {
    private static Log log = LogFactory.getLog(WatchBot.class);

    private final Map<Path, WatchKey> watchers = new HashMap<>();
    private final WatchService watcher;
    private final int maxWatchers;
    private final Set<Path> roots;

    private final Consumer<Path> pathCreatedCallback;
    private final Consumer<Path> pathDeletedCallback;
    private final Runnable watchBotFailedCallback;
    private boolean allOK = true;
    private boolean closed = false;
    private Thread watchBotDaemon;

    /**
     * Creates a new Watchbot with the given setup and starts watching the roots immediately.
     * Note that files that exists when WatchBot is created does not trigger callbacks.
     * @param roots                  the roots that are watched recursively.
     * @param maxWatches             the maximim number of watches to create.
     * @param fileCreatedCallback    called whenever a file or folder is created.
     * @param fileDeletedCallback    called whenever a file or folder is deleted.
     * @param watchBotFailedCallback if the WatchBot fails to register or delete watches this is called.
     *                               If the callback is triggered, WatchBot should be considered unreliable.
     * @throws IOException if the setup failed. This is typically either because the file system does not support
     *                     file watching or because maxWatches was exceeded.
     */
    public WatchBot(List<String> roots, int maxWatches,
                    Consumer<Path> fileCreatedCallback, Consumer<Path> fileDeletedCallback,
                    Runnable watchBotFailedCallback) throws IOException {
        this.maxWatchers = maxWatches;
        this.roots = roots.stream().map(Paths::get).collect(Collectors.toCollection(HashSet::new));
        this.pathCreatedCallback = fileCreatedCallback;
        this.pathDeletedCallback = fileDeletedCallback;
        this.watchBotFailedCallback = watchBotFailedCallback;
        watcher = FileSystems.getDefault().newWatchService();
        if (!(allOK = roots.stream().map(Paths::get).map(Path::toAbsolutePath).allMatch(this::addWatch))) {
            log.error("Error: Unable to add watches for all roots");
        };
        watchBotDaemon = createDaemon();
        log.info("Created " + this);
    }

    @SuppressWarnings("unchecked") // (WatchEvent<Path>) cast
    private Thread createDaemon() {
        Thread watchBotDaemon = new Thread(() -> {
            while (true) {
                WatchKey watchKey;
                try {
                    watchKey = watcher.take();
                } catch (InterruptedException e) {
                    log.warn("Unexpected interruption received while waiting for event");
                    return;
                }
                try {
                    for (WatchEvent<?> event : watchKey.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();

                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            log.error("Overflow detected. Operations are unreliable");
                            allOK = false;
                            watchBotFailedCallback.run();
                            continue;
                        }

                        // The filename is the context of the event.
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path path = ev.context();
                        log.info("Got event " + kind + " for path " + path);
                        if (StandardWatchEventKinds.ENTRY_CREATE.equals(kind)) {
                            pathCreated(path);
                        } else if (StandardWatchEventKinds.ENTRY_DELETE.equals(kind)) {
                            pathDeleted(path);
                        }
                    }
                } finally {
                    watchKey.reset();
                }
        }});
        watchBotDaemon.setName("WatchBotDaemon");
        watchBotDaemon.setDaemon(true);
        watchBotDaemon.start();
        log.info("Created and started watchBotDaemon");
        return watchBotDaemon;
    }

    private void pathDeleted(Path path) {
        path = path.toAbsolutePath();
        pathDeletedCallback.accept(path);
        if (Files.isDirectory(path)) {
            addWatch(path);
        }
    }

    private void pathCreated(Path path) {
        path = path.toAbsolutePath();
        pathCreatedCallback.accept(path);
        if (Files.isDirectory(path)) {
            removeWatch(path);
        }
    }

    /**
     * Add the given root to be watched for changes.
     * @param root the root to watch recursively.
     * @return true if the root was added.
     */
    public boolean addRoot(Path root) {
        root = root.toAbsolutePath();
        synchronized (roots) {
            if (roots.contains(root)) {
                log.info("addRoot(" + root + ") called, but the root was already registered");
                return false;
            }
            if (Files.exists(root) && !Files.isDirectory(root)) {
                log.warn("addRoot(" + root + ") called with something that is not a directory");
                return false;
            }
            roots.add(root);
        }
        if (!Files.exists(root)) {
            log.info("Added root '" + root + "' to the list of roots to be watched, " +
                     "although is it not present at this time");
            return true; // Fully legal to add a watch for a not-yet existing root
        }
        boolean result = addWatch(root);
        allOK &= result;
        return result;
    }

    /**
     * remove the given root and unwatch it.
     * @param root the root to stop watching..
     * @return true if the root was removed.
     */
    public boolean removeRoot(Path root) {
        root = root.toAbsolutePath();
        synchronized (roots) {
            if (!roots.contains(root)) {
                log.info("removeRoot(" + root + ") called, but the root was not registered");
                return false;
            }
            roots.remove(root);
        }
        boolean result = removeWatch(root);
        allOK &= result;
        return result;
    }

    /**
     * @return a copy of the watched roots.
     */
    public Set<Path> getRoots() {
        synchronized (roots) {
            return new HashSet<>(roots);
        }
    }

    /**
     * @return true if no error has been encountered while attempting watching.
     *         If false is returned, the sevice is considered unreliable.
     */
    public boolean isAllOK() {
        return allOK;
    }

    /**
     * Clear watches on the folder recursively, if watches are present.
     * Note that this scans the {@link #watchers} collection and not the file system, as a common reason for un-watching
     * is that folders are deleted.
     * @param path the root to clear watches from.
     * @return true if the recursive remove was successful.
     */
    private boolean removeWatch(Path path) {
        int removed = 0;
        synchronized (watchers) {
            try {
                for (Map.Entry<Path, WatchKey> pathWatchKeyEntry : watchers.entrySet()) {
                    Path entryPath = pathWatchKeyEntry.getKey();
                    if (entryPath.startsWith(path)) {
                        watchers.remove(path).cancel();
                        removed++;
                    }
                }
                log.debug("removeWatch(" + path + ") caused " + removed + " watches to be removed");
                return true;
            } catch (Exception e) {
                log.error("Exception while recursively removing watch for '" + path + "'", e);
                return false;
            }
        }
    }

    /**
     * Add watches to the folder recursively, if they are not already being watched.
     * @param path the root to watch from.
     * @return true if the recursive add was successful.
     */
    private boolean addWatch(Path path) {
        synchronized (watchers) {
            try {
                return descend(path, p -> {
                    if (!Files.isDirectory(path) || watchers.containsKey(path)) {
                        return true;
                    }
                    if (roots.size() == maxWatchers) {
                        log.warn("The number of watched folders has reached maxWatchers == " + maxWatchers);
                        return false;
                    }
                    try {
                        watchers.put(path, path.register(watcher,
                                                         StandardWatchEventKinds.ENTRY_CREATE,
                                                         StandardWatchEventKinds.ENTRY_DELETE,
                                                         StandardWatchEventKinds.OVERFLOW));
                        return true;
                    } catch (IOException e) {
                        log.warn("Unable to add watcher for folder '" + path + "'", e);
                        return false;
                    }
                });
            } catch (Exception e) {
                log.error("Exception while recursively adding watch for '" + path + "'", e);
                return false;
            }
        }
    }
    
    public String toString() {
        return String.format(Locale.ENGLISH, "WatchBot(watched=%d, max=%d, roots=%s)", watchers.size(), maxWatchers, roots);
    }

    /**
     * Descend recursively depth first and call consumer with all encountered paths.
     * If the consumer returns false, the descend is immediately cancelled.
     * @param path a file or folder.
     * @param consumer will be called with the given path and a recursive descend will be performed if it is a folder.
     * @return true if all calls to consumer returned true.
     */
    private boolean descend(Path path, Predicate<Path> consumer) {
        if (!consumer.test(path)) {
            return false;
        }
        if (!Files.isDirectory(path)) {
            return false;
        }
        try (DirectoryStream<Path> pathEntries = Files.newDirectoryStream(path)) {
            for (Path pathEntry: pathEntries) {
                if (!descend(pathEntry, consumer)) {
                    return false;
                }
            }
            return true;
        } catch (AccessDeniedException e) {
            log.debug("AccessDeniedException for path '" + path + "'");
        } catch (IOException e) {
            log.warn("Exception while scanning the content of folder '" + path + "'", e);
        }
        return false;
    }

    /**
     * Stops watching all roots. After close is called, WatchBot will not watch anything, even if new roots are added.
     * @throws IOException if the shutdown failed.
     */
    @Override
    public void close() {
        log.info("close() called: Removing roots " + roots);
        for (Path root: getRoots()) {
            if (!removeRoot(root)) {
                log.warn("Problem removing root '" + root + "' as part of close(). Skipping to next root");
            }
        }
        closed = true;
    }
}
