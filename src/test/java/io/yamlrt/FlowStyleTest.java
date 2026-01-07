package io.yamlrt;

import io.yamlrt.core.CommentedMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.*;

import java.util.*;

public class FlowStyleTest {
    
    @Test
    @DisplayName("Simple Flow Sequence: [8080, 8443, 9000]")
    void test1_simpleFlowSequence() {
        String yaml = "ports: [8080, 8443, 9000]";
        
        Yamlrt y = new Yamlrt();
        CommentedMap<String, Object> root = y.loadYaml(yaml);
        
        @SuppressWarnings("unchecked")
        List<Object> ports = (List<Object>) root.get("ports");
        
        System.out.println("Input: " + yaml);
        System.out.println("Parsed: " + ports);
        
        assertThat(ports).hasSize(3);
        assertThat(ports).containsExactly(8080L, 8443L, 9000L);
    }
    
    @Test
    @DisplayName("Simple Flow Mapping: {app: nginx, env: prod}")
    void test2_simpleFlowMapping() {
        String yaml = "labels: {app: nginx, env: prod}";
        
        Yamlrt y = new Yamlrt();
        CommentedMap<String, Object> root = y.loadYaml(yaml);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> labels = (Map<String, Object>) root.get("labels");
        
        System.out.println("Input: " + yaml);
        System.out.println("Parsed: " + labels);
        
        assertThat(labels).hasSize(2);
        assertThat(labels.get("app")).isEqualTo("nginx");
        assertThat(labels.get("env")).isEqualTo("prod");
    }
    
    @Test
    @DisplayName("Nested Flow Style: [1, [2, 3], {a: b}]")
    void test3_nestedFlowStyle() {
        String yaml = "data: [1, [2, 3], {a: b}]";
        
        Yamlrt y = new Yamlrt();
        CommentedMap<String, Object> root = y.loadYaml(yaml);
        
        @SuppressWarnings("unchecked")
        List<Object> data = (List<Object>) root.get("data");
        
        System.out.println("Input: " + yaml);
        System.out.println("Parsed: " + data);
        
        assertThat(data).hasSize(3);
        assertThat(data.get(0)).isEqualTo(1L);
        assertThat(data.get(1)).isInstanceOf(List.class);
        assertThat(data.get(2)).isInstanceOf(Map.class);
        
        @SuppressWarnings("unchecked")
        List<Object> nested = (List<Object>) data.get(1);
        assertThat(nested).containsExactly(2L, 3L);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> nestedMap = (Map<String, Object>) data.get(2);
        assertThat(nestedMap.get("a")).isEqualTo("b");
    }
    
    @Test
    @DisplayName("Block + Flow Mixed")
    void test4_blockFlowMixed() {
        String yaml = 
            "server:\n" +
            "  host: localhost\n" +
            "  ports: [8080, 8443]\n" +
            "  labels: {app: nginx, env: prod}";
        
        Yamlrt y = new Yamlrt();
        CommentedMap<String, Object> root = y.loadYaml(yaml);
        
        System.out.println("Input:\n" + yaml);
        
        @SuppressWarnings("unchecked")
        CommentedMap<String, Object> server = (CommentedMap<String, Object>) root.get("server");
        
        assertThat(server.get("host")).isEqualTo("localhost");
        
        @SuppressWarnings("unchecked")
        List<Object> ports = (List<Object>) server.get("ports");
        assertThat(ports).containsExactly(8080L, 8443L);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> labels = (Map<String, Object>) server.get("labels");
        assertThat(labels.get("app")).isEqualTo("nginx");
        assertThat(labels.get("env")).isEqualTo("prod");
    }
    
    @Test
    @DisplayName("Round-trip: Flow Style preserved")
    void test5_roundTrip() {
        String yaml = 
            "server:\n" +
            "  ports: [8080, 8443]\n" +
            "  labels: {app: nginx}";
        
        Yamlrt y = new Yamlrt();
        y.loadYaml(yaml);
        String output = y.dump();
        
        System.out.println("Input:\n" + yaml);
        System.out.println("Output:\n" + output);
        
        // Verify output can be re-parsed
        Yamlrt y2 = new Yamlrt();
        CommentedMap<String, Object> root2 = y2.loadYaml(output);
        
        @SuppressWarnings("unchecked")
        CommentedMap<String, Object> server2 = (CommentedMap<String, Object>) root2.get("server");
        
        @SuppressWarnings("unchecked")
        List<Object> ports2 = (List<Object>) server2.get("ports");
        assertThat(ports2).containsExactly(8080L, 8443L);
    }
    
    @Test
    @DisplayName("Empty Flow containers")
    void test6_emptyFlowContainers() {
        String yaml = "empty_list: []\nempty_map: {}";
        
        Yamlrt y = new Yamlrt();
        CommentedMap<String, Object> root = y.loadYaml(yaml);
        
        @SuppressWarnings("unchecked")
        List<Object> emptyList = (List<Object>) root.get("empty_list");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> emptyMap = (Map<String, Object>) root.get("empty_map");
        
        assertThat(emptyList).isEmpty();
        assertThat(emptyMap).isEmpty();
    }
    
    @Test
    @DisplayName("Flow with quoted strings")
    void test7_flowWithQuotedStrings() {
        String yaml = "data: [\"hello, world\", 'key: value']";
        
        Yamlrt y = new Yamlrt();
        CommentedMap<String, Object> root = y.loadYaml(yaml);
        
        @SuppressWarnings("unchecked")
        List<Object> data = (List<Object>) root.get("data");
        
        System.out.println("Parsed: " + data);
        
        assertThat(data).hasSize(2);
        assertThat(data.get(0)).isEqualTo("hello, world");
        assertThat(data.get(1)).isEqualTo("key: value");
    }
}
