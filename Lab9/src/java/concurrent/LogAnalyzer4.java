import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class LogAnalyzer4 {
    // Feito
    static class Result {
        int total200;
        int total500;

        Result(int t200, int t500) {
            total200 = t200;
            total500 = t500;
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Uso: java LogAnalyzer <arquivos_de_log>");
            System.exit(1);
        }

        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<Result>> futures = new ArrayList<>();

        for (String fileName : args) {
            System.out.println("Processando arquivo: " + fileName);

            Callable<Result> task = () -> processFile(fileName);
            Future<Result> future = executor.submit(task);

            futures.add(future);
        }

        int total200 = 0;
        int total500 = 0;
        // Como a agregação de resultados é serial,
        // pode parecer que o processamento também é serial. Mas não
        // é esse o caso. O processamento é concorrente e os prints são seriais
        for (Future<Result> f : futures) {
            try {
                Result r = f.get();
                total200 += r.total200;
                total500 += r.total500;
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();

        System.out.println("===== RESULTADO FINAL =====");
        System.out.println("Total 200: " + total200);
        System.out.println("Total 500: " + total500);
    }

    public static Result processFile(String fileName) {
        int local200 = 0;
        int local500 = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" ");

                if (parts.length == 3) {
                    String code = parts[2];
                    if (code.equals("200")) {
                        local200++;
                    } else if (code.equals("500")) {
                        local500++;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler arquivo: " + fileName);
            e.printStackTrace();
        }
        return new Result(local200, local500);
    }
}