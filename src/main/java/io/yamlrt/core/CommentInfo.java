package io.yamlrt.core;

import java.util.ArrayList;
import java.util.List;

public class CommentInfo {
    
    private List<CommentToken> preComments = new ArrayList<>();
    private CommentToken endOfLineComment;
    private List<CommentToken> postComments = new ArrayList<>();
    private int indent = 0;
    private int lineNumber = -1;
    
    public CommentInfo() {}
    
    public CommentInfo(int lineNumber, int indent) {
        this.lineNumber = lineNumber;
        this.indent = indent;
    }
    
    public List<CommentToken> getPreComments() {
        return preComments;
    }
    
    public void addPreComment(CommentToken comment) {
        preComments.add(comment);
    }
    
    public void addPreComment(String comment, int line, int column) {
        preComments.add(new CommentToken(comment, line, column));
    }
    
    public boolean hasPreComments() {
        return !preComments.isEmpty();
    }
    
    public CommentToken getEndOfLineComment() {
        return endOfLineComment;
    }
    
    public void setEndOfLineComment(CommentToken comment) {
        this.endOfLineComment = comment;
    }
    
    public void setEndOfLineComment(String comment, int line, int column) {
        this.endOfLineComment = new CommentToken(comment, line, column);
    }
    
    public boolean hasEndOfLineComment() {
        return endOfLineComment != null;
    }
    
    public List<CommentToken> getPostComments() {
        return postComments;
    }
    
    public void addPostComment(CommentToken comment) {
        postComments.add(comment);
    }
    
    public boolean hasPostComments() {
        return !postComments.isEmpty();
    }
    
    public int getIndent() {
        return indent;
    }
    
    public void setIndent(int indent) {
        this.indent = indent;
    }
    
    public int getLineNumber() {
        return lineNumber;
    }
    
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }
    
    public boolean hasAnyComment() {
        return hasPreComments() || hasEndOfLineComment() || hasPostComments();
    }
}
