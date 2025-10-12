package com.ecommerce.kafkaecommerce.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class LogService {
    
    private static final String LOG_FILE = "kafka-ecommerce.log";
    
    public List<LogEntry> getRecentLogs(int maxLines) {
        List<LogEntry> logs = new ArrayList<>();
        File logFile = new File(LOG_FILE);
        
        if (!logFile.exists()) {
            // Create initial log entry if file doesn't exist
            LogEntry initialLog = new LogEntry(
                LocalDateTime.now(),
                "INFO",
                "LogService",
                "Log file initialized. Ready to process orders."
            );
            logs.add(initialLog);
            return logs;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            List<String> allLines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                allLines.add(line);
            }
            
            // Get the last maxLines
            int startIndex = Math.max(0, allLines.size() - maxLines);
            List<String> recentLines = allLines.subList(startIndex, allLines.size());
            
            // Parse log entries
            for (String logLine : recentLines) {
                LogEntry entry = parseLogLine(logLine);
                if (entry != null) {
                    logs.add(entry);
                }
            }
            
            // Reverse to show newest first
            Collections.reverse(logs);
            
        } catch (IOException e) {
            LogEntry errorLog = new LogEntry(
                LocalDateTime.now(),
                "ERROR",
                "LogService",
                "Error reading log file: " + e.getMessage()
            );
            logs.add(errorLog);
        }
        
        return logs;
    }
    
    private LogEntry parseLogLine(String line) {
        try {
            // Expected format: 2024-01-15 10:30:15 - INFO - AnalyticsService - Processing order...
            String[] parts = line.split(" - ", 4);
            if (parts.length >= 4) {
                String timestampStr = parts[0];
                String level = parts[1];
                String source = parts[2];
                String message = parts[3];
                
                LocalDateTime timestamp = LocalDateTime.parse(timestampStr, 
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                
                return new LogEntry(timestamp, level, source, message);
            }
        } catch (Exception e) {
            // If parsing fails, create a simple log entry
            return new LogEntry(LocalDateTime.now(), "INFO", "System", line);
        }
        return null;
    }
    
    public static class LogEntry {
        private LocalDateTime timestamp;
        private String level;
        private String source;
        private String message;
        
        public LogEntry(LocalDateTime timestamp, String level, String source, String message) {
            this.timestamp = timestamp;
            this.level = level;
            this.source = source;
            this.message = message;
        }
        
        // Getters
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getLevel() { return level; }
        public String getSource() { return source; }
        public String getMessage() { return message; }
    }
}
