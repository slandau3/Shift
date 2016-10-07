/**
 * Created by Steven Landau on 10/6/2016.
 */
public class SendCard {
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
