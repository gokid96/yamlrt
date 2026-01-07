package io.yamlrt;

import io.yamlrt.core.CommentedMap;
import io.yamlrt.core.CommentedList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.*;

import java.util.*;

public class ListIndexPathTest {
    
    private static final String CONFIG = """
ServerName: TestServer
Services:
  - ServiceName: 1A1
    ServiceType: MQ
    Airline:
      - 7C
      - AC
      - KE
  - ServiceName: 1E
    ServiceType: MQ
    Airline:
      - 3U
      - OZ
""";

    @Test
    @DisplayName("List index path: Services[0].ServiceName")
    @SuppressWarnings("unchecked")
    void testListIndexAccess() {
        Yamlrt y = new Yamlrt();
        CommentedMap<String, Object> root = y.load(CONFIG);
        
        List<Object> services = (List<Object>) root.get("Services");
        CommentedMap<String, Object> service0 = (CommentedMap<String, Object>) services.get(0);
        CommentedMap<String, Object> service1 = (CommentedMap<String, Object>) services.get(1);
        
        System.out.println("ServerName: " + root.get("ServerName"));
        System.out.println("Services[0].ServiceName: " + service0.get("ServiceName"));
        System.out.println("Services[0].ServiceType: " + service0.get("ServiceType"));
        System.out.println("Services[1].ServiceName: " + service1.get("ServiceName"));
        
        assertThat(root.get("ServerName")).isEqualTo("TestServer");
        assertThat(service0.get("ServiceName")).isEqualTo("1A1");
        assertThat(service0.get("ServiceType")).isEqualTo("MQ");
        assertThat(service1.get("ServiceName")).isEqualTo("1E");
    }
    
    @Test
    @DisplayName("List index path: Services[0].Airline")
    @SuppressWarnings("unchecked")
    void testNestedListAccess() {
        Yamlrt y = new Yamlrt();
        CommentedMap<String, Object> root = y.load(CONFIG);
        
        List<Object> services = (List<Object>) root.get("Services");
        CommentedMap<String, Object> service0 = (CommentedMap<String, Object>) services.get(0);
        List<Object> airlines = (List<Object>) service0.get("Airline");
        
        System.out.println("Services[0].Airline: " + airlines);
        
        assertThat(airlines).containsExactly("7C", "AC", "KE");
    }
    
    @Test
    @DisplayName("Set value with list index path")
    @SuppressWarnings("unchecked")
    void testSetWithListIndex() {
        Yamlrt y = new Yamlrt();
        CommentedMap<String, Object> root = y.load(CONFIG);
        
        List<Object> services = (List<Object>) root.get("Services");
        CommentedMap<String, Object> service0 = (CommentedMap<String, Object>) services.get(0);
        
        // Modify
        service0.put("ServiceName", "MODIFIED");
        
        // Add new airline
        List<Object> airlines = (List<Object>) service0.get("Airline");
        airlines.add(0, "NEW");
        
        System.out.println("Modified output:");
        System.out.println(y.dump());
        
        assertThat(service0.get("ServiceName")).isEqualTo("MODIFIED");
        assertThat(airlines).contains("NEW");
    }
}
