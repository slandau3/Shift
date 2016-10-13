import edu.rit.cs.steven_landau.shiftmobile.ContactCard;

import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.util.ArrayList;

/**
 * Created by Steven Landau on 10/12/2016.
 */
public class UpdateContactCards {
    private static String filename = FileSystemView.getFileSystemView().getDefaultDirectory().getPath() + "\\contactCards.ser";

    public UpdateContactCards(ArrayList<ContactCard> cc) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(filename)));
            oos.writeObject(cc);
            oos.flush();
            oos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
