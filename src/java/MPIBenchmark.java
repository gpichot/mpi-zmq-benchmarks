import mpi.*;


public class MPIBenchmark {

    public static int WIDTH = 2048;
    public static int NB_MESSAGES = 1 * 1000 * 1000;

    public static int RECEIVER = 1;
    public static int SENDER = 0;

    public static void send_messages() throws MPIException {
        for(int j = 0; j < NB_MESSAGES; ++j) {
            char buffer[] = new char[WIDTH];
            for(int i = 0; i < WIDTH; ++i) {
                buffer[i] = 'a';
            }
            MPI.COMM_WORLD.send(buffer, WIDTH, MPI.CHAR, RECEIVER, 0);

            if((j + 1) % (1 * 1000 * 1000) == 0) {
                System.out.println(String.format("Sent %d messages with size %d.\n", (j + 1), WIDTH));
            }
        }
    }

    public static void receive_messages() throws MPIException {
        char buffer[] = new char[10];
        int buffer_size = 10;
        for(int j = 0; j < NB_MESSAGES; ++j) {
            int size = WIDTH;
            Status status;

            //status = MPI.COMM_WORLD.probe(SENDER, 0);

            //size = status.getCount(MPI.CHAR);

            if(size > buffer_size) {
                buffer = new char[size];
                buffer_size = size;
            }

            status = MPI.COMM_WORLD.recv(buffer, size, MPI.CHAR, SENDER, 0); 

            ///if((j + 1) % (10 * 1000 * 1000) == 0) {
            ///    printf("Received %d messages with size %d.\n", (j + 1), WIDTH);
            ///    printf("Message: %s\n", buffer+2048-32);
            ///}
        }
    }

    public static void main(String [] args) {
        int NbPE;
        int Me;
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
            e.printStackTrace();
        }

    }

}
