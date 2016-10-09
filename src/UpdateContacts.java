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
            // Just do this so reading the file does not give us a problem
            oos= new ObjectOutputStream(new FileOutputStream(new File("contacts.ser"), true));
            oos.flush();
            ois = new ObjectInputStream(new FileInputStream(new File("contacts.ser")));   // This may not solve our problems
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


    public void addContact(Contact c) { // Could I just store an arraylist of contacts inside the file?
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(new File("contacts.ser"), true));
            oos.writeObject(c);
            oos.flush();
            //System.out.println(c + " Written");
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
    }

    public void removeContact(Contact c) {  // DO NOT USE THIS METHOD FOR NOW
        ObjectInputStream ois = null;
        ObjectOutputStream oos = null;
        ArrayList<Contact> holder = new ArrayList<>();
        try {
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
    public void updateData(Contact c) {
        ObjectOutputStream oos = null;
        ObjectInputStream ois = null;
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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            // End of file
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
    }
}
