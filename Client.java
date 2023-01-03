import java.io.*;
import java.net.*;
import java.util.ArrayList;

import java.util.Scanner;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
 import org.apache.commons.codec.binary.Base64;
public class Client {

    public int status;

    /* Socket of a client*/
    private Socket socket;

    /* BufferedReader and BufferedWriter associated with each client*/
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;

    /* To store username of a client */
    private String username;

    /* To store session key for communication between clients*/
    private SecretKey session_key;
    // defining the algo type
    public static final String ALGO = "AES";
    // Creating the cipher
    private Cipher cipher;
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    public Client() throws NoSuchAlgorithmException, NoSuchPaddingException {
        this.cipher = Cipher.getInstance("RSA");
    }

    /* Client constructor to instantiate a client */
    public Client(Socket socket, String username){
        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.username = username;
            this.status = 0;
            this.session_key = null;
        }
        catch (IOException e){
            closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }

    /*
     * Getting the private key
     */
    public PrivateKey getPrivate(String filename) throws Exception {
        byte[] keyBytes = Files.readAllBytes(new File(filename).toPath());
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }
    /*
     * getting the public key
     */
    public PublicKey getPublic(String filename) throws Exception {
        byte[] keyBytes = Files.readAllBytes(new File(filename).toPath());
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    public void encryptFile(byte[] input, File output, PrivateKey key)
            throws IOException, GeneralSecurityException {
        this.cipher.init(Cipher.ENCRYPT_MODE, key);
        writeToFile(output, this.cipher.doFinal(input));
    }

    public void decryptFile(byte[] input, File output, PublicKey key)
            throws IOException, GeneralSecurityException {
        this.cipher.init(Cipher.DECRYPT_MODE, key);
        writeToFile(output, this.cipher.doFinal(input));
    }
    /*
     * Wrting the output to a file
     */
    private void writeToFile(File output, byte[] toWrite)
            throws IllegalBlockSizeException, BadPaddingException, IOException {
        FileOutputStream fos = new FileOutputStream(output);
        fos.write(toWrite);
        fos.flush();
        fos.close();
    }
    /*
     * Encrypting the message and converting the encoded message to text using the private key
     */
    public String encryptText(String msg, PrivateKey key)
            throws NoSuchAlgorithmException, NoSuchPaddingException,
            UnsupportedEncodingException, IllegalBlockSizeException,
            BadPaddingException, InvalidKeyException {
        this.cipher.init(Cipher.ENCRYPT_MODE, key);
        return Base64.encodeBase64String(cipher.doFinal(msg.getBytes("UTF-8")));
    }
    /*
     * Encrypting the message and converting the encoded message to text using the Public key
     */
    public String encryptText(String msg, PublicKey key)
            throws NoSuchAlgorithmException, NoSuchPaddingException,
            UnsupportedEncodingException, IllegalBlockSizeException,
            BadPaddingException, InvalidKeyException {
        this.cipher.init(Cipher.ENCRYPT_MODE, key);
        return Base64.encodeBase64String(cipher.doFinal(msg.getBytes("UTF-8")));
    }
    /*
     * Decrypting the message and converting the text to cipher using the Public key
     */
    public String decryptText(String msg, PublicKey key)
            throws InvalidKeyException, UnsupportedEncodingException,
            IllegalBlockSizeException, BadPaddingException {
        this.cipher.init(Cipher.DECRYPT_MODE, key);
        return new String(cipher.doFinal(Base64.decodeBase64(msg)),"UTF-8");
    }
    /*
     * Decrypting the message and converting the text to cipher using the private key
     */
    public String decryptText(String msg, PrivateKey key)
            throws InvalidKeyException, UnsupportedEncodingException,
            IllegalBlockSizeException, BadPaddingException {
        this.cipher.init(Cipher.DECRYPT_MODE, key);
        return new String(cipher.doFinal(Base64.decodeBase64(msg)),"UTF-8");
    }

    /*
     * Encrypting the message from the symmetric key using AES-128
     */
    public String encryptMessage(String input, SecretKey key) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {

        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return Base64.encodeBase64String(cipher.doFinal(input.getBytes("UTF-8")));
    }
    /*
     * decrypting message from the Symmetric keys
     */
    public String decryptMessage(String cipherText, SecretKey key) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {

        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return new String(cipher.doFinal(Base64.decodeBase64(cipherText)),"UTF-8");
    }
    /*
     * Sending message from client
     */
    public void sendMessage(){

        try{

            /*
            * Sending the username to server initially
            */
            bufferedWriter.write(username);
            bufferedWriter.newLine();
            bufferedWriter.flush();

            /*
            * To take input from the client
            * */
            Scanner sc = new Scanner(System.in);

            /*
            * Takes input while the socket is connected
            * */
            while(socket.isConnected()) {

                /*
                * Take input from the client for the message to be sent
                * */
                String messageToSend = sc.nextLine();


                // if session key is stored
                if(session_key != null ) {
                    /* message encrypted with the session key*/
                    if(!messageToSend.equalsIgnoreCase("bye"))
                    messageToSend = encryptMessage(messageToSend, session_key);
                    else
                        session_key = null;
                }

                /*Sending message*/
                bufferedWriter.write(messageToSend);
                bufferedWriter.newLine();
                bufferedWriter.flush();

            }
        }
        catch (IOException e)
        {
            closeEverything(socket, bufferedReader, bufferedWriter);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessage(String message){

        try{
            bufferedWriter.write(message);
            bufferedWriter.newLine();
            bufferedWriter.flush();

        }catch (IOException e){
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    public void listenToMessage()
    {
        /*A thread running to listen to the incoming messages*/
        new Thread(new Runnable()
        {
            @Override
            public void run() {
                String msgFromGroupChat;
                boolean isVerified =false;

                /*Listen for the messages while socket it connected*/
                while(socket.isConnected())
                {
                     try
                     {

                         msgFromGroupChat = bufferedReader.readLine();
                         // if the user is not verified then get the nuance and encrypt with private key
                         if(!isVerified){
                             Client ac = new Client();
                             PrivateKey privateKey = ac.getPrivate(String.format("Personal/%s_PrivateKey",username));
                             String encrypted_msg = ac.encryptText(msgFromGroupChat, privateKey);
                             sendMessage(encrypted_msg);
                             isVerified=true;
                         }
                         else if(msgFromGroupChat.contains("//")) {
                                // get private key for user
                             Client ac = new Client();
                                PrivateKey privateKey = ac.getPrivate(String.format("Personal/%s_PrivateKey",username));
                                String encrypted_msg = ac.decryptText(msgFromGroupChat, privateKey);
                             //System.out.println(encrypted_msg);
                                String[] sKeyFromServer = encrypted_msg.split("//");
                                byte[] decodedKey = Base64.decodeBase64(sKeyFromServer[0]);
                                session_key = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");


                             //   msgFromGroupChat = sKeyFromServer[1];
                         }
                         else if(session_key != null) {
                                //System.out.println("Received message"+ msgFromGroupChat);

                                String uname = msgFromGroupChat.split(" ")[0];
                                //System.out.println(msgFromGroupChat.split(" ")[1]);

                                // to see if the second user left
                             if(!msgFromGroupChat.split(" ")[1].equalsIgnoreCase("left")) {
                                 String decoded_msg = decryptMessage(msgFromGroupChat.split(" ")[1], session_key);
                                 msgFromGroupChat = uname.concat(" " + decoded_msg);
                             }
                             else{
                                 // setting session key to nul to establish new connections after connecting back to the server
                                 session_key = null;
                             }
                         }

                         System.out.println(msgFromGroupChat);

                     }
                     catch (IOException e)
                     {
                         closeEverything(socket, bufferedReader, bufferedWriter);
                     } catch (NoSuchPaddingException e) {
                         throw new RuntimeException(e);
                     } catch (IllegalBlockSizeException e) {
                         throw new RuntimeException(e);
                     } catch (NoSuchAlgorithmException e) {
                         throw new RuntimeException(e);
                     } catch (BadPaddingException e) {
                         throw new RuntimeException(e);
                     } catch (InvalidKeyException e) {
                         throw new RuntimeException(e);
                     } catch (Exception e) {
                         throw new RuntimeException(e);
                     }
                 }
            }
        }).start();
    }

    /*To close a client or disconnect a client*/
    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter){
        try {
            if (bufferedReader!=null){
                bufferedReader.close();
            }
            if (bufferedWriter!=null){
                bufferedWriter.close();
            }
            if (socket !=null) {
                socket.close();
            }
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    /*
    Start of client program. To create clients run this file
    */
    public static void main(String[] args) throws IOException {

        /*To take the input*/
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter your username:");
        String username = sc.nextLine();


        /* Creating socket for the client*/
        Socket socket = new Socket("localhost", 5501);
        /*
         Instantiating a client with socket and username
        */
        Client client = new Client(socket, username);

        /*To start client in listening mode*/
        client.listenToMessage();

        /*To display a menu on start*/
        System.out.println("\nPlease use below commands to do the following:" +
                "\n1) To List all clients connected to Server (command -> list)" +
                "\n2) To connect to a client (command -> connect)" +
                "\n3) For main menu (command -> menu)");

        /* To start client in Write mode*/
        client.sendMessage();


    }

}
