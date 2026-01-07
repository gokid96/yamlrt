package io.yamlrt;

import io.yamlrt.core.CommentedMap;
import io.yamlrt.core.CommentedList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

public class RoundTripTest {

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
    QRI:
      TYPE: V  # default is PTP_MSG
      QRI1: H  # default is QUERY

  AutoReplyEnabled: false
  AutoReplyFlights:
    CarrierCode:
    FlightNumber:
    TestCases:


DestinationLayer5Address: XAAPPKR  # for HTH Header: YES
AirlineCodeCheckEnabled: true  # Allowed airline code check
RequestTimeout: 4  # PAXLST timeout
""";

    @Test
    @DisplayName("Round-trip: load and dump without changes")
    void testRoundTripNoChanges() {
        Yamlrt yaml = new Yamlrt();
        yaml.load(CONFIG);
        String output = yaml.dump();
        
        System.out.println("=== Original ===");
        System.out.println(CONFIG);
        System.out.println("\n=== Output ===");
        System.out.println(output);
        
        // Verify key elements
        System.out.println("\n=== Verification ===");
        verify("Document marker (---)", output.startsWith("---"));
        verify("ServerName", output.contains("ServerName: TestServer"));
        verify("Compact list format (- ServiceName:)", output.contains("- ServiceName: 1A1"));
        verify("Block comment preserved", output.contains("# MQ/HTH"));
        verify("Inline comment preserved", output.contains("# for HTH Header"));
        verify("Inline comment preserved", output.contains("# default is PTP_MSG"));
    }
    
    @Test
    @DisplayName("Round-trip: add airline and preserve format")
    @SuppressWarnings("unchecked")
    void testAddAirline() {
        Yamlrt yaml = new Yamlrt();
        CommentedMap<String, Object> root = yaml.load(CONFIG);
        
        // Navigate to Services[0].Airline
        List<Object> services = (List<Object>) root.get("Services");
        CommentedMap<String, Object> service0 = (CommentedMap<String, Object>) services.get(0);
        List<Object> airlines = (List<Object>) service0.get("Airline");
        
        System.out.println("Before: " + airlines);
        
        // Add airline at beginning
        airlines.add(0, "Z1");
        
        String output = yaml.dump();
        
        System.out.println("=== Output after adding Z1 ===");
        System.out.println(output);
        
        System.out.println("\n=== Verification ===");
        verify("Z1 added", output.contains("- Z1"));
        verify("Document marker (---)", output.startsWith("---"));
        verify("Compact list format", output.contains("- ServiceName: 1A1"));
        verify("Block comment preserved", output.contains("# MQ/HTH"));
        verify("Inline comment preserved", output.contains("# for HTH Header"));
    }
    
    @Test
    @DisplayName("Round-trip: modify value and preserve format")
    void testModifyValue() {
        Yamlrt yaml = new Yamlrt();
        CommentedMap<String, Object> root = yaml.load(CONFIG);
        
        root.put("RequestTimeout", 10);
        root.put("AirlineCodeCheckEnabled", false);
        
        String output = yaml.dump();
        
        System.out.println("=== Output after modifications ===");
        System.out.println(output);
        
        System.out.println("\n=== Verification ===");
        verify("RequestTimeout changed", output.contains("RequestTimeout: 10"));
        verify("AirlineCodeCheckEnabled changed", output.contains("AirlineCodeCheckEnabled: false"));
        verify("Inline comment preserved", output.contains("# PAXLST timeout"));
        verify("Inline comment preserved", output.contains("# Allowed airline code check"));
    }
    
    private void verify(String name, boolean passed) {
        System.out.println(name + ": " + (passed ? "PASS" : "FAIL"));
    }
}
