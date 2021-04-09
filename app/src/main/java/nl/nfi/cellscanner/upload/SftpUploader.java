package nl.nfi.cellscanner.upload;

import android.content.Context;
import android.util.Log;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;

import nl.nfi.cellscanner.Preferences;
import nl.nfi.cellscanner.R;

public class SftpUploader implements Uploader {
    protected final JSch jsch = new JSch();
    private boolean strict_host_key_checking = false;

    public void setStrictHostKeyChecking(boolean value) {
        strict_host_key_checking = value;
    }

    @Override
    public URI validate(URI uri) throws Exception {
        if (uri.getHost() == null)
            throw new Exception("Host missing; try a valid URL such as sftp://user@hostname");

        if (uri.getPath() != null && !uri.getPath().equals(""))
            throw new Exception("Upload path not supported; try a URL without a path");

        return uri;
    }

    public void upload(Context ctx, URI uri, InputStream source, String dest_filename) throws Exception {
        String known_hosts = Preferences.getSshKnownHosts(ctx);
        if (known_hosts != null && !known_hosts.equals("")) {
            setStrictHostKeyChecking(true);
            jsch.setKnownHosts(new ByteArrayInputStream(known_hosts.getBytes()));
        }

        String[] userinfo = UploadUtils.getUsernameAndPasswordFromURI(uri);
        int port = uri.getPort();
        Session session = jsch.getSession(userinfo[0], uri.getHost(), port == -1 ? 22 : port);

        if (!strict_host_key_checking) {
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
        }

        if (userinfo[1] != null)
            session.setPassword(userinfo[1]);
        session.connect();
        try {
            ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();
            channel.put(source, UploadUtils.safeFilename(dest_filename));
            channel.disconnect();
        } finally {
            session.disconnect();
        }
    }

}
