package io.yamlrt;

import io.yamlrt.core.CommentedMap;
import io.yamlrt.core.YamlParser;
import org.junit.jupiter.api.Test;

/**
 * Debug parsing issue
 */
public class ParserDebugTest {

    private static final String PROBLEM_YAML = """
---
Services:
- ServiceName: TE1
  USM:
    Layer5Address:
    - Source: BXIAPPUSM
      Airline:
      - BX

    QRI:
      TYPE: V

  AutoReplyEnabled: false
""";

    @Test
    void testDebugParsing() {
        System.out.println("========== INPUT ==========");
        System.out.println(PROBLEM_YAML);
        
        YamlParser parser = new YamlParser();
        parser.setDebug(true);
        
        System.out.println("\n========== PARSING ==========");
        CommentedMap<String, Object> root = parser.parse(PROBLEM_YAML);
        
        System.out.println("\n========== RESULT ==========");
        printMap(root, 0);
    }
    
    @SuppressWarnings("unchecked")
    private void printMap(Object obj, int indent) {
        String prefix = "  ".repeat(indent);
        if (obj instanceof java.util.Map) {
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) obj;
            for (java.util.Map.Entry<String, Object> e : map.entrySet()) {
                System.out.println(prefix + e.getKey() + ":");
                printMap(e.getValue(), indent + 1);
            }
        } else if (obj instanceof java.util.List) {
            java.util.List<Object> list = (java.util.List<Object>) obj;
            for (int i = 0; i < list.size(); i++) {
                System.out.println(prefix + "[" + i + "]:");
                printMap(list.get(i), indent + 1);
            }
        } else {
            System.out.println(prefix + "= " + obj);
        }
    }
}
