# yamlrt

YAML library for Java that preserves comments and formatting.

## Setup

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.gokid96:yamlrt:v0.1.0'
}
```

## Usage

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

## Features

- Comment preservation (inline, block)
- Blank line preservation
- Flow style support & preservation (`[a, b]`, `{k: v}`)
- Key order preservation

## Not Supported

- Multi-line strings (`|`, `>`)
- Anchors (`&`, `*`)
