package com.demo.filestoresdk.utils;

import com.demo.reststarter.exception.InternalErrorException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.web.util.UriComponentsBuilder;

@UtilityClass
@Slf4j
public class FileTools {

    public static final String PATH_DELIMITER = "/";

    public static String buildUri(String folderName, String fileName) {
        return UriComponentsBuilder.newInstance()
            .path(folderName)
            .path(PATH_DELIMITER)
            .path(fileName)
            .build().toUriString();
    }

    public static String getFolderName(String realmAndId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(realmAndId.getBytes(StandardCharsets.UTF_8));

            return Hex.encodeHexString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getLocalizedMessage(), e);
            throw new InternalErrorException("binary.data.save.error", e);
        }
    }

    public static String getFileName(String path) {
        return path != null && path.contains(PATH_DELIMITER)
            ? path.substring(path.lastIndexOf(PATH_DELIMITER) + 1)
            : path;
    }

    public static String getImageFormat(final InputStream inputStream) throws IOException {
        // create an image input stream from the specified file
        final ImageInputStream iis = ImageIO.createImageInputStream(inputStream);

        // get all currently registered readers that recognize the image format
        final Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);

        if (!iter.hasNext()) {
            throw new IOException("No readers found!");
        }

        // get the first reader
        final ImageReader reader = iter.next();
        final String format = reader.getFormatName();

        // close stream
        iis.close();

        return format;
    }
}
