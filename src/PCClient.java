import javafx.application.Application;
import javafx.application.Platform;
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
                waitForResponse();
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
                out.writeObject(txt);
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
     */
    private void waitForResponse() throws IOException {
        do {
            try {
                System.out.println("HERE");
                Thread.sleep(1000);
                //message = (String) in.readLine();
                Object obj = in.readObject();
                if (obj instanceof String) {
                    message = (String) obj;
                    messageDisplay.appendText(message); // Update the messageDisplay
                }

            } catch (Exception e) {
                throw new IOException("Server closed?");
            }
        } while (true);


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
        fillVbox(conversationsList); // TODO: Need to re-order the vbox when a message is received
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
     * @param conversationsList
     */
    private void fillVbox(VBox conversationsList) throws FileNotFoundException {

        ObjectInputStream freader = null;
        try {
            freader = new ObjectInputStream(new FileInputStream(new File("contacts.ser")));
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
        }

        for (int i = conversations.size()-1; i > -1; i--) { // Add contacts gathered from file to the vbox
            System.out.println(conversations.get(i));
            conversationsList.getChildren().add(new ButtonContact(conversations.get(i)));
        }

    }

    public static void main(String[] args) {
        Application.launch(args);

    }
}
