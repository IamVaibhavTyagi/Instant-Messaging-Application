import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

/* Server class */
public class Server
{

    /* to keep records of connected clients */
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    public static HashMap<String, ClientHandler> map = new HashMap<String, ClientHandler>();
    public static int nuance = 0;

    /* to keep track of ongoing session between two clients */
    public static HashMap<String, ClientHandler> connectionMap = new HashMap<String, ClientHandler>();

    /* ServerSocket created */
    private ServerSocket ss;


    /* Constructor of Server class */
    public Server(ServerSocket serverSocket)
    {
        this.ss = serverSocket;
    }

    public void verifyClient(ClientHandler clientHandler){
    nuance =(int)(Math.random()*10000000);

            try {
                System.out.println("Sending nuance "+nuance);
                    clientHandler.bufferedWriter.write(String.valueOf(nuance));
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();

            } catch (IOException e){
                clientHandler.closeEverything(clientHandler.socket, clientHandler.bufferedReader, clientHandler.bufferedWriter);
            }

    }

    /* starting the sever */
    public void startServer()
    {

        try
        {
            /* run the loop until serversocket is closed*/
            while(! ss.isClosed())
            {
                /* server socket accepting new client connections */
                Socket socket = ss.accept();
                System.out.println("New client is connected");
                ClientHandler clientHandler = new ClientHandler(socket);

                /* Adding client to client handlers */
                clientHandlers.add(clientHandler);
                /* storing client details in hash map */
                map.put(clientHandler.clientUsername, clientHandler);
                verifyClient(clientHandler);

                /* starting a new thread for a client */
                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        }
        catch(IOException e)
        {
            System.out.println(e);
        }

    }

    public void closedServerSocket()
    {
        try {
            if (ss != null){
                ss.close();
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }

    }

    /* Server main function */
    public static void main(String[] args) throws IOException {

        /* Initializing server socket */
        ServerSocket ss = new ServerSocket( 5501 );
        /* binding ss to server*/
        Server server = new Server(ss);
        /* starting the server*/
        server.startServer();

    }
}
