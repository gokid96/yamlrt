package io.yamlrt;

import io.yamlrt.core.CommentedMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

public class RoundTripQualityTest {

    @Test
    @DisplayName("Round-trip: exact match without modification")
    void testExactRoundTrip() {
        String original = """
---
ServerName: TestServer
Port: 8080

# Database settings
Database:
  Host: localhost
  Port: 5432         # PostgreSQL default
""";
        
        Yamlrt yaml = new Yamlrt();
        CommentedMap<String, Object> root = yaml.loadYaml(original);
        String output = yaml.dump();
        
        System.out.println("=== Original ===");
        System.out.println(original);
        System.out.println("=== Output ===");
        System.out.println(output);
        
        assertEquals(original.trim(), output.trim(), "Round-trip should produce exact match");
    }
    
    @Test
    @DisplayName("Round-trip: list with blank lines preserved")
    void testListBlankLinesPreserved() {
        String original = """
Services:
- Name: First
  Value: 1


- Name: Second
  Value: 2
""";
        
        Yamlrt yaml = new Yamlrt();
        CommentedMap<String, Object> root = yaml.loadYaml(original);
        String output = yaml.dump();
        
        System.out.println("=== Original (repr) ===");
        System.out.println(original.replace("\n", "\\n\n"));
        System.out.println("=== Output (repr) ===");
        System.out.println(output.replace("\n", "\\n\n"));
        
        // Trim trailing newlines for comparison
        String originalTrimmed = original.replaceAll("\\n+$", "");
        String outputTrimmed = output.replaceAll("\\n+$", "");
        
        System.out.println("=== Trimmed comparison ===");
        System.out.println("Original trimmed length: " + originalTrimmed.length());
        System.out.println("Output trimmed length: " + outputTrimmed.length());
        
        assertEquals(originalTrimmed, outputTrimmed, "Content should match (ignoring trailing newlines)");
    }
    
    @Test
    @DisplayName("Round-trip: inline comments preserved")
    void testInlineCommentsPreserved() {
        String original = """
Key1: value1         # first comment
Key2: value2         # second comment
Key3: value3
""";
        
        Yamlrt yaml = new Yamlrt();
        CommentedMap<String, Object> root = yaml.loadYaml(original);
        String output = yaml.dump();
        
        System.out.println("=== Original ===");
        System.out.println(original);
        System.out.println("=== Output ===");
        System.out.println(output);
        
        assertTrue(output.contains("# first comment"), "First inline comment should be preserved");
        assertTrue(output.contains("# second comment"), "Second inline comment should be preserved");
    }
    
    @Test
    @DisplayName("Round-trip: block comments preserved")  
    void testBlockCommentsPreserved() {
        String original = """
# Header comment
Key1: value1

# Section comment
# Multi-line
Key2: value2
""";
        
        Yamlrt yaml = new Yamlrt();
        CommentedMap<String, Object> root = yaml.loadYaml(original);
        String output = yaml.dump();
        
        System.out.println("=== Original ===");
        System.out.println(original);
        System.out.println("=== Output ===");
        System.out.println(output);
        
        assertTrue(output.contains("# Header comment"), "Header comment should be preserved");
        assertTrue(output.contains("# Section comment"), "Section comment should be preserved");
        assertTrue(output.contains("# Multi-line"), "Multi-line comment should be preserved");
    }
    
    @Test
    @DisplayName("Round-trip: nested list in compact format")
    void testNestedListCompact() {
        String original = """
Services:
- ServiceName: Test
  Airline:
  - AA
  - BB
  - CC
""";
        
        Yamlrt yaml = new Yamlrt();
        CommentedMap<String, Object> root = yaml.loadYaml(original);
        String output = yaml.dump();
        
        System.out.println("=== Original ===");
        System.out.println(original);
        System.out.println("=== Output ===");
        System.out.println(output);
        
        assertTrue(output.contains("- AA"), "AA should be preserved");
        assertTrue(output.contains("- BB"), "BB should be preserved");
        assertTrue(output.contains("- CC"), "CC should be preserved");
    }
    
    @Test
    @DisplayName("Round-trip: modify value preserves comments")
    void testModifyPreservesComments() {
        String original = """
Port: 8080           # Server port
Timeout: 30          # Request timeout
""";
        
        Yamlrt yaml = new Yamlrt();
        CommentedMap<String, Object> root = yaml.loadYaml(original);
        
        // Modify
        root.put("Port", 9090);
        root.put("Timeout", 60);
        
        String output = yaml.dump();
        
        System.out.println("=== Original ===");
        System.out.println(original);
        System.out.println("=== Modified Output ===");
        System.out.println(output);
        
        assertTrue(output.contains("Port: 9090"), "Port should be changed to 9090");
        assertTrue(output.contains("Timeout: 60"), "Timeout should be changed to 60");
        assertTrue(output.contains("# Server port"), "Port comment should be preserved");
        assertTrue(output.contains("# Request timeout"), "Timeout comment should be preserved");
    }
    
    @Test
    @DisplayName("Round-trip: add to list preserves structure")
    @SuppressWarnings("unchecked")
    void testAddToListPreservesStructure() {
        String original = """
Airlines:
- AA
- BB
""";
        
        Yamlrt yaml = new Yamlrt();
        CommentedMap<String, Object> root = yaml.loadYaml(original);
        
        java.util.List<Object> airlines = (java.util.List<Object>) root.get("Airlines");
        airlines.add("CC");
        
        String output = yaml.dump();
        
        System.out.println("=== Original ===");
        System.out.println(original);
        System.out.println("=== Modified Output ===");
        System.out.println(output);
        
        assertTrue(output.contains("- AA"), "AA should be preserved");
        assertTrue(output.contains("- BB"), "BB should be preserved");
        assertTrue(output.contains("- CC"), "CC should be added");
    }
}
