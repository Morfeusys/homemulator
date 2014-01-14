import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.util.Random;

/**
 * Created by morfeusys on 1/10/14.
 */
public class Satellite {

    private final ZContext context = new ZContext(1);

    private ZMQ.Socket control;
    private ZMQ.Socket stream;

    public Satellite() {
        stream = context.createSocket(ZMQ.PUB);
        stream.connect("tcp://localhost:5556");
        createControlSocket();
    }

    private void createControlSocket() {
        control = context.createSocket(ZMQ.REQ);
        control.connect("tcp://localhost:5555");
    }

    public void start() {
        int id = new Random(System.currentTimeMillis()).nextInt(1000);
        String channel = String.valueOf(id);

        control.setReceiveTimeOut(100);
        control.send("");
        String resp = control.recvStr();

        if(resp == null) {
            System.out.println("CAN'T CONNECT");
            context.destroySocket(control);
            createControlSocket();
            return;
        }

        control.setReceiveTimeOut(5000);

        control.send(channel);
        System.out.println("Satellite started " + channel);

        AudioThread audioThread = new AudioThread(channel);
        audioThread.start();

        String result = control.recvStr();
        audioThread.terminate();

        if(result == null) {
            System.out.println("TIMEOUT");
            context.destroySocket(control);
            createControlSocket();
        } else {
            System.out.println(result);
        }
    }

    public void destroy() {
        control.close();
        stream.close();
    }

    private class AudioThread extends Thread {

        private final String channel;
        private boolean stopped;

        private AudioThread(String channel) {
            this.channel = channel;
        }

        @Override
        public void run() {
            Random random = new Random(System.currentTimeMillis());
            while (!stopped) {
                byte[] data = new byte[1024];
                random.nextBytes(data);
                stream.sendMore(channel);
                stream.send(data);
            }
        }

        private void terminate() {
            stopped = true;
        }
    }

    public static void main(String[] args) {
        Satellite satellite = new Satellite();
        while (true) {
            try {
                System.out.print("Press Enter to start Satellite");
                if(System.in.read() == -1) {
                    break;
                }
                satellite.start();
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
        satellite.destroy();
    }
}
