package io.yamlrt.core;

import java.util.*;

public class CommentedMap<K, V> extends LinkedHashMap<K, V> {
    
    private final Map<K, CommentInfo> comments = new LinkedHashMap<>();
    private final List<CommentToken> startComments = new ArrayList<>();
    private final List<CommentToken> endComments = new ArrayList<>();
    private final Set<Integer> blankLines = new LinkedHashSet<>();
    private int detectedIndent = 2;
    private boolean flowStyle = false;
    
    public CommentedMap() {
        super();
    }
    
    public CommentedMap(Map<? extends K, ? extends V> m) {
        super(m);
    }
    
    public void setComment(K key, String comment) {
        CommentInfo info = comments.computeIfAbsent(key, k -> new CommentInfo());
        info.setEndOfLineComment(comment, -1, -1);
    }
    
    public void setCommentBefore(K key, String comment) {
        CommentInfo info = comments.computeIfAbsent(key, k -> new CommentInfo());
        info.addPreComment(comment, -1, -1);
    }
    
    public void setCommentsBefore(K key, List<String> commentLines) {
        CommentInfo info = comments.computeIfAbsent(key, k -> new CommentInfo());
        info.getPreComments().clear();
        for (String line : commentLines) {
            info.addPreComment(line, -1, -1);
        }
    }
    
    public String getComment(K key) {
        CommentInfo info = comments.get(key);
        if (info != null && info.hasEndOfLineComment()) {
            return info.getEndOfLineComment().getContent();
        }
        return null;
    }
    
    public List<String> getCommentsBefore(K key) {
        CommentInfo info = comments.get(key);
        if (info != null && info.hasPreComments()) {
            List<String> result = new ArrayList<>();
            for (CommentToken token : info.getPreComments()) {
                result.add(token.getContent());
            }
            return result;
        }
        return Collections.emptyList();
    }
    
    public CommentInfo getCommentInfo(K key) {
        return comments.get(key);
    }
    
    public void setCommentInfo(K key, CommentInfo info) {
        comments.put(key, info);
    }
    
    public Map<K, CommentInfo> getAllComments() {
        return Collections.unmodifiableMap(comments);
    }
    
    public List<CommentToken> getStartComments() {
        return startComments;
    }
    
    public void addStartComment(String comment) {
        startComments.add(new CommentToken(comment, -1, 0));
    }
    
    public List<CommentToken> getEndComments() {
        return endComments;
    }
    
    public void addEndComment(String comment) {
        endComments.add(new CommentToken(comment, -1, 0));
    }
    
    public Set<Integer> getBlankLines() {
        return blankLines;
    }
    
    public void addBlankLine(int lineNumber) {
        blankLines.add(lineNumber);
    }
    
    public int getDetectedIndent() {
        return detectedIndent;
    }
    
    public void setDetectedIndent(int indent) {
        this.detectedIndent = indent;
    }
    
    // Flow Style support
    public boolean isFlowStyle() {
        return flowStyle;
    }
    
    public void setFlowStyle(boolean flowStyle) {
        this.flowStyle = flowStyle;
    }
    
    public void insert(int position, K key, V value, String comment) {
        List<Map.Entry<K, V>> entries = new ArrayList<>(this.entrySet());
        this.clear();
        
        int i = 0;
        for (Map.Entry<K, V> entry : entries) {
            if (i == position) {
                this.put(key, value);
                if (comment != null) {
                    setComment(key, comment);
                }
            }
            this.put(entry.getKey(), entry.getValue());
            i++;
        }
        
        if (position >= entries.size()) {
            this.put(key, value);
            if (comment != null) {
                setComment(key, comment);
            }
        }
    }
    
    @Override
    public V put(K key, V value) {
        return super.put(key, value);
    }
    
    @Override
    public V remove(Object key) {
        comments.remove(key);
        return super.remove(key);
    }
    
    @Override
    public void clear() {
        comments.clear();
        startComments.clear();
        endComments.clear();
        blankLines.clear();
        super.clear();
    }
}
