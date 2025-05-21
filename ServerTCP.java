import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerTCP {
    private static final int PORT = 20;
    private static final int MAX_CLIENTS = 250;

    private static final List<Question> questions = new ArrayList<>();

    public static void main(String args[]) {
        // zaladuj pytania
        loadQuestions("bazaPytan.txt");

        ExecutorService executor = Executors.newFixedThreadPool(MAX_CLIENTS);;
        try(ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
            // czekamy na zgłoszenie klienta ...
                Socket clientSocket = serverSocket.accept();
                executor.submit(new ServerTCPThread(clientSocket, questions));
            }
        //zamknięcie strumieni i połączenia
        } catch (Exception e) {
            System.err.println(e);
        }
    }


    private static void loadQuestions(String fileName) {
        // otworz plik do odczytu
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            // wczytaj linie
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|"); // podziel na czesci
                // na podstawie linii dodaj nowe pytanie do listy
                if (parts.length == 6) {
                    questions.add(new Question(parts[0], Arrays.copyOfRange(parts, 1, 5), parts[5].trim()));
                }
            }
        } catch (IOException e) {
            System.err.println("Błąd wczytywania pytań: " + e.getMessage());
        }
    }
}

class Question {
    String text; // pytanie
    String[] options; // odpowiedzi
    String correct; // poprawna(-ne) odpowiedź(-dzi)

    public Question(String text, String[] options, String correct) {
        this.text = text;
        this.options = options;
        this.correct = correct;
    }
}

class ServerTCPThread extends Thread {
    Socket socket;
    List<Question> questions;
    public ServerTCPThread(Socket socket, List<Question> questions)
    {
        super(); // konstruktor klasy Thread
        this.socket = socket;
        this.questions = questions;
    }

    public void run() // program wątku
    {
        String clientID = socket.getInetAddress() + ":" + socket.getPort(); // ID klienta
        System.out.println("Połączenie: " + clientID);

        List<String> clientAnswers = new ArrayList<>(); // odpowiedzi klienta
        int score = 0; // wynik

        // pobierz strumienie we/wy
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
            // dla kazdego pytania
            for (int i = 0; i < questions.size(); i++) {
                Question q = questions.get(i);

                // Wyślij pytanie do klienta
                out.write(q.text + "|" + String.join("|", q.options));
                out.newLine();
                out.flush();

                // pobierz odpowiedź
                String answer;

                answer = in.readLine();
                if (answer == null || answer.isEmpty()) {
                    answer = "BRAK";
                }
                clientAnswers.add(answer.toUpperCase());

                // sprawdz poprawnosc odpowiedzi
                if (isCorrect(answer, q.correct)) {
                    score++;
                }
            }

            // Zapisz odpowiedzi i wynik
            saveAnswers(clientID, clientAnswers);
            saveResult(clientID, score, questions.size());

            // Wyślij wynik do klienta
            out.write("Wynik: " + score + "/" + questions.size());
            out.newLine();
            out.flush();

        } catch (IOException e) {
            System.err.println("Błąd klienta " + clientID + ": " + e.getMessage());
        } finally {
            try {
                socket.close(); // zamknij polaczenie
            } catch (IOException e) {
                // ignoruj
            }
        }
    }

    private synchronized void saveAnswers(String clientID, List<String> answers) {
        // Otworz plik bazy odpowiedzi do zapisu w trybie append
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("bazaOdpowiedzi.txt", true))) {
            // zapisz odpowiedzi
            for (int i = 0; i < answers.size(); i++) {
                writer.write(clientID + "|Pytanie " + (i + 1) + "|" + answers.get(i));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Błąd zapisu odpowiedzi: " + e.getMessage());
        }
    }

    private synchronized void saveResult(String clientID, int score, int total) {
        // Otworz plik bazy wyników do zapisu w trybie append
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("wyniki.txt", true))) {
            // zapisz wynik
            writer.write(clientID + "|Wynik:" + score + "/" + total);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Błąd zapisu wyniku: " + e.getMessage());
        }
    }

    // Porównanie odpowiedzi
    private boolean isCorrect(String given, String correct) {
        // przekształć Stringi odpowiedzi podanych i poprawnych na tablice znaków
        char[] a = given.toUpperCase().replaceAll("[^A-D]", "").toCharArray(); // usun wszystkie zbędne znaki
        char[] b = correct.toUpperCase().toCharArray();

        Arrays.sort(a);
        Arrays.sort(b);
        return Arrays.equals(a, b); // sprawdź czy odpowiedzi podane są poprawne
    }
}