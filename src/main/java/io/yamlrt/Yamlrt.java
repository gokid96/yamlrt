package io.yamlrt;

import io.yamlrt.core.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * yamlrt - Round-trip YAML editing with comment preservation.
 */
public class Yamlrt {
    
    private CommentedMap<String, Object> root;
    private YamlParser parser;
    private YamlWriter writer;
    private String originalContent;
    
    private Yamlrt() {
        this.parser = new YamlParser();
        this.writer = new YamlWriter();
    }
    
    // === Factory Methods ===
    
    public static Yamlrt load(File file) throws IOException {
        String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        return load(content);
    }
    
    public static Yamlrt load(Path path) throws IOException {
        String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        return load(content);
    }
    
    public static Yamlrt load(InputStream inputStream) throws IOException {
        String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        return load(content);
    }
    
    public static Yamlrt load(String yamlContent) {
        Yamlrt yaml = new Yamlrt();
        yaml.originalContent = yamlContent;
        yaml.root = yaml.parser.parse(yamlContent);
        return yaml;
    }
    
    public static Yamlrt create() {
        Yamlrt yaml = new Yamlrt();
        yaml.root = new CommentedMap<>();
        return yaml;
    }
    
    // === Read Values ===
    
    public Object get(String path) {
        return getByPath(root, parsePath(path));
    }
    
    public String getString(String path) {
        Object value = get(path);
        return value != null ? value.toString() : null;
    }
    
    public int getInt(String path, int defaultValue) {
        Object value = get(path);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    public boolean getBoolean(String path, boolean defaultValue) {
        Object value = get(path);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            String str = ((String) value).toLowerCase();
            if (str.equals("true") || str.equals("yes")) return true;
            if (str.equals("false") || str.equals("no")) return false;
        }
        return defaultValue;
    }
    
    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String path) {
        Object value = get(path);
        if (value instanceof List) {
            return (List<T>) value;
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMap(String path) {
        Object value = get(path);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }
    
    // === Write Values ===
    
    public void set(String path, Object value) {
        setByPath(root, parsePath(path), value);
    }
    
    @SuppressWarnings("unchecked")
    public void set(String path, Object value, String comment) {
        List<String> pathParts = parsePath(path);
        setByPath(root, pathParts, value);
        
        String lastKey = pathParts.get(pathParts.size() - 1);
        Object parent = getByPath(root, pathParts.subList(0, pathParts.size() - 1));
        
        if (parent instanceof CommentedMap) {
            ((CommentedMap<String, Object>) parent).setComment(lastKey, comment);
        }
    }
    
    public void remove(String path) {
        List<String> pathParts = parsePath(path);
        if (pathParts.size() == 1) {
            root.remove(pathParts.get(0));
        } else {
            Object parent = getByPath(root, pathParts.subList(0, pathParts.size() - 1));
            String lastKey = pathParts.get(pathParts.size() - 1);
            
            if (parent instanceof Map) {
                ((Map<?, ?>) parent).remove(lastKey);
            } else if (parent instanceof List) {
                int index = Integer.parseInt(lastKey);
                ((List<?>) parent).remove(index);
            }
        }
    }
    
    // === Comment API ===
    
    @SuppressWarnings("unchecked")
    public void setComment(String path, String comment) {
        List<String> pathParts = parsePath(path);
        if (pathParts.size() == 1) {
            root.setComment(pathParts.get(0), comment);
        } else {
            Object parent = getByPath(root, pathParts.subList(0, pathParts.size() - 1));
            String lastKey = pathParts.get(pathParts.size() - 1);
            
            if (parent instanceof CommentedMap) {
                ((CommentedMap<String, Object>) parent).setComment(lastKey, comment);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    public void setCommentBefore(String path, String comment) {
        List<String> pathParts = parsePath(path);
        if (pathParts.size() == 1) {
            root.setCommentBefore(pathParts.get(0), comment);
        } else {
            Object parent = getByPath(root, pathParts.subList(0, pathParts.size() - 1));
            String lastKey = pathParts.get(pathParts.size() - 1);
            
            if (parent instanceof CommentedMap) {
                ((CommentedMap<String, Object>) parent).setCommentBefore(lastKey, comment);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    public String getComment(String path) {
        List<String> pathParts = parsePath(path);
        if (pathParts.size() == 1) {
            return root.getComment(pathParts.get(0));
        } else {
            Object parent = getByPath(root, pathParts.subList(0, pathParts.size() - 1));
            String lastKey = pathParts.get(pathParts.size() - 1);
            
            if (parent instanceof CommentedMap) {
                return ((CommentedMap<String, Object>) parent).getComment(lastKey);
            }
        }
        return null;
    }
    
    // === Save ===
    
    public void save(File file) throws IOException {
        String content = dump();
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }
    
    public void save(Path path) throws IOException {
        String content = dump();
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }
    
    public void save(OutputStream outputStream) throws IOException {
        String content = dump();
        outputStream.write(content.getBytes(StandardCharsets.UTF_8));
    }
    
    public String dump() {
        return writer.write(root);
    }
    
    // === Internal ===
    
    public CommentedMap<String, Object> getRoot() {
        return root;
    }
    
    public String getOriginalContent() {
        return originalContent;
    }
    
    private List<String> parsePath(String path) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            
            if (c == '.') {
                if (current.length() > 0) {
                    parts.add(current.toString());
                    current = new StringBuilder();
                }
            } else if (c == '[') {
                if (current.length() > 0) {
                    parts.add(current.toString());
                    current = new StringBuilder();
                }
            } else if (c == ']') {
                if (current.length() > 0) {
                    parts.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            parts.add(current.toString());
        }
        
        return parts;
    }
    
    @SuppressWarnings("unchecked")
    private Object getByPath(Object current, List<String> path) {
        if (path.isEmpty()) return current;
        
        for (String part : path) {
            if (current == null) return null;
            
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else if (current instanceof List) {
                try {
                    int index = Integer.parseInt(part);
                    List<?> list = (List<?>) current;
                    if (index >= 0 && index < list.size()) {
                        current = list.get(index);
                    } else {
                        return null;
                    }
                } catch (NumberFormatException e) {
                    return null;
                }
            } else {
                return null;
            }
        }
        
        return current;
    }
    
    @SuppressWarnings("unchecked")
    private void setByPath(Object current, List<String> path, Object value) {
        if (path.isEmpty()) return;
        
        for (int i = 0; i < path.size() - 1; i++) {
            String part = path.get(i);
            
            if (current instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) current;
                Object next = map.get(part);
                
                if (next == null) {
                    String nextPart = path.get(i + 1);
                    try {
                        Integer.parseInt(nextPart);
                        next = new CommentedList<>();
                    } catch (NumberFormatException e) {
                        next = new CommentedMap<>();
                    }
                    map.put(part, next);
                }
                current = next;
            } else if (current instanceof List) {
                int index = Integer.parseInt(part);
                List<Object> list = (List<Object>) current;
                
                while (list.size() <= index) {
                    list.add(null);
                }
                
                Object next = list.get(index);
                if (next == null) {
                    String nextPart = path.get(i + 1);
                    try {
                        Integer.parseInt(nextPart);
                        next = new CommentedList<>();
                    } catch (NumberFormatException e) {
                        next = new CommentedMap<>();
                    }
                    list.set(index, next);
                }
                current = next;
            }
        }
        
        String lastPart = path.get(path.size() - 1);
        
        if (current instanceof Map) {
            ((Map<String, Object>) current).put(lastPart, value);
        } else if (current instanceof List) {
            int index = Integer.parseInt(lastPart);
            List<Object> list = (List<Object>) current;
            while (list.size() <= index) {
                list.add(null);
            }
            list.set(index, value);
        }
    }
    
    @Override
    public String toString() {
        return dump();
    }
}
