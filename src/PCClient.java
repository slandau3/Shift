import javafx.application.Application;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

/**
 * Created by Steven Landau on 10/6/2016.
 */
public class PCClient extends Application {

    private Socket server;
    private DataInputStream in;
    private DataOutputStream out;
    private TextField inputBar;
    private TextArea messageDisplay;
    private VBox chats;

    /**
     * The constructor for the PCClient class.
     * Sets up the necessary sockets and streams
     * also starts GUI
     * @param ip, the ip to connect to
     */
    public PCClient(String ip) {

    }

    @Override
    public void start(Stage primaryStage) throws Exception {

    }
}
