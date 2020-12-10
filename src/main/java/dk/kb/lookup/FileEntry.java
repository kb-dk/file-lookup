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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Representation of an entry in {@link Storage}.
 */
public class FileEntry {
    final static SimpleDateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);

    /**
     * @param path path to a file, without the filename part. Must not be null.
     * @param filename filename, without the path. Can be null.
     */
    public FileEntry(String path, String filename) {
        this (path, filename, System.currentTimeMillis());
    }

    /**
     * @param path path to a file, without the filename part. Must not be null.
     * @param filename filename, without the path. Can be null.
     * @param lastSeen when the entry was seen in milliseconds since Epoch {@code System.currentTimeMillis()}.
     */
    public FileEntry(String path, String filename, long lastSeen) {
        this.path = path;
        this.filename = filename;
        this.lastSeen = lastSeen;
        if (path == null) {
            throw new NullPointerException("Path was null");
        }
    }

    /**
     * The path for the file. Never null.
     */
    public String path;
    /**
     * The filename. Can be null uf the entry only represents a path.
     */
    public String filename;
    /**
     * When the entry was registered or last updated in Storage, in milliseconds since epoch. Always defined.
     */
    public long lastSeen;

    /**
     * @return lastSeen as the subset {@code YYYY-MM-DDThh:mm:ssZ} of iso-8601.
     */
    public String getLastSeenAsISO8601() {
        synchronized (iso8601) { // SimpleDateFormat is not thread safe
            return iso8601.format(new Date(lastSeen));
        }
    }

    /**
     * @return the concatenated path and the filename. If the filename is null, only the path is returned.
     */
    public String getFullpath() {
        return path + (filename == null ? "" : filename);
    }

    /**
     * Matches the EntryReply in {@code openapi.json}.
     * @return a JSON representation of the entry, intended for external delivery.
     */
    public String toJSON() {
        return String.format(Locale.ENGLISH, "{\"path\": \"%s\", \"filename\": %s, \"lastSeen\": \"%s\", \"lastSeenMS\": %d}",
                             escapeJSON(path), filename == null ? "null" : ("\"" + escapeJSON(filename) + "\""),
                             getLastSeenAsISO8601(), lastSeen);
    }

    String escapeJSON(String value) {
        return value.replace("\"", "\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}
