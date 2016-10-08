import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Created by Steven Landau on 10/6/2016.
 */
public class ServerTest {
    //private PrintWriter out;
    //private BufferedReader out;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private ServerSocket s;
    private Socket client;
    private String message;
    private ArrayList<SendCard> cons = new ArrayList<>();

    public ServerTest() {

            try {
                s = new ServerSocket(8012);
                client = s.accept();
                System.out.println("CONNECTED");

                    //in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    //out = new PrintWriter(client.getOutputStream(), true);
                out = new ObjectOutputStream(client.getOutputStream());
                out.flush();
                in = new ObjectInputStream(client.getInputStream());
                ArrayList<String> a = new ArrayList<>();
                cons.add(new SendCard("one", "1", "yo"));
                cons.add(new SendCard("two", "2", "yo 2"));
                cons.add(new SendCard("three", "3", "yo 3"));
                cons.add(new SendCard("four" , "4", "yo 4"));

                for (SendCard c : cons) {
                    out.writeObject(c);
                    out.flush();
                }

                /*new Thread(() -> {

                    waitForMessage();

                }).start();*/
                    waitForMessage();
                    /*new Thread(() -> {
                        while (true) {
                            Scanner scan = new Scanner(System.in);
                            String s = scan.nextLine();
                            try {
                                out.writeObject(s);
                                out.flush();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            System.out.println("i said: " + s);
                        }
                    }).start();*/

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("here");
            } finally {
                try {
                    out.close();
                    in.close();
                    client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

    }


    private void waitForMessage() throws IOException {
        message = "yo";
        do {
            try {
                Thread.sleep(1000);
                System.out.println("HERE");

                //message = (String) in.readLine();
                Object a = in.readObject();
                if (a instanceof String) {
                    message = (String) a;
                    System.out.println(message);
                }

            } catch (IOException e) {
                //e.printStackTrace();
                throw new IOException("Client closed");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } while (true);
    }

    public static void main(String[] args) {
        ServerTest st = new ServerTest();
    }
}
