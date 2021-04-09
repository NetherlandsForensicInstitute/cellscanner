package nl.nfi.cellscanner.upload;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringBufferInputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import nl.nfi.cellscanner.Database;

import static nl.nfi.cellscanner.Database.getFileTitle;

public class UploadUtils {
    private static final Map<String,Uploader> proto = getUploadProtocols();

    private static Map<String,Uploader> getUploadProtocols() {
        Map<String,Uploader> map = new HashMap<>();
        map.put("ftp", new FtpUploader());
        map.put("sftp", new SftpUploader());
        map.put("mqtt", new MqttUploader());
        return map;
    }

    /**
     * Export data via email (unused but may be useful)
     */
    public static void exportData(Context ctx) {
        if (!Database.getDataFile(ctx).exists()) {
            Toast.makeText(ctx, "No database present.", Toast.LENGTH_SHORT).show();

        } else {
            String[] TO = {""};

            Uri uri = FileProvider.getUriForFile(ctx, "nl.nfi.cellscanner.fileprovider", Database.getDataFile(ctx));

            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);

            sharingIntent.putExtra(Intent.EXTRA_EMAIL, TO);
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getFileTitle());
            sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);

            //need this to prompts email client only
            sharingIntent.setDataAndType(uri, "message/rfc822");

            ActivityCompat.startActivity(ctx, Intent.createChooser(sharingIntent, "Share via"), null);
        }
    }

    public static URI validateURI(String uri) throws Exception {
        URI url;
        if (uri.contains(":"))
            url = new URI(uri);
        else
            url = new URI(uri + ":default");

        if (url.getScheme() == null)
            throw new Exception("Protocol missing; try a valid URL such as sftp://user@hostname");

        Uploader proto = getUploadProtocols().getOrDefault(url.getScheme().toLowerCase(), null);
        if (proto == null)
            throw new UnsupportedOperationException("Unsupported protocol: " + url.getScheme());

        return proto.validate(url);
    }

    public static void upload(Context ctx, String uri_spec, InputStream is, String dest_filename) throws Exception {
        URI uri = validateURI(uri_spec);
        Uploader uploader = UploadUtils.getUploadProtocols().get(uri.getScheme());
        uploader.upload(ctx, uri, is, dest_filename);
    }

    protected static String[] getUsernameAndPasswordFromURI(URI uri) {
        String username = null, password = null;

        String userinfo = uri.getUserInfo();
        if (userinfo != null) {
            int sep = userinfo.indexOf(':');
            if (sep == -1) {
                username = userinfo;
            } else {
                username = userinfo.substring(0, sep);
                password = userinfo.substring(sep + 1);
            }
        }

        return new String[]{username, password};
    }

    protected static void copyStream(InputStream in, OutputStream out) throws IOException {
        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
    }

    protected static byte[] readBytes(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        copyStream(is, os);
        return os.toByteArray();
    }

    protected static String readString(InputStream is) throws IOException {
        return new String(readBytes(is), "UTF-8");
    }

    protected static String safeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
