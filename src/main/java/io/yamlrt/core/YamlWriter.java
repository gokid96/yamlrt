package io.yamlrt.core;

import java.util.*;

/**
 * YAML Writer (ruamel.yaml round-trip style)
 * 
 * Key principles:
 * 1. pre comment (blank lines/comments) -> output before key/item
 * 2. eol comment -> output after value at original column
 * 3. preserve original indent
 * 4. compact block sequence (- key: value)
 * 5. preserve exact blank line count
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
    
    public String write(CommentedMap<String, Object> root) {
        output = new StringBuilder();
        this.indentSize = root.getDetectedIndent();
        
        // Document marker
        if (hasDocumentMarker || root.hasDocumentMarker()) {
            output.append("---").append(lineEnding);
        }
        
        // Container pre-comments
        for (CommentToken token : root.ca().getContainerPre()) {
            writeCommentToken(token, 0);
        }
        
        // Main content
        writeMapping(root, 0);
        
        // End comments
        for (CommentToken token : root.ca().getEnd()) {
            writeCommentToken(token, 0);
        }
        
        return output.toString();
    }
    
    /**
     * Write a mapping (dict)
     */
    @SuppressWarnings("unchecked")
    private void writeMapping(Map<String, Object> map, int indent) {
        String indentStr = spaces(indent);
        Comment ca = (map instanceof CommentedMap) ? ((CommentedMap<String, Object>) map).ca() : null;
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            Comment.CommentSlot slot = (ca != null) ? ca.getSlot(key) : null;
            
            // Pre-comments (blank lines and block comments before key)
            if (slot != null) {
                for (CommentToken token : slot.getKeyPre()) {
                    writeCommentToken(token, indent);
                }
            }
            
            // Key
            output.append(indentStr).append(key).append(":");
            
            // Value with EOL comment
            CommentToken eolComment = (slot != null) ? slot.getValueEol() : null;
            writeValue(value, indent, eolComment, key.length() + indent + 1);
        }
    }
    
    /**
     * Write a value after "key:"
     * @param keyEndCol column position after "key:"
     */
    @SuppressWarnings("unchecked")
    private void writeValue(Object value, int keyIndent, CommentToken eolComment, int keyEndCol) {
        if (value == null) {
            appendEolComment(eolComment, keyEndCol);
            output.append(lineEnding);
        } else if (value instanceof CommentedMap) {
            CommentedMap<String, Object> mapValue = (CommentedMap<String, Object>) value;
            if (mapValue.isFlowStyle()) {
                output.append(" ").append(formatFlowMapping(mapValue));
                appendEolComment(eolComment, keyEndCol + 1 + formatFlowMapping(mapValue).length());
                output.append(lineEnding);
            } else {
                appendEolComment(eolComment, keyEndCol);
                output.append(lineEnding);
                writeMapping(mapValue, keyIndent + indentSize);
            }
        } else if (value instanceof CommentedList) {
            CommentedList<Object> listValue = (CommentedList<Object>) value;
            if (listValue.isFlowStyle()) {
                output.append(" ").append(formatFlowSequence(listValue));
                appendEolComment(eolComment, keyEndCol + 1 + formatFlowSequence(listValue).length());
                output.append(lineEnding);
            } else {
                appendEolComment(eolComment, keyEndCol);
                output.append(lineEnding);
                writeSequence(listValue, keyIndent);
            }
        } else if (value instanceof Map) {
            appendEolComment(eolComment, keyEndCol);
            output.append(lineEnding);
            writeMapping((Map<String, Object>) value, keyIndent + indentSize);
        } else if (value instanceof List) {
            appendEolComment(eolComment, keyEndCol);
            output.append(lineEnding);
            writeSequence((List<Object>) value, keyIndent);
        } else {
            String scalar = formatScalar(value);
            output.append(" ").append(scalar);
            appendEolComment(eolComment, keyEndCol + 1 + scalar.length());
            output.append(lineEnding);
        }
    }
    
    /**
     * Write a sequence (list)
     */
    @SuppressWarnings("unchecked")
    private void writeSequence(List<Object> list, int parentIndent) {
        // Determine actual indent
        int listIndent = parentIndent + indentSize;
        if (list instanceof CommentedList) {
            CommentedList<Object> cl = (CommentedList<Object>) list;
            if (cl.getOriginalIndent() >= 0) {
                listIndent = cl.getOriginalIndent();
            }
        }
        
        String indentStr = spaces(listIndent);
        Comment ca = (list instanceof CommentedList) ? ((CommentedList<Object>) list).ca() : null;
        
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            Comment.CommentSlot slot = (ca != null) ? ca.getSlot(i) : null;
            
            // Pre-comments
            if (slot != null) {
                for (CommentToken token : slot.getKeyPre()) {
                    writeCommentToken(token, listIndent);
                }
            }
            
            // Item
            if (item == null) {
                output.append(indentStr).append("-");
                appendEolComment(slot != null ? slot.getKeyEol() : null, listIndent + 1);
                output.append(lineEnding);
            } else if (item instanceof Map) {
                writeListMapItem((Map<String, Object>) item, listIndent);
            } else if (item instanceof List) {
                output.append(indentStr).append("-").append(lineEnding);
                writeSequence((List<Object>) item, listIndent);
            } else {
                String scalar = formatScalar(item);
                output.append(indentStr).append("- ").append(scalar);
                appendEolComment(slot != null ? slot.getKeyEol() : null, listIndent + 2 + scalar.length());
                output.append(lineEnding);
            }
        }
    }
    
    /**
     * Write a map item inside a list (compact format)
     * Format: - key: value
     *           key2: value2
     */
    @SuppressWarnings("unchecked")
    private void writeListMapItem(Map<String, Object> map, int listIndent) {
        String dashIndentStr = spaces(listIndent);
        String contentIndentStr = spaces(listIndent + indentSize);
        
        Comment ca = (map instanceof CommentedMap) ? ((CommentedMap<String, Object>) map).ca() : null;
        
        boolean firstKey = true;
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            Comment.CommentSlot slot = (ca != null) ? ca.getSlot(key) : null;
            
            if (firstKey) {
                // First key: "- key:"
                output.append(dashIndentStr).append("- ").append(key).append(":");
                CommentToken eolComment = (slot != null) ? slot.getValueEol() : null;
                int keyEndCol = listIndent + 2 + key.length() + 1;
                writeValueCompact(value, listIndent + indentSize, eolComment, keyEndCol);
                firstKey = false;
            } else {
                // Pre-comments for subsequent keys
                if (slot != null) {
                    for (CommentToken token : slot.getKeyPre()) {
                        writeCommentToken(token, listIndent + indentSize);
                    }
                }
                
                // Subsequent keys: "  key:"
                output.append(contentIndentStr).append(key).append(":");
                CommentToken eolComment = (slot != null) ? slot.getValueEol() : null;
                int keyEndCol = listIndent + indentSize + key.length() + 1;
                writeValueCompact(value, listIndent + indentSize, eolComment, keyEndCol);
            }
        }
    }
    
    /**
     * Write value in compact context (inside list map item)
     * NO extra blank line at the end
     */
    @SuppressWarnings("unchecked")
    private void writeValueCompact(Object value, int keyIndent, CommentToken eolComment, int keyEndCol) {
        if (value == null) {
            appendEolComment(eolComment, keyEndCol);
            output.append(lineEnding);
        } else if (value instanceof CommentedMap) {
            CommentedMap<String, Object> mapValue = (CommentedMap<String, Object>) value;
            if (mapValue.isFlowStyle()) {
                output.append(" ").append(formatFlowMapping(mapValue));
                appendEolComment(eolComment, keyEndCol + 1 + formatFlowMapping(mapValue).length());
                output.append(lineEnding);
            } else {
                appendEolComment(eolComment, keyEndCol);
                output.append(lineEnding);
                writeMappingCompact(mapValue, keyIndent + indentSize);
            }
        } else if (value instanceof CommentedList) {
            CommentedList<Object> listValue = (CommentedList<Object>) value;
            if (listValue.isFlowStyle()) {
                output.append(" ").append(formatFlowSequence(listValue));
                appendEolComment(eolComment, keyEndCol + 1 + formatFlowSequence(listValue).length());
                output.append(lineEnding);
            } else {
                appendEolComment(eolComment, keyEndCol);
                output.append(lineEnding);
                writeSequenceCompact(listValue, keyIndent);
            }
        } else if (value instanceof Map) {
            appendEolComment(eolComment, keyEndCol);
            output.append(lineEnding);
            writeMappingCompact((Map<String, Object>) value, keyIndent + indentSize);
        } else if (value instanceof List) {
            appendEolComment(eolComment, keyEndCol);
            output.append(lineEnding);
            writeSequenceCompact((List<Object>) value, keyIndent);
        } else {
            String scalar = formatScalar(value);
            output.append(" ").append(scalar);
            appendEolComment(eolComment, keyEndCol + 1 + scalar.length());
            output.append(lineEnding);
        }
    }
    
    /**
     * Write mapping in compact context (no trailing blank line)
     */
    @SuppressWarnings("unchecked")
    private void writeMappingCompact(Map<String, Object> map, int indent) {
        String indentStr = spaces(indent);
        Comment ca = (map instanceof CommentedMap) ? ((CommentedMap<String, Object>) map).ca() : null;
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            Comment.CommentSlot slot = (ca != null) ? ca.getSlot(key) : null;
            
            // Pre-comments
            if (slot != null) {
                for (CommentToken token : slot.getKeyPre()) {
                    writeCommentToken(token, indent);
                }
            }
            
            output.append(indentStr).append(key).append(":");
            CommentToken eolComment = (slot != null) ? slot.getValueEol() : null;
            int keyEndCol = indent + key.length() + 1;
            writeValueCompact(value, indent, eolComment, keyEndCol);
        }
    }
    
    /**
     * Write sequence in compact context (no extra trailing blank line)
     */
    @SuppressWarnings("unchecked")
    private void writeSequenceCompact(List<Object> list, int parentIndent) {
        int listIndent = parentIndent + indentSize;
        if (list instanceof CommentedList) {
            CommentedList<Object> cl = (CommentedList<Object>) list;
            if (cl.getOriginalIndent() >= 0) {
                listIndent = cl.getOriginalIndent();
            }
        }
        
        String indentStr = spaces(listIndent);
        Comment ca = (list instanceof CommentedList) ? ((CommentedList<Object>) list).ca() : null;
        
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            Comment.CommentSlot slot = (ca != null) ? ca.getSlot(i) : null;
            
            // Pre-comments
            if (slot != null) {
                for (CommentToken token : slot.getKeyPre()) {
                    writeCommentToken(token, listIndent);
                }
            }
            
            if (item == null) {
                output.append(indentStr).append("-");
                appendEolComment(slot != null ? slot.getKeyEol() : null, listIndent + 1);
                output.append(lineEnding);
            } else if (item instanceof Map) {
                writeListMapItemCompact((Map<String, Object>) item, listIndent);
            } else if (item instanceof List) {
                output.append(indentStr).append("-").append(lineEnding);
                writeSequenceCompact((List<Object>) item, listIndent);
            } else {
                String scalar = formatScalar(item);
                output.append(indentStr).append("- ").append(scalar);
                appendEolComment(slot != null ? slot.getKeyEol() : null, listIndent + 2 + scalar.length());
                output.append(lineEnding);
            }
        }
    }
    
    /**
     * Write list map item in compact context (no trailing blank line)
     */
    @SuppressWarnings("unchecked")
    private void writeListMapItemCompact(Map<String, Object> map, int listIndent) {
        String dashIndentStr = spaces(listIndent);
        String contentIndentStr = spaces(listIndent + indentSize);
        
        Comment ca = (map instanceof CommentedMap) ? ((CommentedMap<String, Object>) map).ca() : null;
        
        boolean firstKey = true;
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            Comment.CommentSlot slot = (ca != null) ? ca.getSlot(key) : null;
            
            if (firstKey) {
                output.append(dashIndentStr).append("- ").append(key).append(":");
                CommentToken eolComment = (slot != null) ? slot.getValueEol() : null;
                int keyEndCol = listIndent + 2 + key.length() + 1;
                writeValueCompact(value, listIndent + indentSize, eolComment, keyEndCol);
                firstKey = false;
            } else {
                if (slot != null) {
                    for (CommentToken token : slot.getKeyPre()) {
                        writeCommentToken(token, listIndent + indentSize);
                    }
                }
                
                output.append(contentIndentStr).append(key).append(":");
                CommentToken eolComment = (slot != null) ? slot.getValueEol() : null;
                int keyEndCol = listIndent + indentSize + key.length() + 1;
                writeValueCompact(value, listIndent + indentSize, eolComment, keyEndCol);
            }
        }
    }
    
    // ==================== Comment output ====================
    
    private void writeCommentToken(CommentToken token, int defaultIndent) {
        if (token.isBlankLine()) {
            output.append(lineEnding);
        } else {
            int indent = token.getColumn() >= 0 ? token.getColumn() : defaultIndent;
            output.append(spaces(indent)).append(token.getValue()).append(lineEnding);
        }
    }
    
    /**
     * Append EOL comment at the original column position
     * @param eolComment the comment token
     * @param currentCol the current column position (after value)
     */
    private void appendEolComment(CommentToken eolComment, int currentCol) {
        if (eolComment != null && !eolComment.isBlankLine()) {
            int targetCol = eolComment.getColumn();
            if (targetCol > currentCol) {
                // Pad with spaces to reach original column
                output.append(spaces(targetCol - currentCol));
            } else {
                // At least 2 spaces before comment
                output.append("  ");
            }
            output.append(eolComment.getValue());
        }
    }
    
    // ==================== Scalar formatting ====================
    
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
    
    // ==================== Flow style output ====================
    
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
        if (count <= 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }
}
