# yamlrt

Round-trip YAML editing library for Java with comment and formatting preservation.

### Overview

When modifying YAML files with SnakeYAML or Jackson, comments are lost:

```yaml
# Original
server:
  port: 8080  # production port
```

```yaml
# After modification with SnakeYAML
server:
  port: 9090
# comments gone
```

yamlrt preserves comments, blank lines, and formatting during modifications.

### Features

* Inline comment preservation
* Block comment preservation  
* Blank line preservation
* Indent style detection (2/4 spaces)
* Key order preservation
* Nested Map/List support

### Setup

JitPack:

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.gokid96:yamlrt:0.1.0'
}
```

### Usage

```java
Yamlrt yaml = Yamlrt.load(new File("config.yaml"));

// Read
String host = yaml.getString("server.host");
int port = yaml.getInt("server.port", 8080);

// Modify
yaml.set("server.port", 9090);

// Save (comments preserved)
yaml.save(new File("config.yaml"));
```

Path syntax:
* `server.host` - nested map
* `services[0]` - list index
* `services[0].name` - map inside list

### Comparison

| | yamlrt | SnakeYAML | Jackson |
|---|---|---|---|
| Comment preservation | O | X | X |
| Format preservation | O | X | X |
| POJO mapping | X | O | O |

### Limitations

Not yet supported:
* Flow style (`[a, b, c]`, `{key: value}`)
* Multi-line strings (`|`, `>`)
* Anchors (`&`, `*`)