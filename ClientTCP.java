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

            // odczytaj wstepne informacje i wyswietlij je
            String line = in.readLine();
            System.out.println(line);

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

                // odczytaj odpowiedzi klienta
                System.out.print("Twoje odpowiedzi (A/B/C/D bez spacji, np. AD): ");
                String answer = scanner.nextLine().trim().toUpperCase();

                // sprawdz poprawnosc podanych odpowiedzi
                if (!answer.matches("[A-D]+")) {
                    answer = "BRAK";
                }

                // wyślij odpowiedzi do serwera
                out.write(answer);
                out.newLine();
                out.flush();
            }

        } catch (IOException e) {
            System.err.println("Błąd klienta: " + e.getMessage());
        }
    }
}



