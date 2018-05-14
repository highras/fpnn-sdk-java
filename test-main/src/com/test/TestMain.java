package com.test;

import java.io.*;

public class TestMain {

    public static void main(String[] args) {

        System.out.println(new String("Test with main!"));

        byte[] bytes = loadKey("key/test-secp256k1-public.der");

        new TestCase(bytes);
    }

    static byte[] loadKey(String derPath) {

        File f = new File(derPath);

        if (!f.exists()) {

            System.out.println(new String("file not exists! path: ").concat(f.getAbsolutePath()));
            return null;
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream((int) f.length());
        BufferedInputStream in = null;

        try {

            in = new BufferedInputStream(new FileInputStream(f));
            int buf_size = 1024;
            byte[] buffer = new byte[buf_size];
            int len = 0;

            while (-1 != (len = in.read(buffer, 0, buf_size))) {

                bos.write(buffer, 0, len);
            }

            return bos.toByteArray();
        } catch (Exception ex) {

            ex.printStackTrace();
        } finally {
            try {

                in.close();
                bos.close();
            } catch (Exception ex) {

                ex.printStackTrace();
            }
        }

        return null;
    }
}
