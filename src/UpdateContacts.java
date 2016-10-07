import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Created by Steven Landau on 10/6/2016.
 *
 * TODO: I have yet to decide when exactly this class will be used
 */
public class UpdateContacts {

    public static void addContact(Contact c) { // Could I just store an arraylist of contacts inside the file?
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(new File("contacts.ser")));
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
                    e.printStackTrace(); // Should never get here
                }
            }
        }

    }

    public static void removeContact(Contact c) {
        ObjectInputStream ois = null;
        ObjectOutputStream oos = null;
        ArrayList<Contact> holder = new ArrayList<>();
        try {
            ois = new ObjectInputStream(new FileInputStream(new File("contacts.ser")));
            while (true) {
                Object o = ois.readObject();
                if (o instanceof Contact) {
                    Contact con = (Contact) o;
                    if (!con.equals(c)) {
                        holder.add(con);
                    }
                }
            }
        } catch (IOException e) {
            // Out of things to read.
        } catch (ClassNotFoundException e) {
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
        Path path = FileSystems.getDefault().getPath(System.getProperty("user.dir"), "contacts.ser"); // We are going to delete and remake this file without a specific contact.
        try {
            Files.delete(path);
            oos = new ObjectOutputStream(new FileOutputStream(new File("contacts.ser")));
            for (Contact con : holder) {
                oos.writeObject(con);
                oos.flush();

            }
        } catch (IOException e) {
            e.printStackTrace(); //fnf
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

    /**
     * To be used to update the file when one Contact is changed (for messages only).
     * EX: When we receive a message the message will be added to the contacts ArrayList,
     * here it will be saved in the file.
     * @param c an individual contact
     */
    public static void updateData(Contact c) {
        // TODO: similar to the other two but we just want to put the updated information of a Contact into the file
        ObjectInputStream ois = null;
        ObjectOutputStream oos = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(new File("contacts.ser")));
            oos = new ObjectOutputStream(new FileOutputStream(new File("contacts.ser")));
            while (true) {
                Object in = ois.readObject();
                if (in instanceof Contact) {
                    Contact other = (Contact) in;
                    if (other.equals(c)) {
                        oos.writeObject(c);
                    } else {
                        oos.writeObject(other);
                    }
                    oos.flush();
                }

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            // Reached end of file
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (oos != null) {
                try {
                    oos.close();  // Close output stream
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (ois != null) {
                try {
                    ois.close();  // Close input stream
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
