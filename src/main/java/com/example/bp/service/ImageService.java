package com.example.bp.service;

import java.io.IOException;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.nio.PngWriter;
import com.sksamuel.scrimage.webp.WebpWriter;
import org.springframework.stereotype.Service;

/**
 * Image pipeline (PRD §8.4): decode → cover-crop → encode. Profile images use
 * WebP (quality 85); branding PNG/favicon work is added in PR9.
 */
@Service
public class ImageService {

    public static final int PROFILE_WEBP_QUALITY = 85;

    /** Decode bytes, center-crop to fill {@code w×h}, encode as WebP. */
    public byte[] toWebpCover(byte[] input, int width, int height, int quality) throws IOException {
        ImmutableImage image = ImmutableImage.loader().fromBytes(input);
        return image.cover(width, height).bytes(WebpWriter.DEFAULT.withQ(quality));
    }

    /** Decode bytes, center-crop to a {@code size×size} square, encode as PNG (branding/favicon). */
    public byte[] toPngSquare(byte[] input, int size) throws IOException {
        ImmutableImage image = ImmutableImage.loader().fromBytes(input);
        return image.cover(size, size).bytes(PngWriter.MaxCompression);
    }
}
