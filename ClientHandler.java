import javax.crypto.*;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;

import org.apache.commons.codec.binary.Base64;

public class ClientHandler implements Runnable{

    //    public ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    public Socket socket;
    public BufferedReader bufferedReader;
    public BufferedWriter bufferedWriter;
    String clientUsername;
    boolean busy;

    public static final Integer Ksize = 128;

    boolean isVerified;
    //    ClientHandler construction - called each time a new client is created.
    public ClientHandler(Socket socket){

        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.busy = false;
            this.clientUsername = bufferedReader.readLine();
            this.isVerified=false;

        } catch (IOException e){
            closeEverything(socket, bufferedReader, bufferedWriter);
        }

    }

    /* Function to display main menu to the client */
    public void mainMenu() throws IOException {
        this.bufferedWriter.write("\nPlease use below commands to do the following:" +
                "\n1) To List all clients connected to Server (command -> list)" +
                "\n2) To connect to a client (command -> connect client2_Name)" +
                "\n3) For main menu (command -> menu)");
        this.bufferedWriter.newLine();
        this.bufferedWriter.flush();
    }


    //    Function to list the users connected to the server currently - execute by typing 'list' command in each client terminal
    public void listClientForConversation() throws IOException {

        this.bufferedWriter.write("List of Available Users:" + String.valueOf(Server.map.keySet()));
        this.bufferedWriter.newLine();
        this.bufferedWriter.flush();

        System.out.println(Server.map);

        mainMenu();

    }

    /*
    function to send session key and update connectionMap when client to client communication is established
    * */
    public void sendClientToClientCommunicationDetails(ClientHandler client2) throws Exception {
        if ((this.busy == false) && (client2.busy == false)){


            this.busy = true;
            client2.busy = true;

            // create session key for both clients
            SecretKey sessionKey = generateKey(128);
            byte[] rawData = sessionKey.getEncoded();
            String encodedKey = Base64.encodeBase64String(rawData);
            // getting the public and private keys of the user
            Client ac = new Client();
            PublicKey publicKey1 = ac.getPublic(String.format("Server/%s_PublicKey",this.clientUsername));
            PublicKey publicKey2 = ac.getPublic(String.format("Server/%s_PublicKey",client2.clientUsername));
            String encodedKey1 = ac.encryptText(encodedKey, publicKey1);
            String encodedKey2 = ac.encryptText(encodedKey, publicKey2);

            Server.connectionMap.put(this.clientUsername,client2);
            Server.connectionMap.put(client2.clientUsername, this);

            this.bufferedWriter.write(encodedKey1 + "//" + "Starting chat with " + this.clientUsername + "!");
            this.bufferedWriter.newLine();
            this.bufferedWriter.flush();


            client2.bufferedWriter.write(encodedKey2 + "//" + "Starting chat with " + client2.clientUsername + "!");
            client2.bufferedWriter.newLine();
            client2.bufferedWriter.flush();

        }
        else{

            /* If connection cannot be established due to one of the user being busy. Send the busy message */
            this.bufferedWriter.write("" + client2.clientUsername + " is busy! Connection not established. ");
            this.bufferedWriter.newLine();
            this.bufferedWriter.flush();

            mainMenu();

        }
    }


    /* A thread listening for client requests */
    @Override
    public void run() {
        String messageFromClient;

        while(socket.isConnected())
        {
            try
            {
                messageFromClient = bufferedReader.readLine();
//                System.out.println(messageFromClient);
                if(!this.isVerified){
                    Client ac = new Client();
                    PublicKey publicKey = ac.getPublic(String.format("Server/%s_PublicKey",this.clientUsername));
                    String decrypted_msg = ac.decryptText(messageFromClient, publicKey);
                    System.out.println("Decrypted message is "+decrypted_msg);
                    if(decrypted_msg.equals(String.valueOf(Server.nuance)))
                        this.isVerified=true;
                }
                else {

                    /*If client is not busy then only they can use other commands*/
                    if (this.busy == false) {

                        /*To call 'list' command to show list of available users*/
                        if (messageFromClient.toLowerCase().equals("list")) {
                            listClientForConversation();
                        /* To connect a client with request client, if not busy */
                        } else if (messageFromClient.toLowerCase().contains("connect")) {
                            String[] temp = messageFromClient.split(" ");
                            String clientToConnectName = temp[1];
                            if ((!this.clientUsername.equals(clientToConnectName)) && (Server.map.containsKey(String.valueOf(clientToConnectName)))) {
                                System.out.println("Wants to talk to: " + clientToConnectName);
                                ClientHandler client2 = Server.map.get(clientToConnectName);
                                sendClientToClientCommunicationDetails(client2);
                            } else {

                                /* Can only connect to the user connected to the SERVER. A client cannot connect to itself. */
                                this.bufferedWriter.write("Connection loopback or Entered user is not connected to the SERVER");
                                this.bufferedWriter.newLine();
                                this.bufferedWriter.flush();
                            }
                        }
                        /*To display menu*/
                        else if (messageFromClient.toLowerCase().equals("menu")) {
                            mainMenu();
                        }
                        /* Not entering a valid choice */
                        else {
                            this.bufferedWriter.write("Enter a valid choice");
                            this.bufferedWriter.newLine();
                            this.bufferedWriter.flush();
                            mainMenu();
                        }

                    } else {
                        ClientHandler client2 = Server.connectionMap.get(this.clientUsername);
                        /* if message is not bye then continue to chat */
                        if (!messageFromClient.toLowerCase().equals("bye")){

                            client2.bufferedWriter.write(this.clientUsername + ": " + messageFromClient);
                            client2.bufferedWriter.newLine();
                            client2.bufferedWriter.flush();
                        }

                        /* End the chat and connect the client back to the SERVER */
                        else
                        {
                            client2.bufferedWriter.write(this.clientUsername+" Left the chat! Connecting you to Server again.");
                            client2.bufferedWriter.newLine();
                            client2.bufferedWriter.flush();

                            this.bufferedWriter.write("You Left the chat! Connecting you to Server again.");
                            this.bufferedWriter.newLine();
                            this.bufferedWriter.flush();

                            Server.connectionMap.remove(this);
                            Server.connectionMap.remove(client2);
                            client2.busy = false;
                            this.busy = false;
                        }
                    }
                }
            }
            catch(IOException e)
            {
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            } catch (NoSuchPaddingException e) {
                throw new RuntimeException(e);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /* To broadcast message to all the clients */
    public void broadcastMessage(String messageToSend){
        for(ClientHandler clientHandler: Server.clientHandlers){
            try {
                if (!clientHandler.clientUsername.equals(clientUsername)){
                    clientHandler.bufferedWriter.write(messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
            } catch (IOException e){
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    /* To remove a client record from the client handler */
    public void removeClientHandler(){
        Server.clientHandlers.remove(this);
        Server.map.remove(this);

        broadcastMessage("SERVER: "+clientUsername+" is DISCONNECTED to Server!");
    }

    // function to generate session key
    public static SecretKey generateKey(int key_size) throws NoSuchAlgorithmException 
    {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        SecureRandom sRandom = new SecureRandom();
        keyGenerator.init(key_size, sRandom);
        SecretKey key = keyGenerator.generateKey();
        return key;
    }

    /* Close everything in case of an undefined error */
    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter){
        removeClientHandler();
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

}

