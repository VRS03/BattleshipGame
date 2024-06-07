import java.io.Serializable;

public class Message implements Serializable {
    static final long serialVersionUID = 42L;

    String messageType;

    String data;

    int row;
    int col;

}
