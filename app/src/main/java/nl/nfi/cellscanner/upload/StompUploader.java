package nl.nfi.cellscanner.upload;

import android.content.Context;

import java.io.InputStream;
import java.net.URI;

import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;

public class StompUploader implements Uploader {
    @Override
    public void upload(Context ctx, URI uri, InputStream source, String dest_filename) throws Exception {

    }
}
