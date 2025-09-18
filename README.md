# KXAPI - Extended Kotlin API for BotWithUs

[![Kotlin Version](https://img.shields.io/badge/kotlin-2.2.0-blue.svg)](https://kotlinlang.org/)
[![JVM Target](https://img.shields.io/badge/JVM-24-green.svg)](https://openjdk.java.net/)
[![Version](https://img.shields.io/badge/version-0.1--SNAPSHOT-orange.svg)](https://github.com/botwithus/kxapi)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Lines of Code](https://img.shields.io/endpoint?url=https://ghloc.vercel.app/api/BotWithUs/BWU-V2-XAPI-KT/badge&color=blueviolet&label=Lines%20of%20Code)](https://github.com/BotWithUs/BWU-V2-XAPI-KT)

A powerful and intuitive Kotlin DSL for building ImGui-based user interfaces in BotWithUs RuneScape 3 bot development. KXAPI provides a fluent, type-safe interface that makes UI development clean, readable, and maintainable.

## 📦 Installation

<details>
<summary><strong>Maven</strong></summary>

Add the following to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>botwithus-snapshots</id>
        <url>https://nexus.botwithus.net/repository/maven-snapshots/</url>
    </repository>
    <repository>
        <id>botwithus-releases</id>
        <url>https://nexus.botwithus.net/repository/maven-releases/</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>net.botwithus.kxapi</groupId>
        <artifactId>kxapi</artifactId>
        <version>0.1-SNAPSHOT</version>
    </dependency>
</dependencies>
```

</details>

<details open>
<summary><strong>Gradle</strong></summary>

Add the following to your `build.gradle.kts`:

```kotlin
repositories {
    maven("https://nexus.botwithus.net/repository/maven-snapshots/")
    maven("https://nexus.botwithus.net/repository/maven-releases/")
}

dependencies {
    implementation("net.botwithus.kxapi:kxapi:0.1-SNAPSHOT")
}
```

</details>

## 🔧 Requirements

- **Kotlin**: 2.2.0+
- **JVM**: 24+
- **BotWithUs API**: 1.0+
- **ImGui**: 1.0+

## 📖 Documentation

For detailed documentation and examples, visit our [Wiki](https://github.com/botwithus/kxapi/wiki).

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🔗 Links

- **Source Code**: [GitHub Repository](https://github.com/BotWithUs/BWU-V2-XAPI-KT)
- **Issues**: [GitHub Issues](https://github.com/BotWithUs/BWU-V2-XAPI-KT/issues)
- **BotWithUs**: [Official Website](https://botwithus.net)
- **Documentation**: [Wiki](https://github.com/BotWithUs/BWU-V2-XAPI-KT/wiki)

---

**Made with ❤️ by the BotWithUs team**
