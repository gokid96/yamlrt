package io.yamlrt.core;

import java.util.*;

public class YamlWriter {
    
    private StringBuilder output;
    private int indentSize = 2;
    private String lineEnding = "\n";
    
    public YamlWriter() {}
    
    public YamlWriter(int indentSize) {
        this.indentSize = indentSize;
    }
    
    public String write(CommentedMap<String, Object> map) {
        output = new StringBuilder();
        this.indentSize = map.getDetectedIndent();
        
        for (CommentToken token : map.getStartComments()) {
            output.append(token.getValue()).append(lineEnding);
        }
        
        writeMap(map, 0);
        
        for (CommentToken token : map.getEndComments()) {
            output.append(token.getValue()).append(lineEnding);
        }
        
        return output.toString();
    }
    
    @SuppressWarnings("unchecked")
    private void writeMap(CommentedMap<String, Object> map, int indent) {
        String indentStr = spaces(indent);
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            CommentInfo commentInfo = map.getCommentInfo(key);
            
            if (commentInfo != null && commentInfo.hasPreComments()) {
                for (CommentToken token : commentInfo.getPreComments()) {
                    int commentIndent = token.getColumn() >= 0 ? token.getColumn() : indent;
                    output.append(spaces(commentIndent))
                          .append(ensureCommentHash(token.getValue()))
                          .append(lineEnding);
                }
            }
            
            output.append(indentStr).append(key).append(":");
            
            if (value == null) {
                if (commentInfo != null && commentInfo.hasEndOfLineComment()) {
                    output.append("  ").append(commentInfo.getEndOfLineComment().getValue());
                }
                output.append(lineEnding);
            } else if (value instanceof CommentedMap) {
                if (commentInfo != null && commentInfo.hasEndOfLineComment()) {
                    output.append("  ").append(commentInfo.getEndOfLineComment().getValue());
                }
                output.append(lineEnding);
                writeMap((CommentedMap<String, Object>) value, indent + indentSize);
            } else if (value instanceof CommentedList) {
                if (commentInfo != null && commentInfo.hasEndOfLineComment()) {
                    output.append("  ").append(commentInfo.getEndOfLineComment().getValue());
                }
                output.append(lineEnding);
                writeList((CommentedList<Object>) value, indent + indentSize);
            } else if (value instanceof Map) {
                output.append(lineEnding);
                writeGenericMap((Map<String, Object>) value, indent + indentSize);
            } else if (value instanceof List) {
                output.append(lineEnding);
                writeGenericList((List<Object>) value, indent + indentSize);
            } else {
                output.append(" ").append(formatScalar(value));
                if (commentInfo != null && commentInfo.hasEndOfLineComment()) {
                    output.append("  ").append(commentInfo.getEndOfLineComment().getValue());
                }
                output.append(lineEnding);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private void writeList(CommentedList<Object> list, int indent) {
        String indentStr = spaces(indent);
        
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            CommentInfo commentInfo = list.getCommentInfo(i);
            
            if (commentInfo != null && commentInfo.hasPreComments()) {
                for (CommentToken token : commentInfo.getPreComments()) {
                    int commentIndent = token.getColumn() >= 0 ? token.getColumn() : indent;
                    output.append(spaces(commentIndent))
                          .append(ensureCommentHash(token.getValue()))
                          .append(lineEnding);
                }
            }
            
            output.append(indentStr).append("-");
            
            if (item == null) {
                if (commentInfo != null && commentInfo.hasEndOfLineComment()) {
                    output.append("  ").append(commentInfo.getEndOfLineComment().getValue());
                }
                output.append(lineEnding);
            } else if (item instanceof CommentedMap) {
                output.append(lineEnding);
                writeMap((CommentedMap<String, Object>) item, indent + indentSize);
            } else if (item instanceof CommentedList) {
                output.append(lineEnding);
                writeList((CommentedList<Object>) item, indent + indentSize);
            } else if (item instanceof Map) {
                output.append(lineEnding);
                writeGenericMap((Map<String, Object>) item, indent + indentSize);
            } else if (item instanceof List) {
                output.append(lineEnding);
                writeGenericList((List<Object>) item, indent + indentSize);
            } else {
                output.append(" ").append(formatScalar(item));
                if (commentInfo != null && commentInfo.hasEndOfLineComment()) {
                    output.append("  ").append(commentInfo.getEndOfLineComment().getValue());
                }
                output.append(lineEnding);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private void writeGenericMap(Map<String, Object> map, int indent) {
        String indentStr = spaces(indent);
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            output.append(indentStr).append(entry.getKey()).append(":");
            
            Object value = entry.getValue();
            if (value == null) {
                output.append(lineEnding);
            } else if (value instanceof Map) {
                output.append(lineEnding);
                writeGenericMap((Map<String, Object>) value, indent + indentSize);
            } else if (value instanceof List) {
                output.append(lineEnding);
                writeGenericList((List<Object>) value, indent + indentSize);
            } else {
                output.append(" ").append(formatScalar(value)).append(lineEnding);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private void writeGenericList(List<Object> list, int indent) {
        String indentStr = spaces(indent);
        
        for (Object item : list) {
            output.append(indentStr).append("-");
            
            if (item == null) {
                output.append(lineEnding);
            } else if (item instanceof Map) {
                output.append(lineEnding);
                writeGenericMap((Map<String, Object>) item, indent + indentSize);
            } else if (item instanceof List) {
                output.append(lineEnding);
                writeGenericList((List<Object>) item, indent + indentSize);
            } else {
                output.append(" ").append(formatScalar(item)).append(lineEnding);
            }
        }
    }
    
    private String formatScalar(Object value) {
        if (value == null) return "null";
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
    
    private String ensureCommentHash(String comment) {
        if (comment == null) return "# ";
        String trimmed = comment.trim();
        if (trimmed.startsWith("#")) return trimmed;
        return "# " + trimmed;
    }
    
    private String spaces(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }
}
