package io.yamlrt.core;

import java.util.*;
import java.util.regex.*;

public class YamlParser {
    
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("^(\\s*)([^:#\\[\\]{}][^:#]*?):\\s*(.*)$");
    private static final Pattern LIST_ITEM_PATTERN = Pattern.compile("^(\\s*)-\\s?(.*)$");
    private static final Pattern BLANK_LINE_PATTERN = Pattern.compile("^\\s*$");
    private static final Pattern COMMENT_ONLY_PATTERN = Pattern.compile("^\\s*#.*$");
    private static final Pattern DOCUMENT_START_PATTERN = Pattern.compile("^---.*$");
    private static final Pattern DOCUMENT_END_PATTERN = Pattern.compile("^\\.\\.\\.\\s*$");
    
    private List<String> lines;
    private int currentLine;
    private int detectedIndent = 2;
    private List<CommentToken> pendingComments = new ArrayList<>();
    
    public CommentedMap<String, Object> parse(String yaml) {
        this.lines = Arrays.asList(yaml.split("\n", -1));
        this.currentLine = 0;
        this.pendingComments.clear();
        
        CommentedMap<String, Object> root = new CommentedMap<>();
        
        // Detect document marker (---)
        root.setDocumentMarker(detectDocumentMarker());
        
        detectIndent();
        root.setDetectedIndent(detectedIndent);
        
        parseDocument(root);
        
        for (CommentToken token : pendingComments) {
            root.addEndComment(token.getValue());
        }
        
        return root;
    }
    
    private boolean detectDocumentMarker() {
        for (String line : lines) {
            if (DOCUMENT_START_PATTERN.matcher(line).matches()) {
                return true;
            }
            // Stop looking if we hit actual content
            if (!isSkippable(line)) {
                break;
            }
        }
        return false;
    }
    
    private void detectIndent() {
        for (String line : lines) {
            if (isSkippable(line)) continue;
            int indent = getIndent(line);
            if (indent > 0) {
                detectedIndent = indent;
                return;
            }
        }
    }
    
    private void parseDocument(CommentedMap<String, Object> root) {
        while (currentLine < lines.size()) {
            String line = lines.get(currentLine);
            
            if (isDocumentMarker(line) || isBlankLine(line)) {
                if (isBlankLine(line)) root.addBlankLine(currentLine);
                currentLine++;
                continue;
            }
            
            if (isCommentOnly(line)) {
                pendingComments.add(new CommentToken(line.trim(), currentLine, getIndent(line)));
                currentLine++;
                continue;
            }
            
            if (LIST_ITEM_PATTERN.matcher(line).matches()) {
                return;
            }
            
            parseKeyValue(root, 0);
        }
    }
    
    private void parseKeyValue(CommentedMap<String, Object> map, int expectedIndent) {
        while (currentLine < lines.size()) {
            String line = lines.get(currentLine);
            
            if (isDocumentMarker(line)) {
                currentLine++;
                continue;
            }
            
            if (isBlankLine(line)) {
                map.addBlankLine(currentLine);
                currentLine++;
                continue;
            }
            
            if (isCommentOnly(line)) {
                pendingComments.add(new CommentToken(line.trim(), currentLine, getIndent(line)));
                currentLine++;
                continue;
            }
            
            int indent = getIndent(line);
            
            if (indent < expectedIndent) {
                return;
            }
            
            if (LIST_ITEM_PATTERN.matcher(line).matches()) {
                return;
            }
            
            Matcher kvMatcher = KEY_VALUE_PATTERN.matcher(line);
            if (kvMatcher.matches() && indent == expectedIndent) {
                String key = kvMatcher.group(2).trim();
                String rest = kvMatcher.group(3);
                
                String[] parts = splitValueAndComment(rest);
                String valueStr = parts[0];
                String comment = parts[1];
                
                CommentInfo info = new CommentInfo(currentLine, indent);
                for (CommentToken t : pendingComments) {
                    info.addPreComment(t);
                }
                pendingComments.clear();
                
                if (comment != null) {
                    info.setEndOfLineComment(comment, currentLine, -1);
                }
                
                currentLine++;
                
                Object value = parseValue(valueStr, indent);
                map.put(key, value);
                map.setCommentInfo(key, info);
                continue;
            }
            
            currentLine++;
        }
    }
    
    private Object parseValue(String valueStr, int keyIndent) {
        if (!valueStr.isEmpty()) {
            return parseScalar(valueStr);
        }
        
        String nextLine = peekNextContent();
        if (nextLine == null) {
            return null;
        }
        
        int nextIndent = getIndent(nextLine);
        
        if (LIST_ITEM_PATTERN.matcher(nextLine).matches()) {
            if (nextIndent >= keyIndent) {
                CommentedList<Object> list = new CommentedList<>();
                parseList(list, nextIndent);
                return list;
            }
            return null;
        }
        
        if (nextIndent > keyIndent) {
            CommentedMap<String, Object> nested = new CommentedMap<>();
            nested.setDetectedIndent(detectedIndent);
            parseKeyValue(nested, nextIndent);
            return nested;
        }
        
        return null;
    }
    
    private void parseList(CommentedList<Object> list, int listIndent) {
        int itemIndex = 0;
        
        while (currentLine < lines.size()) {
            String line = lines.get(currentLine);
            
            if (isDocumentMarker(line)) {
                currentLine++;
                continue;
            }
            
            if (isBlankLine(line)) {
                list.addBlankLine(currentLine);
                currentLine++;
                continue;
            }
            
            if (isCommentOnly(line)) {
                pendingComments.add(new CommentToken(line.trim(), currentLine, getIndent(line)));
                currentLine++;
                continue;
            }
            
            int indent = getIndent(line);
            
            if (indent < listIndent) {
                return;
            }
            
            Matcher listMatcher = LIST_ITEM_PATTERN.matcher(line);
            if (!listMatcher.matches() || indent != listIndent) {
                return;
            }
            
            String afterDash = listMatcher.group(2);
            
            CommentInfo info = new CommentInfo(currentLine, indent);
            for (CommentToken t : pendingComments) {
                info.addPreComment(t);
            }
            pendingComments.clear();
            
            currentLine++;
            
            Object item = parseListItem(afterDash, indent);
            list.add(item);
            list.setCommentInfo(itemIndex, info);
            itemIndex++;
        }
    }
    
    private Object parseListItem(String afterDash, int dashIndent) {
        String[] parts = splitValueAndComment(afterDash);
        String content = parts[0];
        
        Matcher kvMatcher = KEY_VALUE_PATTERN.matcher(content);
        if (kvMatcher.matches()) {
            String firstKey = kvMatcher.group(2).trim();
            String firstValueStr = kvMatcher.group(3).trim();
            
            CommentedMap<String, Object> itemMap = new CommentedMap<>();
            itemMap.setDetectedIndent(detectedIndent);
            
            String[] firstParts = splitValueAndComment(firstValueStr);
            String firstVal = firstParts[0];
            String firstComment = firstParts[1];
            
            CommentInfo firstInfo = new CommentInfo(currentLine - 1, dashIndent + 2);
            if (firstComment != null) {
                firstInfo.setEndOfLineComment(firstComment, currentLine - 1, -1);
            }
            
            Object firstValue;
            if (firstVal.isEmpty()) {
                firstValue = parseNestedValueForListItem(dashIndent + 2, dashIndent);
            } else {
                firstValue = parseScalar(firstVal);
            }
            
            itemMap.put(firstKey, firstValue);
            itemMap.setCommentInfo(firstKey, firstInfo);
            
            parseListItemRemainingKeys(itemMap, dashIndent + 2, dashIndent);
            
            return itemMap;
        }
        
        if (!content.isEmpty()) {
            return parseScalar(content);
        }
        
        return parseNestedValueForListItem(dashIndent + 2, dashIndent);
    }
    
    private Object parseNestedValueForListItem(int contentIndent, int dashIndent) {
        String nextLine = peekNextContent();
        if (nextLine == null) {
            return null;
        }
        
        int nextIndent = getIndent(nextLine);
        
        if (nextIndent < contentIndent) {
            return null;
        }
        
        if (LIST_ITEM_PATTERN.matcher(nextLine).matches()) {
            CommentedList<Object> list = new CommentedList<>();
            parseList(list, nextIndent);
            return list;
        } else {
            CommentedMap<String, Object> map = new CommentedMap<>();
            map.setDetectedIndent(detectedIndent);
            parseKeyValue(map, nextIndent);
            return map;
        }
    }
    
    private void parseListItemRemainingKeys(CommentedMap<String, Object> itemMap, int contentIndent, int dashIndent) {
        while (currentLine < lines.size()) {
            String line = lines.get(currentLine);
            
            if (isDocumentMarker(line)) {
                currentLine++;
                continue;
            }
            
            if (isBlankLine(line)) {
                currentLine++;
                continue;
            }
            
            if (isCommentOnly(line)) {
                pendingComments.add(new CommentToken(line.trim(), currentLine, getIndent(line)));
                currentLine++;
                continue;
            }
            
            int indent = getIndent(line);
            
            if (LIST_ITEM_PATTERN.matcher(line).matches() && indent <= dashIndent) {
                return;
            }
            
            if (indent < contentIndent) {
                return;
            }
            
            Matcher kvMatcher = KEY_VALUE_PATTERN.matcher(line);
            if (kvMatcher.matches() && indent == contentIndent) {
                String key = kvMatcher.group(2).trim();
                String rest = kvMatcher.group(3);
                
                String[] parts = splitValueAndComment(rest);
                String valueStr = parts[0];
                String comment = parts[1];
                
                CommentInfo info = new CommentInfo(currentLine, indent);
                for (CommentToken t : pendingComments) {
                    info.addPreComment(t);
                }
                pendingComments.clear();
                
                if (comment != null) {
                    info.setEndOfLineComment(comment, currentLine, -1);
                }
                
                currentLine++;
                
                Object value;
                if (valueStr.isEmpty()) {
                    value = parseValueInListItemContext(indent, dashIndent);
                } else {
                    value = parseScalar(valueStr);
                }
                
                itemMap.put(key, value);
                itemMap.setCommentInfo(key, info);
                continue;
            }
            
            return;
        }
    }
    
    private Object parseValueInListItemContext(int keyIndent, int dashIndent) {
        String nextLine = peekNextContent();
        if (nextLine == null) {
            return null;
        }
        
        int nextIndent = getIndent(nextLine);
        
        if (LIST_ITEM_PATTERN.matcher(nextLine).matches()) {
            if (nextIndent >= keyIndent) {
                CommentedList<Object> list = new CommentedList<>();
                parseList(list, nextIndent);
                return list;
            }
            return null;
        }
        
        if (nextIndent > keyIndent) {
            CommentedMap<String, Object> nested = new CommentedMap<>();
            nested.setDetectedIndent(detectedIndent);
            parseKeyValue(nested, nextIndent);
            return nested;
        }
        
        return null;
    }
    
    private String peekNextContent() {
        int temp = currentLine;
        while (temp < lines.size()) {
            String line = lines.get(temp);
            if (!isSkippable(line)) {
                return line;
            }
            temp++;
        }
        return null;
    }
    
    private boolean isSkippable(String line) {
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
    
    private String[] splitValueAndComment(String str) {
        if (str == null || str.isEmpty()) {
            return new String[]{"", null};
        }
        
        boolean inSingle = false, inDouble = false;
        
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\'' && !inDouble) inSingle = !inSingle;
            else if (c == '"' && !inSingle) inDouble = !inDouble;
            else if (c == '#' && !inSingle && !inDouble) {
                return new String[]{str.substring(0, i).trim(), str.substring(i).trim()};
            }
        }
        
        return new String[]{str.trim(), null};
    }
    
    private Object parseScalar(String value) {
        if (value == null || value.isEmpty()) return null;
        value = value.trim();
        
        if (value.equals("null") || value.equals("~")) return null;
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        
        // Flow Style detection
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
    
    // ==================== Flow Style Parsing ====================
    
    /**
     * Parse Flow Sequence: [a, b, c] or [1, [2, 3], {a: b}]
     */
    private List<Object> parseFlowSequence(String str) {
        CommentedList<Object> result = new CommentedList<>();
        result.setFlowStyle(true);  // Mark as Flow Style
        
        // Remove [ ]
        String content = str.substring(1, str.length() - 1).trim();
        if (content.isEmpty()) {
            return result;
        }
        
        // Split by comma (ignore commas inside nested [], {})
        List<String> items = splitFlowItems(content);
        
        for (String item : items) {
            item = item.trim();
            if (!item.isEmpty()) {
                result.add(parseFlowValue(item));
            }
        }
        
        return result;
    }
    
    /**
     * Parse Flow Mapping: {key: value, key2: value2}
     */
    private Map<String, Object> parseFlowMapping(String str) {
        CommentedMap<String, Object> result = new CommentedMap<>();
        result.setFlowStyle(true);  // Mark as Flow Style
        
        // Remove { }
        String content = str.substring(1, str.length() - 1).trim();
        if (content.isEmpty()) {
            return result;
        }
        
        // Split by comma
        List<String> pairs = splitFlowItems(content);
        
        for (String pair : pairs) {
            pair = pair.trim();
            if (pair.isEmpty()) continue;
            
            // Split key: value
            int colonIndex = findFlowColon(pair);
            if (colonIndex > 0) {
                String key = pair.substring(0, colonIndex).trim();
                String value = pair.substring(colonIndex + 1).trim();
                
                // Remove quotes from key
                if ((key.startsWith("\"") && key.endsWith("\"")) ||
                    (key.startsWith("'") && key.endsWith("'"))) {
                    key = key.substring(1, key.length() - 1);
                }
                
                result.put(key, parseFlowValue(value));
            }
        }
        
        return result;
    }
    
    /**
     * Parse Flow value (recursive for nested structures)
     */
    private Object parseFlowValue(String value) {
        if (value == null || value.isEmpty()) return null;
        value = value.trim();
        
        // Nested Flow Style
        if (value.startsWith("[") && value.endsWith("]")) {
            return parseFlowSequence(value);
        }
        if (value.startsWith("{") && value.endsWith("}")) {
            return parseFlowMapping(value);
        }
        
        // Scalar values
        if (value.equals("null") || value.equals("~")) return null;
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        
        try {
            if (value.contains(".")) return Double.parseDouble(value);
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
        }
        
        // Remove quotes
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
            (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        
        return value;
    }
    
    /**
     * Split flow content by comma (ignore commas inside nested [], {})
     */
    private List<String> splitFlowItems(String content) {
        List<String> items = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            
            // Track quote state
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            }
            
            // Track nesting depth (only outside quotes)
            if (!inSingleQuote && !inDoubleQuote) {
                if (c == '[' || c == '{') {
                    depth++;
                } else if (c == ']' || c == '}') {
                    depth--;
                }
            }
            
            // Split on comma (only at top level)
            if (c == ',' && depth == 0 && !inSingleQuote && !inDoubleQuote) {
                items.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        
        // Add last item
        if (current.length() > 0) {
            items.add(current.toString());
        }
        
        return items;
    }
    
    /**
     * Find colon position for key:value in Flow Mapping
     * (ignore colons inside nested [], {})
     */
    private int findFlowColon(String str) {
        int depth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            }
            
            if (!inSingleQuote && !inDoubleQuote) {
                if (c == '[' || c == '{') {
                    depth++;
                } else if (c == ']' || c == '}') {
                    depth--;
                } else if (c == ':' && depth == 0) {
                    return i;
                }
            }
        }
        
        return -1;
    }
}
