package nl.nfi.cellscanner.upload;

import android.content.Context;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;

import nl.nfi.cellscanner.R;

public class NfiUploader extends SftpUploader {
    @Override
    public URI validate(URI uri) throws Exception {
        return new URI("nfi:default");
    }

    @Override
    public void upload(Context ctx, URI uri, InputStream source, String dest_filename) throws Exception {
        setStrictHostKeyChecking(true);
        jsch.setKnownHosts(new ByteArrayInputStream(ctx.getResources().getText(R.string.nfi_ssh_known_hosts).toString().getBytes()));
        super.upload(ctx, new URI(ctx.getResources().getText(R.string.nfi_upload_uri).toString()), source, dest_filename);
    }
}
