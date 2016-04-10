import java.net.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import javax.swing.*;

import static java.lang.Thread.sleep;


//TODO

/**
 * 1. Select who to chat with
 * 2. Refresh the list of clients (add and delete)
 * 3. Check client disconnects properly
 * 4. Fix UI
 * 5. No ~ or , or ! in username or private message names
 */

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
    JTextField sendToNames;

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
        upperPanel.setLayout (new GridLayout (6,2));
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


        upperPanel.add ( new JLabel ("Send to: ", JLabel.RIGHT) );
        sendToNames = new JTextField ("");
        sendToNames.addActionListener( this );
        upperPanel.add( sendToNames );

        setSize( 800, 400 );
        setVisible( true );

    } // end CountDown constructor

    public static void main( String args[] )
    {
        final Client application = new Client();

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

            //Global message
            if(sendToNames.getText().equals("")){
                out.println(username.getText() + ": " + message.getText());
            }
            //Private message
            else{
                out.println("!" + sendToNames.getText() + "," + username.getText() + "~" + username.getText() + ": " + message.getText());
            }


        }
        catch (Exception e) {
            history.insert("Error in processing message ", 0);
        }
    }

    //Send a false message in order for the server to process and return the list of clients
    public void doSendActivationMessage()
    {
        try
        {

            out.println(" " + " " + " " );

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
            int i = 0;
            String[] line = new String[10];
            int count = 0;

            while(true){

                try {

                    while(in.ready()) {
                        line[i] = in.readLine();
                        char c = line[i].charAt(0);

                        if( ((int) c > 47) && ((int) c < 58) )
                        {
                            people.setText("");
                            people.append("Clients connected to the server: \n");

                            for(int k = 0; k < i; k++)
                            {
                                if(line[i].equals(line[k]))
                                    count++;
                            }

                            if(count == 0)
                            {
                                i++;
                            }
                            line[i] = "";


                            for(int j = 0;j < 10; j ++)
                            {
                                if(line[j] != null)
                                    people.append(line[j] + "\n");

                            }

                        }
                        //Print message to chat pane
                        else {
                            //Check if the message is meant for the user
                            if(line[i].charAt(0) == '!'){
                                //Get the list of names
                                String userString = line[i];
                                String nameSubstring = line[i].substring(userString.indexOf('!') + 1, userString.indexOf('~'));

                                //Get the string to be displayed
                                String displayString = line[i].substring(userString.indexOf('~')+1);

                                //Split the names up, ignoring commas and whitespace
                                String[] names = nameSubstring.split("\\s*,\\s*");


                                //TODO
                                //Blank out repeated names


                                for(String s: names){
                                    //If the string was sent to us, display
                                    if(username.getText().equals(s)){
                                        history.insert("PRIVATE: " + displayString + "\n", 0);
                                    }
                                }

                            }
                            else{
                                history.insert(line[i] + "\n", 0);
                            }
                        }

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

                //Start listening for messages from the server
                messageReceiverThread t = new messageReceiverThread(in);
                t.start();

                //Get the list of clients
                doSendActivationMessage();

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