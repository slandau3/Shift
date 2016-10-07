import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Steven Landau on 10/6/2016.
 *
 * This class will be used throughout Shift
 */
public class Contact implements Serializable {
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
        UpdateContacts.updateData(this);
    }

    public void changeName(String name) {
        this.name = name;
        UpdateContacts.updateData(this);  // Not sure if this is best here. Maybe do it once all contact info is updated
    }

    public void changeNumber(String number) {
        this.phoneNumber = number;
        UpdateContacts.updateData(this);
    }

    public ArrayList<String> getMessages() {
        return messages;
    }

    public String getMostRecentMessage() {
        return messages.get(messages.size()-1);
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return this.phoneNumber;
    }



    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Contact) {
            Contact o = (Contact) obj;
            return this.name.equals(o.name) && this.phoneNumber.equals(o.phoneNumber);
        }
        return false;
    }

    @Override
    public String toString() { //TODO: need to rework the toString. Not entirely sure what I'll use it for just yet.
        return "Contact{" +
                "name='" + name + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", messages=" + messages +
                '}';
    }
}
