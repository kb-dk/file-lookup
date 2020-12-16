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

import java.nio.file.Path;

/**
 *
 */
public class H2Persistence {
    private static Log log = LogFactory.getLog(H2Persistence.class);

    private static H2Persistence instance;
    private final Path location;
    private final boolean createdNewOnStartup;

    public static H2Persistence getInstance() {
        if (instance == null) {
            instance = new H2Persistence();
        }
        return instance;
    }

    H2Persistence() {
        location = null;
        createdNewOnStartup = false;
    }
}
