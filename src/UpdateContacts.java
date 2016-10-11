import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.zip.InflaterInputStream;

/**
 * Created by Steven Landau on 10/6/2016.
 *
 *
 */
public class UpdateContacts {


    public UpdateContacts() {
        ObjectOutputStream oos = null;
        ObjectInputStream ois = null;
        try {
            /**
             * Bare with me on this one.
             * So I learned that you cannot technically 'append' to an ObjectOutputStream,
             * attempting to do so will give you a ton of errors. The way to get around this
             * is to make a new class ( I called it AppendableObjectOutputStream ) which overrides
             * a method, thus letting you 'append' to a file. This is black magic. What I believe
             * the method actually does is it writes a new header every time, thus by resetting
             * the header in the overridden method, we are technically appending to the file by
             * making a bunch of new "files within the file". The code below will attempt to
             * a AppendableObjectOutputStream. The reason it is in a try catch is because, in
             * the event the original header is overwritten, an error will be thrown when we
             * try to read from the file and file will be corrupted. We can get past this
             * by resetting the file with a normal ObjectOutputStream if this occurs. So to
             * reiterate, use AppendableObjectOutputStream when you want to append to a file.
             * Be careful with it, I have noticed that appending to a serializable file can
             * easily corrupt said file.
             */
            try {
                oos = new AppendableObjectOutputStream(new FileOutputStream(new File("contacts.ser"), true));
                oos.flush();
                ois = new ObjectInputStream(new FileInputStream(new File("contacts.ser")));
            } catch (StreamCorruptedException sce) {
                oos = new ObjectOutputStream(new FileOutputStream(new File("contacts.ser")));
                oos.flush();
                ois = new ObjectInputStream(new FileInputStream(new File("contacts.ser")));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (oos != null) {
                    oos.close();
                }
                if (ois != null) {
                    ois.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void addContact(final Contact c) { // Could I just store an arraylist of contacts inside the file?
        AppendableObjectOutputStream oos = null;
        try {
            oos = new AppendableObjectOutputStream(new FileOutputStream(new File("contacts.ser"), true));
            oos.flush();
            oos.writeObject(c);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
            // Should not get here
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        ArrayList<Contact> temp = new ArrayList<>();
        getContacts(temp);
        for (Contact e : temp) {
            System.out.println(e);
        }
    }


    /**
     * To be used to update the file when one Contact is changed (for messages only).
     * EX: When we receive a message the message will be added to the contacts ArrayList,
     * here it will be saved in the file.
     * @param c an individual contact
     */
    public void updateData(Contact c) {
        ObjectOutputStream oos = null;
        ArrayList<Contact> temp = new ArrayList<>();
        getContacts(temp);

        try {
            oos = new ObjectOutputStream(new FileOutputStream(new File("contacts.ser"))); // We want this to delete the contents of the file
            for (Contact con : temp) {
                if (con.equals(c)) {
                    oos.writeObject(c);
                } else {
                    oos.writeObject(con);
                }
                oos.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public void getContacts(ArrayList<Contact> cons) {

        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(new File("contacts.ser")));

            while (true) {
                Object o = ois.readObject();
                if (o instanceof Contact) {
                    Contact c = (Contact) o;
                    cons.add(c);
                }
            }
        } catch (FileNotFoundException | ClassNotFoundException e) {
            e.printStackTrace();
        } catch (EOFException eofe) {
            // do nothing
        }
        catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
