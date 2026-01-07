package io.yamlrt.core;

import java.util.*;

/**
 * CommentedList (ruamel.yaml style CommentedSeq)
 * 
 * - Based on ArrayList
 * - Comment object for comment/blank line management
 * - Access via ca property
 */
public class CommentedList<E> extends ArrayList<E> {
    
    private Comment ca = new Comment();  // comment attribute
    private boolean flowStyle = false;
    private int originalIndent = -1;  // preserve original indent
    
    // Line/Column info
    private int line = -1;
    private int col = -1;
    
    public CommentedList() {
        super();
    }
    
    public CommentedList(Collection<? extends E> c) {
        super(c);
    }
    
    // ==================== Comment Attribute (ca) ====================
    
    public Comment ca() {
        return ca;
    }
    
    // ==================== Convenience methods ====================
    
    /**
     * Set end-of-line comment for an item
     */
    public void setEolComment(int index, String comment) {
        Comment.CommentSlot slot = ca.getOrCreateSlot(index);
        slot.setKeyEol(CommentToken.comment(comment, 0));
    }
    
    /**
     * Get end-of-line comment for an item
     */
    public String getEolComment(int index) {
        Comment.CommentSlot slot = ca.getSlot(index);
        if (slot != null && slot.getKeyEol() != null) {
            return slot.getKeyEol().getContent();
        }
        return null;
    }
    
    /**
     * Add pre-comment (block comment before item)
     */
    public void addPreComment(int index, String comment) {
        Comment.CommentSlot slot = ca.getOrCreateSlot(index);
        slot.addKeyPre(CommentToken.comment(comment, 0));
    }
    
    /**
     * Add blank line before item
     */
    public void addBlankLineBefore(int index) {
        Comment.CommentSlot slot = ca.getOrCreateSlot(index);
        slot.addKeyPre(CommentToken.blankLine());
    }
    
    // ==================== Original indent ====================
    
    public int getOriginalIndent() {
        return originalIndent;
    }
    
    public void setOriginalIndent(int indent) {
        this.originalIndent = indent;
    }
    
    // ==================== Flow style ====================
    
    public boolean isFlowStyle() {
        return flowStyle;
    }
    
    public void setFlowStyle(boolean flowStyle) {
        this.flowStyle = flowStyle;
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
    
    // ==================== List operations with comment handling ====================
    
    @Override
    public void add(int index, E element) {
        ca.shiftIndicesUp(index);
        super.add(index, element);
    }
    
    @Override
    public E remove(int index) {
        ca.shiftIndicesDown(index);
        return super.remove(index);
    }
    
    @Override
    public void clear() {
        ca = new Comment();
        super.clear();
    }
    
    // ==================== Legacy compatibility ====================
    
    @Deprecated
    public void setCommentInfo(int index, CommentInfo info) {
        Comment.CommentSlot slot = ca.getOrCreateSlot(index);
        if (info.hasPreComments()) {
            for (CommentToken token : info.getPreComments()) {
                slot.addKeyPre(token);
            }
        }
        if (info.hasEndOfLineComment()) {
            slot.setKeyEol(info.getEndOfLineComment());
        }
    }
    
    @Deprecated
    public CommentInfo getCommentInfo(int index) {
        Comment.CommentSlot slot = ca.getSlot(index);
        if (slot == null) return null;
        
        CommentInfo info = new CommentInfo();
        for (CommentToken token : slot.getKeyPre()) {
            info.addPreComment(token);
        }
        if (slot.getKeyEol() != null) {
            info.setEndOfLineComment(slot.getKeyEol());
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
    
    @Deprecated
    public void setComment(int index, String comment) {
        setEolComment(index, comment);
    }
    
    @Deprecated
    public String getComment(int index) {
        return getEolComment(index);
    }
    
    @Deprecated
    public Set<Integer> getBlankLines() {
        return new HashSet<>();
    }
    
    @Deprecated
    public void addBlankLine(int lineNumber) {
        // No-op for compatibility
    }
    
    @Deprecated
    public void setCommentBefore(int index, String comment) {
        addPreComment(index, comment);
    }
    
    @Deprecated
    public List<String> getCommentsBefore(int index) {
        Comment.CommentSlot slot = ca.getSlot(index);
        if (slot == null) return Collections.emptyList();
        
        List<String> result = new ArrayList<>();
        for (CommentToken token : slot.getKeyPre()) {
            if (!token.isBlankLine()) {
                result.add(token.getContent());
            }
        }
        return result;
    }
}
