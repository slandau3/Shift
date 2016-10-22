import edu.rit.cs.steven_landau.shiftmobile.ContactCard;
import edu.rit.cs.steven_landau.shiftmobile.RetrievedContacts;
import edu.rit.cs.steven_landau.shiftmobile.SendCard;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
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
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;


import java.awt.*;
import java.io.*;
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
 */
public class PCClient extends Application {

    private Socket server;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    /**
     * The area where the user will type their message.
     * The user will not be able to click on the text
     * field until a contact is selected. The user can
     * send a message by hitting the enter key.
     */
    private TextField inputBar;

    /**
     * The text area that displays the message. The text area
     * can only display askii characters so anything like emoji's
     * will show be represented ... strangely... Directly linked to
     * buttonContacts and is updated any time a buttonContact is pressed.
     * When a message is received (which happens to be for the contact
     * you are looking at) the button will press itself, thus updating
     * the screen.
     */
    private TextArea messageDisplay;

    /**
     * THe VBox that holds all the conversations.
     * AKA the buttonContacts. Frequently get's overwritten
     * and rearranged in fillVBox.
     */
    private VBox conversationsBox;

    private ArrayList<Contact> conversations = new ArrayList<>();

    /**
     * The contact we are currently looking at.
     * This allows us to determine if we are looking at someone,
     * what that persons phone number is and what that persons
     * name is. All of which is necesarry to create a SendCard.
     */
    private Contact lookingAt;

    /**
     * Essentially the contact info for every person in the users phone.
     * The user can choose to start a new conversation in which the names
     * from the contact cards will be searched. When/if a name is selected
     * a new contact/buttonContact will be created for said person which will
     * allow the user to communicate with the chosen person.
     */
    private ArrayList<ContactCard> contactCards = new ArrayList<>();
    private Scene primaryScene;
    private Stage primaryStage;
    private VBox connectionStatus;
    private BorderPane bp;

    /**
     * The ip of the server you wish to connect to.
     */
    private static final String IP = "";

    /**
     * The port number of the server you wish to connect to.
     */
    private static final int PORT = 8012;

    /**
     * The first non JavaFX class that gets called.
     * Starts the server and sends the initial message which tells the server who we are.
     * Goes on to wait for responses from the server.
     * @param ip, the ip to connect to
     */
    public void startPCClient(String ip) {
        try {
            server = new Socket(ip, PORT);

            out = new ObjectOutputStream(server.getOutputStream());
            out.flush();

            in = new ObjectInputStream(server.getInputStream());

            out.writeObject(new PC());  // Let the server know that we are a pc.
            out.flush();

            onReceiveFromServer();


        } catch (UnknownHostException e) {
            System.out.println("Cannot connect to ip");
        } catch (Exception e) {  // TODO: modify this catch. may want it to just error out
            e.printStackTrace(); // IO exception will show up here
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
                    System.out.println("sent a sendcard");
                    //uc.updateData(lookingAt);
                } else {
                    out.writeObject(o); // I know this can easily be made into one statement. I'll do it later. Maybe.
                    System.out.println("sent a ... something else");
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
        while(true) {
            try {
                Thread.sleep(100);
                //if (in.available() > 0) {
                Object obj = in.readObject();
                System.out.println(obj.getClass());

                //}
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
                        System.out.println("sendcard received");
                    }
                }  // TODO: Brainstorm a few more possible objects
                else if (obj instanceof RetrievedContacts) {  // Should be the first thing we receive
                    System.out.println("retrievedContactacts received from server");
                    RetrievedContacts rc = (RetrievedContacts) obj;
                    if (rc.cc != null) {
                        this.contactCards = rc.cc;
                        //System.out.println(this.contactCards.size());
                        //System.out.println(rc.cc);
                    }
                } else if (obj instanceof ConversationHolder) {  // Keep an eye on this one
                    System.out.println("got conversation holder");
                    ConversationHolder ch = (ConversationHolder) obj;
                    if (ch.getContactHolder() != null) {
                        //System.out.println(ch.getContactHolder());
                        conversations = ch.getContactHolder();
                        if (conversations != null) {
                            Collections.reverse(conversations);  // Reverse them so they are put in the vbox in the correct order
                            Platform.runLater(() -> {
                                fillVbox();
                                System.out.println("fillbox complete");
                            });
                        }
                    }
                } else if (obj instanceof ConnectedToMobile) {  //TODO: set the indicator so that it takes up the entire right side of the screen even when scaled
                    boolean isConnected = ((ConnectedToMobile) obj).isConnected();
                    Platform.runLater(() -> {
                        Color color;
                        if (isConnected) {
                            connectionStatus.setStyle("-fx-background-color: green");
                        } else {
                            connectionStatus.setStyle("-fx-background-color: red");
                        }
                    });
                }
                else {
                    if (obj != null) {
                        System.out.println("Could not figure out what it was!");
                    }
                };
            } catch (EOFException eofe) {
                //do nothing.
            } catch (Exception e) {
                e.printStackTrace();
                throw new IOException("Server closed?");
            }
        }
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
        Toolkit.getDefaultToolkit().beep();
        });
    }


    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Shift Client");

        bp = new BorderPane();
        bp.setScaleShape(true);

        //Set up menu at top
        MenuBar mb = new MenuBar();
        Menu mFile = new Menu("File");
        mb.getMenus().add(mFile);

        // Set up the new conversation menu item
        MenuItem newConversation = new MenuItem("New Conversation");
        newConversation.setOnAction(event1 -> newConversationStage());

        // Set up the clear all menu item
        MenuItem clearAll = new MenuItem("Clear All");
        clearAll.setOnAction(e -> {
            sendMessage(new ClearRequest());
            lookingAt = null;
            conversations.clear();
            conversationsBox.getChildren().clear();
        });
        // Set up the options menu item
        MenuItem options = new MenuItem("Options & Settings");
        options.setOnAction(event -> optionsScene());  //TODO: Make an options menu.

        //Retry connection
        MenuItem retry = new MenuItem("Reconnect");
        retry.setOnAction(event -> {
            new Thread(() -> {
                startPCClient(IP);
            }).start();
        });

        mFile.getItems().addAll(newConversation, clearAll, options, retry);



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
                    lookingAt.addMessage("--Client--: " + inputBar.getText());  // Save the messages we sent too because we will need to recall it when we change people.
                    sendMessage(inputBar.getText());
                    messageDisplay.appendText("--Client--: " + inputBar.getText() + "\n");
                    inputBar.clear();
                }
            }
        });

        bp.setBottom(inputBar);

        connectionStatus = new VBox();
        connectionStatus.setStyle("-fx-background-color: red");
        bp.setRight(connectionStatus);
        //System.out.println(bp.getRight());
        
        // Set up the conversationsBox

        ScrollPane sp = new ScrollPane();
        conversationsBox = new VBox();
        conversationsBox.setMouseTransparent(true);  // Fixes a bug that results from the user clicking a button too fast on startup.
        //fillVbox();  // Do not fill vbox until we get the conversations from the server
        sp.setContent(conversationsBox);
        bp.setLeft(sp);

        // Set up the connectionStatus symbol

        // Open the GUI
        this.primaryScene = new Scene(bp);
        primaryStage.setScene(this.primaryScene);
        primaryStage.show();
        connectionStatus.setPrefWidth(primaryScene.getWidth()*.015);
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
                startPCClient(IP); // Start the server once the GUI is set up.
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
     * the other party to initiate it. The user can select
     * the "new conversation" MenuItem from the menuBar
     * and begin searching for contacts on their phone.
     * Every time they type a letter, the action event handler
     * will determine what contacts have said letters
     * in their name. The order of those letters matter.
     * Perhaps another way to say this would be that
     * the action event handler determines if any of
     * your contacts names contain a substring equal
     * to that of what you typed. For every letter typed
     * the screen will update and display new buttons
     * of different contacts. When a button is clicked
     * the user will be taken back to the primaryScene
     * and will be able to begin chatting with the
     * chosen person.
     */
    private void newConversationStage() {
        System.out.println("in new conversation stage");
        BorderPane bp = new BorderPane();
        GridPane gp = new GridPane();
        TextField tf = new TextField();  // Where the user will type the name of the person they want to talk to
        ArrayList<ContactCard> matchingContact = new ArrayList<>();
        tf.setPromptText("Search by name");
        tf.setOnKeyReleased(event -> {  // On any and all keys pressed
            CharSequence userText = tf.getText();
            //System.out.println(userText);
            gp.getChildren().clear();
            if (userText.toString().trim().length() != 0) {
                try {
                    for (ContactCard cc : contactCards) {  // TODO: optomize this
                        if (cc.getName().toLowerCase().contains(((String) userText).toLowerCase())) {
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
                    //System.out.println(matchingContact);
                    int row = 0;
                    gp.getChildren().clear(); // Remove everything that could have possibly been left over.
                    for (int i = 0; i < matchingContact.size(); i++) {
                        ButtonContactCard bcc = new ButtonContactCard(matchingContact.get(i));
                        final ContactCard cc = bcc.c;
                        bcc.setOnAction(event1 -> {
                            Contact c = new Contact(cc.getName(), cc.getNumber(), null);

                            if (conversations != null && conversations.contains(c)) {
                                this.primaryStage.setScene(this.primaryScene);
                                Contact t = conversations.get(conversations.indexOf(c));
                                conversations.remove(t);
                                conversations.add(t);
                                fillVbox();
                            } else {
                                this.primaryStage.setScene(this.primaryScene);
                                conversations.add(c);
                                fillVbox();
                            }
                        });
                        gp.add(bcc, i % 6, row);
                        if (i % 6 == 5) { // If we already have five buttons
                            row++;
                        }
                    }
                    matchingContact.clear();
                } catch (NullPointerException npe) {
                    //do nothing for now
                    System.out.println("problem");
                }
            }
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
        bottomButtons.getChildren().add(back);
        bp.setBottom(bottomButtons);
        this.primaryStage.setScene(new Scene(bp, this.primaryScene.getWidth(), this.primaryScene.getHeight()));
        //newChat.show();
    }


    public void optionsScene() { //TODO
        AnchorPane ap = new AnchorPane();
        BorderPane bp = new BorderPane();
        ScrollPane sp = new ScrollPane();
        ComboBox fonts = new ComboBox();
        fonts.getItems().addAll("Times New Roman", "Arial", "Comic Sans MS");
        fonts.setOnAction(event -> {
        });



    }
    /**
     * Fills the left side of the screen with your active conversations.
     * Also responsible for updating/organizing the contacts. This function
     * is typically called after a sendCard is received from the server
     * indicating that the list/order of buttons must be rearranged.
     */
    private void fillVbox() {
        /*if (start) {  // Get the contacts from the file only once the program starts

            start = false;
            return;
        }*/

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
     * The action listener is responsible for displaying
     * the text on the screen. Every time the button is clicked
     * the display is wiped and rewritten to fit the conversation
     * the user was having with said person. This method also assigns
     * right click functionality to the button. At the moment right clicking
     * a button contact will only allow you to remove/delete your conversation
     * with them.
     */
    private void functionality(ButtonContact bc) { // Another way to do this would be to store the TextArea in the Contacts class. I might switch to that later.
        bc.setOnMouseClicked(event -> {  //TODO: add functionality to save unsent message as draft
            //bc.fire();  // I don't remember why I had this here.
            if (event.getButton().equals(MouseButton.SECONDARY))
            inputBar.clear();
            inputBar.setEditable(true);
            messageDisplay.clear();
            for (int i = 0; i < bc.getContact().getMessages().size(); i++) {
                if (bc.getContact().getMessages().get(i).startsWith("--Client--:")) {
                    messageDisplay.appendText("" + bc.getContact().getMessages().get(i) + "\n");

                } else {
                    messageDisplay.appendText("--" + bc.getContact().getName() + "--: " + bc.getContact().getMessages().get(i) + "\n");

                }
            }
            lookingAt = bc.getContact(); // now we know we are looking at this contact
            bc.setStyle("-fx-background-color: gray");
            for (Node n : conversationsBox.getChildren()) {
                ButtonContact c = (ButtonContact) n;
                if (!c.getContact().equals(bc.getContact()) && !c.getStyle().equals("-fx-background-color: ORANGE")) { // Also need to make sure we are not overwritting the notification
                    c.setStyle(null);
                }
            }

            messageDisplay.setScrollTop(messageDisplay.getHeight());


        });

        bc.getStylesheets().add("button.css");
        bc.setWrapText(true);

        ContextMenu cm = new ContextMenu();
        MenuItem mi = new MenuItem("Remove conversation");
        mi.setOnAction(event -> {
            sendMessage(new RemoveRequest(bc.getContact())); // TODO. Test this.
            conversations.remove(bc.getContact());
            fillVbox();
            messageDisplay.clear();
            if (lookingAt.equals(bc.getContact())) {
                lookingAt = null;
            }
        });
        cm.getItems().add(mi);
        bc.setContextMenu(cm);
    }


    public static void main(String[] args) {
        Application.launch(args);

    }
}
