import java.net.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import javax.swing.*;

/**
 * Client.java - This class holds all of the information required for GUI display, connections
 * to the server, as well as information relay of names and messages.
 * Many instances of the chat executable are allowed to connect to the server, and all information
 * is abstracted away from clients by passing through the server.
 * A client must first connect to the server with a unique name, then message sending capabilities are
 * enabled, and the connection will close upon clicking the button or the exit field of the GUI.
 * @version     1.0.0
 * @university  University of Illinois at Chicago
 * @course      CS342 - Software Design
 * @package     Project #04 - Chat
 * @category    GUI
 * @author      Marek Rybakiewicz
 * @author      Michael McClory
 * @license     GNU Public License <http://www.gnu.org/licenses/gpl-3.0.txt>
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
    Vector<String> currentClientList;
    messageReceiverThread messageThread;

    /**
     * Sets up the GUI and initializes the client list vector.
     */
    public Client()
    {
        super( "Echo Client" );

        // get content pane and set its layout
        Container container = getContentPane();
        container.setLayout (new BorderLayout ());

        // set up the North panel
        JPanel upperPanel = new JPanel ();
        upperPanel.setLayout (new GridLayout (7,3));
        container.add (upperPanel, BorderLayout.NORTH);

        // create buttons
        connected = false;

        upperPanel.add ( new JLabel ("Server Address: ", JLabel.CENTER) );
        upperPanel.add ( new JLabel ("", JLabel.CENTER));
        upperPanel.add ( new JLabel ("Username: ", JLabel.CENTER) );

        machineInfo = new JTextField ("127.0.0.1");
        upperPanel.add( machineInfo );
        upperPanel.add ( new JLabel ("", JLabel.CENTER));
        username = new JTextField ("");
        username.addActionListener( this );
        upperPanel.add( username );

        upperPanel.add ( new JLabel ("Server Port: ", JLabel.CENTER) );
        upperPanel.add ( new JLabel ("", JLabel.CENTER));
        upperPanel.add ( new JLabel ("Send to: ", JLabel.CENTER) );

        portInfo = new JTextField ("");
        upperPanel.add( portInfo );
        upperPanel.add ( new JLabel ("", JLabel.CENTER));
        sendToNames = new JTextField ("");
        sendToNames.addActionListener( this );
        upperPanel.add( sendToNames );

        upperPanel.add ( new JLabel ("", JLabel.CENTER));
        upperPanel.add ( new JLabel ("", JLabel.CENTER));

        upperPanel.add ( new JLabel ("Message: ", JLabel.CENTER) );
        connectButton = new JButton( "Connect to Server" );
        connectButton.addActionListener( this );
        upperPanel.add( connectButton );
        upperPanel.add ( new JLabel ("", JLabel.CENTER));
        message = new JTextField ("");
        message.addActionListener( this );
        upperPanel.add( message );

        upperPanel.add ( new JLabel ("", JLabel.CENTER));
        upperPanel.add ( new JLabel ("", JLabel.CENTER));
        sendButton = new JButton( "Send Message" );
        sendButton.addActionListener( this );
        sendButton.setEnabled (false);
        upperPanel.add( sendButton );

        history = new JTextArea ( 10, 35 );
        history.setEditable(false);
        container.add( new JScrollPane(history) ,  BorderLayout.EAST);

        people = new JTextArea ( 10, 25 );
        people.setEditable(false);
        people.append("Clients connected to the server:\n");
        container.add( new JScrollPane(people) ,  BorderLayout.WEST);


        setSize( 683, 600 );
        setVisible( true );

        currentClientList = new Vector<>();

    } // end Client constructor


    /**
     * Instantiates the class and sets closing behavior to make sure the socket also disconnects
     * @param args
     */
    public static void main( String args[] )
    {
        final Client application = new Client();

        //If the client exits, make sure to disconnect them as well
        application.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    if(application.echoSocket != null && application.echoSocket.isConnected()) {
                        //Tell the server to decrease the vector of sockets
                        application.echoSocket.close();
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        application.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
    } // End of main method

    /**
     * Handles the button events,
     * activating/deactivating connections to the server and
     * activating message processing.
     *
     * @param event
     */
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
    } // End of actionPerformed method

    /**
     * Sends a message to the server.
     * A check is done to ensure the client is not maliciously injecting server commands, after which
     * the type of message is checked and sent out to the server for processing.
     */
    public void doSendMessage()
    {
        try
        {
            //Check for illegal server commands in message and replace
            if(message.getText().contains("~SOCKETS_INCOMING!") || message.getText().contains("~NEW_CONNECTION!") || message.getText().contains("~SOCKETS_ENDED!") ){
                JOptionPane.showMessageDialog(null, "ILLEGAL SERVER COMMAND CONTAINED IN MESSAGE. REMOVING PORTION.", "Error",
                        JOptionPane.ERROR_MESSAGE);

                //Repetition of statements needed to check all cases
                if(message.getText().contains("~SOCKETS_INCOMING!"))
                    message.setText(message.getText().replace("~SOCKETS_INCOMING!",""));

                if(message.getText().contains("~NEW_CONNECTION!"))
                    message.setText(message.getText().replace("~NEW_CONNECTION!",""));

                if(message.getText().contains("~SOCKETS_ENDED!"))
                    message.setText(message.getText().replace("~SOCKETS_ENDED!",""));
            }

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
    } // End of doSendMessage method

    /**
     * Called upon a new client logging in.
     * Sends a false message indicating a new connection to the server,
     * refreshing the client list in order to be current.
     */
    public void doSendActivationMessage()
    {
        try
        {
            out.println(username.getText() + ":" + "~NEW_CONNECTION!" );
        }
        catch (Exception e) {
            history.insert("Error in processing message ", 0);
        }
    } // End doSendActivationMessage method


    /**
     * The messageReceiverThread thread actively awaits input from the server and processes it accordingly.
     * Messages are first checked for global or private flags, upon which names are processed for the receiver list,
     * and the message is distributed.
     * Client list updates occur whenever a client connects or disconnects.
     */
    public class messageReceiverThread extends Thread{
        BufferedReader in;

        public messageReceiverThread(BufferedReader in){
            this.in = in;
        }

        //Run the thread, waiting for input from the server
        public void run() {
            String line;
            while(true){

                try {
                    while(in.ready()) {
                        //Get the line that is to be displayed in the chat box
                        line = in.readLine();

                        //If we receive a connection socket
                        if( line.startsWith("~SOCKETS_INCOMING!"))
                        {
                            //Reset vector
                            currentClientList.removeAllElements();

                            while(in.ready()) {
                                line = in.readLine();

                                //Finished reading sockets
                                if (line.startsWith("~SOCKETS_ENDED!")){
                                    people.setText("");
                                    people.append("Clients connected to the server: \n");
                                    //Add to user list
                                    for (String s : currentClientList) {
                                        people.append(s + "\n");
                                    }
                                    break;
                                }

                                //Add name to name list if not a duplicate
                                if (currentClientList.size() == 0){
                                    currentClientList.add(line.substring(line.lastIndexOf('!') + 1));
                                }
                                else{
                                    String name = line.substring(line.lastIndexOf('!') + 1);
                                    int duplicateCounter = 0;
                                    for(String s: currentClientList){
                                       //Duplicate found
                                        if (s.equals(name)){
                                           duplicateCounter++;
                                       }
                                    }

                                    if(duplicateCounter == 0) {
                                        currentClientList.add(name);
                                    }
                                    //Duplicate, notify the user to rename themselves
                                    else{
                                        try
                                        {
                                            out.println(name + ":" + "~DELETE_CONNECTION!" );
                                        }
                                        catch (Exception e) {
                                            history.insert("Error in processing message ", 0);
                                        }
                                    }

                                }

                            }
                        }
                        //Print message to chat pane
                        else {
                            //Check if the message is meant for the user
                            if(line.charAt(0) == '!'){
                                //Get the list of names
                                String userString = line;
                                String nameSubstring = line.substring(userString.indexOf('!') + 1, userString.indexOf('~'));

                                //Get the string to be displayed
                                String displayString = line.substring(userString.indexOf('~')+1);

                                //Split the names up, ignoring commas and whitespace
                                String[] names = nameSubstring.split("\\s*,\\s*");

                                //Split the remaining message into the actual message and the senders name
                                String[] sender_and_message = displayString.split(":");

                                //Blank out repeated names
                                int x = names.length;

                                for(int y = 0; y < x; y++){
                                    String nameAtPosition = names[y];
                                    for(int z = 0; z < x; z++){
                                        //If the names are the same and at a different position, blank out
                                        if(names[z].equals(nameAtPosition) && (y != z)){
                                            names[z] = "";
                                        }
                                        //If the user is not logged in, blank out
                                        int loggedIn = 0;
                                        for(String s : currentClientList){
                                            if(s.equals(names[z])){
                                               loggedIn = 1;
                                            }
                                        }
                                        if (loggedIn == 0){
                                            names[z] = "";
                                        }
                                    }
                                }

                                String chatNames = "";
                                for(String s: names){
                                    //If the string was sent to us, display
                                    if(username.getText().equals(s)){
                                        if(sender_and_message[0].equals(username.getText())){
                                            int z = 0;
                                            for(String n: names) {
                                                //Account for comma
                                                if (z == 0 && !n.equals(username.getText()) && !n.equals("")) {
                                                    chatNames = chatNames + n;
                                                    z++;
                                                }
                                                else if (!n.equals(username.getText()) && !n.equals("")) {
                                                    chatNames = chatNames + ", " + n;
                                                }
                                            }

                                            //Handle case where user sends to himself (remove name)
                                            if(chatNames.endsWith(", ")){
                                                chatNames = chatNames.substring(0, chatNames.length()-2);
                                                chatNames = chatNames + "";
                                            }

                                            //Print if at least one valid (online, non-self-referential) entry
                                            if( z != 0 ){
                                                history.insert("PRIVATE (with " + chatNames + ") : " + displayString + "\n", 0);
                                            }


                                        }
                                        else{
                                            chatNames = sender_and_message[0];
                                            history.insert("PRIVATE (with " + sender_and_message[0] + ") : " + displayString + "\n", 0);
                                        }
                                    }
                                }
                            }
                            //Global message
                            else{
                                history.insert(line + "\n", 0);
                            }
                        }

                    }

                } catch (IOException e) {
                }
            }
        }
    } // End of messageReceiverThread class

    /**
     * Initializes or stops a connection with the server.
     * Upon initialization an activity message is sent to the server in order to log the username,
     * and then processing is passed to the messageReceiverThread which actively waits for input
     * from the server.
     * Upon closing the connection all information about the client is removed from the server.
     */
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
                messageThread = new messageReceiverThread(in);
                messageThread.start();

                //Check if illegal username, fix if so
                if(username.getText().contains("!") || username.getText().contains("~") || username.getText().contains(",")){
                    JOptionPane.showMessageDialog(null, "ILLEGAL CHARACTER IN USERNAME. REMOVING PORTION", "Error",
                            JOptionPane.ERROR_MESSAGE);

                    if(username.getText().contains("!"))
                        username.setText(username.getText().replace("!", ""));

                    if(username.getText().contains("~"))
                        username.setText(username.getText().replace("~", ""));

                    if(username.getText().contains(","))
                        username.setText(username.getText().replace(",", ""));
                }

                //Get the list of clients
                doSendActivationMessage();

                //Disable username changing
                username.setEnabled(false);

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
                //Enable username changing
                username.setEnabled(true);

                out.close();
                in.close();
                echoSocket.close();
                sendButton.setEnabled(false);
                connected = false;
                connectButton.setText("Connect to Server");

                //Stop message receiving
                messageThread.interrupt();

                //Clear user list
                people.setText("");
            }
            catch (IOException e)
            {
                history.insert ("Error in closing down Socket ", 0);
            }
        }

    } // End of doManageConnection class

} // end of Client class