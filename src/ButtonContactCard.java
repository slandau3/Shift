import edu.rit.cs.steven_landau.shiftmobile.ContactCard;
import javafx.scene.control.Button;

/**
 * Created by slandau on 10/13/2016.
 */
public class ButtonContactCard extends Button {
    public ContactCard c;
    public ButtonContactCard(ContactCard c) {
        super(c.getName());
        this.c = c;
    }
}
