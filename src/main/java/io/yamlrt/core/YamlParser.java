package io.yamlrt.core;

import java.util.*;
import java.util.regex.*;

/**
 * YAML Parser (ruamel.yaml round-trip style)
 */
public class YamlParser {
    
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("^(\\s*)([^:#\\[\\]{}][^:#]*?):\\s*(.*)$");
    private static final Pattern LIST_ITEM_PATTERN = Pattern.compile("^(\\s*)-\\s?(.*)$");
    private static final Pattern BLANK_LINE_PATTERN = Pattern.compile("^\\s*$");
    private static final Pattern COMMENT_ONLY_PATTERN = Pattern.compile("^(\\s*)#.*$");
    private static final Pattern DOCUMENT_START_PATTERN = Pattern.compile("^---.*$");
    private static final Pattern DOCUMENT_END_PATTERN = Pattern.compile("^\\.\\.\\.\\s*$");
    
    private List<String> lines;
    private int currentLine;
    private int detectedIndent = 2;
    
    private List<CommentToken> pendingTokens = new ArrayList<>();
    
    private boolean debug = false;
    
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    
    private void log(String msg) {
        if (debug) {
            System.out.println("[Parser] " + msg);
        }
    }
    
    /**
     * Result of splitting value and comment
     */
    private static class ValueAndComment {
        String value;
        String comment;
        int commentColumn;  // Original column position of comment
        
        ValueAndComment(String value, String comment, int commentColumn) {
            this.value = value;
            this.comment = comment;
            this.commentColumn = commentColumn;
        }
    }
    
    public CommentedMap<String, Object> parse(String yaml) {
        this.lines = Arrays.asList(yaml.split("\n", -1));
        this.currentLine = 0;
        this.pendingTokens.clear();
        
        CommentedMap<String, Object> root = new CommentedMap<>();
        
        root.setDocumentMarker(detectDocumentMarker());
        detectIndent();
        root.setDetectedIndent(detectedIndent);
        
        // Detect trailing newline: if yaml ends with \n, last split element will be empty
        root.setTrailingNewline(yaml.endsWith("\n"));
        
        parseMapping(root, 0);
        
        for (CommentToken token : pendingTokens) {
            root.ca().addEnd(token);
        }
        
        return root;
    }
    
    private boolean detectDocumentMarker() {
        for (String line : lines) {
            if (DOCUMENT_START_PATTERN.matcher(line).matches()) {
                return true;
            }
            if (!isSkippableLine(line)) {
                break;
            }
        }
        return false;
    }
    
    private void detectIndent() {
        for (String line : lines) {
            if (isSkippableLine(line)) continue;
            int indent = getIndent(line);
            if (indent > 0) {
                detectedIndent = indent;
                return;
            }
        }
    }
    
    private void parseMapping(CommentedMap<String, Object> map, int expectedIndent) {
        log("parseMapping START expectedIndent=" + expectedIndent);
        while (currentLine < lines.size()) {
            String line = lines.get(currentLine);
            log("parseMapping line=" + currentLine + " '" + line + "'");
            
            if (isDocumentMarker(line)) {
                currentLine++;
                continue;
            }
            
            if (isBlankLine(line)) {
                pendingTokens.add(CommentToken.blankLine());
                log("parseMapping: blank line added to pending, pending.size=" + pendingTokens.size());
                currentLine++;
                continue;
            }
            
            Matcher commentMatcher = COMMENT_ONLY_PATTERN.matcher(line);
            if (commentMatcher.matches()) {
                int commentIndent = commentMatcher.group(1).length();
                pendingTokens.add(new CommentToken(line.trim(), commentIndent));
                currentLine++;
                continue;
            }
            
            int indent = getIndent(line);
            
            if (indent < expectedIndent) {
                log("parseMapping: indent=" + indent + " < expected=" + expectedIndent + " -> RETURN");
                return;
            }
            
            if (LIST_ITEM_PATTERN.matcher(line).matches() && indent <= expectedIndent) {
                log("parseMapping: list item at indent=" + indent + " <= expected=" + expectedIndent + " -> RETURN");
                return;
            }
            
            Matcher kvMatcher = KEY_VALUE_PATTERN.matcher(line);
            if (kvMatcher.matches() && indent == expectedIndent) {
                String key = kvMatcher.group(2).trim();
                String rest = kvMatcher.group(3);
                
                // Calculate the column where value starts (after "key: ")
                int keyEndCol = indent + key.length() + 1; // +1 for ':'
                
                ValueAndComment vac = splitValueAndComment(rest, keyEndCol);
                String valueStr = vac.value;
                String inlineComment = vac.comment;
                int commentColumn = vac.commentColumn;
                
                Comment.CommentSlot slot = map.ca().getOrCreateSlot(key);
                for (CommentToken token : pendingTokens) {
                    slot.addKeyPre(token);
                }
                pendingTokens.clear();
                
                if (inlineComment != null) {
                    slot.setValueEol(new CommentToken(inlineComment, commentColumn));
                }
                
                currentLine++;
                log("parseMapping: key='" + key + "' valueStr='" + valueStr + "'");
                
                Object value = parseValue(valueStr, indent);
                map.put(key, value);
                continue;
            }
            
            log("parseMapping: unknown line, skip");
            currentLine++;
        }
        log("parseMapping END");
    }
    
    private Object parseValue(String valueStr, int keyIndent) {
        if (!valueStr.isEmpty()) {
            return parseScalar(valueStr);
        }
        
        String nextLine = peekNextContentLine();
        if (nextLine == null) {
            return null;
        }
        
        int nextIndent = getIndent(nextLine);
        log("parseValue: keyIndent=" + keyIndent + " nextIndent=" + nextIndent + " nextLine='" + nextLine + "'");
        
        // List item: must be at greater indent than key
        // OR at same indent if inside a nested structure (ruamel.yaml behavior)
        if (LIST_ITEM_PATTERN.matcher(nextLine).matches()) {
            // List items at same or greater indent are values of this key
            if (nextIndent >= keyIndent) {
                CommentedList<Object> list = new CommentedList<>();
                list.setOriginalIndent(nextIndent);
                parseSequence(list, nextIndent);
                return list;
            }
            return null;
        }
        
        // Map: must be at greater indent
        if (nextIndent > keyIndent) {
            CommentedMap<String, Object> nested = new CommentedMap<>();
            nested.setDetectedIndent(detectedIndent);
            parseMapping(nested, nextIndent);
            return nested;
        }
        
        return null;
    }
    
    private void parseSequence(CommentedList<Object> list, int listIndent) {
        log("parseSequence START listIndent=" + listIndent);
        int itemIndex = 0;
        
        while (currentLine < lines.size()) {
            String line = lines.get(currentLine);
            log("parseSequence line=" + currentLine + " '" + line + "' itemIndex=" + itemIndex);
            
            if (isDocumentMarker(line)) {
                currentLine++;
                continue;
            }
            
            if (isBlankLine(line)) {
                pendingTokens.add(CommentToken.blankLine());
                log("parseSequence: blank line added to pending, pending.size=" + pendingTokens.size());
                currentLine++;
                continue;
            }
            
            Matcher commentMatcher = COMMENT_ONLY_PATTERN.matcher(line);
            if (commentMatcher.matches()) {
                int commentIndent = commentMatcher.group(1).length();
                pendingTokens.add(new CommentToken(line.trim(), commentIndent));
                currentLine++;
                continue;
            }
            
            int indent = getIndent(line);
            
            if (indent < listIndent) {
                log("parseSequence: indent=" + indent + " < listIndent=" + listIndent + " -> RETURN");
                return;
            }
            
            Matcher listMatcher = LIST_ITEM_PATTERN.matcher(line);
            if (listMatcher.matches() && indent == listIndent) {
                String afterDash = listMatcher.group(2);
                
                Comment.CommentSlot slot = list.ca().getOrCreateSlot(itemIndex);
                log("parseSequence: attaching " + pendingTokens.size() + " pending tokens to item " + itemIndex);
                for (CommentToken token : pendingTokens) {
                    slot.addKeyPre(token);
                }
                pendingTokens.clear();
                
                currentLine++;
                
                Object item = parseListItem(afterDash, listIndent, line);
                list.add(item);
                itemIndex++;
                log("parseSequence: added item " + (itemIndex-1) + ", continuing...");
                continue;
            }
            
            log("parseSequence: not a list item at this indent -> RETURN");
            return;
        }
        log("parseSequence END");
    }
    
    private Object parseListItem(String afterDash, int dashIndent, String originalLine) {
        // Calculate column where content starts (after "- ")
        int contentStartCol = dashIndent + 2;
        
        ValueAndComment vac = splitValueAndComment(afterDash, contentStartCol);
        String content = vac.value;
        String inlineComment = vac.comment;
        
        log("parseListItem: dashIndent=" + dashIndent + " content='" + content + "'");
        
        Matcher kvMatcher = KEY_VALUE_PATTERN.matcher(content);
        if (kvMatcher.matches()) {
            String firstKey = kvMatcher.group(2).trim();
            String firstValueStr = kvMatcher.group(3).trim();
            
            // Calculate column for first key's value
            int firstKeyEndCol = contentStartCol + firstKey.length() + 1;
            ValueAndComment firstVac = splitValueAndComment(firstValueStr, firstKeyEndCol);
            String firstVal = firstVac.value;
            String firstComment = firstVac.comment;
            int firstCommentCol = firstVac.commentColumn;
            
            CommentedMap<String, Object> itemMap = new CommentedMap<>();
            itemMap.setDetectedIndent(detectedIndent);
            
            if (firstComment != null) {
                Comment.CommentSlot slot = itemMap.ca().getOrCreateSlot(firstKey);
                slot.setValueEol(new CommentToken(firstComment, firstCommentCol));
            }
            
            Object firstValue;
            if (firstVal.isEmpty()) {
                firstValue = parseNestedValue(dashIndent + detectedIndent);
            } else {
                firstValue = parseScalar(firstVal);
            }
            itemMap.put(firstKey, firstValue);
            
            int contentIndent = dashIndent + detectedIndent;
            log("parseListItem: calling parseMapInListItem contentIndent=" + contentIndent + " dashIndent=" + dashIndent);
            parseMapInListItem(itemMap, contentIndent, dashIndent);
            log("parseListItem: parseMapInListItem returned, currentLine=" + currentLine);
            
            return itemMap;
        }
        
        if (!content.isEmpty()) {
            return parseScalar(content);
        }
        
        return parseNestedValue(dashIndent + detectedIndent);
    }
    
    private void parseMapInListItem(CommentedMap<String, Object> map, int contentIndent, int dashIndent) {
        log("parseMapInListItem START contentIndent=" + contentIndent + " dashIndent=" + dashIndent);
        while (currentLine < lines.size()) {
            String line = lines.get(currentLine);
            log("parseMapInListItem line=" + currentLine + " '" + line + "'");
            
            if (isDocumentMarker(line)) {
                currentLine++;
                continue;
            }
            
            if (isBlankLine(line)) {
                pendingTokens.add(CommentToken.blankLine());
                log("parseMapInListItem: blank line added to pending, pending.size=" + pendingTokens.size());
                currentLine++;
                continue;
            }
            
            Matcher commentMatcher = COMMENT_ONLY_PATTERN.matcher(line);
            if (commentMatcher.matches()) {
                int commentIndent = commentMatcher.group(1).length();
                pendingTokens.add(new CommentToken(line.trim(), commentIndent));
                currentLine++;
                continue;
            }
            
            int indent = getIndent(line);
            
            if (LIST_ITEM_PATTERN.matcher(line).matches() && indent <= dashIndent) {
                log("parseMapInListItem: list item at indent=" + indent + " <= dashIndent=" + dashIndent + " -> RETURN");
                return;
            }
            
            if (indent < contentIndent) {
                log("parseMapInListItem: indent=" + indent + " < contentIndent=" + contentIndent + " -> RETURN");
                return;
            }
            
            Matcher kvMatcher = KEY_VALUE_PATTERN.matcher(line);
            if (kvMatcher.matches() && indent == contentIndent) {
                String key = kvMatcher.group(2).trim();
                String rest = kvMatcher.group(3);
                
                int keyEndCol = indent + key.length() + 1;
                ValueAndComment vac = splitValueAndComment(rest, keyEndCol);
                String valueStr = vac.value;
                String comment = vac.comment;
                int commentCol = vac.commentColumn;
                
                Comment.CommentSlot slot = map.ca().getOrCreateSlot(key);
                for (CommentToken token : pendingTokens) {
                    slot.addKeyPre(token);
                }
                pendingTokens.clear();
                
                if (comment != null) {
                    slot.setValueEol(new CommentToken(comment, commentCol));
                }
                
                currentLine++;
                log("parseMapInListItem: key='" + key + "'");
                
                Object value;
                if (valueStr.isEmpty()) {
                    value = parseNestedValue(contentIndent);
                } else {
                    value = parseScalar(valueStr);
                }
                map.put(key, value);
                continue;
            }
            
            log("parseMapInListItem: unknown -> RETURN");
            return;
        }
        log("parseMapInListItem END");
    }
    
    private Object parseNestedValue(int minIndent) {
        String nextLine = peekNextContentLine();
        if (nextLine == null) {
            return null;
        }
        
        int nextIndent = getIndent(nextLine);
        log("parseNestedValue: minIndent=" + minIndent + " nextIndent=" + nextIndent + " nextLine='" + nextLine + "'");
        
        if (nextIndent < minIndent) {
            return null;
        }
        
        if (LIST_ITEM_PATTERN.matcher(nextLine).matches()) {
            CommentedList<Object> list = new CommentedList<>();
            list.setOriginalIndent(nextIndent);
            parseSequence(list, nextIndent);
            return list;
        } else {
            CommentedMap<String, Object> map = new CommentedMap<>();
            map.setDetectedIndent(detectedIndent);
            parseMapping(map, nextIndent);
            return map;
        }
    }
    
    private String peekNextContentLine() {
        int temp = currentLine;
        while (temp < lines.size()) {
            String line = lines.get(temp);
            if (!isSkippableLine(line)) {
                return line;
            }
            temp++;
        }
        return null;
    }
    
    private boolean isSkippableLine(String line) {
        return isBlankLine(line) || isCommentOnly(line) || isDocumentMarker(line);
    }
    
    private boolean isDocumentMarker(String line) {
        return DOCUMENT_START_PATTERN.matcher(line).matches() || 
               DOCUMENT_END_PATTERN.matcher(line).matches();
    }
    
    private boolean isBlankLine(String line) {
        return BLANK_LINE_PATTERN.matcher(line).matches();
    }
    
    private boolean isCommentOnly(String line) {
        return COMMENT_ONLY_PATTERN.matcher(line).matches();
    }
    
    private int getIndent(String line) {
        int indent = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') indent++;
            else if (c == '\t') indent += 4;
            else break;
        }
        return indent;
    }
    
    /**
     * Split value and inline comment, preserving the original column position of the comment
     * 
     * @param str The string after "key: " or "- "
     * @param startCol The column where str starts in the original line
     * @return ValueAndComment with value, comment, and comment's column position
     */
    private ValueAndComment splitValueAndComment(String str, int startCol) {
        if (str == null || str.isEmpty()) {
            return new ValueAndComment("", null, -1);
        }
        
        boolean inSingle = false, inDouble = false;
        
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\'' && !inDouble) inSingle = !inSingle;
            else if (c == '"' && !inSingle) inDouble = !inDouble;
            else if (c == '#' && !inSingle && !inDouble) {
                String value = str.substring(0, i).trim();
                String comment = str.substring(i).trim();
                // Calculate actual column: startCol + position in str
                // But we need to account for leading space after ":"
                // The actual column is where '#' appears in the original line
                int commentColumn = startCol + i + 1; // +1 for space after ':'
                return new ValueAndComment(value, comment, commentColumn);
            }
        }
        
        return new ValueAndComment(str.trim(), null, -1);
    }
    
    private Object parseScalar(String value) {
        if (value == null || value.isEmpty()) return null;
        value = value.trim();
        
        if (value.equals("null") || value.equals("~")) return null;
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        
        if (value.startsWith("[") && value.endsWith("]")) {
            return parseFlowSequence(value);
        }
        if (value.startsWith("{") && value.endsWith("}")) {
            return parseFlowMapping(value);
        }
        
        try {
            if (value.contains(".")) return Double.parseDouble(value);
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
        }
        
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
            (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        
        return value;
    }
    
    private List<Object> parseFlowSequence(String str) {
        CommentedList<Object> result = new CommentedList<>();
        result.setFlowStyle(true);
        
        String content = str.substring(1, str.length() - 1).trim();
        if (content.isEmpty()) return result;
        
        for (String item : splitFlowItems(content)) {
            item = item.trim();
            if (!item.isEmpty()) {
                result.add(parseFlowValue(item));
            }
        }
        return result;
    }
    
    private Map<String, Object> parseFlowMapping(String str) {
        CommentedMap<String, Object> result = new CommentedMap<>();
        result.setFlowStyle(true);
        
        String content = str.substring(1, str.length() - 1).trim();
        if (content.isEmpty()) return result;
        
        for (String pair : splitFlowItems(content)) {
            pair = pair.trim();
            if (pair.isEmpty()) continue;
            
            int colonIndex = findFlowColon(pair);
            if (colonIndex > 0) {
                String key = pair.substring(0, colonIndex).trim();
                String value = pair.substring(colonIndex + 1).trim();
                
                if ((key.startsWith("\"") && key.endsWith("\"")) ||
                    (key.startsWith("'") && key.endsWith("'"))) {
                    key = key.substring(1, key.length() - 1);
                }
                
                result.put(key, parseFlowValue(value));
            }
        }
        return result;
    }
    
    private Object parseFlowValue(String value) {
        if (value == null || value.isEmpty()) return null;
        value = value.trim();
        
        if (value.startsWith("[") && value.endsWith("]")) return parseFlowSequence(value);
        if (value.startsWith("{") && value.endsWith("}")) return parseFlowMapping(value);
        
        if (value.equals("null") || value.equals("~")) return null;
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        
        try {
            if (value.contains(".")) return Double.parseDouble(value);
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
        }
        
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
            (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        
        return value;
    }
    
    private List<String> splitFlowItems(String content) {
        List<String> items = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean inSingleQuote = false, inDoubleQuote = false;
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            
            if (c == '\'' && !inDoubleQuote) inSingleQuote = !inSingleQuote;
            else if (c == '"' && !inSingleQuote) inDoubleQuote = !inDoubleQuote;
            
            if (!inSingleQuote && !inDoubleQuote) {
                if (c == '[' || c == '{') depth++;
                else if (c == ']' || c == '}') depth--;
            }
            
            if (c == ',' && depth == 0 && !inSingleQuote && !inDoubleQuote) {
                items.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            items.add(current.toString());
        }
        
        return items;
    }
    
    private int findFlowColon(String str) {
        int depth = 0;
        boolean inSingleQuote = false, inDoubleQuote = false;
        
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            
            if (c == '\'' && !inDoubleQuote) inSingleQuote = !inSingleQuote;
            else if (c == '"' && !inSingleQuote) inDoubleQuote = !inDoubleQuote;
            
            if (!inSingleQuote && !inDoubleQuote) {
                if (c == '[' || c == '{') depth++;
                else if (c == ']' || c == '}') depth--;
                else if (c == ':' && depth == 0) return i;
            }
        }
        
        return -1;
    }
}
