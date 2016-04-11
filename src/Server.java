import java.net.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import javax.swing.*;

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

    public static void main( String args[] )
    {
        Server application = new Server();
        application.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
    }

    // handle button event
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


} // end class EchoServer3


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
}


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
                }

                //Print output to client
                for(PrintWriter s : socketVector){
                    s.println(inputLine);

                    s.println("~SOCKETS_INCOMING!");
                    for(String t: nameVector){
                        s.println(t);
                    }
                    s.println("~SOCKETS_ENDED!");

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
}