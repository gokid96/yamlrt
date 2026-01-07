package io.yamlrt.core;

import java.util.*;

/**
 * Comment storage structure (ruamel.yaml style)
 * 
 * Structure:
 * - comment: [eol_comment, pre_comments] (for the container itself)
 * - items: Map of key to [key_eol, key_pre, value_eol, value_pre]
 * - end: end comments
 * 
 * Blank lines are stored as CommentToken with empty value ("")
 */
public class Comment {
    
    // Container-level comment: [eol, pre_list]
    private CommentToken containerEol = null;
    private List<CommentToken> containerPre = new ArrayList<>();
    
    // Per-item comments: key -> [key_eol, key_pre, value_eol, value_pre]
    // For sequences: index -> [item_eol, item_pre, null, null]
    private Map<Object, CommentSlot> items = new LinkedHashMap<>();
    
    // End of document comments
    private List<CommentToken> end = new ArrayList<>();
    
    public Comment() {}
    
    // ==================== Container-level ====================
    
    public CommentToken getContainerEol() {
        return containerEol;
    }
    
    public void setContainerEol(CommentToken token) {
        this.containerEol = token;
    }
    
    public List<CommentToken> getContainerPre() {
        return containerPre;
    }
    
    public void addContainerPre(CommentToken token) {
        containerPre.add(token);
    }
    
    // ==================== Item-level ====================
    
    public CommentSlot getSlot(Object key) {
        return items.get(key);
    }
    
    public CommentSlot getOrCreateSlot(Object key) {
        return items.computeIfAbsent(key, k -> new CommentSlot());
    }
    
    public void setSlot(Object key, CommentSlot slot) {
        items.put(key, slot);
    }
    
    public Map<Object, CommentSlot> getItems() {
        return items;
    }
    
    // ==================== End comments ====================
    
    public List<CommentToken> getEnd() {
        return end;
    }
    
    public void addEnd(CommentToken token) {
        end.add(token);
    }
    
    // ==================== Utility ====================
    
    /**
     * Shift indices for sequence operations
     */
    public void shiftIndicesUp(int fromIndex) {
        Map<Object, CommentSlot> newItems = new LinkedHashMap<>();
        for (Map.Entry<Object, CommentSlot> entry : items.entrySet()) {
            Object key = entry.getKey();
            if (key instanceof Integer) {
                int idx = (Integer) key;
                if (idx >= fromIndex) {
                    newItems.put(idx + 1, entry.getValue());
                } else {
                    newItems.put(idx, entry.getValue());
                }
            } else {
                newItems.put(key, entry.getValue());
            }
        }
        items = newItems;
    }
    
    public void shiftIndicesDown(int fromIndex) {
        Map<Object, CommentSlot> newItems = new LinkedHashMap<>();
        for (Map.Entry<Object, CommentSlot> entry : items.entrySet()) {
            Object key = entry.getKey();
            if (key instanceof Integer) {
                int idx = (Integer) key;
                if (idx > fromIndex) {
                    newItems.put(idx - 1, entry.getValue());
                } else if (idx < fromIndex) {
                    newItems.put(idx, entry.getValue());
                }
                // idx == fromIndex is removed
            } else {
                newItems.put(key, entry.getValue());
            }
        }
        items = newItems;
    }
    
    @Override
    public String toString() {
        return "Comment{containerEol=" + containerEol + 
               ", containerPre=" + containerPre + 
               ", items=" + items + 
               ", end=" + end + "}";
    }
    
    /**
     * Comment slot for each key/index
     * [0]: key/item EOL comment
     * [1]: key/item PRE comments (including blank lines)
     * [2]: value EOL comment (for maps)
     * [3]: value PRE comments (for maps)
     */
    public static class CommentSlot {
        private CommentToken keyEol = null;
        private List<CommentToken> keyPre = new ArrayList<>();
        private CommentToken valueEol = null;
        private List<CommentToken> valuePre = new ArrayList<>();
        
        public CommentToken getKeyEol() { return keyEol; }
        public void setKeyEol(CommentToken token) { this.keyEol = token; }
        
        public List<CommentToken> getKeyPre() { return keyPre; }
        public void addKeyPre(CommentToken token) { keyPre.add(token); }
        
        public CommentToken getValueEol() { return valueEol; }
        public void setValueEol(CommentToken token) { this.valueEol = token; }
        
        public List<CommentToken> getValuePre() { return valuePre; }
        public void addValuePre(CommentToken token) { valuePre.add(token); }
        
        public boolean hasKeyPre() { return !keyPre.isEmpty(); }
        public boolean hasValuePre() { return !valuePre.isEmpty(); }
        
        @Override
        public String toString() {
            return "[keyEol=" + keyEol + ", keyPre=" + keyPre + 
                   ", valueEol=" + valueEol + ", valuePre=" + valuePre + "]";
        }
    }
}
