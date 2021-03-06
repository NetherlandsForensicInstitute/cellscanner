package nl.nfi.cellscanner.upload;

import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Crypto {
    private static Cipher getAesInstance() throws Exception {
        return Cipher.getInstance("AES/CBC/PKCS5Padding");
    }

    private static Cipher getRsaInstance() throws Exception {
        return Cipher.getInstance("RSA/NONE/NoPadding");
    }

    private static SecretKey generateAESKey() throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        SecretKey key = generator.generateKey();
        return key;
    }

    private static IvParameterSpec generateIV() throws Exception {
        SecureRandom rnd = new SecureRandom();
        return new IvParameterSpec(rnd.generateSeed(16));
    }

    private static void writeAESKey(OutputStream os, PublicKey rsa_key, SecretKey aes_key) throws Exception {
        Cipher cipher = getRsaInstance();
        cipher.init(Cipher.WRAP_MODE, rsa_key);
        byte[] encrypted_key = cipher.wrap(aes_key);
        os.write(Base64.encode(encrypted_key, Base64.DEFAULT));
        os.write('\n');
    }

    private static void writeIV(OutputStream os, IvParameterSpec iv) throws Exception {
        os.write(Base64.encode(iv.getIV(), Base64.DEFAULT));
        os.write('\n');
    }

    private static PublicKey getPublicKeyFromPem(String pem) throws Exception {
        String encoded = pem
                .replaceAll("-----[A-Z ]*-----", "")
                .replaceAll("\\s+", "");

        byte[] bytes = Base64.decode(encoded, Base64.DEFAULT);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    public static PrivateKey getPrivateKeyFromPem(String pem) throws Exception {
        String encoded = pem
                .replaceAll("-----[A-Z ]*-----", "")
                .replaceAll("\\s+", "");

        byte[] bytes = Base64.decode(encoded, Base64.DEFAULT);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    private static SecretKey decryptKey(byte[] encrypted_key, String private_key_pem) throws Exception {
        PrivateKey rsa_key = Crypto.getPrivateKeyFromPem(private_key_pem);
        Cipher cipher = getRsaInstance();
        cipher.init(Cipher.UNWRAP_MODE, rsa_key);
        return (SecretKey)cipher.unwrap(encrypted_key, "AES", Cipher.SECRET_KEY);
    }

    private static byte[] readLine(InputStream is) throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        for (int i=0; i<500; i++) {
            int b = is.read();
            if (b == -1)
                throw new Exception("premature eof");

            if (b == '\n') {
                return Base64.decode(buf.toByteArray(), Base64.DEFAULT);
            }

            buf.write(b);
        }

        throw new Exception("newline not found");
    }

    public static void encrypt(File in, File out, String public_key_pem) throws Exception {
        InputStream is = new FileInputStream(in);
        try {
            OutputStream os = new GZIPOutputStream(new FileOutputStream(out));
            try {
                if (public_key_pem != null) {
                    SecretKey aes_key = generateAESKey();
                    writeAESKey(os, getPublicKeyFromPem(public_key_pem), aes_key);

                    IvParameterSpec iv = generateIV();
                    writeIV(os, iv);

                    Cipher aes = getAesInstance();
                    aes.init(Cipher.ENCRYPT_MODE, aes_key, iv);

                    os = new CipherOutputStream(os, aes);
                }
                UploadUtils.copyStream(is, os);
            } finally {
                os.close();
            }
        } finally {
            is.close();
        }
    }

    public static void decrypt(File in, File out, String private_key_pem) throws Exception {
        InputStream is = new GZIPInputStream(new FileInputStream(in));
        try {
            SecretKey aes_key = decryptKey(readLine(is), private_key_pem);
            IvParameterSpec iv = new IvParameterSpec(readLine(is));

            Cipher aes = getAesInstance();
            aes.init(Cipher.DECRYPT_MODE, aes_key, iv);

            OutputStream os = new CipherOutputStream(new FileOutputStream(out), aes);
            try {
                UploadUtils.copyStream(is, os);
            } finally {
                os.close();
            }
        } finally {
            is.close();
        }
    }
}
