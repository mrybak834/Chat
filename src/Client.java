import java.net.*; 
import java.io.*; 
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import javax.swing.*;

import static java.lang.Thread.sleep;

public class Client extends JFrame implements ActionListener
{  
  // GUI items
  JButton sendButton;
  JButton connectButton;
  JTextField machineInfo;
  JTextField portInfo;
  JTextField message;
  JTextArea history;
  JTextArea people;
    JTextField username;

  // Network Items
  boolean connected;
  Socket echoSocket;
  PrintWriter out;
  BufferedReader in;

   // set up GUI
   public Client()
   {
      super( "Echo Client" );

      // get content pane and set its layout
      Container container = getContentPane();
      container.setLayout (new BorderLayout ());
      
      // set up the North panel
      JPanel upperPanel = new JPanel ();
      upperPanel.setLayout (new GridLayout (5,2));
      container.add (upperPanel, BorderLayout.NORTH);
      
      // create buttons
      connected = false;

      upperPanel.add ( new JLabel ("Message: ", JLabel.RIGHT) );
      message = new JTextField ("");
      message.addActionListener( this );
      upperPanel.add( message );
      
      sendButton = new JButton( "Send Message" );
      sendButton.addActionListener( this );
      sendButton.setEnabled (false);
      upperPanel.add( sendButton );

      connectButton = new JButton( "Connect to Server" );
      connectButton.addActionListener( this );
      upperPanel.add( connectButton );
                      
      upperPanel.add ( new JLabel ("Server Address: ", JLabel.RIGHT) );
      machineInfo = new JTextField ("127.0.0.1");
      upperPanel.add( machineInfo );
                      
      upperPanel.add ( new JLabel ("Server Port: ", JLabel.RIGHT) );
      portInfo = new JTextField ("");
      upperPanel.add( portInfo );
                      
      history = new JTextArea ( 10, 35 );
      history.setEditable(false);
      container.add( new JScrollPane(history) ,  BorderLayout.EAST);
      
      people = new JTextArea ( 10, 25 );
      people.setEditable(false);
      people.append("Clients connected to the server:\n");
      container.add( new JScrollPane(people) ,  BorderLayout.WEST);

       upperPanel.add ( new JLabel ("Username: ", JLabel.RIGHT) );
       username = new JTextField ("");
       username.addActionListener( this );
       upperPanel.add( username );

      setSize( 800, 400 );
      setVisible( true );

   } // end CountDown constructor

   public static void main( String args[] )
   { 
      Client application = new Client();

       //If the client exits, make sure to disconnect them as well
       application.addWindowListener(new WindowAdapter() {
           @Override
           public void windowClosing(WindowEvent e) {
               try {
                   System.out.println("CLOSING");

                   if(application.echoSocket != null && application.echoSocket.isConnected()) {
                       //Tell the server to decrease the vector of sockets
                       //TODO


                       application.echoSocket.close();
                   }
               } catch (IOException e1) {
                   e1.printStackTrace();
               }
           }
       });

       application.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
   }

    // handle button event
    public void actionPerformed( ActionEvent event )
    {
       if ( connected && 
           (event.getSource() == sendButton || 
            event.getSource() == message ) )
       {
         doSendMessage();
       }
       else if (event.getSource() == connectButton)
       {
         doManageConnection();
       }
    }

    public void doSendMessage()
    {
      try
      {

              out.println(username.getText() + ": " + message.getText());
              //history.insert(in.readLine() + "\n", 0);


      }
      catch (Exception e) {
          history.insert("Error in processing message ", 0);
      }
    }


    public class messageReceiverThread extends Thread{
        BufferedReader in;

        public messageReceiverThread(BufferedReader in){
            this.in = in;
        }
        public void run() {
            while(true){
                try {
                    while(in.ready()) {
                        String line;
                        line = in.readLine();
                        history.insert(line + "\n", 0);
                    }

                    //System.out.println(line);

                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("EXCEPTION THROWN");
                }
            }
        }
    }

    public void doManageConnection()
    {
      if (connected == false)
      {
        String machineName = null;
        int portNum = -1;
        try {
            machineName = machineInfo.getText();
            portNum = Integer.parseInt(portInfo.getText());
            echoSocket = new Socket(machineName, portNum );
            out = new PrintWriter(echoSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(
                                        echoSocket.getInputStream()));
            sendButton.setEnabled(true);
            connected = true;
            connectButton.setText("Disconnect from Server");

            messageReceiverThread t = new messageReceiverThread(in);
            t.start();

        } catch (NumberFormatException e) {
            history.insert ( "Server Port must be an integer\n", 0);
        } catch (UnknownHostException e) {
            history.insert("Don't know about host: " + machineName , 0);
        } catch (IOException e) {
            history.insert ("Couldn't get I/O for "
                               + "the connection to: " + machineName , 0);
        }

      }
      else
      {
        try 
        {
          out.close();
          in.close();
          echoSocket.close();
          sendButton.setEnabled(false);
          connected = false;
          connectButton.setText("Connect to Server");
        }
        catch (IOException e) 
        {
            history.insert ("Error in closing down Socket ", 0);
        }
      }

        
    }

 } // end class EchoServer3