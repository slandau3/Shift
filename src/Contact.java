import java.util.ArrayList;

/**
 * Created by Steven Landau on 10/6/2016.
 */
public class Contact {
    private String name;
    private String phoneNumber;
    private ArrayList<String> messages;


    public Contact(String name, String phoneNumber, ArrayList<String> messages) {
        this.name = name;
        this.messages = messages;
        this.phoneNumber = phoneNumber;
    }

    public void addMessage(String message) {
        messages.add(message);
    }

    public ArrayList<String> getMessages() {
        return messages;
    }

    public String getMostRecentMessage() {
        return messages.get(messages.size()-1);
    }
}
