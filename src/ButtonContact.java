import javafx.scene.control.Button;

/**
 * Created by Steven Landau on 10/6/2016.
 *
 * Acts like a button but also stores the Contact of the person the Button represents.
 * This allows for much easier modification of the GUI.
 */
public class ButtonContact extends Button {
    private Contact contact;

    public ButtonContact(Contact c) {
        super(c.getName());
        this.contact = c;
    }

    public Contact getContact() {
        return this.contact;
    }


}
