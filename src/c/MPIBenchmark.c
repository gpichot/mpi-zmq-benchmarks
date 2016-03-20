#include <mpi.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>


#define WIDTH 2048
#define NB_MESSAGES (1 * 1000 * 1000)

#define RECEIVER 1
#define SENDER 0


void send_messages() {
    int j, i;
    for(j = 0; j < NB_MESSAGES; ++j) {
        char buffer[WIDTH];
        for(i = 0; i < WIDTH; ++i) {
            buffer[i] = 'a';
        }
        MPI_Send(&buffer, WIDTH, MPI_CHAR, RECEIVER, 0, MPI_COMM_WORLD); 

        if((j + 1) % (1 * 1000 * 1000) == 0) {
            printf("Sent %d messages with size %d.\n", (j + 1), WIDTH);
        }
    }
}

void receive_messages() {
    int j;
    char* buffer = (char*) malloc(20 * sizeof(char));
    int buffer_size = 20;
    for(j = 0; j < NB_MESSAGES; ++j) {
        int size = WIDTH;
        MPI_Status status;

        //MPI_Probe(SENDER, 0, MPI_COMM_WORLD, &status);

        //MPI_Get_count(&status, MPI_CHAR, &size);

        if(size > buffer_size) {
            free(buffer);
            printf("Free buffer and alloc.\n");
            buffer = (char*) malloc(size * sizeof(char));
            buffer_size = size;
        }


        MPI_Recv(buffer, size, MPI_CHAR, SENDER, 0, MPI_COMM_WORLD, &status); 

        ///if((j + 1) % (10 * 1000 * 1000) == 0) {
        ///    printf("Received %d messages with size %d.\n", (j + 1), WIDTH);
        ///    printf("Message: %s\n", buffer+2048-32);
        ///}
    }
    free(buffer);

}


int main(int argc, char * argv[]) {

    int NbPE;
    int Me;
    int provided;

    MPI_Init_thread(&argc, &argv, MPI_THREAD_MULTIPLE, &provided);
    printf("Threading level provided: %d\n", provided);

    MPI_Comm_size(MPI_COMM_WORLD,&NbPE);
    MPI_Comm_rank(MPI_COMM_WORLD,&Me);

    if(NbPE != 2) {
        printf("This binary should be launched with 2 MPI Processes.i\n");
        return -1;
    }

    MPI_Barrier(MPI_COMM_WORLD);
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
            send_messages();
        } else {
            receive_messages();
        }
        time_end = MPI_Wtime();
        cumul += time_end - time_start;
        printf("%d loop done in %.4fs.\n", i, time_end - time_start);
        sleep(1);
    }

    printf("Sent %d in %.3fs.\n", nb_loops * NB_MESSAGES, (cumul));


    MPI_Finalize();

    return EXIT_SUCCESS;
}
