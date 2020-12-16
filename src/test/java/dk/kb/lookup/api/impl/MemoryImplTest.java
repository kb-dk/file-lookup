package dk.kb.lookup.api.impl;

import dk.kb.lookup.api.MergedApi;
import dk.kb.lookup.config.ServiceConfig;
import dk.kb.lookup.model.EntryReplyDto;
import dk.kb.webservice.exception.NoContentServiceException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
        ServiceConfig.initialize("file-lookup-test.yaml");
        impl = setupTestImpl(root);
    }

    // Explorative test. To be replaced  by a WatchBot unit test
    // https://docs.oracle.com/javase/tutorial/essential/io/notification.html
    @Test
    void testWatching() throws IOException, InterruptedException {
        AtomicInteger creations = new AtomicInteger(0);
        AtomicInteger deletions = new AtomicInteger(0);

        createFile("watchfolder/foo.bar.origin");
        Path watchRoot = Paths.get(root.toString(), "watchfolder");

        WatchService watcher = FileSystems.getDefault().newWatchService();
        WatchKey watchKey = watchRoot.register(watcher,
                                               StandardWatchEventKinds.ENTRY_CREATE,
                                               StandardWatchEventKinds.ENTRY_DELETE,
                                               StandardWatchEventKinds.OVERFLOW);
        Thread t = new Thread(() -> {
            while (true) {
                try {
                    watcher.take();
                } catch (InterruptedException e) {
                    return;
                }
                try {
                    for (WatchEvent<?> event : watchKey.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();

                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            log.info("Overflow detected");
                            continue;
                        }

                        // The filename is the context of the event.
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path filename = ev.context();
                        log.info("Got event " + kind + " for path " + filename);
                        if (StandardWatchEventKinds.ENTRY_CREATE.equals(kind)) {
                            creations.incrementAndGet();
                        } else if (StandardWatchEventKinds.ENTRY_DELETE.equals(kind)) {
                            deletions.incrementAndGet();
                        }
                    }
                } finally {
                    watchKey.reset();
                }
        }});
        t.setDaemon(true);
        t.start();

        // Make changes
        createFile("watchfolder/foo.bar.1");
        createFile("watchfolder/foo.bar.1b");
        Thread.sleep(100);
        deleteFile("watchfolder/foo.bar.1");
        Thread.sleep(100);
        assertEquals(2, creations.get(), "The right number of files should be created");
        assertEquals(1, deletions.get(), "The right number of files should be deleted");

        createFile("watchfolder/subfolder/foo.bar.2");
        Thread.sleep(100);
        createFile("watchfolder/subfolder/foo.bar.3");
        Thread.sleep(100);
        assertEquals(3, creations.get(),
                     "Creating multiple entries in a single sub-folder is only 1 create at root level");

        // What happens when we remove the root folder?
        FileUtils.deleteDirectory(watchRoot.toFile());
        Thread.sleep(100);
        createFile("watchfolder/foo.bar.originB");
        Thread.sleep(100);
        // Probably because the watched folder still exists (under Linux/EXT4 at least), as the watcher has its handle
        assertEquals(3, creations.get(), "Re-creating the watched folder with a file should not trigger an event");

        // Clean up for next test
        setupTestImpl(root);
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
        assertEquals(1, countEntries(impl.getEntries(".*1", null, null, null,  100, false)),
                     "The expected number of files should be located");
    }

    private int countEntries(javax.ws.rs.core.Response entries) {
        if (entries.getEntity() instanceof List) {
            return toList(entries).size();
        }
        if (entries.getEntity() instanceof InputStream) {
            int count = 0;
            BufferedReader in = new BufferedReader(
                    new InputStreamReader((InputStream) entries.getEntity(), StandardCharsets.UTF_8));
            while (true) {
                try {
                    if ((in.readLine() == null)) break;
                } catch (IOException e) {
                    throw new RuntimeException("IOException counting lines from stream", e);
                }
                count++;
            }
            return count;
        }
        throw new UnsupportedOperationException(
                "Cannot count entries for reply class " + entries.getEntity().getClass());
    }

    @SuppressWarnings("unchecked")
    private List<EntryReplyDto> toList(Response entries) {
        if (entries.getEntity() instanceof List) {
            return (List<EntryReplyDto>)entries.getEntity();
        }
        throw new UnsupportedOperationException(
                "Cannot return entries for reply class " + entries.getEntity().getClass());
    }



    @Test
    void testGlobLookup() {
        assertEquals(1, countEntries(impl.getEntries(null,"**/f*1", null, null,  100, false)),
                     "The expected number of files should be located");
    }

    @Test
    void testRegexpLookupStream() throws IOException {
        assertEquals(1, countEntries(impl.getEntries(".*1", null, null, null,  -1, false)),
                     "The expected number of files should be located using streaming");
        assertFalse(impl.startScan(".*").getRoots().isEmpty(),
                    "Starting a new scan after stream export should work");
    }

    // TODO: Reimplement this
//    @Test
//    void testRegexpLookupStreamForceClose() throws IOException {
//        // max = -1 triggers streaming
//        try {
//            impl.getEntries(null, null, ".*1", null, null, null, -1, false);
//        } catch (StreamingServiceException e) {
//            InputStream json = (InputStream)e.getEntity();
//            assertNotEquals(-1, json.read(), "A byte should be returned");
//            json.close();
//        }
//        assertFalse(impl.startScan(".*").getRoots().isEmpty(),
//                    "Starting a new scan after an untimely closed stream export should work");
//    }
    
    //You can fix your tests yourself
    @Test
    void testTimeMSLookup()  {
        // Get the timestamp for an entry and the total entry count
        List<EntryReplyDto> all = toList(impl.getEntries(".*", null, null, null, 1000, true));
        assertFalse(all.isEmpty(), "some files should be located");
        long firstTime = all.get(0).getLastSeenEpochMS();

        // Try requesting a bit later (1 ms later than the first)
        List<EntryReplyDto> oneMsLater = toList(impl.getEntries(null, null, null, firstTime+1, 1000, true));
        assertNotEquals(oneMsLater.size(), all.size(),
                        "Requesting 1 ms later than first entry should result in another number of entries returned");
    }

    @Test
    void testTimeISOLookup() throws Exception {
        // Get the timestamp for an entry and the total entry count
        List<EntryReplyDto> all = toList(impl.getEntries(".*", null, null, null, 1000, true));
        assertFalse(all.isEmpty(), "some files should be located");
        String firstISO = all.get(0).getLastSeen();
        long firstTime = MemoryImpl.iso8601.parse(firstISO).getTime();

        // Try requesting a bit later (1 s as ISO-time only goes down to 1 second granularity in this API)
        String since = MemoryImpl.iso8601.format(new Date(firstTime + 1000)); // 1 s later than the first
        List<EntryReplyDto> oneMsLater = toList(impl.getEntries(".*", null, since, null, 1000, true));
        assertNotEquals(oneMsLater.size(), all.size(),
                        "Requesting 1 second later than first entry should result in another number of entries returned");
    }

    private static MergedApi setupTestImpl(Path root) throws IOException, InterruptedException {
        String[] files = new String[]{
                "file1",
                "file2"
        };

        if (Files.exists(root)) {
            FileUtils.deleteDirectory(root.toFile());
        }
        if (!Files.exists(root)) {
            Files.createDirectory(root);
        }
        for (String file: files) {
            createFile(file);
        }

        MergedApi impl = new MemoryImpl();
        assertEquals("idle", impl.getStatus().getState(), "Before scanning, the ScanBot should be idle");

        performScan(impl);
        assertEquals(files.length, impl.getFilecount(), "There should be the expected number of files");

        return impl;
    }

    private static void createFile(String file) throws IOException {
        log.debug("Creating test file " + file);
        Path realFile = Paths.get(root.toString(), file);
        if (!Files.exists(realFile.getParent())) {
            Files.createDirectory(realFile.getParent());
        }
        try (FileOutputStream out = new FileOutputStream(realFile.toFile())) {
            out.write(87);
            out.flush();
        }
    }

   private static void createFolder(String folder) throws IOException {
        log.debug("Creating test file " + folder);
        Path realFile = Paths.get(root.toString(), folder);
        try (FileOutputStream out = new FileOutputStream(realFile.toFile())) {
            out.write(87);
            out.flush();
        }
    }

    private static void deleteFile(String file) throws IOException {
        log.debug("Deleting test file " + file);
        Path realFile = Paths.get(root.toString(), file);
        Files.delete(realFile);
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