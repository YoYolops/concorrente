import java.io.*;
import java.util.concurrent.*;

public class LogAnalyzer3 {

    static int total200 = 0;
    static int total500 = 0;

    public static synchronized void addResults(int t200, int t500) {
        total200 += t200;
        total500 += t500;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Uso: java LogAnalyzer3 <arquivos_de_log>");
            System.exit(1);
        }

        // Já que são 10 arquivos (no exemplo do lab pelo menos), optei por criar 10 threads. uma para cada arquivo de log
        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (String fileName : args) {
            executor.execute(new FileProcessor(fileName));
        }

        executor.shutdown();
        try {
            // Espera pela a eternidade
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("===== RESULTADO FINAL =====");
        System.out.println("Total 200: " + total200);
        System.out.println("Total 500: " + total500);
    }

    static class FileProcessor implements Runnable {
        private final String fileName;

        FileProcessor(String fileName) {
            this.fileName = fileName;
        }

        public void run() {
            int local200 = 0;
            int local500 = 0;
            System.out.println("Processando arquivo: " + fileName);
            try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(" ");
                    if (parts.length == 3) {
                        String code = parts[2];
                        if (code.equals("200")) local200++;
                        else if (code.equals("500")) local500++;
                    }
                }
            } catch (IOException e) {
                System.err.println("Erro ao ler arquivo: " + fileName);
            }

            addResults(local200, local500);
        }
    }
}