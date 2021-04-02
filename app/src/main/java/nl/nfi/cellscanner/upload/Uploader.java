package nl.nfi.cellscanner.upload;

import android.content.Context;

import java.io.InputStream;
import java.net.URI;

public interface Uploader {
    void upload(Context ctx, URI uri, InputStream source, String dest_filename) throws Exception;
}
