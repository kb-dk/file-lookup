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

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * The central service calls.
 */
public interface Storage {
    enum STATE {ready, scanning, error}

    /**
     * @return the file paths that are tracked.
     */
    List<String> getRoots();

    /**
     * Set the file paths that are tracked. Not-yet-existing roots are accepted.
     * @param roots a list of file paths.
     */
    void setRoots(List<String> roots);

    /**
     * @param root a single file path.
     */
    void addRoot(String root);

    /**
     * @param root a single file path.
     * @return true if the path was registered.
     */
    boolean removeRoot(String root);

    /**
     * Initiate a full scan of all tracked roots.
     * If a scan is already running, a new one will not be started.
     * @return true if the scan was started.
     */
    boolean startScan();

    /**
     * Initiate a scan of the specific root.
     * If a scan is already running, a new one will not be started.
     * If the root is not registered, a scan will not be started.
     * @return true if the scan was started.
     */
    boolean startScan(String root);

    /**
     * @return the state of the storage.
     */
    STATE getState();

    /**
     * @return human readable state of the storage, e.g. "ready", "Scanning root foo/", "error: Persistence failed".
     */
    String getStateMessage();

    /**
     * Perform a lookup for the given filename.
     * @param filename a filename.
     * @return path, filename and lastSeen for the file. Null if the file could be located.
     * @throws IOException if a storage problem occurred.
     */
    FileEntry getEntryFromFilename(String filename)  throws IOException;

    /**
     * Iterate all the entries that were added since startTime.
     * The entries are not guaranteed to be in order.
     * @param startTime timestamp (milliseconds since epoch) for the earliest entry to process.
     * @param consumer callback for the entries iterated.
     * @return the number of iterated entries.
     * @throws Exception if a storage problem occurred or the consumer threw an Exception.
     */
    long iterateEntriesSince(long startTime, Consumer<FileEntry> consumer) throws Exception;
}
