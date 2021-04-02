package nl.nfi.cellscanner.upload;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class UploadUtils {
    public static Map<String,Uploader> getUploadProtocols() {
        Map<String,Uploader> map = new HashMap<>();
        map.put("ftp", new FtpUploader());
        map.put("sftp", new SftpUploader());
        //map.put("http", new StompUploader());
        return map;
    }

    public static boolean testURI(URI uri) {
        return getUploadProtocols().containsKey(uri.getScheme());
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
}
