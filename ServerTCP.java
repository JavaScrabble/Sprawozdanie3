import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.sql.DriverManager;
import java.sql.SQLException;


public class ServerTCP {
    private static final int PORT = 20;
    private static final int MAX_CLIENTS = 250;

    private static List<Question> questions;

    public static void main(String args[]) {
        // zaladuj pytania
        JDBCQuestionProvider provider = new JDBCQuestionProvider("jdbc:mysql://localhost:3306", "root", "");
        questions = provider.getQuestions();

        ExecutorService executor = Executors.newFixedThreadPool(MAX_CLIENTS);
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
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/quiz", "root", "")) {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO answer_log (client_id, question_number, answer) VALUES (?, ?, ?)");
            for (int i = 0; i < answers.size(); i++) {
                ps.setString(1, clientID);
                ps.setInt(2, i + 1);
                ps.setString(3, answers.get(i));
                ps.addBatch();
            }
            ps.executeBatch();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Błąd zapisu odpowiedzi do bazy: " + e.getMessage());
        }
    }

    private synchronized void saveResult(String clientID, int score, int total) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/quiz", "root", "")) {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO result (client_id, score, total) VALUES (?, ?, ?)");
            ps.setString(1, clientID);
            ps.setInt(2, score);
            ps.setInt(3, total);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Błąd zapisu wyniku do bazy: " + e.getMessage());
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