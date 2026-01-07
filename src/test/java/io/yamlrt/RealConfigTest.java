package io.yamlrt;

import io.yamlrt.core.CommentedMap;
import io.yamlrt.core.CommentedList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

public class RealConfigTest {

    private static final String CONFIG = """
---
ServerName: TestServer
Services:
- ServiceName: 1A1
  ServiceType: MQ
  HostToHostHeader: NO
  Airline:
  - 7C
  - AC
  - KE
  - OZ

  AutoReplyEnabled: false
  AutoReplyFlights:
    CarrierCode:
    FlightNumber:
    TestCases:


- ServiceName: 1E
  ServiceType: MQ
  HostToHostHeader: YES
  Layer5Address:
  - Source: 1EIAPP
    Airline:
    - 3U
    - 7C
    - KE
    - OZ

  AutoReplyEnabled: false
  AutoReplyFlights:
    CarrierCode:
    FlightNumber:
    TestCases:


# MQ/HTH (AS-IS & TO-BE: 1E, 1S, BX, DL, LJ, etc)

- ServiceName: TE1
  ServiceType: MQ
  HostToHostHeader: YES
  Layer5Address:
  - Source: TEIAPP
    Airline:
    - TE
    - BX

  USM:
    Layer5Address:
    - Source: BXIAPPUSM
      Destination: XAAPPKR
      Airline:
      - BX

    QRI:
      TYPE: V           # default is PTP_MSG
      QRI1: H           # default is QUERY

  AutoReplyEnabled: false
  AutoReplyFlights:
    CarrierCode:
    FlightNumber:
    TestCases:


DestinationLayer5Address: XAAPPKR         # for HTH Header: YES

AirlineCodeCheckEnabled: true             # Allowed airline code check
UnsolicitedMessageCheckEnabled: false     # USM Check

RequestTimeout: 4                         # PAXLST timeout
LocalRedisURL: redis://:eborder%40@127.0.0.1:7399/3   # Local prod server redis URL
""";

    @Test
    @DisplayName("Debug - Check Services parsing")
    @SuppressWarnings("unchecked")
    void testDebugServicesParsing() {
        Yamlrt yaml = new Yamlrt();
        CommentedMap<String, Object> root = yaml.loadYaml(CONFIG);
        
        System.out.println("=== Debug: Root keys ===");
        for (String key : root.keySet()) {
            Object value = root.get(key);
            System.out.println("Key: " + key + " -> " + (value != null ? value.getClass().getSimpleName() : "null"));
        }
        
        Object services = root.get("Services");
        System.out.println("\n=== Debug: Services ===");
        System.out.println("Services type: " + (services != null ? services.getClass().getName() : "null"));
        
        if (services instanceof List) {
            List<Object> serviceList = (List<Object>) services;
            System.out.println("Services size: " + serviceList.size());
            for (int i = 0; i < serviceList.size(); i++) {
                Object item = serviceList.get(i);
                System.out.println("  [" + i + "] type: " + (item != null ? item.getClass().getSimpleName() : "null"));
                if (item instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) item;
                    System.out.println("      keys: " + map.keySet());
                }
            }
        }
    }

    @Test
    @DisplayName("Read config values")
    @SuppressWarnings("unchecked")
    void testReadConfig() {
        Yamlrt yaml = new Yamlrt();
        CommentedMap<String, Object> root = yaml.loadYaml(CONFIG);
        
        System.out.println("=== Read Test ===");
        System.out.println("ServerName: " + root.get("ServerName"));
        
        Object servicesObj = root.get("Services");
        if (servicesObj == null) {
            System.out.println("Services is null!");
            return;
        }
        
        List<Object> services = (List<Object>) servicesObj;
        System.out.println("Services count: " + services.size());
        
        if (services.isEmpty()) {
            System.out.println("Services is empty!");
            return;
        }
        
        CommentedMap<String, Object> service0 = (CommentedMap<String, Object>) services.get(0);
        System.out.println("Services[0].ServiceName: " + service0.get("ServiceName"));
        System.out.println("Services[0].HostToHostHeader: " + service0.get("HostToHostHeader"));
        System.out.println("Services[0].Airline: " + service0.get("Airline"));
        
        if (services.size() > 1) {
            CommentedMap<String, Object> service1 = (CommentedMap<String, Object>) services.get(1);
            System.out.println("Services[1].ServiceName: " + service1.get("ServiceName"));
            
            Object layer5Obj = service1.get("Layer5Address");
            if (layer5Obj instanceof List) {
                List<Object> layer5Address = (List<Object>) layer5Obj;
                if (!layer5Address.isEmpty()) {
                    CommentedMap<String, Object> layer5_0 = (CommentedMap<String, Object>) layer5Address.get(0);
                    System.out.println("Services[1].Layer5Address[0].Source: " + layer5_0.get("Source"));
                    System.out.println("Services[1].Layer5Address[0].Airline: " + layer5_0.get("Airline"));
                }
            }
        }
        
        System.out.println("DestinationLayer5Address: " + root.get("DestinationLayer5Address"));
        System.out.println("RequestTimeout: " + root.get("RequestTimeout"));
    }
    
    @Test
    @DisplayName("Add airline to service - comments preserved")
    @SuppressWarnings("unchecked")
    void testAddAirline() {
        Yamlrt yaml = new Yamlrt();
        CommentedMap<String, Object> root = yaml.loadYaml(CONFIG);
        
        System.out.println("=== Add Airline Test ===");
        
        List<Object> services = (List<Object>) root.get("Services");
        if (services == null || services.isEmpty()) {
            System.out.println("Services is null or empty!");
            return;
        }
        
        CommentedMap<String, Object> service0 = (CommentedMap<String, Object>) services.get(0);
        List<Object> airlines = (List<Object>) service0.get("Airline");
        
        if (airlines == null) {
            System.out.println("Airline list is null!");
            return;
        }
        
        System.out.println("Before: " + airlines);
        
        // Add new airline at beginning
        airlines.add(0, "ZZ");
        
        System.out.println("After: " + airlines);
        
        // Save and check comments preserved
        String output = yaml.dump();
        System.out.println("\n=== Output ===");
        System.out.println(output);
        
        // Verify
        System.out.println("\n=== Verify ===");
        System.out.println("ZZ added: " + output.contains("- ZZ"));
        System.out.println("Block comment preserved: " + output.contains("# MQ/HTH"));
        System.out.println("Inline comment preserved: " + output.contains("# for HTH Header"));
        System.out.println("Inline comment preserved: " + output.contains("# PAXLST timeout"));
    }
    
    @Test
    @DisplayName("Modify nested Layer5Address")
    @SuppressWarnings("unchecked")
    void testModifyLayer5Address() {
        Yamlrt yaml = new Yamlrt();
        CommentedMap<String, Object> root = yaml.loadYaml(CONFIG);
        
        System.out.println("=== Modify Layer5Address Test ===");
        
        List<Object> services = (List<Object>) root.get("Services");
        if (services == null || services.size() < 2) {
            System.out.println("Services is null or has fewer than 2 items!");
            System.out.println("Services: " + services);
            return;
        }
        
        CommentedMap<String, Object> service1 = (CommentedMap<String, Object>) services.get(1);
        Object layer5Obj = service1.get("Layer5Address");
        
        if (!(layer5Obj instanceof List)) {
            System.out.println("Layer5Address is not a list: " + layer5Obj);
            return;
        }
        
        List<Object> layer5Address = (List<Object>) layer5Obj;
        if (layer5Address.isEmpty()) {
            System.out.println("Layer5Address is empty!");
            return;
        }
        
        CommentedMap<String, Object> layer5_0 = (CommentedMap<String, Object>) layer5Address.get(0);
        List<Object> airlines = (List<Object>) layer5_0.get("Airline");
        
        if (airlines == null) {
            System.out.println("Airline is null!");
            return;
        }
        
        System.out.println("Before: " + airlines);
        
        // Add new airline
        airlines.add("NEW");
        
        String output = yaml.dump();
        System.out.println("\n=== Output ===");
        System.out.println(output);
        
        System.out.println("NEW added: " + output.contains("- NEW"));
    }
    
    @Test
    @DisplayName("Modify boolean and number values")
    void testModifyValues() {
        Yamlrt yaml = new Yamlrt();
        CommentedMap<String, Object> root = yaml.loadYaml(CONFIG);
        
        System.out.println("=== Modify Values Test ===");
        
        // Change values
        root.put("AirlineCodeCheckEnabled", false);
        root.put("RequestTimeout", 10);
        
        String output = yaml.dump();
        System.out.println(output);
        
        System.out.println("\n=== Verify ===");
        System.out.println("AirlineCodeCheckEnabled changed: " + output.contains("AirlineCodeCheckEnabled: false"));
        System.out.println("RequestTimeout changed: " + output.contains("RequestTimeout: 10"));
        System.out.println("Comments preserved: " + output.contains("# Allowed airline code check"));
    }
}
