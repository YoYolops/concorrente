import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class DnaConcurrentMain {

    private static class DnaWorker implements Runnable {
        private File[] files;
        private String pattern;
        private long[] total;

        public DnaWorker(File[] files, String pattern, long[] total) {
            this.files = files;
            this.pattern = pattern;
            this.total = total;
        }

        @Override
        public void run() {
            try {
                long localTotal = 0;
                for(File f : files){
                    localTotal += countInFile(f, pattern);
                }
                synchronized(total) {
                    total[0] += localTotal;
                }
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: java DnaSerialMain DIRETORIO_ARQUIVOS PADRAO");
            System.err.println("Exemplo: java DnaSerialMain dna_inputs CGTAA");
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

        try {
            long[] total = {0};

            int numThreads = 10;
            List<Thread> threads = new ArrayList<>();
            int filesPerThread = files.length / numThreads;
            int remainder = files.length % numThreads;
            int start = 0;

            for(int i = 0; i < numThreads; i++){
                int end = start + filesPerThread + (i < remainder ? 1 : 0);
                File[] subset = Arrays.copyOfRange(files, start, end);
                DnaWorker worker = new DnaWorker(subset, pattern, total);
                Thread t = new Thread(worker);
                threads.add(t);
                t.start();
                start = end;
            }

            for(Thread t : threads){
                try{
                    t.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            System.out.println("Sequência " + pattern + " foi encontrada " + total[0] + " vezes.");
        } catch (Exception e) {
            System.err.println("Erro geral: " + e.getMessage());
            System.exit(4);
        }
    }


    public static long countInFile(File file, String pattern) throws IOException {
        long total = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
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