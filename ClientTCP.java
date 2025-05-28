import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ClientTCP {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        String address;
        int port;

        if (args.length == 2) {
            address = args[0];
            port = Integer.parseInt(args[1]);
        } else {
            System.out.print("Podaj adres serwera: ");
            address = scanner.nextLine();

            System.out.print("Podaj port serwera: ");
            while (true) {
                try {
                    port = Integer.parseInt(scanner.nextLine());
                    break;
                } catch (NumberFormatException e) {
                    System.out.print("Niepoprawny port. Podaj liczbę całkowitą: ");
                }
            }
        }

        System.out.println("Łączenie z serwerem " + address + " na porcie " + port + "...");

        try(Socket socket = new Socket(address, port);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

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



