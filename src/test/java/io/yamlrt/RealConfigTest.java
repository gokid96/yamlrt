package io.yamlrt;

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
    @DisplayName("Read config values")
    void testReadConfig() {
        Yamlrt yaml = Yamlrt.load(CONFIG);
        
        System.out.println("=== Read Test ===");
        System.out.println("ServerName: " + yaml.getString("ServerName"));
        System.out.println("Services[0].ServiceName: " + yaml.getString("Services[0].ServiceName"));
        System.out.println("Services[0].HostToHostHeader: " + yaml.getString("Services[0].HostToHostHeader"));
        System.out.println("Services[0].Airline: " + yaml.getList("Services[0].Airline"));
        System.out.println("Services[1].Layer5Address[0].Source: " + yaml.getString("Services[1].Layer5Address[0].Source"));
        System.out.println("Services[1].Layer5Address[0].Airline: " + yaml.getList("Services[1].Layer5Address[0].Airline"));
        System.out.println("DestinationLayer5Address: " + yaml.getString("DestinationLayer5Address"));
        System.out.println("RequestTimeout: " + yaml.getInt("RequestTimeout", 0));
    }
    
    @Test
    @DisplayName("Add airline to service - comments preserved")
    void testAddAirline() {
        Yamlrt yaml = Yamlrt.load(CONFIG);
        
        System.out.println("=== Add Airline Test ===");
        
        // Get current airlines
        List<Object> airlines = yaml.getList("Services[0].Airline");
        System.out.println("Before: " + airlines);
        
        // Add new airline
        List<Object> newAirlines = new ArrayList<>(airlines);
        newAirlines.add(0, "ZZ");  // Add at beginning
        yaml.set("Services[0].Airline", newAirlines);
        
        System.out.println("After: " + yaml.getList("Services[0].Airline"));
        
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
    void testModifyLayer5Address() {
        Yamlrt yaml = Yamlrt.load(CONFIG);
        
        System.out.println("=== Modify Layer5Address Test ===");
        
        // Get current airlines in Layer5Address
        List<Object> airlines = yaml.getList("Services[1].Layer5Address[0].Airline");
        System.out.println("Before: " + airlines);
        
        // Add new airline
        List<Object> newAirlines = new ArrayList<>(airlines);
        newAirlines.add("NEW");
        yaml.set("Services[1].Layer5Address[0].Airline", newAirlines);
        
        String output = yaml.dump();
        System.out.println("\n=== Output ===");
        System.out.println(output);
        
        System.out.println("NEW added: " + output.contains("- NEW"));
    }
    
    @Test
    @DisplayName("Modify boolean and number values")
    void testModifyValues() {
        Yamlrt yaml = Yamlrt.load(CONFIG);
        
        System.out.println("=== Modify Values Test ===");
        
        // Change values
        yaml.set("AirlineCodeCheckEnabled", false);
        yaml.set("RequestTimeout", 10);
        
        String output = yaml.dump();
        System.out.println(output);
        
        System.out.println("\n=== Verify ===");
        System.out.println("AirlineCodeCheckEnabled changed: " + output.contains("AirlineCodeCheckEnabled: false"));
        System.out.println("RequestTimeout changed: " + output.contains("RequestTimeout: 10"));
        System.out.println("Comments preserved: " + output.contains("# Allowed airline code check"));
    }
}
