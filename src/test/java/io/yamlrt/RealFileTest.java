package io.yamlrt;

import org.junit.jupiter.api.Test;
import java.nio.file.*;

public class RealFileTest {

    private static final String FILE_PATH = 
        "C:\\Users\\eborder\\gitea\\eborder-system\\dashboard-backend\\src\\main\\resources\\config\\airlineMessageHandler_v3.conf.20260107153728";

    @Test
    void testRoundTrip() throws Exception {
        String original = Files.readString(Path.of(FILE_PATH));
        
        System.out.println("Original lines: " + original.split("\n", -1).length);
        
        // Load
        Yamlrt yaml = Yamlrt.load(original);
        
        // Dump without modification
        String output = yaml.dump();
        
        System.out.println("Output lines: " + output.split("\n", -1).length);
        
        // Check key elements
        System.out.println("\n=== VERIFY ===");
        System.out.println("Contains TE1: " + output.contains("ServiceName: TE1"));
        System.out.println("Contains TE2: " + output.contains("ServiceName: TE2"));
        System.out.println("Contains TE3: " + output.contains("ServiceName: TE3"));
        System.out.println("Contains JU1: " + output.contains("ServiceName: JU1"));
        System.out.println("Contains SH1: " + output.contains("ServiceName: SH1"));
        System.out.println("Contains USM: " + output.contains("USM:"));
        System.out.println("Contains QRI: " + output.contains("QRI:"));
        System.out.println("Contains # MQ/HTH: " + output.contains("# MQ/HTH"));
        System.out.println("Contains # MQ/No HTH: " + output.contains("# MQ/No HTH"));
        System.out.println("Contains # Direct MATIP: " + output.contains("# Direct MATIP"));
        System.out.println("Contains DestinationLayer5Address: " + output.contains("DestinationLayer5Address:"));
        System.out.println("Contains LocalRedisURL: " + output.contains("LocalRedisURL:"));
        System.out.println("Contains PFMSEnabled: " + output.contains("PFMSEnabled:"));
        
        // Show diff
        String[] origLines = original.split("\n", -1);
        String[] outLines = output.split("\n", -1);
        
        System.out.println("\n=== DIFF (first 20 differences) ===");
        int diffCount = 0;
        int maxLines = Math.max(origLines.length, outLines.length);
        for (int i = 0; i < maxLines && diffCount < 20; i++) {
            String orig = i < origLines.length ? origLines[i] : "<missing>";
            String out = i < outLines.length ? outLines[i] : "<missing>";
            
            if (!orig.equals(out)) {
                System.out.println("Line " + (i+1) + ":");
                System.out.println("  ORIG: '" + orig + "'");
                System.out.println("  OUT:  '" + out + "'");
                diffCount++;
            }
        }
        
        if (diffCount == 0) {
            System.out.println("NO DIFFERENCES! Perfect round-trip!");
        }
    }
    
    @Test
    void testAddAirline() throws Exception {
        String original = Files.readString(Path.of(FILE_PATH));
        
        Yamlrt yaml = Yamlrt.load(original);
        
        // Add airline to 1A1 service
        @SuppressWarnings("unchecked")
        java.util.List<Object> services = yaml.getList("Services");
        
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> service1A1 = (java.util.Map<String, Object>) services.get(0);
        
        @SuppressWarnings("unchecked")
        java.util.List<Object> airlines = (java.util.List<Object>) service1A1.get("Airline");
        
        System.out.println("Before: " + airlines.size() + " airlines");
        airlines.add(0, "1Z");
        System.out.println("After: " + airlines.size() + " airlines");
        
        String output = yaml.dump();
        
        // Verify all services preserved
        System.out.println("\n=== VERIFY ===");
        System.out.println("Contains 1Z: " + output.contains("- 1Z"));
        System.out.println("Contains TE1: " + output.contains("ServiceName: TE1"));
        System.out.println("Contains TE2: " + output.contains("ServiceName: TE2"));
        System.out.println("Contains TE3: " + output.contains("ServiceName: TE3"));
        System.out.println("Contains JU1: " + output.contains("ServiceName: JU1"));
        System.out.println("Contains SH1: " + output.contains("ServiceName: SH1"));
        System.out.println("Contains USM: " + output.contains("USM:"));
        System.out.println("Contains QRI: " + output.contains("QRI:"));
        System.out.println("Contains DestinationLayer5Address: " + output.contains("DestinationLayer5Address:"));
        System.out.println("Contains PFMSEnabled: " + output.contains("PFMSEnabled:"));
        System.out.println("Contains LocalRedisURL: " + output.contains("LocalRedisURL:"));
        
        System.out.println("\nOutput lines: " + output.split("\n", -1).length);
    }
}
