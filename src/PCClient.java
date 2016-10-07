import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.function.BooleanSupplier;

/**
 * Created by Steven Landau on 10/6/2016.
 */
public class PCClient extends Application {

    private Socket server;
    //private BufferedReader in;
    //private PrintWriter out;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private TextField inputBar;
    private TextArea messageDisplay;
    private VBox conversationsList;
    private ArrayList<Contact> conversations = new ArrayList<>();
    private String message = "";
    private Contact lookingAt;
    private Boolean start = true;

    /**
     * The constructor for the PCClient class.
     * Sets up the necessary sockets and streams
     * also starts GUI
     * @param ip, the ip to connect to
     */
    public void PCClient(String ip) {
        try {
            server = new Socket(ip, 8012);

            out = new ObjectOutputStream(server.getOutputStream());
            out.flush();

            in = new ObjectInputStream(server.getInputStream());

            out.writeObject("Desktop checking in");
            out.flush();
            while (true) {
                onReceive();
            }

        } catch (UnknownHostException e) {
            System.out.println("Cannot connect to ip");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
                in.close();
                server.close();
                System.exit(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    /**
     * Sends message to server
     * @param txt, String the user typed into inputBar
     */
    private void sendMessage(final String txt) {
        Platform.runLater(() -> {
            try {
                out.writeObject(new SendCard(inputBar.getText(), lookingAt)); // SendCard containing contact info and message
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("wrote: " + txt);
        });
    }

    /**
     * Keeps the client checking for the server's response.
     * Once a response is received, the messageDisplay will be updated.
     *
     * MsgRec -- a new message has been received
     */
    private void onReceive() throws IOException {  // Thought this name fit better.
        do {
            try {
                Thread.sleep(1000);

                Object obj = in.readObject();
                if (obj instanceof Contact) { // TODO: Need to make sure this works
                    //MsgRec mr = (MsgRec) obj;
                    //message = mr.getMsg();
                    //String name = mr.getName();
                    //String pn = mr.getPhoneNumber();
                    Contact c = (Contact) obj; // The received contact is assumed to have the updated message
                    if (!conversations.contains(c)) {
                        UpdateContacts.addContact(c);
                        conversations.add(c); // only adding it here so I don't have to error check when I remove it next.
                        System.out.println("Obtained " + c + " about to hand it over to handleOnReceive()");
                    } else {
                        System.out.println("Obtained " + c + " about to update then go to handleOnReceive");
                        UpdateContacts.updateData(c);
                    }
                    handleOnReceive(c);
                }  // TODO: Brainstorm a few more possible objects

            } catch (Exception e) {
                e.printStackTrace();
                throw new IOException("Server closed?");
            }
        } while (true);
    }

    private void handleOnReceive(Contact c) throws FileNotFoundException {
        conversations.remove(c);
        System.out.println("Removed " + c);
        conversations.add(c); // TODO: make sure this actually puts it at the front.
        System.out.println("Added " + c); // Going to optomize this later

        // Re-arrange the buttons
        Platform.runLater(() -> {
            try {
                System.out.println("about to fill the box");
                fillVbox();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });

    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Shift");
        BorderPane bp = new BorderPane();

        // Set up the messageDisplay
        messageDisplay = new TextArea("");
        messageDisplay.setWrapText(true);
        messageDisplay.setEditable(false);
        bp.setCenter(messageDisplay);

        // Set up the inputBar
        inputBar = new TextField();
        inputBar.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                if (!inputBar.getText().trim().equals("")) { // Make sure the user actually sends a message and not a space
                    sendMessage(inputBar.getText());
                    messageDisplay.appendText(inputBar.getText());
                    inputBar.clear(); // Is this the same as setting setText to ""
                }
            }
        });

        bp.setBottom(inputBar);

        
        // Set up the conversationsList

        ScrollPane sp = new ScrollPane();
        conversationsList = new VBox();
        fillVbox();
        sp.setContent(conversationsList);
        bp.setLeft(sp);
        
        
        // Open the GUI

        primaryStage.setScene(new Scene(bp, 500, 500));
        primaryStage.show();
        primaryStage.setOnCloseRequest(event -> {
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        new Thread(() -> {
            PCClient("localhost"); // Start the server once the GUI is set up.
        }).start();
    }

    /**
     * Fills the left side of the screen with your contacts.
     * Will eventually request the updated contact info from the server.
     * Should store said contact info to a file.
     *
     *
     *
     */
    private void fillVbox() throws FileNotFoundException {
        if (start) {
            ObjectInputStream freader = null;
            try {
                freader = new ObjectInputStream(new FileInputStream(new File("contacts.ser")));

                conversations.clear();
                while (true) {
                    Object o = freader.readObject();
                    if (o instanceof Contact) {
                        Contact c = (Contact) o;
                        //System.out.println("added at 221  " + c);
                        conversations.add(c);
                    }
                    // TODO: Decide whether there will ever be anything else here.
                }
            } catch (IOException e) {
                // go to finally, we ran out of file to read.
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (freader != null) {
                    try {
                        freader.close();
                    } catch (IOException e) {
                        e.printStackTrace(); // Should never get here
                    }
                }
                start = false;
            }
        }
        conversationsList.getChildren().clear();
        for (int i = conversations.size()-1; i > -1; i--) { // Add contacts gathered from file to the vbox
            System.out.println(conversations.get(i));
            ButtonContact bc = new ButtonContact(conversations.get(i));
            functionality(bc);
            conversationsList.getChildren().add(bc);

        }

    }

    /**
     * Gives all the buttons in the vbox the appropriate functionality.
     */
    private void functionality(ButtonContact bc) { // Another way to do this would be to store the TextArea in the Contacts class. I might switch to that later.
        bc.setOnAction(event -> {  //TODO: add functionality to save unsent message as draft
            System.out.println("Setting the nonsense on button lick");
            inputBar.clear();
            messageDisplay.clear();
            for (int i = 0; i < bc.getContact().getMessages().size(); i++) {
                System.out.println("appending text for " + bc.getContact() + " with message: " + bc.getContact().getMessages().get(i));
                messageDisplay.appendText(bc.getContact().getMessages().get(i) + "\n");
            }
            lookingAt = bc.getContact(); // now we know we are looking at this contact
            // TODO: I feel like I'm forgetting something
        });
    }


    public static void main(String[] args) {
        Application.launch(args);

    }
}
