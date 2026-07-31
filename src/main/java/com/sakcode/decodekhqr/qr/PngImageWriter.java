package com.sakcode.decodekhqr.qr;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritablePixelFormat;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/** Minimal, dependency-free PNG encoder for JavaFX images — no java.awt/BufferedImage involved. */
public final class PngImageWriter {

    private static final byte[] SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final int BYTES_PER_PIXEL = 4;

    private PngImageWriter() {
    }

    public static void write(Image image, File file) throws IOException {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        byte[] compressed = deflate(toRawScanlines(image, width, height));

        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
            out.write(SIGNATURE);
            writeChunk(out, "IHDR", ihdr(width, height));
            writeChunk(out, "IDAT", compressed);
            writeChunk(out, "IEND", new byte[0]);
        }
    }

    private static byte[] toRawScanlines(Image image, int width, int height) {
        PixelReader reader = image.getPixelReader();
        int[] pixels = new int[width * height];
        reader.getPixels(0, 0, width, height, WritablePixelFormat.getIntArgbInstance(), pixels, 0, width);

        byte[] raw = new byte[height * (1 + width * BYTES_PER_PIXEL)];
        int pos = 0;
        for (int y = 0; y < height; y++) {
            raw[pos++] = 0; // filter type: none
            for (int x = 0; x < width; x++) {
                int argb = pixels[y * width + x];
                raw[pos++] = (byte) ((argb >> 16) & 0xFF);
                raw[pos++] = (byte) ((argb >> 8) & 0xFF);
                raw[pos++] = (byte) (argb & 0xFF);
                raw[pos++] = (byte) ((argb >> 24) & 0xFF);
            }
        }
        return raw;
    }

    private static byte[] deflate(byte[] raw) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        try (DeflaterOutputStream deflaterStream = new DeflaterOutputStream(buffer, deflater)) {
            deflaterStream.write(raw);
        }
        deflater.end();
        return buffer.toByteArray();
    }

    private static byte[] ihdr(int width, int height) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(buffer);
        data.writeInt(width);
        data.writeInt(height);
        data.writeByte(8); // bit depth
        data.writeByte(6); // color type: RGBA
        data.writeByte(0); // compression method
        data.writeByte(0); // filter method
        data.writeByte(0); // interlace method
        return buffer.toByteArray();
    }

    private static void writeChunk(DataOutputStream out, String type, byte[] data) throws IOException {
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);

        out.writeInt(data.length);
        out.write(typeBytes);
        out.write(data);
        out.writeInt((int) crc.getValue());
    }
}
