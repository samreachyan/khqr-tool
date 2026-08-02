package com.sakcode.decodekhqr.ui;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads flat, single-color SVG icons (path elements with a {@code fill}) from a classpath
 * resource into a JavaFX node using the built-in {@link SVGPath} shape — no external SVG
 * rendering library required.
 */
public final class SvgIcon {

    private static final Pattern PATH_PATTERN = Pattern.compile("<path\\s+d=\"([^\"]+)\"\\s+fill=\"([^\"]+)\"\\s*/>");
    private static final Pattern VIEW_BOX_PATTERN = Pattern.compile("viewBox=\"[^\"]*?\\s([\\d.]+)\\s+([\\d.]+)\"");

    private SvgIcon() {
    }

    /** Loads the SVG at {@code resourcePath} scaled so its height matches {@code targetHeight}. */
    public static Node load(String resourcePath, double targetHeight) {
        String svg = readResource(resourcePath);

        Group group = new Group();
        Matcher pathMatcher = PATH_PATTERN.matcher(svg);
        while (pathMatcher.find()) {
            SVGPath path = new SVGPath();
            path.setContent(pathMatcher.group(1));
            path.setFill(Color.web(pathMatcher.group(2)));
            group.getChildren().add(path);
        }

        double scale = targetHeight / viewBoxHeight(svg);
        group.setScaleX(scale);
        group.setScaleY(scale);
        return group;
    }

    private static double viewBoxHeight(String svg) {
        Matcher matcher = VIEW_BOX_PATTERN.matcher(svg);
        if (!matcher.find()) {
            throw new IllegalStateException("SVG has no viewBox: cannot determine scale");
        }
        return Double.parseDouble(matcher.group(2));
    }

    private static String readResource(String resourcePath) {
        try (InputStream in = SvgIcon.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource: " + resourcePath);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read resource: " + resourcePath, e);
        }
    }
}
