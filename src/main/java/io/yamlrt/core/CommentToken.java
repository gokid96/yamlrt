package io.yamlrt.core;

public class CommentToken {
    
    private final String value;
    private final int line;
    private final int column;
    
    public CommentToken(String value, int line, int column) {
        this.value = value;
        this.line = line;
        this.column = column;
    }
    
    public String getValue() {
        return value;
    }
    
    public int getLine() {
        return line;
    }
    
    public int getColumn() {
        return column;
    }
    
    public String getContent() {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.startsWith("#")) {
            return trimmed.substring(1).trim();
        }
        return trimmed;
    }
}
