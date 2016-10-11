import edu.rit.cs.steven_landau.shiftmobile.SendCard;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
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
    private UpdateContacts uc = new UpdateContacts();

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

            onReceiveFromServer();


        } catch (UnknownHostException e) {
            System.out.println("Cannot connect to ip");
        } catch (Exception e) {
            //e.printStackTrace(); // IO exception will show up here
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
     *           String will be turned into edu.rit.cs.steven_landau.shiftmobile.SendCard which informs the server
     *           that we want to send a text message.
     */
    private void sendMessage(Object o) {
        Platform.runLater(() -> {
            try {
                if (o instanceof String) {
                    String s = (String) o;

                    out.writeObject(new SendCard(s, lookingAt.getPhoneNumber(), lookingAt.getName())); // edu.rit.cs.steven_landau.shiftmobile.SendCard containing contact info and message
                    uc.updateData(lookingAt);
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
                if (obj instanceof SendCard) {
                    SendCard sc = (SendCard) obj;
                    Contact curr = null;
                    for (int i = 0; i < conversations.size(); i++) {
                        if (sc.getNumber().equals(conversations.get(i).getPhoneNumber())) {
                            conversations.get(i).addMessage(sc.getMsg());
                            uc.updateData(conversations.get(i));
                            curr = conversations.get(i);   // We found the contact in our list of contacts
                            break;
                        }
                    }
                    if (curr != null) {   // If we found them do this.
                        final Contact finalCurr = curr;     // Make final for sake of lambda
                        new Thread(() -> { // IO is slow. No need to waste time on this thread, reading and writing to a file.
                            uc.updateData(finalCurr);
                        }).start();
                        handleOnReceive(finalCurr);

                    } else {    // Do not currently have this contact
                        ArrayList<String> temp = new ArrayList<String>();
                        temp.add(sc.getMsg());
                        curr = new Contact(sc.getName(), sc.getNumber(), temp);
                        final Contact finalCurr1 = curr; // For sake of lambda
                        new Thread(() -> {  // IO is slow. No need to waste time on this thread, writing to a file
                            uc.addContact(finalCurr1);
                        }).start();

                        conversations.add(curr); // only adding it here so I don't have to error check when I remove it next.
                        handleOnReceive(curr);
                    }
                }  // TODO: Brainstorm a few more possible objects

            } catch (Exception e) {
                //e.printStackTrace();
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
        inputBar.setEditable(false);
        inputBar.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                if (!inputBar.getText().trim().equals("")) { // Make sure the user actually sends a message and not a space
                    lookingAt.addMessage("--CLIENT--: " + inputBar.getText());  // Save the messages we sent too because we will need to recall it when we change people.
                    sendMessage(inputBar.getText());
                    messageDisplay.appendText("--CLIENT--: " + inputBar.getText());
                    inputBar.clear();
                }
            }
        });

        bp.setBottom(inputBar);

        
        // Set up the conversationsBox

        ScrollPane sp = new ScrollPane();
        conversationsBox = new VBox();
        conversationsBox.setMouseTransparent(true);  // Fixes a bug that results from the user clicking a button too fast on startup.
        fillVbox();
        sp.setContent(conversationsBox);
        bp.setLeft(sp);
        
        
        // Open the GUI

        primaryStage.setScene(new Scene(bp, 500, 500));
        primaryStage.show();
        primaryStage.setOnCloseRequest(event -> {
            try {
                in.close();
                out.close();
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        try {
            new Thread(() -> {
                startPCClient("68.180.86.23"); // Start the server once the GUI is set up.
            }).start();
        } catch (Exception e) {
            // do nothing (for now);
        }
    }

    /**
     * Fills the left side of the screen with your contacts.
     * Will eventually request the updated contact info from the server.
     * Should store said contact info to a file.
     *
     *
     */
    private void fillVbox() throws FileNotFoundException {
        if (start) {  // Get the contacts from the file only once the program starts
            uc.getContacts(conversations);

            start = false;
        }

        conversationsBox.getChildren().clear();
        for (int i = conversations.size()-1; i > -1; i--) { // Add contacts gathered from file to the vbox
            ButtonContact bc = new ButtonContact(conversations.get(i));
            functionality(bc);
            conversationsBox.getChildren().add(bc);

        }
        try {
            Thread.sleep(100);  // Fixes a bug that results from the user clicking a button too fast.
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        conversationsBox.setMouseTransparent(false);

    }

    /**
     * Add an action listener to the ButtonContact
     */
    private void functionality(ButtonContact bc) { // Another way to do this would be to store the TextArea in the Contacts class. I might switch to that later.
        bc.setOnAction(event -> {  //TODO: add functionality to save unsent message as draft
            inputBar.clear();
            inputBar.setEditable(true);
            messageDisplay.clear();
            for (int i = 0; i < bc.getContact().getMessages().size(); i++) {
                if (bc.getContact().getMessages().get(i).startsWith("--CLIENT--:")) {
                    messageDisplay.appendText(bc.getContact().getMessages().get(i) + "\n");
                } else {
                    messageDisplay.appendText("--" + bc.getContact().getName() + "--: " + bc.getContact().getMessages().get(i) + "\n");
                }
            }
            lookingAt = bc.getContact(); // now we know we are looking at this contact
        });
    }


    public static void main(String[] args) {
        Application.launch(args);

    }
}
