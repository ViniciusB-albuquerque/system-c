#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>

int do_sum(const char *path) {
    FILE *file = fopen(path, "rb");  // open in binary mode
    if (file == NULL) {
        return -1;  // indicate error
    }

    int sum = 0;
    int byte;

    while ((byte = fgetc(file)) != EOF) {
        sum += byte;
    }

    fclose(file);
    return sum;
}

typedef struct {
    const char *path;
    int sum;
} Arg;

void *worker(void *p) {
    Arg *a = (Arg *) p;
    a->sum = do_sum(a->path);
    return NULL;
}

int main(int argc, char *argv[]) {
    int n = argc - 1;
    pthread_t threads[n];
    Arg args[n];

    for (int i = 0; i < n; i++) {
        args[i].path = argv[i + 1];
        pthread_create(&threads[i], NULL, worker, &args[i]);
    }

    for (int i = 0; i < n; i++) {
        pthread_join(threads[i], NULL);
    }

    for (int i = 0; i < n; i++) {
        if (args[i].sum >= 0) {
            printf("%s : %d\n", args[i].path, args[i].sum);
        }
    }

    return 0;
}
