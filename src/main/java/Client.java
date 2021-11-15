import java.net.Socket;

public class Client {
    public Socket socket;
    public Thread thread;

    public Client(Socket socket, Thread thread) {
        this.socket = socket;
        this.thread = thread;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public Thread getThread() {
        return thread;
    }

    public void setThread(Thread thread) {
        this.thread = thread;
    }
}
