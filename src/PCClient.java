import edu.rit.cs.steven_landau.shiftmobile.ContactCard;
import edu.rit.cs.steven_landau.shiftmobile.RetrievedContacts;
import edu.rit.cs.steven_landau.shiftmobile.SendCard;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


import java.awt.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

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
    private ArrayList<Contact> conversations;
    private Contact lookingAt;
    private Boolean start = true;
    //private UpdateContacts uc = new UpdateContacts();
    private ArrayList<ContactCard> contactCards;
    private Scene primaryScene;
    private Stage primaryStage;
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
        } catch (Exception e) {  // TODO: modify this catch. may want it to just error out
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
                    //uc.updateData(lookingAt);
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
                    System.out.println("received");
                    SendCard sc = (SendCard) obj;
                    Contact curr = null;
                    for (int i = 0; i < conversations.size(); i++) {
                        if (sc.getNumber().equals(conversations.get(i).getPhoneNumber())) {
                            conversations.get(i).addMessage(sc.getMsg());
                           // uc.updateData(conversations.get(i));
                            curr = conversations.get(i);   // We found the contact in our list of contacts
                            System.out.println("found");
                            break;
                        }
                    }
                    if (curr != null) {   // If we found them do this.
                        /*final Contact finalCurr = curr;     // Make final for sake of lambda
                        new Thread(() -> { // IO is slow. No need to waste time on this thread, reading and writing to a file.
                            uc.updateData(finalCurr);
                            System.out.println("updating contact");
                        }).start();*/
                        handleOnReceive(curr);

                    } else {    // Do not currently have this contact
                        ArrayList<String> temp = new ArrayList<String>();
                        temp.add(sc.getMsg());
                        curr = new Contact(sc.getName(), sc.getNumber(), temp);
                        /*final Contact finalCurr1 = curr; // For sake of lambda
                        new Thread(() -> {  // IO is slow. No need to waste time on this thread, writing to a file
                            uc.addContact(finalCurr1);
                            System.out.println("adding contact");
                        }).start();*/

                        conversations.add(curr); // only adding it here so I don't have to error check when I remove it next.
                        handleOnReceive(curr);
                    }
                }  // TODO: Brainstorm a few more possible objects
                else if (obj instanceof RetrievedContacts) {  // Should be the first thing we receive
                    RetrievedContacts rc = (RetrievedContacts) obj;
                    this.contactCards = rc;  // TODO: set up starting a new conversation
                } else if (obj instanceof ConversationHolder){  // Keep an eye on this one
                    conversations = ((ConversationHolder) obj).getContactHolder();
                    Collections.reverse(conversations);  // Reverse them so they are put in the vbox in the correct order
                    fillVbox();
                }
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
                fillVbox();

                    for (Node n : conversationsBox.getChildren()) {
                        if (n instanceof ButtonContact) {
                            ButtonContact bc = (ButtonContact) n;
                            if (bc.getContact().equals(c)) {
                                if (c.equals(lookingAt)) {
                                    bc.fire(); // Automatically update the screen
                                } // Automatically update the screen if we are already looking at the button we just received a message from
                                else {
                                    bc.setStyle("-fx-background-color: ORANGE");
                                }
                            }
                        }
                    }
                 // Automatically update the screen if we are already looking at the button we just received a message from
            } catch (FileNotFoundException e) {
                e.printStackTrace();  // Should not get here
            }
            Toolkit.getDefaultToolkit().beep();
        });
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Shift Client");
        BorderPane bp = new BorderPane();
        //Set up menu at top
        MenuBar mb = new MenuBar();
        Menu mFile = new Menu("File");
        mb.getMenus().add(mFile); //TODO: add functionality to start a conversation with a contact who is not yet in the contacts.ser file.
        MenuItem newConversation = new MenuItem("New Conversation");
        newConversation.setOnAction(event1 -> newConversationStage());
        mFile.getItems().add(newConversation);

        bp.setTop(mb);

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
                    messageDisplay.appendText("--CLIENT--: " + inputBar.getText() + "\n");
                    inputBar.clear();
                }
            }
        });

        bp.setBottom(inputBar);

        
        // Set up the conversationsBox

        ScrollPane sp = new ScrollPane();
        conversationsBox = new VBox();
        conversationsBox.setMouseTransparent(true);  // Fixes a bug that results from the user clicking a button too fast on startup.
        //fillVbox();  // Do not fill vbox until we get the conversations from the server
        sp.setContent(conversationsBox);
        bp.setLeft(sp);
        
        
        // Open the GUI
        this.primaryScene = new Scene(bp);
        primaryStage.setScene(this.primaryScene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(event -> {
            try {
                in.close();
                out.close();
                server.close();
                System.out.println("closed");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                // Illigal state exception. TODO: research why this is happening in the future.
            }
        });
        try {
            new Thread(() -> {
                startPCClient(""); // Start the server once the GUI is set up.
            }).start();
        } catch (Exception e) {
            // do nothing (for now);
            System.out.println("connection closed unexpectedly");  // TODO: Create an error message
            primaryStage.close();
        }
    }

    /**
     * This method shows a new stage where the user can
     * begin a new conversation without having to wait for
     * the other party to initiate it.
     */
    private void newConversationStage() {
        Stage newChat = new Stage();
        BorderPane bp = new BorderPane();
        GridPane gp = new GridPane();
        TextField tf = new TextField();  // Where the user will type the name of the person they want to talk to
        ArrayList<ContactCard> matchingContact = new ArrayList<>();
        tf.setPromptText("Search by name");
        tf.setOnKeyPressed(event -> {  // On any and all keys pressed
            CharSequence userText = tf.getText();
            for (ContactCard cc : contactCards) {  // TODO: optomize this
                if (cc.getName().contains(userText)) {
                    matchingContact.add(cc);
                }
            }
            Collections.sort(matchingContact, new Comparator<ContactCard>() {  // Keep an eye on this
                @Override
                public int compare(ContactCard o1, ContactCard o2) {
                    if (o1.getName().equals(o2.getName())) {
                        return o1.getNumber().compareTo(o2.getNumber());
                    } else {
                        return o1.getName().compareTo(o2.getName());
                    }
                }
            });
            int row = 0;
            for (int i = 0; i < matchingContact.size(); i++) {
                ButtonContactCard bcc = new ButtonContactCard(matchingContact.get(i));
                final ContactCard cc = bcc.c;
                bcc.setOnAction(event1 -> {
                    Contact c = new Contact(cc.getName(), cc.getNumber(), null);
                    if (conversations.contains(c)) {
                        this.primaryStage.setScene(this.primaryScene);
                        Contact t = conversations.get(conversations.indexOf(c));
                        conversations.remove(t);
                        conversations.add(t);
                        try {
                            fillVbox();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    } else {
                        this.primaryStage.setScene(this.primaryScene);
                        conversations.add(c);
                        try {
                            fillVbox();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                });
                gp.add(bcc, i % 6, row);
                if (i % 6 == 5) { // If we already have five buttons
                    row++;
                }
            }
            matchingContact.clear();
        });
        bp.setTop(tf);
        bp.setCenter(gp);

        HBox bottomButtons = new HBox();
        Button back = new Button("Back");
        back.setOnAction(event -> {
            this.primaryStage.setScene(this.primaryScene);
            lookingAt = null;
            matchingContact.clear();

        });
        bp.setBottom(bottomButtons);
        newChat.setScene(new Scene(bp));
        newChat.show();
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

            start = false;
            return;
        }

        conversationsBox.getChildren().clear();
        for (int i = conversations.size()-1; i > -1; i--) { // Add contacts gathered from file to the vbox
            ButtonContact bc = new ButtonContact(conversations.get(i));
            bc.setPrefSize(100, 40);
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
            bc.setStyle(null);
        });
        bc.getStylesheets().add("button.css");
        bc.setWrapText(true);
    }


    public static void main(String[] args) {
        Application.launch(args);

    }
}
