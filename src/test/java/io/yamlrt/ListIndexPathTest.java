package io.yamlrt;

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
    void testListIndexAccess() {
        Yamlrt y = Yamlrt.load(CONFIG);
        
        System.out.println("ServerName: " + y.getString("ServerName"));
        System.out.println("Services[0].ServiceName: " + y.getString("Services[0].ServiceName"));
        System.out.println("Services[0].ServiceType: " + y.getString("Services[0].ServiceType"));
        System.out.println("Services[1].ServiceName: " + y.getString("Services[1].ServiceName"));
        
        assertThat(y.getString("ServerName")).isEqualTo("TestServer");
        assertThat(y.getString("Services[0].ServiceName")).isEqualTo("1A1");
        assertThat(y.getString("Services[0].ServiceType")).isEqualTo("MQ");
        assertThat(y.getString("Services[1].ServiceName")).isEqualTo("1E");
    }
    
    @Test
    @DisplayName("List index path: Services[0].Airline")
    void testNestedListAccess() {
        Yamlrt y = Yamlrt.load(CONFIG);
        
        List<Object> airlines = y.getList("Services[0].Airline");
        System.out.println("Services[0].Airline: " + airlines);
        
        assertThat(airlines).containsExactly("7C", "AC", "KE");
    }
    
    @Test
    @DisplayName("Set value with list index path")
    void testSetWithListIndex() {
        Yamlrt y = Yamlrt.load(CONFIG);
        
        // Modify
        y.set("Services[0].ServiceName", "MODIFIED");
        
        // Add new airline
        List<Object> airlines = new ArrayList<>(y.getList("Services[0].Airline"));
        airlines.add(0, "NEW");
        y.set("Services[0].Airline", airlines);
        
        System.out.println("Modified output:");
        System.out.println(y.dump());
        
        assertThat(y.getString("Services[0].ServiceName")).isEqualTo("MODIFIED");
        assertThat(y.getList("Services[0].Airline")).contains("NEW");
    }
}
