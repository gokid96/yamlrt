package io.yamlrt.core;

/**
 * Comment Token (ruamel.yaml style)
 * 
 * - value: comment content (with #) or empty string (blank line)
 * - column: start column position (indent)
 * - line: line number
 */
public class CommentToken {
    
    private String value;
    private int column;
    private int line;
    
    public CommentToken(String value, int column) {
        this.value = value;
        this.column = column;
        this.line = -1;
    }
    
    public CommentToken(String value, int line, int column) {
        this.value = value;
        this.line = line;
        this.column = column;
    }
    
    /**
     * Create a blank line token
     */
    public static CommentToken blankLine() {
        return new CommentToken("", 0);
    }
    
    /**
     * Create a comment token
     */
    public static CommentToken comment(String text, int column) {
        if (!text.startsWith("#")) {
            text = "# " + text;
        }
        return new CommentToken(text, column);
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public int getColumn() {
        return column;
    }
    
    public void setColumn(int column) {
        this.column = column;
    }
    
    public int getLine() {
        return line;
    }
    
    public void setLine(int line) {
        this.line = line;
    }
    
    /**
     * Check if this is a blank line (empty value)
     */
    public boolean isBlankLine() {
        return value == null || value.isEmpty() || value.equals("\n");
    }
    
    /**
     * Check if this is a comment (starts with #)
     */
    public boolean isComment() {
        return value != null && value.trim().startsWith("#");
    }
    
    /**
     * Get content without # prefix
     */
    public String getContent() {
        if (value == null) return "";
        String trimmed = value.trim();
        if (trimmed.startsWith("#")) {
            return trimmed.substring(1).trim();
        }
        return trimmed;
    }
    
    @Override
    public String toString() {
        if (isBlankLine()) {
            return "BlankLine(col=" + column + ")";
        }
        return "Comment('" + value + "', col=" + column + ")";
    }
}
