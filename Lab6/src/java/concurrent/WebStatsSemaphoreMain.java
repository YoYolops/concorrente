import java.util.Random;
import java.util.concurrent.Semaphore;

class WebStatsSemaphore {
    private final Semaphore controleAcesso = new Semaphore(1);
    private long totalAccess = 0;
    private int totalPurchases = 0;
    private int totalFailures = 0;
    private int totalNothing = 0;
    private int onlineUsers = 0;

    // Usuario acessa o sistema
    public void access() {
        try {
            controleAcesso.acquire();
            totalAccess++;
            onlineUsers++;
        } catch (InterruptedException e) {
            System.out.println("FAILED TO ACQUIRE CONTROLE ACESSO");
        } finally {
            controleAcesso.release();
        }
    }

    // Usuario realiza uma compra
    public void purchase() {
        try {
            controleAcesso.acquire();
            totalPurchases++;
        } catch (InterruptedException e) {
            System.out.println("FAILED TO ACQUIRE CONTROLE ACESSO");
        } finally {
            controleAcesso.release();
        }
    }

    // Ocorreu uma falha
    public void failure() {
        try {
            controleAcesso.acquire();
            totalFailures++;
        } catch (InterruptedException e) {
            System.out.println("FAILED TO ACQUIRE CONTROLE ACESSO");
        } finally {
            controleAcesso.release();
        }
    }

    // Usuario nem compra nem falha
    public void nothing() {
        try {
            controleAcesso.acquire();
            totalNothing++;
        } catch (InterruptedException e) {
            System.out.println("FAILED TO ACQUIRE CONTROLE ACESSO");
        } finally {
            controleAcesso.release();
        }
    }

    // Usuario faz logout
    public void logout() {
        try {
            controleAcesso.acquire();
            onlineUsers--;
        } catch (InterruptedException e) {
            System.out.println("FAILED TO ACQUIRE CONTROLE ACESSO");
        } finally {
            controleAcesso.release();
        }
    }

    // Impressao das estatisticas atuais
    public void printStats() {
        System.out.println("========= Estatisticas do Sistema SEMAPHORE =========");
        System.out.println("Total de Acessos: " + totalAccess);
        System.out.println("Total de Compras: " + totalPurchases);
        System.out.println("Total de Falhas: " + totalFailures);
        System.out.println("Total de acessos sem compras ou falhas: " + totalNothing);
        System.out.println("Usuarios Online: " + onlineUsers);
        System.out.println("=======================================================");
    }
}

// Classe que simula acoes de um usuario no sistema
class UserSimulationSemaphore implements Runnable {
    private WebStatsSemaphore stats;
    private Random random;

    public UserSimulationSemaphore(WebStatsSemaphore stats) {
        this.stats = stats;
        this.random = new Random();
    }

    @Override
    public void run() {
        try {
            // Usuario acessa o sistema
            stats.access();

            // Simula tempo navegando
            Thread.sleep(random.nextInt(300));

            // Decide se faz compra, falha ou apenas navega
            int action = random.nextInt(3); // 0 = compra, 1 = falha, 2 = nada
            if (action == 0) {
                stats.purchase();
            } else if (action == 1) {
                stats.failure();
            } else {
                stats.nothing();
            }

            // Simula tempo antes de logout
            Thread.sleep(random.nextInt(200));

            // Usuario sai do sistema
            stats.logout();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

// Classe principal que executa a simulacao concorrente
public class WebStatsSemaphoreMain {
    public static void main(String[] args) {

        if (args.length < 1) {
            System.err.println("Usage: java WebStatsSemaphoreMain number_users");
            System.exit(1);
        }

        int numUsers = Integer.valueOf(args[0]); // quantidade de threads (usuarios simultaneos)

        WebStatsSemaphore stats = new WebStatsSemaphore();
        Thread[] users = new Thread[numUsers];

        // Criacao e inicializacao das threads
        for (int i = 0; i < numUsers; i++) {
            users[i] = new Thread(new UserSimulationSemaphore(stats));
            users[i].start();
        }

        // Aguarda todas as threads terminarem
        for (int i = 0; i < numUsers; i++) {
            try {
                users[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Imprime estatisticas finais (possivelmente incorretas)
        stats.printStats();
    }
}