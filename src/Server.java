import java.net.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import javax.swing.*;



/**
 * Server.java - The server handles many client connections and relays information
 * related to messages and name display to each client through ports. The server must
 * be initialized before any client can connect, and all information relay is handled
 * automatically.
 * @version     1.0.0
 * @university  University of Illinois at Chicago
 * @course      CS342 - Software Design
 * @package     Project #04 - Chat
 * @category    Server
 * @author      Marek Rybakiewicz
 * @author      Michael McClory
 * @license     GNU Public License <http://www.gnu.org/licenses/gpl-3.0.txt>
 */
public class Server extends JFrame implements ActionListener{

    // GUI items
    JButton ssButton;
    JLabel machineInfo;
    JLabel portInfo;
    JTextArea history;
    private boolean running;

    // Network Items
    boolean serverContinue;
    ServerSocket serverSocket;

    // set up GUI
    public Server()
    {
        super( "Echo Server" );

        // get content pane and set its layout
        Container container = getContentPane();
        container.setLayout( new FlowLayout() );

        // create buttons
        running = false;
        ssButton = new JButton( "Start Listening" );
        ssButton.addActionListener( this );
        container.add( ssButton );

        String machineAddress = null;
        try
        {
            InetAddress addr = InetAddress.getLocalHost();
            machineAddress = addr.getHostAddress();
        }
        catch (UnknownHostException e)
        {
            machineAddress = "127.0.0.1";
        }
        machineInfo = new JLabel (machineAddress);
        container.add( machineInfo );
        portInfo = new JLabel (" Not Listening ");
        container.add( portInfo );

        history = new JTextArea ( 10, 40 );
        history.setEditable(false);
        container.add( new JScrollPane(history) );

        setSize( 500, 420 );
        setVisible( true );

    } // end CountDown constructor

    //Initialize the server
    public static void main( String args[] )
    {
        Server application = new Server();
        application.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
    }

    // Handle the button event
    public void actionPerformed( ActionEvent event )
    {
        if (running == false)
        {
            new ConnectionThread (this);
        }
        else
        {
            serverContinue = false;
            ssButton.setText ("Start Listening");
            portInfo.setText (" Not Listening ");
        }
    }


} // End of Server class

/**
 * Attempts to establish a connection for the server to operate under,
 * and leaves the server in a running state through the thread in order to listen
 * to incoming connections
 */
class ConnectionThread extends Thread
{
    Server gui;
    public Vector<PrintWriter> socketVector;
    public Vector<String> nameVector;

    public ConnectionThread (Server es3)
    {
        gui = es3;
        socketVector = new Vector<>();
        nameVector = new Vector<>();
        start();
    }

    public void run()
    {
        gui.serverContinue = true;

        try
        {
            gui.serverSocket = new ServerSocket(0);
            gui.portInfo.setText("Listening on Port: " + gui.serverSocket.getLocalPort());
            System.out.println ("Connection Socket Created");
            try {
                while (gui.serverContinue)
                {
                    System.out.println ("Waiting for Connection");
                    gui.ssButton.setText("Stop Listening");
                    CommunicationThread thread = new CommunicationThread (gui.serverSocket.accept(), gui, socketVector, nameVector);
                    thread.start();
                }
            }
            catch (IOException e)
            {
                System.err.println("Accept failed.");
                System.exit(1);
            }
        }
        catch (IOException e)
        {
            System.err.println("Could not listen on port: 10008.");
            System.exit(1);
        }
        finally
        {
            try {
                gui.serverSocket.close();
            }
            catch (IOException e)
            {
                System.err.println("Could not close port: 10008.");
                System.exit(1);
            }
        }
    }
} //End of ConnectionThread class

/**
 * Handles all communications that are relayed through the server. Each client is designated with an input and output
 * stream. Since the streams are stored in a vector, clients are allowed to pick specific people they wish to communicate
 * with or may select to distribute the message globally. When a client shuts down connections, all related information
 * is removed from the server as well.
 */
class CommunicationThread extends Thread
{
    private Socket clientSocket;
    private Server gui;
    public Vector<PrintWriter> socketVector;
    public Vector<String> nameVector;


    public CommunicationThread (Socket clientSoc, Server ec3, Vector<PrintWriter> socketVectorg,Vector<String> nameVectorg)
    {
        clientSocket = clientSoc;
        this.socketVector = socketVectorg;
        this.nameVector = nameVectorg;
        gui = ec3;
    }

    public void run()
    {
        System.out.println ("New Communication Thread Started");

        try {

            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(),
                    true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));

            String inputLine;

            //Add the new connection
            socketVector.add(out);


            //While there is input
            while ((inputLine = in.readLine()) != null) {
                //Display message to server
                gui.history.insert(inputLine + "\n", 0);

                //Add the client name
                String[] nameSplit = inputLine.split(":");


                //If it is a new client, add to names
                if(nameSplit[1] != null && nameSplit[1].startsWith("~NEW_CONNECTION!")) {
                    nameVector.add(nameSplit[0]);

                    //Print output to client
                    for(PrintWriter s : socketVector){
                        s.println("~SOCKETS_INCOMING!");
                        for(String t: nameVector){
                            s.println(t);
                        }
                        s.println("~SOCKETS_ENDED!");

                    }
                }
                //If it is a deletion signal, tell user to change name
                else if(nameSplit[1] != null && nameSplit[1].startsWith("~DELETE_CONNECTION!")) {
                    nameVector.set(nameVector.size()-1, nameVector.lastElement() + "*");

                    socketVector.lastElement().println("DUPLICATE NAME. PLEASE RE-LOG AND CHANGE YOUR NAME. YOU CANNOT SEND MESSAGES");

                    //Print output to client
                    for(PrintWriter s : socketVector){

                        s.println("~SOCKETS_INCOMING!");
                        for(String t: nameVector){
                            s.println(t);
                        }
                        s.println("~SOCKETS_ENDED!");

                    }
                }
                else{
                    //Print output to client
                    for(PrintWriter s : socketVector){
                        s.println(inputLine);

                        s.println("~SOCKETS_INCOMING!");
                        for(String t: nameVector){
                            s.println(t);
                        }
                        s.println("~SOCKETS_ENDED!");

                    }
                }


                //Edge cases
                if (inputLine.equals("Bye."))
                    break;

                if (inputLine.equals("End Server."))
                    gui.serverContinue = false;

            }


            //Remove client from vector
            for( int i = 0; i < socketVector.size(); i++){
                if(socketVector.elementAt(i) == out){
                    socketVector.remove(i);
                    nameVector.remove(i);
                }
            }

            //Close the connection
            out.close();
            in.close();
            clientSocket.close();


            //Refresh client list
            for(PrintWriter s : socketVector){
                s.println("~SOCKETS_INCOMING!");
                for(String t: nameVector){
                    s.println(t);
                }
                s.println("~SOCKETS_ENDED!");
            }

        }
        catch (IOException e)
        {
            System.err.println("Problem with Communication Server");
        }
    }
} // End of CommunicationThread class