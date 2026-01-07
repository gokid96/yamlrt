package io.yamlrt;

import io.yamlrt.core.CommentedMap;
import org.junit.jupiter.api.Test;

import java.util.*;

/**
 * Real config round-trip debug test
 */
public class RealConfigDebugTest {

    private static final String REAL_CONFIG = """
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


# MQ/No HTH (TO-BE: 1A, XS, UA, 1T)

- ServiceName: TE2
  ServiceType: MQ
  HostToHostHeader: NO
  Airline:
    - TE
    - 7C
    - MM

  AutoReplyEnabled: true
  AutoReplyFlights:
    CarrierCode:
    FlightNumber:
    TestCases:


DestinationLayer5Address: XAAPPKR         # for HTH Header: YES
AirlineCodeCheckEnabled: true             # Allowed airline code check
RequestTimeout: 4                         # PAXLST timeout
""";

    @Test
    void testRoundTripDebug() {
        System.out.println("========== ORIGINAL ==========");
        System.out.println(REAL_CONFIG);
        System.out.println("========== ORIGINAL END ==========\n");
        
        // Load
        Yamlrt yaml = Yamlrt.load(REAL_CONFIG);
        CommentedMap<String, Object> root = yaml.getRoot();
        
        System.out.println("========== PARSED STRUCTURE ==========");
        printStructure(root, 0);
        System.out.println("========== PARSED STRUCTURE END ==========\n");
        
        // Dump without modification
        String output = yaml.dump();
        System.out.println("========== OUTPUT (no modification) ==========");
        System.out.println(output);
        System.out.println("========== OUTPUT END ==========\n");
        
        // Check differences
        System.out.println("========== DIFF CHECK ==========");
        String[] origLines = REAL_CONFIG.split("\n", -1);
        String[] outLines = output.split("\n", -1);
        
        System.out.println("Original lines: " + origLines.length);
        System.out.println("Output lines: " + outLines.length);
        
        int maxLines = Math.max(origLines.length, outLines.length);
        for (int i = 0; i < maxLines; i++) {
            String orig = i < origLines.length ? origLines[i] : "<missing>";
            String out = i < outLines.length ? outLines[i] : "<missing>";
            
            if (!orig.equals(out)) {
                System.out.println("Line " + (i+1) + " DIFFERS:");
                System.out.println("  ORIG: '" + orig + "'");
                System.out.println("  OUT:  '" + out + "'");
            }
        }
        System.out.println("========== DIFF CHECK END ==========");
    }
    
    @Test
    void testAddAirlineAndDump() {
        System.out.println("========== ADD AIRLINE TEST ==========");
        
        Yamlrt yaml = Yamlrt.load(REAL_CONFIG);
        
        // Add airline to 1A1
        @SuppressWarnings("unchecked")
        List<Object> services = yaml.getList("Services");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> service1A1 = (Map<String, Object>) services.get(0);
        
        @SuppressWarnings("unchecked")
        List<Object> airlines = (List<Object>) service1A1.get("Airline");
        
        System.out.println("Before: " + airlines);
        airlines.add(0, "1Z");
        System.out.println("After: " + airlines);
        
        String output = yaml.dump();
        System.out.println("\n========== OUTPUT ==========");
        System.out.println(output);
        System.out.println("========== OUTPUT END ==========");
        
        // Verify structure preserved
        System.out.println("\n========== VERIFY ==========");
        System.out.println("Contains TE1: " + output.contains("ServiceName: TE1"));
        System.out.println("Contains TE2: " + output.contains("ServiceName: TE2"));
        System.out.println("Contains USM: " + output.contains("USM:"));
        System.out.println("Contains QRI: " + output.contains("QRI:"));
        System.out.println("Contains # MQ/HTH: " + output.contains("# MQ/HTH"));
        System.out.println("Contains # MQ/No HTH: " + output.contains("# MQ/No HTH"));
        System.out.println("Contains DestinationLayer5Address: " + output.contains("DestinationLayer5Address:"));
    }
    
    @SuppressWarnings("unchecked")
    private void printStructure(Object obj, int indent) {
        String indentStr = "  ".repeat(indent);
        
        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                System.out.println(indentStr + entry.getKey() + ":");
                printStructure(entry.getValue(), indent + 1);
            }
        } else if (obj instanceof List) {
            List<Object> list = (List<Object>) obj;
            for (int i = 0; i < list.size(); i++) {
                System.out.println(indentStr + "[" + i + "]:");
                printStructure(list.get(i), indent + 1);
            }
        } else {
            System.out.println(indentStr + "= " + obj + " (" + (obj != null ? obj.getClass().getSimpleName() : "null") + ")");
        }
    }
}
