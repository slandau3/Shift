import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by slandau on 10/13/2016.
 */
public class ConversationHolder implements Serializable {

    private ArrayList<Contact> contactHolder;
    public ConversationHolder(ArrayList<Contact> c) {
        this.contactHolder = c;
    }

    public ArrayList<Contact> getContactHolder() {
        return this.contactHolder;
    }
}
