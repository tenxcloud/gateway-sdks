package com.apigateway.sdk.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

/**
 * @author hpy
 * @date 2021
 */
public class GZipUtils {

    static public byte[] gzipBytes(byte[] bytes) throws IOException {
        GZIPOutputStream gzip = null;
        ByteArrayOutputStream out = null;
        try {
            out = new ByteArrayOutputStream();
            gzip = new GZIPOutputStream(out);
            gzip.write(bytes);
            gzip.close();
            return out.toByteArray();
        } finally {
            if (gzip != null) {
                gzip.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }
}
