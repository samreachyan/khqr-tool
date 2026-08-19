package com.sakcode.decodekhqr.history;

import java.util.Map;

public record HistoryEntry(
        String id,
        long timestamp,
        String type,
        String qrString,
        String json,
        String merchantName,
        String amount,
        String currency,
        Map<String, String> formSnapshot
) {
}
