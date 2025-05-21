import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileQuestionLoader {
    public static List<Question> loadQuestions(String fileName) {
        // otworz plik do odczytu
        List<Question> questions = new ArrayList<>();
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

            return questions;
        }
        catch (IOException e) {
            System.err.println("Błąd wczytywania pytań: " + e.getMessage());
            System.exit(1);
        }
        return null;
    }
}
