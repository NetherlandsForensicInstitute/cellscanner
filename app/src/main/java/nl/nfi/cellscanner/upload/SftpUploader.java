package nl.nfi.cellscanner.upload;

import android.content.Context;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;

import nl.nfi.cellscanner.R;

public class SftpUploader implements Uploader {
    public void upload(Context ctx, URI uri, InputStream source, String dest_filename) throws JSchException, SftpException {
        JSch jsch = new JSch();
        jsch.setKnownHosts(new ByteArrayInputStream(ctx.getResources().getText(R.string.ssh_known_hosts).toString().getBytes()));
        String[] userinfo = UploadUtils.getUsernameAndPasswordFromURI(uri);
        int port = uri.getPort();
        Session session = jsch.getSession(userinfo[0], uri.getHost(), port == -1 ? 22 : port);
        if (userinfo[1] != null)
            session.setPassword(userinfo[1]);
        session.connect();
        try {
            ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();
            channel.put(source, dest_filename);
            channel.disconnect();
        } finally {
            session.disconnect();
        }
    }

}
