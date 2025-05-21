import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ClientTCP {
    private static final int PORT = 20;

    public static void main(String args[]) {
        try(Socket socket = new Socket("localhost", PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            Scanner scanner = new Scanner(System.in)) {

            // główna pętla
            while (true) {
                // odczytaj pytanie z odpowiedziami
                String question = in.readLine();
                // jesli brak pytania/podano wynik, wyswietlij go i zakoncz
                if (question == null || question.startsWith("Wynik:")) {
                    System.out.println(question);
                    break;
                }

                // podziel na czesci
                String[] parts = question.split("\\|");

                // wyswietlij pytanie z opcjami odpowiedzi
                System.out.println("\n" + parts[0]);
                for (int i = 1; i <= 4; i++) {
                    System.out.println(parts[i]);
                }

                try {
                    String answer = readInputWithTimeout(30);

                    // sprawdz poprawnosc podanych odpowiedzi
                    if (!answer.matches("[A-D]+")) {
                        answer = "BRAK";
                    }

                    // wyślij odpowiedzi do serwera
                    out.write(answer);
                    out.newLine();
                    out.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        } catch (IOException e) {
            System.err.println("Błąd klienta: " + e.getMessage());
        }
    }

    private static String readInputWithTimeout(int timeoutSeconds) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        long endTime = System.currentTimeMillis() + timeoutSeconds * 1000L;

        System.out.print("Twoje odpowiedzi (A/B/C/D bez spacji, np. AD): ");
        try {
            while (System.currentTimeMillis() < endTime) {
                if (reader.ready()) {
                    return reader.readLine().trim().toUpperCase();
                }
                Thread.sleep(100);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("\nCzas minął. Brak odpowiedzi.");
        return "BRAK";
    }
}



