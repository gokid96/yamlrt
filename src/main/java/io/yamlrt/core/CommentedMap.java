package io.yamlrt.core;

import java.util.*;

/**
 * CommentedMap (ruamel.yaml style)
 * 
 * - Based on LinkedHashMap for order preservation
 * - Comment object for comment/blank line management
 * - Access via ca property (comment attribute)
 */
public class CommentedMap<K, V> extends LinkedHashMap<K, V> {
    
    private Comment ca = new Comment();  // comment attribute
    private boolean flowStyle = false;
    private int detectedIndent = 2;
    private boolean hasDocumentMarker = false;
    private boolean hasTrailingNewline = true;  // Default: most files have trailing newline
    
    // Line/Column info
    private int line = -1;
    private int col = -1;
    
    public CommentedMap() {
        super();
    }
    
    public CommentedMap(Map<? extends K, ? extends V> m) {
        super(m);
    }
    
    // ==================== Comment Attribute (ca) ====================
    
    public Comment ca() {
        return ca;
    }
    
    // ==================== Convenience methods ====================
    
    /**
     * Set end-of-line comment for a key's value
     */
    public void setEolComment(K key, String comment) {
        Comment.CommentSlot slot = ca.getOrCreateSlot(key);
        slot.setValueEol(CommentToken.comment(comment, 0));
    }
    
    /**
     * Get end-of-line comment for a key's value
     */
    public String getEolComment(K key) {
        Comment.CommentSlot slot = ca.getSlot(key);
        if (slot != null && slot.getValueEol() != null) {
            return slot.getValueEol().getContent();
        }
        return null;
    }
    
    /**
     * Add pre-comment (block comment before key)
     */
    public void addPreComment(K key, String comment) {
        Comment.CommentSlot slot = ca.getOrCreateSlot(key);
        slot.addKeyPre(CommentToken.comment(comment, 0));
    }
    
    /**
     * Add blank line before key
     */
    public void addBlankLineBefore(K key) {
        Comment.CommentSlot slot = ca.getOrCreateSlot(key);
        slot.addKeyPre(CommentToken.blankLine());
    }
    
    // ==================== Document marker ====================
    
    public boolean hasDocumentMarker() {
        return hasDocumentMarker;
    }
    
    public void setDocumentMarker(boolean hasMarker) {
        this.hasDocumentMarker = hasMarker;
    }
    
    // ==================== Flow style ====================
    
    public boolean isFlowStyle() {
        return flowStyle;
    }
    
    public void setFlowStyle(boolean flowStyle) {
        this.flowStyle = flowStyle;
    }
    
    // ==================== Indent ====================
    
    public int getDetectedIndent() {
        return detectedIndent;
    }
    
    public void setDetectedIndent(int indent) {
        this.detectedIndent = indent;
    }
    
    // ==================== Trailing Newline ====================
    
    public boolean hasTrailingNewline() {
        return hasTrailingNewline;
    }
    
    public void setTrailingNewline(boolean hasTrailingNewline) {
        this.hasTrailingNewline = hasTrailingNewline;
    }
    
    // ==================== Line/Col ====================
    
    public int getLine() {
        return line;
    }
    
    public void setLine(int line) {
        this.line = line;
    }
    
    public int getCol() {
        return col;
    }
    
    public void setCol(int col) {
        this.col = col;
    }
    
    // ==================== Legacy compatibility ====================
    
    @Deprecated
    public void setCommentInfo(K key, CommentInfo info) {
        // Convert old CommentInfo to new Comment structure
        Comment.CommentSlot slot = ca.getOrCreateSlot(key);
        if (info.hasPreComments()) {
            for (CommentToken token : info.getPreComments()) {
                slot.addKeyPre(token);
            }
        }
        if (info.hasEndOfLineComment()) {
            slot.setValueEol(info.getEndOfLineComment());
        }
        if (info.hasPostComments()) {
            // Post comments go to value pre of the NEXT key
            // This is handled during parsing
        }
    }
    
    @Deprecated
    public CommentInfo getCommentInfo(K key) {
        Comment.CommentSlot slot = ca.getSlot(key);
        if (slot == null) return null;
        
        CommentInfo info = new CommentInfo();
        for (CommentToken token : slot.getKeyPre()) {
            info.addPreComment(token);
        }
        if (slot.getValueEol() != null) {
            info.setEndOfLineComment(slot.getValueEol());
        }
        return info;
    }
    
    @Deprecated
    public List<CommentToken> getStartComments() {
        return ca.getContainerPre();
    }
    
    @Deprecated
    public void addStartComment(String comment) {
        ca.addContainerPre(CommentToken.comment(comment, 0));
    }
    
    @Deprecated
    public List<CommentToken> getEndComments() {
        return ca.getEnd();
    }
    
    @Deprecated
    public void addEndComment(String comment) {
        ca.addEnd(CommentToken.comment(comment, 0));
    }
}
