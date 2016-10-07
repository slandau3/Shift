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
import sun.plugin2.message.Conversation;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.function.BooleanSupplier;

/**
 * Created by Steven Landau on 10/6/2016.
 * 
 * This is the main class for the Shift Client application. 
 * This class handles setting up the GUI as well as receiving/evaluating output from the server.
 * This class/application does not follow any particular design pattern.
 * That said, it has similarities to MVC.
 * Usage - coming soon //TODO
 */
public class PCClient extends Application {

    private Socket server;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private TextField inputBar;
    private TextArea messageDisplay;
    private VBox conversationsBox;
    private ArrayList<Contact> conversations = new ArrayList<>();
    private Contact lookingAt;
    private Boolean start = true;

    /**
     * The first non JavaFX class that gets called.
     * Starts the server and sends the initial message which tells the server who we are.
     * Goes on to wait for responses from the server.
     * @param ip, the ip to connect to
     */
    public void startPCClient(String ip) {
        try {
            server = new Socket(ip, 8012);

            out = new ObjectOutputStream(server.getOutputStream());
            out.flush();

            in = new ObjectInputStream(server.getInputStream());

            out.writeObject(new PC());  // Let the server know that we are a pc.
            out.flush();
            while (true) {
                onReceiveFromServer();
            }

        } catch (UnknownHostException e) {
            System.out.println("Cannot connect to ip");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
                in.close();
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.exit(0);
        }
    }



    /**
     * Sends message to server
     * @param o, An object that differs per intent
     * EX:
     *           String will be turned into SendCard which informs the server
     *           that we want to send a text message.
     */
    private void sendMessage(Object o) {
        Platform.runLater(() -> {
            try {
                if (o instanceof String) {
                    String s = (String) o;
                    out.writeObject(new SendCard(s, lookingAt)); // SendCard containing contact info and message
                }
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Keeps the client checking for the server's response.
     * Once a response is received, the messageDisplay will be updated.
     *
     * If a Contact object is received then we know that we just received a text message.
     */
    private void onReceiveFromServer() throws IOException {  // Thought this name fit better.
        do {
            try {
                Thread.sleep(100);

                Object obj = in.readObject();
                if (obj instanceof Contact) {
                    Contact c = (Contact) obj; // The received contact is assumed to have the updated message
                    if (!conversations.contains(c)) {
                        new Thread(() -> {  // IO is slow. No need to waste time on this thread, writing to a file
                            UpdateContacts.addContact(c);
                        }).start();
                        conversations.add(c); // only adding it here so I don't have to error check when I remove it next.
                        handleOnReceive(c);
                    } else {
                        conversations.get(conversations.indexOf(c)).addMessage(c.getMostRecentMessage());  // Add only the most recent messages otherwise user sent messages will be overwritten
                        Contact updated = conversations.get(conversations.indexOf(c));
                        new Thread(() -> { // IO is slow. No need to waste time on this thread, reading and writing to a file.
                            UpdateContacts.updateData(updated);
                        }).start();
                        handleOnReceive(updated);
                    }
                }  // TODO: Brainstorm a few more possible objects

            } catch (Exception e) {
                e.printStackTrace();
                throw new IOException("Server closed?");
            }
        } while (true);
    }

    /**]
     * Adds the contact to our list of conversations.
     * Updates the GUI to reflect the newly received message.
     * @param c, the contact updated with whatever message was just received from the server.
     * @throws FileNotFoundException
     */
    private void handleOnReceive(Contact c) throws FileNotFoundException {
        conversations.remove(c);
        conversations.add(c); // add it to the end of the list.

        // Re-arrange the buttons
        Platform.runLater(() -> {
            try {
                //System.out.println("about to fill the box");
                fillVbox();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });

    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Shift Client");
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
                    messageDisplay.appendText("\n" + "--CLIENT--: " + inputBar.getText());
                    lookingAt.addMessage("\n" + "--CLIENT--: " + inputBar.getText());  // Save the messages we sent too because we will need to recall it when we change people.
                    inputBar.clear();
                }
            }
        });

        bp.setBottom(inputBar);

        
        // Set up the conversationsBox

        ScrollPane sp = new ScrollPane();
        conversationsBox = new VBox();
        fillVbox();
        sp.setContent(conversationsBox);
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
            startPCClient("localhost"); // Start the server once the GUI is set up.
        }).start();
    }

    /**
     * Fills the left side of the screen with your contacts.
     * Will eventually request the updated contact info from the server.
     * Should store said contact info to a file.
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
        conversationsBox.getChildren().clear();
        for (int i = conversations.size()-1; i > -1; i--) { // Add contacts gathered from file to the vbox
            System.out.println(conversations.get(i));
            ButtonContact bc = new ButtonContact(conversations.get(i));
            functionality(bc);
            conversationsBox.getChildren().add(bc);

        }

    }

    /**
     * Add an action listener to the ButtonContact
     */
    private void functionality(ButtonContact bc) { // Another way to do this would be to store the TextArea in the Contacts class. I might switch to that later.
        bc.setOnAction(event -> {  //TODO: add functionality to save unsent message as draft
            inputBar.clear();
            messageDisplay.clear();
            for (int i = 0; i < bc.getContact().getMessages().size(); i++) {
                //System.out.println("appending text for " + bc.getContact() + " with message: " + bc.getContact().getMessages().get(i));
                messageDisplay.appendText(bc.getContact().getMessages().get(i) + "\n");
            }
            lookingAt = bc.getContact(); // now we know we are looking at this contact
        });
    }


    public static void main(String[] args) {
        Application.launch(args);

    }
}
