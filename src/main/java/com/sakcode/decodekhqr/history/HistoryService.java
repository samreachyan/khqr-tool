package com.sakcode.decodekhqr.history;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class HistoryService {

    private static final int MAX_ENTRIES = 100;
    private static final String HISTORY_DIR = ".khqr-tool";
    private static final String HISTORY_FILE = "history.json";

    private final ObjectMapper objectMapper;
    private final Path historyPath;
    private List<HistoryEntry> entries;

    public HistoryService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        String home = System.getProperty("user.home");
        this.historyPath = Path.of(home, HISTORY_DIR, HISTORY_FILE);
        this.entries = load();
    }

    public synchronized List<HistoryEntry> list() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public synchronized void save(HistoryEntry entry) {
        HistoryEntry toSave = entry.id() == null
                ? new HistoryEntry(UUID.randomUUID().toString(), entry.timestamp(), entry.type(),
                entry.qrString(), entry.json(), entry.merchantName(), entry.amount(),
                entry.currency(), entry.formSnapshot())
                : entry;

        entries.removeIf(e -> e.id().equals(toSave.id()));
        entries.add(0, toSave);

        if (entries.size() > MAX_ENTRIES) {
            entries = new ArrayList<>(entries.subList(0, MAX_ENTRIES));
        }
        persist();
    }

    public synchronized void delete(String id) {
        entries.removeIf(e -> e.id().equals(id));
        persist();
    }

    private List<HistoryEntry> load() {
        if (!Files.exists(historyPath)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(historyPath.toFile(), new TypeReference<List<HistoryEntry>>() {});
        } catch (IOException e) {
            try {
                Path backup = historyPath.resolveSibling(HISTORY_FILE + ".backup." + System.currentTimeMillis());
                Files.move(historyPath, backup);
            } catch (IOException ignored) {
            }
            return new ArrayList<>();
        }
    }

    private void persist() {
        try {
            Files.createDirectories(historyPath.getParent());
            objectMapper.writeValue(historyPath.toFile(), entries);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
