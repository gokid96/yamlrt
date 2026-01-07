package io.yamlrt.core;

import java.util.*;

/**
 * YAML Writer - ruamel.yaml 스타일 round-trip 출력
 * 
 * 핵심 원칙:
 * 1. Block Sequence에서 Map 아이템은 compact 형식 (- key: value)
 * 2. 주석/빈줄 보존
 * 3. 문서 마커 (---) 보존
 * 4. Flow Style ([a,b], {k:v}) 보존
 */
public class YamlWriter {
    
    private StringBuilder output;
    private int indentSize = 2;
    private String lineEnding = "\n";
    private boolean hasDocumentMarker = false;
    
    public YamlWriter() {}
    
    public YamlWriter(int indentSize) {
        this.indentSize = indentSize;
    }
    
    public void setDocumentMarker(boolean hasMarker) {
        this.hasDocumentMarker = hasMarker;
    }
    
    public String write(CommentedMap<String, Object> map) {
        output = new StringBuilder();
        this.indentSize = map.getDetectedIndent();
        
        // Document start marker
        if (hasDocumentMarker) {
            output.append("---").append(lineEnding);
        }
        
        // Start comments (header)
        for (CommentToken token : map.getStartComments()) {
            output.append(token.getValue()).append(lineEnding);
        }
        
        // Main content
        writeMap(map, 0, false);
        
        // End comments
        for (CommentToken token : map.getEndComments()) {
            output.append(token.getValue()).append(lineEnding);
        }
        
        return output.toString();
    }
    
    /**
     * Write Map (Block Style)
     * @param map Map to write
     * @param indent current indent level
     * @param inListItem true if this map is inside a list item (첫 키 이미 출력됨)
     */
    @SuppressWarnings("unchecked")
    private void writeMap(Map<String, Object> map, int indent, boolean inListItem) {
        String indentStr = spaces(indent);
        CommentedMap<String, Object> commented = (map instanceof CommentedMap) ? 
            (CommentedMap<String, Object>) map : null;
        
        boolean firstKey = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            CommentInfo commentInfo = (commented != null) ? commented.getCommentInfo(key) : null;
            
            // Skip first key if in list item (already written by writeListMapItem)
            if (inListItem && firstKey) {
                firstKey = false;
                continue;
            }
            firstKey = false;
            
            // Pre-comments (block comments above key)
            if (commentInfo != null && commentInfo.hasPreComments()) {
                for (CommentToken token : commentInfo.getPreComments()) {
                    output.append(indentStr).append(token.getValue()).append(lineEnding);
                }
            }
            
            // Key
            output.append(indentStr).append(key).append(":");
            
            // Value
            writeValue(value, indent, commentInfo);
        }
    }
    
    /**
     * Write value after key:
     */
    @SuppressWarnings("unchecked")
    private void writeValue(Object value, int keyIndent, CommentInfo commentInfo) {
        if (value == null) {
            appendEndOfLineComment(commentInfo);
            output.append(lineEnding);
        } else if (value instanceof CommentedMap) {
            CommentedMap<String, Object> mapValue = (CommentedMap<String, Object>) value;
            if (mapValue.isFlowStyle()) {
                output.append(" ").append(formatFlowMapping(mapValue));
                appendEndOfLineComment(commentInfo);
                output.append(lineEnding);
            } else {
                appendEndOfLineComment(commentInfo);
                output.append(lineEnding);
                writeMap(mapValue, keyIndent + indentSize, false);
            }
        } else if (value instanceof CommentedList) {
            CommentedList<Object> listValue = (CommentedList<Object>) value;
            if (listValue.isFlowStyle()) {
                output.append(" ").append(formatFlowSequence(listValue));
                appendEndOfLineComment(commentInfo);
                output.append(lineEnding);
            } else {
                appendEndOfLineComment(commentInfo);
                output.append(lineEnding);
                writeList(listValue, keyIndent);
            }
        } else if (value instanceof Map) {
            appendEndOfLineComment(commentInfo);
            output.append(lineEnding);
            writeMap((Map<String, Object>) value, keyIndent + indentSize, false);
        } else if (value instanceof List) {
            appendEndOfLineComment(commentInfo);
            output.append(lineEnding);
            writeList((List<Object>) value, keyIndent);
        } else {
            output.append(" ").append(formatScalar(value));
            appendEndOfLineComment(commentInfo);
            output.append(lineEnding);
        }
    }
    
    /**
     * Write List (Block Style)
     * ruamel.yaml 스타일: Map 아이템은 compact 형식 (- key: value)
     */
    @SuppressWarnings("unchecked")
    private void writeList(List<Object> list, int indent) {
        String indentStr = spaces(indent);
        CommentedList<Object> commented = (list instanceof CommentedList) ? 
            (CommentedList<Object>) list : null;
        
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            CommentInfo commentInfo = (commented != null) ? commented.getCommentInfo(i) : null;
            
            // Pre-comments
            if (commentInfo != null && commentInfo.hasPreComments()) {
                // Add blank line before comment block (between items)
                if (i > 0) {
                    output.append(lineEnding);
                }
                for (CommentToken token : commentInfo.getPreComments()) {
                    output.append(token.getValue()).append(lineEnding);
                }
            }
            
            if (item == null) {
                output.append(indentStr).append("-");
                appendEndOfLineComment(commentInfo);
                output.append(lineEnding);
            } else if (item instanceof Map) {
                // Map item: compact format (- key: value)
                writeListMapItem((Map<String, Object>) item, indent, commentInfo);
            } else if (item instanceof List) {
                output.append(indentStr).append("-").append(lineEnding);
                writeList((List<Object>) item, indent + indentSize);
            } else {
                output.append(indentStr).append("- ").append(formatScalar(item));
                appendEndOfLineComment(commentInfo);
                output.append(lineEnding);
            }
        }
    }
    
    /**
     * Write Map item in list with compact format
     * Format: - ServiceName: 1A1
     *           ServiceType: MQ
     */
    @SuppressWarnings("unchecked")
    private void writeListMapItem(Map<String, Object> map, int listIndent, CommentInfo listItemComment) {
        String indentStr = spaces(listIndent);
        String contentIndentStr = spaces(listIndent + indentSize);
        
        CommentedMap<String, Object> commented = (map instanceof CommentedMap) ? 
            (CommentedMap<String, Object>) map : null;
        
        boolean firstKey = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            CommentInfo commentInfo = (commented != null) ? commented.getCommentInfo(key) : null;
            
            if (firstKey) {
                // First key: - key: value (compact)
                output.append(indentStr).append("- ").append(key).append(":");
                writeValueCompact(value, listIndent + indentSize, commentInfo);
                firstKey = false;
            } else {
                // Subsequent keys: indented under dash
                
                // Pre-comments
                if (commentInfo != null && commentInfo.hasPreComments()) {
                    for (CommentToken token : commentInfo.getPreComments()) {
                        output.append(contentIndentStr).append(token.getValue()).append(lineEnding);
                    }
                }
                
                output.append(contentIndentStr).append(key).append(":");
                writeValueCompact(value, listIndent + indentSize, commentInfo);
            }
        }
        
        // Blank line after each service
        output.append(lineEnding);
    }
    
    /**
     * Write value in compact context (inside list map item)
     */
    @SuppressWarnings("unchecked")
    private void writeValueCompact(Object value, int keyIndent, CommentInfo commentInfo) {
        if (value == null) {
            appendEndOfLineComment(commentInfo);
            output.append(lineEnding);
        } else if (value instanceof CommentedMap) {
            CommentedMap<String, Object> mapValue = (CommentedMap<String, Object>) value;
            if (mapValue.isFlowStyle()) {
                output.append(" ").append(formatFlowMapping(mapValue));
                appendEndOfLineComment(commentInfo);
                output.append(lineEnding);
            } else {
                appendEndOfLineComment(commentInfo);
                output.append(lineEnding);
                writeMapInListContext(mapValue, keyIndent + indentSize);
            }
        } else if (value instanceof CommentedList) {
            CommentedList<Object> listValue = (CommentedList<Object>) value;
            if (listValue.isFlowStyle()) {
                output.append(" ").append(formatFlowSequence(listValue));
                appendEndOfLineComment(commentInfo);
                output.append(lineEnding);
            } else {
                appendEndOfLineComment(commentInfo);
                output.append(lineEnding);
                writeListInListContext(listValue, keyIndent);
            }
        } else if (value instanceof Map) {
            appendEndOfLineComment(commentInfo);
            output.append(lineEnding);
            writeMapInListContext((Map<String, Object>) value, keyIndent + indentSize);
        } else if (value instanceof List) {
            appendEndOfLineComment(commentInfo);
            output.append(lineEnding);
            writeListInListContext((List<Object>) value, keyIndent);
        } else {
            output.append(" ").append(formatScalar(value));
            appendEndOfLineComment(commentInfo);
            output.append(lineEnding);
        }
    }
    
    /**
     * Write Map inside list context (nested maps)
     */
    @SuppressWarnings("unchecked")
    private void writeMapInListContext(Map<String, Object> map, int indent) {
        String indentStr = spaces(indent);
        CommentedMap<String, Object> commented = (map instanceof CommentedMap) ? 
            (CommentedMap<String, Object>) map : null;
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            CommentInfo commentInfo = (commented != null) ? commented.getCommentInfo(key) : null;
            
            // Pre-comments
            if (commentInfo != null && commentInfo.hasPreComments()) {
                for (CommentToken token : commentInfo.getPreComments()) {
                    output.append(indentStr).append(token.getValue()).append(lineEnding);
                }
            }
            
            output.append(indentStr).append(key).append(":");
            writeValueCompact(value, indent, commentInfo);
        }
    }
    
    /**
     * Write List inside list context (nested lists)
     */
    @SuppressWarnings("unchecked")
    private void writeListInListContext(List<Object> list, int indent) {
        String indentStr = spaces(indent);
        CommentedList<Object> commented = (list instanceof CommentedList) ? 
            (CommentedList<Object>) list : null;
        
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            CommentInfo commentInfo = (commented != null) ? commented.getCommentInfo(i) : null;
            
            // Pre-comments
            if (commentInfo != null && commentInfo.hasPreComments()) {
                if (i > 0) {
                    output.append(lineEnding);
                }
                for (CommentToken token : commentInfo.getPreComments()) {
                    output.append(token.getValue()).append(lineEnding);
                }
            }
            
            if (item == null) {
                output.append(indentStr).append("-");
                appendEndOfLineComment(commentInfo);
                output.append(lineEnding);
            } else if (item instanceof Map) {
                writeListMapItem((Map<String, Object>) item, indent, commentInfo);
            } else if (item instanceof List) {
                output.append(indentStr).append("-").append(lineEnding);
                writeListInListContext((List<Object>) item, indent + indentSize);
            } else {
                output.append(indentStr).append("- ").append(formatScalar(item));
                appendEndOfLineComment(commentInfo);
                output.append(lineEnding);
            }
        }
    }
    
    /**
     * Append end-of-line comment if present
     */
    private void appendEndOfLineComment(CommentInfo commentInfo) {
        if (commentInfo != null && commentInfo.hasEndOfLineComment()) {
            output.append("  ").append(commentInfo.getEndOfLineComment().getValue());
        }
    }
    
    // ==================== Scalar Formatting ====================
    
    private String formatScalar(Object value) {
        if (value == null) return "";
        if (value instanceof Boolean) return value.toString();
        if (value instanceof Number) return value.toString();
        
        String str = value.toString();
        if (needsQuoting(str)) {
            return "\"" + escapeString(str) + "\"";
        }
        return str;
    }
    
    private boolean needsQuoting(String str) {
        if (str.isEmpty()) return true;
        if (str.contains(":") || str.contains("#") || str.contains("\n")) return true;
        if (str.startsWith(" ") || str.endsWith(" ")) return true;
        if (str.equals("true") || str.equals("false") || str.equals("null")) return true;
        // Don't quote YES/NO - they're valid YAML 1.1 booleans but we want to preserve them as-is
        // if (str.equals("yes") || str.equals("no")) return true;
        // if (str.equals("YES") || str.equals("NO")) return true;
        if (str.startsWith("\"") || str.startsWith("'")) return true;
        if (str.startsWith("{") || str.startsWith("[")) return true;
        if (str.startsWith("*") || str.startsWith("&")) return true;
        if (str.startsWith("!") || str.startsWith("%")) return true;
        if (str.startsWith("@") || str.startsWith("`")) return true;
        return false;
    }
    
    private String escapeString(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\t", "\\t");
    }
    
    // ==================== Flow Style Output ====================
    
    private String formatFlowSequence(List<?> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(formatFlowValue(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }
    
    private String formatFlowMapping(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            String key = entry.getKey().toString();
            if (needsFlowQuoting(key)) {
                key = "\"" + escapeString(key) + "\"";
            }
            sb.append(key).append(": ").append(formatFlowValue(entry.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }
    
    private String formatFlowValue(Object value) {
        if (value == null) return "null";
        if (value instanceof Boolean) return value.toString();
        if (value instanceof Number) return value.toString();
        if (value instanceof List) return formatFlowSequence((List<?>) value);
        if (value instanceof Map) return formatFlowMapping((Map<?, ?>) value);
        
        String str = value.toString();
        if (needsFlowQuoting(str)) {
            return "\"" + escapeString(str) + "\"";
        }
        return str;
    }
    
    private boolean needsFlowQuoting(String str) {
        if (str.isEmpty()) return true;
        if (str.contains(",") || str.contains(":") || str.contains("#")) return true;
        if (str.contains("[") || str.contains("]")) return true;
        if (str.contains("{") || str.contains("}")) return true;
        if (str.startsWith(" ") || str.endsWith(" ")) return true;
        if (str.equals("true") || str.equals("false") || str.equals("null")) return true;
        return false;
    }
    
    // ==================== Utility ====================
    
    private String spaces(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }
}
