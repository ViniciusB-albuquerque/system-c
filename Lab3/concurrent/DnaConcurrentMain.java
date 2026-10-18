import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

class DnaWorker implements Runnable {
    private File[] files;
    private String pattern;
    private int threadId;
    private int numThreads;
    private long result;

    public DnaWorker(File[] files, String pattern, int threadId, int numThreads) {
        this.files = files;
        this.pattern = pattern;
        this.threadId = threadId;
        this.numThreads = numThreads;
        this.result = 0;
    }

    @Override
    public void run() {
        long localTotal = 0;

        for (int i = threadId; i < files.length; i += numThreads) {
            try {
                localTotal += DnaConcurrentMain.countInFile(files[i], pattern);
            } catch (IOException e) {
                System.err.println("Erro ao ler arquivo " + files[i].getName() + ": " + e.getMessage());
            }
        }

        this.result = localTotal;
    }

    public long getResult() {
        return result;
    }
}

public class DnaConcurrentMain {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: java DnaConcurrentMain DIRETORIO_ARQUIVOS PADRAO");
            System.err.println("Exemplo: java DnaConcurrentMain dna_inputs CGTAA");
            System.exit(1);
        }

        String dirName = args[0];
        String pattern = args[1];

        File dir = new File(dirName);
        if (!dir.isDirectory()) {
            System.err.println("Caminho não é um diretório: " + dirName);
            System.exit(2);
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));
        if (files == null || files.length == 0) {
            System.err.println("Nenhum arquivo .txt encontrado em: " + dirName);
            System.exit(3);
        }

        int numThreads = Math.min(files.length, Runtime.getRuntime().availableProcessors());

        Thread[] threads = new Thread[numThreads];
        DnaWorker[] workers = new DnaWorker[numThreads];

        for (int i = 0; i < numThreads; i++) {
            workers[i] = new DnaWorker(files, pattern, i, numThreads);
            threads[i] = new Thread(workers[i]);
            threads[i].start();
        }

        for (int i = 0; i < numThreads; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Execução interrompida.");
                System.exit(4);
            }
        }

        long total = 0;

        for (int i = 0; i < numThreads; i++) {
            total += workers[i].getResult();
        }

        System.out.println("Sequência " + pattern + " foi encontrada " + total + " vezes.");
    }

    public static long countInFile(File file, String pattern) throws IOException {
        long total = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();

                // simulando delay de processamento
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return total;
                }

                if (!line.isEmpty()) {
                    total += countInSequence(line, pattern);
                }
            }
        }

        return total;
    }

    public static long countInSequence(String sequence, String pattern) {
        if (sequence == null || pattern == null) {
            return 0;
        }

        int n = sequence.length();
        int m = pattern.length();

        if (m == 0 || n < m) {
            return 0;
        }

        long count = 0;

        for (int i = 0; i <= n - m; i++) {
            if (sequence.regionMatches(false, i, pattern, 0, m)) {
                count++;
            }
        }

        return count;
    }
}