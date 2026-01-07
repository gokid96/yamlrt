package io.yamlrt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

/**
 * New API test - README style usage
 */
public class NewApiTest {

    private static final String CONFIG = """
---
ServerName: TestServer
server:
  host: localhost
  port: 8080
Services:
- ServiceName: 1A1
  ServiceType: MQ
  HostToHostHeader: NO
  Airline:
  - 7C
  - AC
  - KE

- ServiceName: 1E
  ServiceType: MQ
  HostToHostHeader: YES
  Layer5Address:
  - Source: 1EIAPP
    Airline:
    - 3U
    - 7C

DestinationLayer5Address: XAAPPKR         # for HTH Header: YES
AirlineCodeCheckEnabled: true             # Allowed airline code check
RequestTimeout: 4                         # PAXLST timeout
""";

    @Test
    @DisplayName("Static load from string")
    void testStaticLoadFromString() {
        Yamlrt yaml = Yamlrt.load(CONFIG);
        assertNotNull(yaml);
        assertNotNull(yaml.getRoot());
        assertEquals("TestServer", yaml.getString("ServerName"));
    }

    @Test
    @DisplayName("Static load from file")
    void testStaticLoadFromFile() throws IOException {
        // Create temp file
        File tempFile = File.createTempFile("yamlrt-test", ".yaml");
        tempFile.deleteOnExit();
        
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(CONFIG);
        }
        
        Yamlrt yaml = Yamlrt.load(tempFile);
        assertNotNull(yaml);
        assertEquals("TestServer", yaml.getString("ServerName"));
    }

    @Test
    @DisplayName("Get string values with path notation")
    void testGetString() {
        Yamlrt yaml = Yamlrt.load(CONFIG);
        
        assertEquals("TestServer", yaml.getString("ServerName"));
        assertEquals("localhost", yaml.getString("server.host"));
        assertEquals("1A1", yaml.getString("Services[0].ServiceName"));
        assertEquals("MQ", yaml.getString("Services[0].ServiceType"));
        assertEquals("1E", yaml.getString("Services[1].ServiceName"));
    }

    @Test
    @DisplayName("Get int values with path notation")
    void testGetInt() {
        Yamlrt yaml = Yamlrt.load(CONFIG);
        
        assertEquals(8080, yaml.getInt("server.port"));
        assertEquals(4, yaml.getInt("RequestTimeout"));
        assertEquals(9999, yaml.getInt("nonexistent", 9999));
    }

    @Test
    @DisplayName("Get boolean values with path notation")
    void testGetBoolean() {
        Yamlrt yaml = Yamlrt.load(CONFIG);
        
        assertTrue(yaml.getBoolean("AirlineCodeCheckEnabled"));
        assertFalse(yaml.getBoolean("nonexistent", false));
    }

    @Test
    @DisplayName("Get list values with path notation")
    void testGetList() {
        Yamlrt yaml = Yamlrt.load(CONFIG);
        
        List<Object> airlines = yaml.getList("Services[0].Airline");
        assertNotNull(airlines);
        assertEquals(3, airlines.size());
        assertTrue(airlines.contains("7C"));
        assertTrue(airlines.contains("AC"));
        assertTrue(airlines.contains("KE"));
    }

    @Test
    @DisplayName("Get nested values with path notation")
    void testGetNestedPath() {
        Yamlrt yaml = Yamlrt.load(CONFIG);
        
        // Services[1].Layer5Address[0].Source
        assertEquals("1EIAPP", yaml.getString("Services[1].Layer5Address[0].Source"));
        
        // Services[1].Layer5Address[0].Airline
        List<Object> airlines = yaml.getList("Services[1].Layer5Address[0].Airline");
        assertNotNull(airlines);
        assertEquals(2, airlines.size());
    }

    @Test
    @DisplayName("Set values with path notation")
    void testSet() {
        Yamlrt yaml = Yamlrt.load(CONFIG);
        
        // Set simple value
        yaml.set("server.port", 9090);
        assertEquals(9090, yaml.getInt("server.port"));
        
        // Set nested value
        yaml.set("Services[0].ServiceType", "HTTP");
        assertEquals("HTTP", yaml.getString("Services[0].ServiceType"));
        
        // Set boolean
        yaml.set("AirlineCodeCheckEnabled", false);
        assertFalse(yaml.getBoolean("AirlineCodeCheckEnabled"));
    }

    @Test
    @DisplayName("Set list value with path notation")
    void testSetList() {
        Yamlrt yaml = Yamlrt.load(CONFIG);
        
        List<String> newAirlines = Arrays.asList("AA", "BB", "CC");
        yaml.set("Services[0].Airline", newAirlines);
        
        List<Object> airlines = yaml.getList("Services[0].Airline");
        assertEquals(3, airlines.size());
        assertEquals("AA", airlines.get(0));
    }

    @Test
    @DisplayName("Comments preserved after modifications")
    void testCommentsPreserved() {
        Yamlrt yaml = Yamlrt.load(CONFIG);
        
        // Modify values
        yaml.set("server.port", 9090);
        yaml.set("AirlineCodeCheckEnabled", false);
        
        String output = yaml.dump();
        
        // Check comments preserved
        assertTrue(output.contains("# for HTH Header: YES"), "Inline comment should be preserved");
        assertTrue(output.contains("# Allowed airline code check"), "Inline comment should be preserved");
        assertTrue(output.contains("# PAXLST timeout"), "Inline comment should be preserved");
    }

    @Test
    @DisplayName("Save to file")
    void testSaveToFile() throws IOException {
        Yamlrt yaml = Yamlrt.load(CONFIG);
        yaml.set("server.port", 9090);
        
        File tempFile = File.createTempFile("yamlrt-save-test", ".yaml");
        tempFile.deleteOnExit();
        
        yaml.save(tempFile);
        
        // Reload and verify
        Yamlrt reloaded = Yamlrt.load(tempFile);
        assertEquals(9090, reloaded.getInt("server.port"));
    }

    @Test
    @DisplayName("Create empty and build config")
    void testCreateEmpty() {
        Yamlrt yaml = Yamlrt.create();
        
        yaml.set("ServerName", "NewServer");
        yaml.set("port", 8080);
        
        assertEquals("NewServer", yaml.getString("ServerName"));
        assertEquals(8080, yaml.getInt("port"));
        
        String output = yaml.dump();
        assertTrue(output.contains("ServerName: NewServer"));
        assertTrue(output.contains("port: 8080"));
    }

    @Test
    @DisplayName("exists() method")
    void testExists() {
        Yamlrt yaml = Yamlrt.load(CONFIG);
        
        assertTrue(yaml.exists("ServerName"));
        assertTrue(yaml.exists("server.host"));
        assertTrue(yaml.exists("Services[0].ServiceName"));
        assertFalse(yaml.exists("nonexistent"));
        assertFalse(yaml.exists("server.nonexistent"));
    }

    @Test
    @DisplayName("remove() method")
    void testRemove() {
        Yamlrt yaml = Yamlrt.load(CONFIG);
        
        assertTrue(yaml.exists("server.port"));
        yaml.remove("server.port");
        assertFalse(yaml.exists("server.port"));
        
        // server.host should still exist
        assertTrue(yaml.exists("server.host"));
    }

    @Test
    @DisplayName("Full README example workflow")
    void testReadmeExample() throws IOException {
        // Create temp file with config
        File configFile = File.createTempFile("config", ".yaml");
        configFile.deleteOnExit();
        
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write(CONFIG);
        }
        
        // README style usage
        Yamlrt yaml = Yamlrt.load(configFile);
        
        // Read
        String host = yaml.getString("server.host");
        int port = yaml.getInt("server.port", 8080);
        
        assertEquals("localhost", host);
        assertEquals(8080, port);
        
        // Modify
        yaml.set("server.port", 9090);
        
        // Save (comments preserved)
        yaml.save(configFile);
        
        // Verify
        Yamlrt reloaded = Yamlrt.load(configFile);
        assertEquals(9090, reloaded.getInt("server.port"));
        
        String content = reloaded.dump();
        assertTrue(content.contains("# for HTH Header"));
    }
}
