import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
/* class GenerateKeys to generate keys pairs
* create 2 folders one to store public key and one for the private key
* The server can use the keys in the server folder
* The clients can use their respective keys in the private folder
* Just enter more names if to add and run the java class
* */
public class GenerateKeys {
    private KeyPairGenerator keyGen;
    private KeyPair pair;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    public GenerateKeys(int keylength) throws NoSuchAlgorithmException, NoSuchProviderException {
        this.keyGen = KeyPairGenerator.getInstance("RSA");
        this.keyGen.initialize(keylength);
    }
// create a public and a private key pair
    public void createKeys() {
        this.pair = this.keyGen.generateKeyPair();
        this.privateKey = pair.getPrivate();
        this.publicKey = pair.getPublic();
    }

    public PrivateKey getPrivateKey() {
        return this.privateKey;
    }

    public PublicKey getPublicKey() {
        return this.publicKey;
    }
// Write the keys in the file
    public void writeToFile(String path, byte[] key) throws IOException {
        File f = new File(path);
        f.getParentFile().mkdirs();

        FileOutputStream fos = new FileOutputStream(f);
        fos.write(key);
        fos.flush();
        fos.close();
    }

    public static void main(String[] args) {

        // Arraylist to define users in the file sysytem
        ArrayList<String> names = new ArrayList<>();
        names.add("Shubham");
        names.add("Vaibhav");
        names.add("Sahil");
        names.add("Aishwarya");

        GenerateKeys gk;
    for(String name: names){
        try {
            gk = new GenerateKeys(1024);
            gk.createKeys();
            gk.writeToFile(String.format("Server/%s_PublicKey",name ), gk.getPublicKey().getEncoded());
            gk.writeToFile(String.format("Personal/%s_PrivateKey",name ), gk.getPrivateKey().getEncoded());
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            System.err.println(e.getMessage());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
    }

}
