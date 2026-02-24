import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LogAnalyzer2 {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Uso: java LogAnalyzer <arquivos_de_log>");
            System.exit(1);
        }

        List<LogProcessor> processors = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();

        // Cria e inicia thread pra cada arquivo
        for (String fileName : args) {
            LogProcessor processor = new LogProcessor(fileName);
            processors.add(processor);
            
            Thread thread = new Thread(processor);
            threads.add(thread);
            thread.start();
        }

        // Aguarda a conclusão todas pra poder somar os totais
        for (Thread thread : threads) {
            try {
                thread.join(); 
            } catch (InterruptedException e) {
                System.err.println("A thread principal foi interrompida.");
                Thread.currentThread().interrupt();
            }
        }

        // Com todas as threads encerradosa vamos iniciar a contagem:
        int total200 = 0;
        int total500 = 0;
        for (LogProcessor processor : processors) {
            total200 += processor.getCount200();
            total500 += processor.getCount500();
        }

        System.out.println("===== RESULTADO FINAL =====");
        System.out.println("Total 200: " + total200);
        System.out.println("Total 500: " + total500);
    }
}

class LogProcessor implements Runnable {
    private String fileName;
    // Contador local de thread
    private int count200 = 0;
    private int count500 = 0;

    public LogProcessor(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public void run() {
        System.out.println("Processando arquivo: " + fileName);
        
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length == 3) {
                    String code = parts[2];
                    if (code.equals("200")) {
                        count200++;
                    } else if (code.equals("500")) {
                        count500++;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler arquivo: " + fileName);
            e.printStackTrace();
        }
    }

    public int getCount200() {
        return count200;
    }

    public int getCount500() {
        return count500;
    }
}