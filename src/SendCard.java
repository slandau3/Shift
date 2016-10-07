import java.io.Serializable;

/**
 * Created by Steven Landau on 10/6/2016.
 *
 * SendCard lets the server know that we want to send
 * a text message. The text message will contain a message and the contact info of that person.
 */
public class SendCard implements Serializable{
    private String msg;
    private Contact contact;

    public SendCard(String msg, Contact contact) {
        this.msg = msg;
        this.contact = contact;
    }

    public String getMsg() {
        return msg;
    }

    public Contact getContact() {
        return contact;
    }
}
