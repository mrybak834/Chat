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
  JTextArea people;
  private boolean running;

  // Network Items
  boolean serverContinue;
  ServerSocket serverSocket;
    public Vector<Socket> socketVector;

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
      
      people = new JTextArea ( 10, 25 );
      people.setEditable(false);
      people.append("Clients connected to the server:\n");
      container.add( new JScrollPane(people) );

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
   
   public ConnectionThread (Server es3)
   {
     gui = es3;
       socketVector = new Vector<>();
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
           CommunicationThread thread = new CommunicationThread (gui.serverSocket.accept(), gui, socketVector);
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
 //private boolean serverContinue = true;
 private Socket clientSocket;
 private Server gui;
    public Vector<PrintWriter> socketVector;



 public CommunicationThread (Socket clientSoc, Server ec3, Vector<PrintWriter> socketVectorg)
   {
    clientSocket = clientSoc;
       //socketVectorg.add(clientSocket);
       this.socketVector = socketVectorg;
       //socketVector.add(clientSocket);
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

            socketVector.add(out);



            while ((inputLine = in.readLine()) != null) {
                System.out.println("Server: " + inputLine);
                gui.history.insert(inputLine + "\n", 0);


                for(PrintWriter s : socketVector){
                    s.println(inputLine);
                    //System.out.println(s);
                    //System.out.println(inputLine);

                }


                //out.println(inputLine);

                if (inputLine.equals("Bye."))
                    break;

                if (inputLine.equals("End Server."))
                    gui.serverContinue = false;

                if (inputLine.equals("!DISCONNECT ME!")){
                    //TODO
                }


            }

            //Remove client from vector
            for( int i = 0; i < socketVector.size(); i++){
                if(socketVector.elementAt(i) == out){
                    socketVector.remove(i);
                }
            }

            out.close();
            in.close();
            clientSocket.close();


            System.out.println(socketVector.size());


        } 
    catch (IOException e) 
        {
            //LOGOUT CLIENTS HERE (REMOVE FROM VECTOR)
         System.err.println("Problem with Communication Server");
         //System.exit(1); 
        } 
    }
} 





 
