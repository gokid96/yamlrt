package io.yamlrt;

import io.yamlrt.core.*;

/**
 * yamlrt - YAML Round-Trip Library (ruamel.yaml style)
 * 
 * 주석과 포맷을 완벽하게 보존하는 YAML 라이브러리
 * 
 * Usage:
 *   Yamlrt yaml = new Yamlrt();
 *   CommentedMap<String, Object> data = yaml.load(yamlString);
 *   data.put("key", "newValue");
 *   String output = yaml.dump();
 */
public class Yamlrt {
    
    private YamlParser parser;
    private YamlWriter writer;
    private CommentedMap<String, Object> root;
    
    public Yamlrt() {
        this.parser = new YamlParser();
        this.writer = new YamlWriter();
    }
    
    /**
     * Load YAML string and return root map
     */
    public CommentedMap<String, Object> load(String yaml) {
        this.root = parser.parse(yaml);
        return root;
    }
    
    /**
     * Dump root map to YAML string
     */
    public String dump() {
        if (root == null) {
            throw new IllegalStateException("No YAML loaded. Call load() first.");
        }
        writer.setDocumentMarker(root.hasDocumentMarker());
        return writer.write(root);
    }
    
    /**
     * Dump given map to YAML string
     */
    public String dump(CommentedMap<String, Object> data) {
        writer.setDocumentMarker(data.hasDocumentMarker());
        return writer.write(data);
    }
    
    /**
     * Get root map
     */
    public CommentedMap<String, Object> getRoot() {
        return root;
    }
    
    /**
     * Set root map
     */
    public void setRoot(CommentedMap<String, Object> root) {
        this.root = root;
    }
    
    /**
     * Quick round-trip test
     */
    public static String roundTrip(String yaml) {
        Yamlrt yamlrt = new Yamlrt();
        yamlrt.load(yaml);
        return yamlrt.dump();
    }
}
