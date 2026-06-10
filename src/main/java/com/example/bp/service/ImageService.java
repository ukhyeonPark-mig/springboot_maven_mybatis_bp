package com.example.bp.service;

import java.io.IOException;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.nio.PngWriter;
import com.sksamuel.scrimage.webp.WebpWriter;
import org.springframework.stereotype.Service;

/**
 * 이미지 파이프라인 (PRD §8.4): 디코드 → cover-crop → 인코드. 프로필 이미지는
 * WebP(품질 85)를 사용한다. 브랜딩 PNG/favicon 작업은 PR9에서 추가된다.
 */
@Service
public class ImageService {

    public static final int PROFILE_WEBP_QUALITY = 85;

    /** 바이트를 디코드하고, {@code w×h}를 채우도록 중앙 크롭한 뒤, WebP로 인코드한다. */
    public byte[] toWebpCover(byte[] input, int width, int height, int quality) throws IOException {
        ImmutableImage image = ImmutableImage.loader().fromBytes(input);
        return image.cover(width, height).bytes(WebpWriter.DEFAULT.withQ(quality));
    }

    /** 바이트를 디코드하고, {@code size×size} 정사각형으로 중앙 크롭한 뒤, PNG로 인코드한다 (브랜딩/favicon). */
    public byte[] toPngSquare(byte[] input, int size) throws IOException {
        ImmutableImage image = ImmutableImage.loader().fromBytes(input);
        return image.cover(size, size).bytes(PngWriter.MaxCompression);
    }
}
