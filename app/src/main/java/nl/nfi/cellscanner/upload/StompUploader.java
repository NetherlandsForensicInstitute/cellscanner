package nl.nfi.cellscanner.upload;

import android.content.Context;

import java.io.InputStream;
import java.net.URI;

public class StompUploader implements Uploader {
    @Override
    public URI validate(URI uri) throws Exception {
        throw new Exception();
    }

    @Override
    public void upload(Context ctx, URI uri, InputStream source, String dest_filename) throws Exception {
        throw new Exception();
    }
}
