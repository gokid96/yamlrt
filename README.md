# yamlrt

YAML library for Java that preserves comments and formatting.

## Setup

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.gokid96:yamlrt:v0.1.2'
}
```

## Usage

```java
// Load from file
Yamlrt yaml = Yamlrt.load(new File("config.yaml"));

// Load from string
Yamlrt yaml = Yamlrt.load(yamlString);

// Read values (path notation supported)
String host = yaml.getString("server.host");
int port = yaml.getInt("server.port", 8080);
boolean enabled = yaml.getBoolean("feature.enabled");
List<Object> items = yaml.getList("items");

// Access nested values with path notation
String serviceName = yaml.getString("Services[0].ServiceName");
String source = yaml.getString("Services[1].Layer5Address[0].Source");

// Modify values
yaml.set("server.port", 9090);
yaml.set("Services[0].Airline", newAirlineList);

// Check existence and remove
if (yaml.exists("old.config")) {
    yaml.remove("old.config");
}

// Save (comments preserved)
yaml.save(new File("config.yaml"));

// Or dump to string
String output = yaml.dump();
```

### Create new config

```java
Yamlrt yaml = Yamlrt.create();
yaml.set("ServerName", "MyServer");
yaml.set("port", 8080);
yaml.save(new File("new-config.yaml"));
```

### Direct root access

```java
// For advanced manipulation
CommentedMap<String, Object> root = yaml.getRoot();
root.put("key", "value");
root.setEolComment("key", "# inline comment");
```

## Features

- Comment preservation (inline, block)
- Blank line preservation
- Flow style support & preservation (`[a, b]`, `{k: v}`)
- Key order preservation
- Path notation for nested access (`server.host`, `list[0].key`)

## Not Supported

- Multi-line strings (`|`, `>`)
- Anchors (`&`, `*`)
