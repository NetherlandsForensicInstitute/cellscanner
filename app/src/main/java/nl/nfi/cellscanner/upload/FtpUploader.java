package nl.nfi.cellscanner.upload;

import android.content.Context;
import android.util.Log;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class FtpUploader implements Uploader {
    @Override
    public URI validate(URI uri) throws Exception {
        if (uri.getHost() == null)
            throw new Exception("Host missing; try a valid URL such as ftp://user@hostname");

        if (uri.getPath() != null && !uri.getPath().equals(""))
            throw new Exception("Upload path not supported; try a URL without a path");

        return uri;
    }

    @Override
    public void upload(Context ctx, URI uri, InputStream source, String dest_filename) throws IOException {
        FTPClient con = new FTPClient();

        String[] userinfo = UploadUtils.getUsernameAndPasswordFromURI(uri);
        con.connect(uri.getHost());
        if (con.login(userinfo[0], userinfo[1]))
        {
            con.enterLocalPassiveMode(); // important!
            con.setFileType(FTP.BINARY_FILE_TYPE);

            // get the file and send it. A
            boolean result = con.storeFile(dest_filename, source);
            source.close();

            if (result) {
                Log.e("cellscanner", "upload result: succeeded");

                /*when file has been uploaded, the old data can be flushed*/

            } else {
                Log.e("cellscanner", "upload result: Failed");
            }

            // disconnect from the server
            con.logout();
            con.disconnect();
        }
    }
}
