package io.yamlrt;

import io.yamlrt.core.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * yamlrt - YAML Round-Trip Library (ruamel.yaml style)
 * 
 * 주석과 포맷을 완벽하게 보존하는 YAML 라이브러리
 * 
 * Usage:
 *   // Load from file
 *   Yamlrt yaml = Yamlrt.load(new File("config.yaml"));
 *   
 *   // Load from string
 *   Yamlrt yaml = Yamlrt.load(yamlString);
 *   
 *   // Read values
 *   String host = yaml.getString("server.host");
 *   int port = yaml.getInt("server.port", 8080);
 *   List<Object> items = yaml.getList("items");
 *   
 *   // Modify values (path notation supported)
 *   yaml.set("server.port", 9090);
 *   yaml.set("Services[0].Airline", airlines);
 *   
 *   // Save (comments preserved)
 *   yaml.save(new File("config.yaml"));
 *   String output = yaml.dump();
 */
public class Yamlrt {
    
    private YamlParser parser;
    private YamlWriter writer;
    private CommentedMap<String, Object> root;
    
    public Yamlrt() {
        this.parser = new YamlParser();
        this.writer = new YamlWriter();
    }
    
    // ==================== Static Factory Methods ====================
    
    /**
     * Load YAML from string (static factory)
     */
    public static Yamlrt load(String yaml) {
        Yamlrt instance = new Yamlrt();
        instance.root = instance.parser.parse(yaml);
        return instance;
    }
    
    /**
     * Load YAML from file (static factory)
     */
    public static Yamlrt load(File file) throws IOException {
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        return load(content);
    }
    
    /**
     * Create empty Yamlrt instance
     */
    public static Yamlrt create() {
        Yamlrt instance = new Yamlrt();
        instance.root = new CommentedMap<>();
        return instance;
    }
    
    // ==================== Instance Methods (Legacy API) ====================
    
    /**
     * Load YAML string and return root map (legacy instance method)
     * For new code, prefer static Yamlrt.load(yaml)
     */
    public CommentedMap<String, Object> loadYaml(String yaml) {
        this.root = parser.parse(yaml);
        return root;
    }
    
    // ==================== Getter Methods (Path Notation) ====================
    
    /**
     * Get value by path (e.g., "server.host", "Services[0].ServiceName")
     */
    public Object get(String path) {
        return getByPath(root, path);
    }
    
    /**
     * Get value by path with default
     */
    public Object get(String path, Object defaultValue) {
        Object value = get(path);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Get String value by path
     */
    public String getString(String path) {
        Object value = get(path);
        return value != null ? String.valueOf(value) : null;
    }
    
    /**
     * Get String value by path with default
     */
    public String getString(String path, String defaultValue) {
        String value = getString(path);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Get int value by path
     */
    public int getInt(String path) {
        Object value = get(path);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        throw new IllegalArgumentException("Cannot convert to int: " + path);
    }
    
    /**
     * Get int value by path with default
     */
    public int getInt(String path, int defaultValue) {
        try {
            Object value = get(path);
            if (value == null) return defaultValue;
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            if (value instanceof String) {
                return Integer.parseInt((String) value);
            }
        } catch (Exception e) {
            // ignore
        }
        return defaultValue;
    }
    
    /**
     * Get long value by path
     */
    public long getLong(String path) {
        Object value = get(path);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            return Long.parseLong((String) value);
        }
        throw new IllegalArgumentException("Cannot convert to long: " + path);
    }
    
    /**
     * Get long value by path with default
     */
    public long getLong(String path, long defaultValue) {
        try {
            Object value = get(path);
            if (value == null) return defaultValue;
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            if (value instanceof String) {
                return Long.parseLong((String) value);
            }
        } catch (Exception e) {
            // ignore
        }
        return defaultValue;
    }
    
    /**
     * Get double value by path
     */
    public double getDouble(String path) {
        Object value = get(path);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            return Double.parseDouble((String) value);
        }
        throw new IllegalArgumentException("Cannot convert to double: " + path);
    }
    
    /**
     * Get double value by path with default
     */
    public double getDouble(String path, double defaultValue) {
        try {
            Object value = get(path);
            if (value == null) return defaultValue;
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            if (value instanceof String) {
                return Double.parseDouble((String) value);
            }
        } catch (Exception e) {
            // ignore
        }
        return defaultValue;
    }
    
    /**
     * Get boolean value by path
     */
    public boolean getBoolean(String path) {
        Object value = get(path);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        throw new IllegalArgumentException("Cannot convert to boolean: " + path);
    }
    
    /**
     * Get boolean value by path with default
     */
    public boolean getBoolean(String path, boolean defaultValue) {
        try {
            Object value = get(path);
            if (value == null) return defaultValue;
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            if (value instanceof String) {
                return Boolean.parseBoolean((String) value);
            }
        } catch (Exception e) {
            // ignore
        }
        return defaultValue;
    }
    
    /**
     * Get List value by path
     */
    @SuppressWarnings("unchecked")
    public List<Object> getList(String path) {
        Object value = get(path);
        if (value instanceof List) {
            return (List<Object>) value;
        }
        return null;
    }
    
    /**
     * Get Map value by path
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMap(String path) {
        Object value = get(path);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }
    
    // ==================== Setter Methods (Path Notation) ====================
    
    /**
     * Set value by path (e.g., "server.port", "Services[0].Airline")
     */
    public void set(String path, Object value) {
        setByPath(root, path, value);
    }
    
    // ==================== Dump / Save Methods ====================
    
    /**
     * Dump to YAML string
     */
    public String dump() {
        if (root == null) {
            throw new IllegalStateException("No YAML loaded. Call load() first.");
        }
        writer.setDocumentMarker(root.hasDocumentMarker());
        return writer.write(root);
    }
    
    /**
     * Save to file
     */
    public void save(File file) throws IOException {
        String content = dump();
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
    }
    
    // ==================== Root Access ====================
    
    /**
     * Get root map (for direct manipulation)
     */
    public CommentedMap<String, Object> getRoot() {
        return root;
    }
    
    /**
     * Set root map
     */
    public void setRoot(CommentedMap<String, Object> root) {
        this.root = root;
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Quick round-trip test
     */
    public static String roundTrip(String yaml) {
        Yamlrt yamlrt = Yamlrt.load(yaml);
        return yamlrt.dump();
    }
    
    /**
     * Check if path exists
     */
    public boolean exists(String path) {
        return get(path) != null;
    }
    
    /**
     * Remove value by path
     */
    public void remove(String path) {
        removeByPath(root, path);
    }
    
    // ==================== Path Resolution (Internal) ====================
    
    /**
     * Parse path into segments
     * "server.host" -> ["server", "host"]
     * "Services[0].ServiceName" -> ["Services", 0, "ServiceName"]
     */
    private List<Object> parsePath(String path) {
        List<Object> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            
            if (c == '.') {
                if (current.length() > 0) {
                    segments.add(current.toString());
                    current = new StringBuilder();
                }
            } else if (c == '[') {
                if (current.length() > 0) {
                    segments.add(current.toString());
                    current = new StringBuilder();
                }
                // Find closing bracket
                int end = path.indexOf(']', i);
                if (end > i + 1) {
                    String indexStr = path.substring(i + 1, end);
                    try {
                        segments.add(Integer.parseInt(indexStr));
                    } catch (NumberFormatException e) {
                        // Treat as string key (for map access like ["key"])
                        segments.add(indexStr);
                    }
                    i = end;
                }
            } else if (c == ']') {
                // Skip, already handled
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            segments.add(current.toString());
        }
        
        return segments;
    }
    
    /**
     * Get value by path from container
     */
    @SuppressWarnings("unchecked")
    private Object getByPath(Object container, String path) {
        List<Object> segments = parsePath(path);
        Object current = container;
        
        for (Object segment : segments) {
            if (current == null) return null;
            
            if (segment instanceof Integer) {
                int index = (Integer) segment;
                if (current instanceof List) {
                    List<?> list = (List<?>) current;
                    if (index >= 0 && index < list.size()) {
                        current = list.get(index);
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                String key = (String) segment;
                if (current instanceof Map) {
                    current = ((Map<String, Object>) current).get(key);
                } else {
                    return null;
                }
            }
        }
        
        return current;
    }
    
    /**
     * Set value by path in container
     */
    @SuppressWarnings("unchecked")
    private void setByPath(Object container, String path, Object value) {
        List<Object> segments = parsePath(path);
        Object current = container;
        
        // Navigate to parent
        for (int i = 0; i < segments.size() - 1; i++) {
            Object segment = segments.get(i);
            Object next = null;
            
            if (segment instanceof Integer) {
                int index = (Integer) segment;
                if (current instanceof List) {
                    List<Object> list = (List<Object>) current;
                    if (index >= 0 && index < list.size()) {
                        next = list.get(index);
                    }
                }
            } else {
                String key = (String) segment;
                if (current instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) current;
                    next = map.get(key);
                    
                    // Auto-create nested structure if needed
                    if (next == null) {
                        Object nextSegment = segments.get(i + 1);
                        if (nextSegment instanceof Integer) {
                            next = new CommentedList<>();
                        } else {
                            next = new CommentedMap<>();
                        }
                        map.put(key, next);
                    }
                }
            }
            
            if (next == null) {
                throw new IllegalArgumentException("Cannot navigate path: " + path + " (failed at segment " + segment + ")");
            }
            current = next;
        }
        
        // Set value at final segment
        Object lastSegment = segments.get(segments.size() - 1);
        
        if (lastSegment instanceof Integer) {
            int index = (Integer) lastSegment;
            if (current instanceof List) {
                List<Object> list = (List<Object>) current;
                while (list.size() <= index) {
                    list.add(null);
                }
                list.set(index, value);
            } else {
                throw new IllegalArgumentException("Cannot set index on non-list: " + path);
            }
        } else {
            String key = (String) lastSegment;
            if (current instanceof Map) {
                ((Map<String, Object>) current).put(key, value);
            } else {
                throw new IllegalArgumentException("Cannot set key on non-map: " + path);
            }
        }
    }
    
    /**
     * Remove value by path
     */
    @SuppressWarnings("unchecked")
    private void removeByPath(Object container, String path) {
        List<Object> segments = parsePath(path);
        Object current = container;
        
        // Navigate to parent
        for (int i = 0; i < segments.size() - 1; i++) {
            Object segment = segments.get(i);
            
            if (segment instanceof Integer) {
                int index = (Integer) segment;
                if (current instanceof List) {
                    List<?> list = (List<?>) current;
                    if (index >= 0 && index < list.size()) {
                        current = list.get(index);
                    } else {
                        return;
                    }
                } else {
                    return;
                }
            } else {
                String key = (String) segment;
                if (current instanceof Map) {
                    current = ((Map<String, Object>) current).get(key);
                } else {
                    return;
                }
            }
            
            if (current == null) return;
        }
        
        // Remove at final segment
        Object lastSegment = segments.get(segments.size() - 1);
        
        if (lastSegment instanceof Integer) {
            int index = (Integer) lastSegment;
            if (current instanceof List) {
                List<?> list = (List<?>) current;
                if (index >= 0 && index < list.size()) {
                    ((List<?>) current).remove(index);
                }
            }
        } else {
            String key = (String) lastSegment;
            if (current instanceof Map) {
                ((Map<?, ?>) current).remove(key);
            }
        }
    }
}
