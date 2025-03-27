# Generator - Decode KHQR

## Java support with JDK 17
This application is used to generate or decode KHQR.

1. User can upload KHQR to decode
2. User can copy/paste string khqr
3. Generate KHQR

## Run

```cmd
mvn clean package
```
And build app:

```cmd
jpackage \
  --name KHQR \
  --input target \
  --main-jar decodekhqr-1.0-SNAPSHOT.jar \
  --main-class com.sakcode.decodekhqr.MainKHQRApplication \
  --type app-image \
  --java-options "--module-path /Users/samreachyan/Documents/OPT/javafx-sdk-21/lib --add-modules javafx.controls,javafx.fxml" \
  --dest target \
  --mac-package-identifier com.sakcode.decodekhqr \
  --icon /Users/xxxxxx/Documents/OPT/simple-icons/MyIcon.iconset/icon_1024.icns 
```

## Support and Give star <3

```
@author samreach
@date 29 May 2024
@description Just a tool to run locally to generate/decode khqr for testing purpose!
```
