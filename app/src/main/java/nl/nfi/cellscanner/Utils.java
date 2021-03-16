package nl.nfi.cellscanner;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

public class Utils {
    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
    }

    public static void copyFileGzipped(File in, File out) throws IOException {
        InputStream is = new FileInputStream(in);
        try {
            OutputStream os = new GZIPOutputStream(new FileOutputStream(out));
            try {
                copyStream(is, os);
            } finally {
                os.close();
            }
        } finally {
            is.close();
        }
    }

    public static File createTempFile(Context context) throws IOException {
        File outputDir = context.getCacheDir(); // context being the Activity pointer
        return File.createTempFile("temp-db-", null, outputDir);
    }
}
