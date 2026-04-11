# KHQR Tool - Cross-Platform Desktop Application

A JavaFX desktop application for generating and decoding KHQR codes with native installer support for macOS, Windows, and Linux.

## Features

- **Generate KHQR Codes**: Create KHQR codes for payments
- **Decode KHQR Codes**: Decode KHQR codes from images or text
- **Cross-Platform**: Native installers for macOS (.dmg), Windows (.msi), and Linux (.deb/.rpm)
- **No Java Required**: Bundled JRE included in installers
- **User-Friendly Interface**: Modern JavaFX GUI with intuitive controls

## System Requirements

- **Development**: JDK 17+ with JavaFX modules
- **Runtime**: No Java installation required (JRE bundled)
- **Operating Systems**:
  - macOS 10.15+
  - Windows 10+
  - Linux (glibc 2.17+)

## Project Structure

```
khqr-tool/
├── src/main/java/com/sakcode/decodekhqr/
│   ├── MainKHQRApplication.java    # Main JavaFX application
│   └── BakongUtils.java            # KHQR utility functions
├── src/main/resources/
│   ├── icon_64.png                 # Linux application icon
│   ├── icon_1024.icns              # macOS application icon
│   └── icon-khqr.ico               # Windows application icon
├── pom.xml                         # Maven build configuration
├── .github/workflows/
│   └── build-release.yml           # GitHub Actions CI/CD
└── LICENSE                         # MIT License
```

## Quick Start

### 1. Prerequisites

- **JDK 17+** (with JavaFX support)
- **Maven 3.6+**
- **Git** (for version control)

### 2. Clone and Build

```bash
# Clone the repository
git clone https://github.com/samreachyan/khqr-tool.git
cd khqr-tool

# Build the project
mvn clean package
```

### 3. Run the Application

```bash
# Run directly with Maven
mvn javafx:run

# Or run the packaged JAR
java -jar target/decodekhqr-1.0-SNAPSHOT.jar

## OR
----

# Build and run application
mvn clean compile javafx:run

# Build macOS installer
mvn clean package jpackage:jpackage@jpackage-macos -DskipTests

# Build Windows installer (on Windows)
mvn clean package jpackage:jpackage@jpackage-windows -DskipTests

# Build Linux installer (on Linux)
mvn clean package jpackage:jpackage@jpackage-linux-deb -DskipTests
mvn clean package jpackage:jpackage@jpackage-linux-rpm -DskipTests

```

## Creating Native Installers

### Local Build (Platform-Specific)

#### macOS (.dmg)
```bash
mvn clean package jpackage:jpackage@jpackage-macos
# Installer will be at: target/jpackage/KHQR Tool-1.0-SNAPSHOT.dmg
```

#### Windows (.msi)
```bash
mvn clean package jpackage:jpackage@jpackage-windows
# Installer will be at: target/jpackage/KHQR Tool-1.0-SNAPSHOT.msi
```

#### Linux (.deb for Debian/Ubuntu)
```bash
mvn clean package jpackage:jpackage@jpackage-linux-deb
# Installer will be at: target/jpackage/decodekhqr_1.0-SNAPSHOT-1_amd64.deb
```

#### Linux (.rpm for RedHat/Fedora)
```bash
mvn clean package jpackage:jpackage@jpackage-linux-rpm
# Installer will be at: target/jpackage/decodekhqr-1.0-SNAPSHOT-1.x86_64.rpm
```

### Automated Builds with GitHub Actions

The project includes a GitHub Actions workflow that automatically builds installers for all platforms when you push a version tag:

1. **Create a version tag**:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

2. **GitHub Actions will**:
   - Build on macOS, Windows, and Linux runners
   - Create native installers for each platform
   - Create a GitHub Release with all installers
   - Upload installers as release assets

3. **Download installers** from the GitHub Releases page

## Platform-Specific Notes

### macOS
- **Icon**: Uses `icon_1024.icns` (already included)
- **Installation**: Double-click the `.dmg` file and drag to Applications
- **Signing**: For App Store distribution, set `<macSign>true</macSign>` in pom.xml

### Windows
- **Icon**: Uses `icon-khqr.ico` (already included)
- **Installation**: Run the `.msi` installer
- **Shortcuts**: Creates Start Menu and Desktop shortcuts
- **Registry**: Adds uninstaller entry in Windows Programs and Features

### Linux
- **Icons**: Uses `icon_64.png` (already included)
- **Dependencies**: Requires `fakeroot` and `rpm` for building (installed automatically in CI)
- **Package Managers**:
  - `.deb` for Debian/Ubuntu: `sudo dpkg -i khqr-tool.deb`
  - `.rpm` for RedHat/Fedora: `sudo rpm -i khqr-tool.rpm`
- **Application Menu**: Added to Utilities category

## Customization

### Changing Application Icons

Replace the icon files in `src/main/resources/`:
- `icon_64.png` - Linux icon (64x64 PNG)
- `icon_1024.icns` - macOS icon (1024x1024 ICNS)
- `icon-khqr.ico` - Windows icon (multiple sizes in ICO format)

### Updating Application Metadata

Edit the properties in `pom.xml`:
```xml
<app.name>KHQR Tool</app.name>
<app.vendor>Sakcode</app.vendor>
<app.description>KHQR Code Generator and Decoder</app.description>
<app.version>${project.version}</app.version>
```

### Adding Dependencies

Add new dependencies to the `<dependencies>` section in `pom.xml`. For non-modular JARs, the build automatically handles them via classpath.

## Troubleshooting

### Common Issues

1. **"Module not found" errors**
   - Ensure all dependencies are in the pom.xml
   - Non-modular JARs work automatically with classpath mode

2. **jpackage fails on Linux**
   - Install required packages: `sudo apt-get install fakeroot rpm`
   - Ensure you're using JDK 14+ with jpackage support

3. **Application doesn't start after installation**
   - Check system requirements (64-bit OS required)
   - Ensure antivirus isn't blocking the application
   - Try running from terminal for error messages

4. **JavaFX not found**
   - JavaFX dependencies are included via Maven
   - No separate JavaFX SDK installation needed

### Building for Specific Platforms

To build installers for a specific platform only, run the corresponding jpackage execution:
```bash
# macOS only
mvn clean package jpackage:jpackage@jpackage-macos

# Windows only  
mvn clean package jpackage:jpackage@jpackage-windows

# Linux DEB only
mvn clean package jpackage:jpackage@jpackage-linux-deb

# Linux RPM only
mvn clean package jpackage:jpackage@jpackage-linux-rpm
```

## Development

### Running Tests
```bash
mvn test
```

### Code Style
The project follows standard Java coding conventions. Use the provided Maven plugins for code quality checks.

### Contributing
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## License

MIT License - see [LICENSE](LICENSE) file for details.

## Support

For issues, questions, or contributions:
- Create an issue on GitHub
- Contact: @samreachyan

---

**Built with**: Java 17, JavaFX, Maven, jpackage, GitHub Actions

**Last Updated**: November 2024
