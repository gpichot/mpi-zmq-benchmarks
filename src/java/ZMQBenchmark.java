import java.net.InetAddress;

import mpi.*;
import org.zeromq.ZMQ;


public class ZMQBenchmark {

    public static int WIDTH = 2048;
    public static int NB_MESSAGES = 1 * 1000 * 1000;

    public static int RECEIVER = 1;
    public static int SENDER = 0;

    public static ZMQ.Context context;
    public static ZMQ.Socket publisher;
    public static ZMQ.Socket suscriber;

    public static void send_messages() throws MPIException {
        for(int j = 0; j < NB_MESSAGES; ++j) {
            char buffer[] = new char[WIDTH];
            for(int i = 0; i < WIDTH; ++i) {
                buffer[i] = 'a';
            }
            publisher.send((new String(buffer)).getBytes(), 0);

            if((j + 1) % (1 * 1000 * 1000) == 0) {
                System.out.println(String.format("Sent %d messages with size %d.\n", (j + 1), WIDTH));
            }
        }
    }

    public static void receive_messages() throws MPIException {
        byte buffer[] = new byte[10];
        int buffer_size = 0;
        for(int j = 0; j < NB_MESSAGES; ++j) {
            int size = WIDTH;
            //Status status;

            //status = MPI.COMM_WORLD.probe(SENDER, 0);

            //size = status.getCount(MPI.CHAR);

            //if(size > buffer_size) {
            //    buffer = new char[size];
            //}

            //status = MPI.COMM_WORLD.recv(buffer, size, MPI.CHAR, SENDER, 0); 
            buffer = suscriber.recv(0);
            String mess = new String(buffer);

            ///if((j + 1) % (10 * 1000 * 1000) == 0) {
            ///    printf("Received %d messages with size %d.\n", (j + 1), WIDTH);
            ///    printf("Message: %s\n", buffer+2048-32);
            ///}
        }
    }

    public static void main(String [] args) {
        int NbPE;
        int Me = -1;
        int provided;

        try {
            provided = MPI.InitThread(args, MPI.THREAD_MULTIPLE);
            System.out.println(String.format("Threading level provided: %d\n", provided));

            NbPE = MPI.COMM_WORLD.getSize();
            Me = MPI.COMM_WORLD.getRank();

            if(NbPE != 2) {
                System.out.println("This binary should be launched with 2 MPI Processes.\n");
                System.exit(-1);
            }

            String hostname = InetAddress.getLocalHost().getHostName();

            context = ZMQ.context(1);
            if(Me == RECEIVER) {
                MPI.COMM_WORLD.send(hostname.toCharArray(), 5, MPI.CHAR, SENDER, 0);
                suscriber = context.socket(ZMQ.PULL);
                suscriber.bind("tcp://*:3002");
            } else {
                char receiver_host[] = new char[5];
                MPI.COMM_WORLD.recv(receiver_host, 5, MPI.CHAR, RECEIVER, 0);
                publisher = context.socket(ZMQ.PUSH);
                System.out.println(receiver_host);
                String ip = InetAddress.getByName(new String(receiver_host)).getHostAddress();
                publisher.connect("tcp://" + ip + ":3002");
            }

            MPI.COMM_WORLD.barrier();

            double time_start;
            double time_end;
            double cumul = 0;

            int nb_loops = 5;
            int i;
            for(i = 0; i < nb_loops; ++i) {
                // Ensure the loop starts at the same time
                MPI.COMM_WORLD.barrier();
                time_start = MPI.wtime();
                if(Me == SENDER) {
                    send_messages();
                } else {
                    receive_messages();
                }
                time_end = MPI.wtime();
                cumul += time_end - time_start;
                System.out.println(String.format("%d loop done in %.4fs.\n", i, time_end - time_start));
                Thread.sleep(1);
            }

            System.out.println(String.format("Sent %d in %.3fs.\n", NB_MESSAGES * nb_loops, cumul));


            MPI.Finalize();
        } catch(Exception e) {
            System.out.println("Rank: " + Me);
            e.printStackTrace();
        }

    }

}
