import org.zeromq.ZMQ;

import java.util.Random;

/**
 * Created by morfeusys on 1/10/14.
 */
public class Recognizer {

    private final ZMQ.Context context = ZMQ.context(1);

    private final ZMQ.Socket control;
    private final ZMQ.Socket stream;
    private final ZMQ.Socket openhab;

    public Recognizer() {
        control = context.socket(ZMQ.REP);
        stream = context.socket(ZMQ.SUB);
        openhab = context.socket(ZMQ.PUSH);
    }

    public void start() {
        control.bind("tcp://*:5555");
        stream.bind("tcp://*:5556");
        openhab.connect("tcp://localhost:5557");

        System.out.println("Recognizer started");

        while (!Thread.currentThread().isInterrupted()) {
            String channel = control.recvStr();
            String response = processStream(channel);
            control.send(response);
            openhab.send("Lights ON");
        }
    }

    private String processStream(String channel) {
        stream.subscribe(channel.getBytes());
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < 3000) {
            stream.recv();
            byte[] data = stream.recv();
            if(data == null) {
                break;
            }
            System.out.print(".");
        }

        stream.unsubscribe(channel.getBytes());
        String result = new Random(System.currentTimeMillis()).nextInt(5) == 0 ? "ERROR" : "SUCCESS";
        System.out.println();
        System.out.println(result);
        return result;
    }

    public static void main(String[] args) {
        Recognizer recognizer = new Recognizer();
        recognizer.start();
    }

}
