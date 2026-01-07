package io.yamlrt.core;

import java.util.*;

public class CommentedList<E> extends ArrayList<E> {
    
    private final Map<Integer, CommentInfo> comments = new LinkedHashMap<>();
    private final List<CommentToken> startComments = new ArrayList<>();
    private final List<CommentToken> endComments = new ArrayList<>();
    private final Set<Integer> blankLines = new LinkedHashSet<>();
    private boolean flowStyle = false;
    
    public CommentedList() {
        super();
    }
    
    public CommentedList(Collection<? extends E> c) {
        super(c);
    }
    
    public void setComment(int index, String comment) {
        CommentInfo info = comments.computeIfAbsent(index, k -> new CommentInfo());
        info.setEndOfLineComment(comment, -1, -1);
    }
    
    public void setCommentBefore(int index, String comment) {
        CommentInfo info = comments.computeIfAbsent(index, k -> new CommentInfo());
        info.addPreComment(comment, -1, -1);
    }
    
    public String getComment(int index) {
        CommentInfo info = comments.get(index);
        if (info != null && info.hasEndOfLineComment()) {
            return info.getEndOfLineComment().getContent();
        }
        return null;
    }
    
    public List<String> getCommentsBefore(int index) {
        CommentInfo info = comments.get(index);
        if (info != null && info.hasPreComments()) {
            List<String> result = new ArrayList<>();
            for (CommentToken token : info.getPreComments()) {
                result.add(token.getContent());
            }
            return result;
        }
        return Collections.emptyList();
    }
    
    public CommentInfo getCommentInfo(int index) {
        return comments.get(index);
    }
    
    public void setCommentInfo(int index, CommentInfo info) {
        comments.put(index, info);
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
    
    // Flow Style support
    public boolean isFlowStyle() {
        return flowStyle;
    }
    
    public void setFlowStyle(boolean flowStyle) {
        this.flowStyle = flowStyle;
    }
    
    @Override
    public void add(int index, E element) {
        shiftCommentsUp(index);
        super.add(index, element);
    }
    
    @Override
    public E remove(int index) {
        comments.remove(index);
        shiftCommentsDown(index);
        return super.remove(index);
    }
    
    @Override
    public void clear() {
        comments.clear();
        startComments.clear();
        endComments.clear();
        blankLines.clear();
        super.clear();
    }
    
    private void shiftCommentsUp(int fromIndex) {
        Map<Integer, CommentInfo> newComments = new LinkedHashMap<>();
        for (Map.Entry<Integer, CommentInfo> entry : comments.entrySet()) {
            int idx = entry.getKey();
            if (idx >= fromIndex) {
                newComments.put(idx + 1, entry.getValue());
            } else {
                newComments.put(idx, entry.getValue());
            }
        }
        comments.clear();
        comments.putAll(newComments);
    }
    
    private void shiftCommentsDown(int fromIndex) {
        Map<Integer, CommentInfo> newComments = new LinkedHashMap<>();
        for (Map.Entry<Integer, CommentInfo> entry : comments.entrySet()) {
            int idx = entry.getKey();
            if (idx > fromIndex) {
                newComments.put(idx - 1, entry.getValue());
            } else if (idx < fromIndex) {
                newComments.put(idx, entry.getValue());
            }
        }
        comments.clear();
        comments.putAll(newComments);
    }
}
