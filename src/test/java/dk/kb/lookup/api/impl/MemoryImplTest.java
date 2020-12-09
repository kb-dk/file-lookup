package dk.kb.lookup.api.impl;

import dk.kb.lookup.api.MergedApi;
import dk.kb.lookup.config.LookupServiceConfig;
import dk.kb.lookup.model.EntriesRequestDto;
import dk.kb.lookup.model.EntryReplyDto;
import dk.kb.webservice.exception.NoContentServiceException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
class MemoryImplTest {
    private static final Logger log = LoggerFactory.getLogger(MemoryImplTest.class);

    private static Path root = Paths.get("/tmp/file-lookup/"); // Should be requested from the system and the config adjusted
    private static MergedApi impl;

    @BeforeAll
    static void useTestConfig() throws IOException, InterruptedException {
        LookupServiceConfig.initialize("file-lookup-test.yaml");
        impl = setupTestImpl(root);
    }

    @Test
    void testFilenameLookup()  {
        impl.getEntryFromFilename("file1");
    }

    @Test
    void testRemoveNonValidating() throws InterruptedException {
        EntryReplyDto f1 = impl.getEntryFromFilename("file1");
        assertEquals(1, impl.removeFiles(Collections.singletonList(f1.getPath() + "/" + f1.getFilename()), false).size(),
                "The expected number of entries should be removed without validation");
        try {
            impl.getEntryFromFilename("file1"); // Should fail
            fail("Removing of the file 'file1' failed");
        } catch (NoContentServiceException e) {
            performScan(impl); // Rediscover
            impl.getEntryFromFilename("file1"); // Should not fail now
        }
    }

    @Test
    void testRemoveValidatingExisting() {
        EntryReplyDto f1 = impl.getEntryFromFilename("file1");
        assertTrue(impl.removeFiles(Collections.singletonList(f1.getPath() + "/" + f1.getFilename()), true).isEmpty(),
                   "Nothing should be removed");
        impl.getEntryFromFilename("file1"); // Should still work fine
    }

    @Test
    void testRemoveValidatingNonExisting() {
        assertEquals(1, impl.removeFiles(Collections.singletonList("/foo/bar.zoo"), true).size(),
                "The expected number of entries should be removed");
    }

    @Test
    void testNonexistingFilenameLookup() {
        try {
            impl.getEntryFromFilename("not_there");
        } catch (NoContentServiceException e) {
            return;
            // Expected
        }
        fail("Requesting 'not_there' should fail properly");
    }

    @Test
    void testRegexpLookup() {
        EntriesRequestDto request = new EntriesRequestDto();
        request.setRegexp(".*1");
        assertEquals(1, impl.getEntries(request, 100).size(), "The expected number of files should be located");
    }

    @Test
    void testTimeMSLookup()  {
        // Get the timestamp for an entry and the total entry count
        EntriesRequestDto request = new EntriesRequestDto();
        request.setRegexp(".*");
        List<EntryReplyDto> all = impl.getEntries(request, Integer.MAX_VALUE);
        assertFalse(all.isEmpty(), "some files should be located");
        long firstTime = all.get(0).getLastSeenEpochMS();

        // Try requesting a bit later
        EntriesRequestDto timeRequest = new EntriesRequestDto();
        timeRequest.setSinceEpochMS(firstTime+1); // 1 ms later than the first
        List<EntryReplyDto> oneMsLater = impl.getEntries(timeRequest, Integer.MAX_VALUE);
        assertNotEquals(oneMsLater.size(), all.size(),
                        "Requesting 1 ms later than first entry should result in another number of entries returned");
    }

    @Test
    void testTimeISOLookup() throws ParseException {
        // Get the timestamp for an entry and the total entry count
        EntriesRequestDto request = new EntriesRequestDto();
        request.setRegexp(".*");
        List<EntryReplyDto> all = impl.getEntries(request, Integer.MAX_VALUE);
        assertFalse(all.isEmpty(), "some files should be located");
        String firstISO = all.get(0).getLastSeen();
        long firstTime = MemoryImpl.iso8601.parse(firstISO).getTime();
        
        // Try requesting a bit later
        EntriesRequestDto timeRequest = new EntriesRequestDto();
        timeRequest.setSince(MemoryImpl.iso8601.format(new Date(firstTime+1000))); // 1 s later than the first
        List<EntryReplyDto> oneMsLater = impl.getEntries(timeRequest, Integer.MAX_VALUE);
        assertNotEquals(oneMsLater.size(), all.size(),
                        "Requesting 1 second later than first entry should result in another number of entries returned");
    }

    private static MergedApi setupTestImpl(Path root) throws IOException, InterruptedException {
        Path[] files = new Path[]{
                Paths.get(root.toString(), "file1"),
                Paths.get(root.toString(), "file2")
        };

        if (!Files.exists(root)) {
            Files.createDirectory(root);
        }
        for (Path file: files) {
            log.debug("Creating test file " + file);

            try (FileOutputStream out = new FileOutputStream(file.toFile())) {
                out.write(87);
                out.flush();
            }
        }

        MergedApi impl = new MemoryImpl();
        assertEquals("idle", impl.getStatus().getState(), "Before scanning, the ScanBot should be idle");

        performScan(impl);
        assertEquals(files.length, impl.getFilecount(), "There should be the expected number of files");

        return impl;
    }

    @SuppressWarnings("BusyWait")
    private static void performScan(MergedApi impl) throws InterruptedException {
        assertFalse(impl.startScan(".*").getRoots().isEmpty(), "At least 1 root should be scanned");
        final long maxTime = System.currentTimeMillis()+60000; // 1 minute is insanely overkill, but we err on the side of caution
        while (System.currentTimeMillis() < maxTime && !impl.getStatus().getState().equals("idle")) {
            Thread.sleep(10); // Yeah, busy wait. Hard to avoid without adding a callback to ScanBot
        }
        assertEquals("idle", impl.getStatus().getState(), "After waiting for scan to stop, the ScanBot should be idle");
    }
}