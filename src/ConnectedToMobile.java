import java.io.Serializable;

/**
 * Created by slandau on 10/18/16.
 */
public class ConnectedToMobile implements Serializable {
    private boolean isConnected;

    public ConnectedToMobile(boolean isConnected) {
        this.isConnected = isConnected;
    }

    public boolean isConnected() {
        return this.isConnected;
    }
}
