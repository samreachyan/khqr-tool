# KHQR Tool

A JavaFX desktop application for generating and decoding [Bakong KHQR](https://bakong.nbc.gov.kh/) codes, with a branded QR card preview built to the official KHQR Card Appearance Guideline.

## Key Features

- **Generate KHQR codes** for both individual (remittance) and merchant payments, with full control over merchant info, currency, amount, and optional fields (bill number, store/terminal label, mobile number).
- **Decode KHQR codes** from raw text, a picked image file, or drag-and-drop — decoding also verifies the CRC and reports whether the code is valid.
- **On-brand QR card preview** (`KhqrCardView`) laid out to the KHQR Card Guideline's 20:29 ratio, header proportions, margins, and dashed divider, with a currency badge (USD `$` / KHR `៛`) centered on the QR. The card is built fresh from the decoded/generated payload — it never renders a dropped file's own image.
- **Smart amount formatting**: thousands are grouped, and decimals are padded to the currency's minor units without forcing them — `100` stays `100`, `100.3` becomes `100.30` in USD.
- **The card stays hidden** until a QR actually exists, and disappears again on Clear or on error — no empty frame.
- **Copy QR text, copy response JSON, and save the card as a PNG.**
- **Light/dark theme toggle.**
- **Field validation** with inline errors before generating a code.

## System Requirements

- **Development**: JDK 21+
- **Runtime**: JDK 21+ (or a bundled JRE if packaged with `jpackage`, see below)
- **Operating Systems**: macOS, Windows, Linux (anywhere JavaFX 21 runs)

## Project Structure

```
khqr-tool/
├── src/main/java/com/sakcode/decodekhqr/
│   ├── MainKHQRApplication.java     # JavaFX entry point, scene + theme wiring
│   ├── khqr/
│   │   ├── KhqrService.java         # Generate/decode facade over the Bakong SDK
│   │   └── KhqrFormMapper.java      # Form fields ↔ SDK request/response objects
│   ├── model/
│   │   ├── Currency.java            # USD/KHR, minor units, KHQR SDK mapping
│   │   └── MerchantType.java        # Remittance (individual) vs Merchant
│   ├── qr/
│   │   ├── QrImageCodec.java        # ZXing-based QR encode/decode
│   │   └── PngImageWriter.java      # Dependency-free PNG export for the card
│   ├── ui/
│   │   ├── KhqrFormView.java        # Scene graph: Generate/Decode tabs + preview panel
│   │   ├── KhqrFormController.java  # Wires buttons/drag-drop to KhqrService
│   │   ├── KhqrCardView.java        # The branded KHQR card, per the guideline
│   │   ├── FormValidator.java       # Field validation before generation
│   │   ├── FormDefaults.java        # Default values for the Generate form
│   │   ├── SvgIcon.java             # Minimal SVG-path loader for the header logo
│   │   └── Theme.java               # Light/dark stylesheet pair
│   └── util/
│       ├── BakongUtils.java         # Builds the raw KHQR payload string
│       └── Timestamps.java          # Created/expires timestamp formatting
├── src/main/resources/
│   ├── css/                         # base + light/dark theme stylesheets
│   ├── khqr-assets/                 # KHQR wordmark SVGs, currency badge PNGs
│   └── icon_64.png / icon_1024.icns / icon-khqr.ico
├── build.gradle                     # Gradle build, shadow JAR, macOS jpackage task
└── LICENSE
```

## Quick Start

### Prerequisites

- **JDK 21+**
- **Git**

Gradle itself does not need to be installed — the project ships the Gradle wrapper (`gradlew`).

### Clone and Run

```bash
git clone https://github.com/samreachyan/khqr-tool.git
cd khqr-tool

# Run directly
./gradlew run

# Or build a fat jar and run it
./gradlew shadowJar
java -jar build/libs/decodekhqr-1.0-SNAPSHOT.jar
```

### Run Tests

```bash
./gradlew test
```

## Building a Distributable

### Fat JAR (any platform)

```bash
./gradlew shadowJar
# Output: build/libs/decodekhqr-1.0-SNAPSHOT.jar
```

The shadow JAR bundles all runtime dependencies (Bakong KHQR SDK, ZXing, Jackson, Commons Lang, ControlsFX, BootstrapFX) — it just needs a JDK 21+ with JavaFX available to run.

### macOS `.app` via jpackage

```bash
./gradlew packageMacApp
# Output: build/dist/KHQR-Tool.app
```

This depends on `shadowJar` and needs the JavaFX SDK's platform-specific module JARs present in `lib/` (already checked into this project for `mac-aarch64`). For other platforms, add the equivalent `jpackage` task to `build.gradle` with that platform's JavaFX module JARs.

## Customization

### Application icon and title

- Icons live in `src/main/resources/` (`icon_64.png`, `icon_1024.icns`, `icon-khqr.ico`); swap them and update `packageMacApp` in `build.gradle` for `.app` icon.
- Window title is set in `MainKHQRApplication.start()`.

### Generate form defaults

Edit `src/main/java/com/sakcode/decodekhqr/ui/FormDefaults.java` to change what pre-fills the Generate tab (Bakong account ID, acquiring bank, merchant category code, etc.).

### KHQR card appearance

`KhqrCardView.java` expresses every dimension as a fraction of the card height, matching the KHQR Card Appearance Guideline (header height, margins, text baselines, badge size). Adjust the constants at the top of the class rather than the layout code.

## Troubleshooting

1. **"Module not found" / JavaFX errors when running the jar directly** — run via `./gradlew run` (which wires the JavaFX module path for you), or add `--module-path`/`--add-modules` flags matching the JavaFX SDK version used to build.
2. **`packageMacApp` fails: missing JavaFX module jars** — ensure the four `javafx-*-21-mac-aarch64.jar` files exist under `lib/`, matching your CPU architecture.
3. **Decoding a QR image fails** — the ZXing reader needs a QR code with a clean quiet zone; heavily compressed or cropped screenshots can fail to decode.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## License

MIT License — see [LICENSE](LICENSE) file for details.

## Support

For issues, questions, or contributions:
- Create an issue on GitHub
- Contact: @samreachyan
