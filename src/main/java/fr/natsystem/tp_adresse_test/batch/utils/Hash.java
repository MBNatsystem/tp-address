package fr.natsystem.tp_adresse_test.batch.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import lombok.extern.slf4j.Slf4j;
import net.openhft.hashing.LongHashFunction;

@Slf4j
public class Hash {
    private static final LongHashFunction HASHER = LongHashFunction.xx3();

    public String fastHash(String value) {
        long hash = HASHER.hashChars(value);
        return Long.toUnsignedString(hash, 16);
    }

    public static String sha256(Path file){
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            try(InputStream is = Files.newInputStream(file);
                DigestInputStream dis = new DigestInputStream(is, md)
                ){
                    byte[] buffer = new byte[8192];

                    while(dis.read(buffer) != -1){

                    }
                    return HexFormat.of().formatHex(md.digest());
                } catch (IOException e) {
                    log.info("ouais");
                    e.printStackTrace();
                }

        } catch (NoSuchAlgorithmException e) {
            log.info("non");
            e.printStackTrace();
        }
        return "coucou";
    }
}
