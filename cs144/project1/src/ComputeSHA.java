import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

class ComputeSHA {
    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
        File file = new File(args[0]);
        FileInputStream in = new FileInputStream(file);
        byte[] bytes = new byte[(int)file.length()];
        in.read(bytes);

        MessageDigest md = MessageDigest.getInstance("SHA-1");
        Formatter formatter = new Formatter();
        for (byte b : md.digest(bytes)) {
            formatter.format("%x", b);
        }
        System.out.println(formatter.toString());
    }
}
