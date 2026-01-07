package io.yamlrt;

import io.yamlrt.core.CommentedMap;
import io.yamlrt.core.YamlParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

public class SimpleListTest {

    @Test
    @DisplayName("Final - nested map with blank lines")
    @SuppressWarnings("unchecked")
    void testNestedMapWithBlankLines() {
        String yaml = """
Items:
- Name: First
  Nested:
    Key1:


- Name: Second
""";
        
        Yamlrt y = new Yamlrt();
        CommentedMap<String, Object> root = y.load(yaml);
        
        java.util.List<Object> items = (java.util.List<Object>) root.get("Items");
        
        System.out.println("Items count: " + items.size());
        org.junit.jupiter.api.Assertions.assertEquals(2, items.size(), "Should parse 2 items");
        
        java.util.Map<String, Object> item0 = (java.util.Map<String, Object>) items.get(0);
        java.util.Map<String, Object> item1 = (java.util.Map<String, Object>) items.get(1);
        
        org.junit.jupiter.api.Assertions.assertEquals("First", item0.get("Name"));
        org.junit.jupiter.api.Assertions.assertEquals("Second", item1.get("Name"));
        
        System.out.println("[0] Name: " + item0.get("Name"));
        System.out.println("[1] Name: " + item1.get("Name"));
    }
}
