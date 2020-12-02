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

import java.util.Locale;

/**
 * Representation of an entry in {@link Storage}.
 */
public class FileEntry {
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
     * @return a JSON representation of the entry, intended for external delivery.
     */
    public String toJSON() {
        return String.format(Locale.ENGLISH, "{\"path\": \"%s\", \"filename\": %s, \"lastSeen\": %d}",
                             path, filename == null ? "null" : ("\"" + filename + "\""), lastSeen);
    }

    String escapeJSON(String value) {
        return value.replace("\"", "\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}
