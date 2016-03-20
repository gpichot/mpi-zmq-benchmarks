#include <mpi.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <zmq.h>


#define WIDTH 2048
#define NB_MESSAGES (1 * 1000 * 1000)

#define RECEIVER 1
#define SENDER 0


void send_messages(void *socket) {
    int j, i;
    int rc;
    zmq_msg_t query;
    for(j = 0; j < NB_MESSAGES; ++j) {
        char buffer[WIDTH];
        for(i = 0; i < WIDTH; ++i) {
            buffer[i] = 'a';
        }
        //MPI_Send(&buffer, WIDTH, MPI_CHAR, RECEIVER, 0, MPI_COMM_WORLD); 
        rc = zmq_send(socket, &buffer, WIDTH, 0);
        

        if((j + 1) % (1 * 1000 * 1000) == 0) {
            printf("Sent %d messages with size %d.\n", (j + 1), WIDTH);
        }
    }
}

void receive_messages(void *socket) {
    int j;
    int rc;
    zmq_msg_t query;
    int buffer_size = 20;
    char* buffer = (char*) malloc(buffer_size * sizeof(char));
    for(j = 0; j < NB_MESSAGES; ++j) {
        int size = WIDTH;
        //MPI_Status status;

        //MPI_Probe(SENDER, 0, MPI_COMM_WORLD, &status);

        //MPI_Get_count(&status, MPI_CHAR, &size);
        if(size > buffer_size) {
            printf("Reallocate buffer\n");
            free(buffer);
            buffer = (char*) malloc(size * sizeof(char));
            buffer_size = size;
        }
        rc = zmq_recv(socket, buffer, WIDTH, 0);

        //MPI_Recv(buffer, size, MPI_CHAR, SENDER, 0, MPI_COMM_WORLD, &status); 

        //if((j + 1) % (1 * 1000 * 1000) == 0) {
        //    printf("Received %d messages with size %d.\n", (j + 1), WIDTH);
        //}
    }
    free(buffer);

}


int main(int argc, char * argv[]) {

    int NbPE;
    int Me;
    int provided;
    void *context;
    void *socket;
    int rc;

    MPI_Init_thread(&argc, &argv, MPI_THREAD_MULTIPLE, &provided);
    printf("Threading level provided: %d\n", provided);
    

    MPI_Comm_size(MPI_COMM_WORLD,&NbPE);
    MPI_Comm_rank(MPI_COMM_WORLD,&Me);

    if(NbPE != 2) {
        printf("This binary should be launched with 2 MPI Processes.i\n");
        return -1;
    }

    MPI_Barrier(MPI_COMM_WORLD);

    context = zmq_ctx_new();
    if(Me == SENDER) {
        socket = zmq_socket(context, ZMQ_PUSH);
        rc = zmq_connect(socket, "tcp://cam01:3002");
    } else {
        socket = zmq_socket(context, ZMQ_PULL);
        rc = zmq_bind(socket, "tcp://*:3002");
    }

    double time_start;
    double time_end;
    double cumul = 0;

    int nb_loops = 5;
    int i;
    for(i = 0; i < nb_loops; ++i) {
        // Ensure the loop starts at the same time
        MPI_Barrier(MPI_COMM_WORLD);
        time_start = MPI_Wtime();
        if(Me == SENDER) {
            send_messages(socket);
        } else {
            receive_messages(socket);
        }
        time_end = MPI_Wtime();
        cumul += time_end - time_start;
        printf("%d loop done in %.4fs.\n", i, time_end - time_start);
        sleep(1);
    }

    printf("Sent %d in %.3fs.\n", nb_loops * NB_MESSAGES, (cumul));


    zmq_close(socket);
    zmq_term(context);
    MPI_Finalize();

    return EXIT_SUCCESS;
}
