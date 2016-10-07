import javafx.scene.control.Button;

/**
 * Created by Steven Landau on 10/6/2016.
 */
public class ButtonContact extends Button {
    private Contact contact;

    public ButtonContact(Contact c) {
        super(c.getName());
        this.contact = c;
    }
}
