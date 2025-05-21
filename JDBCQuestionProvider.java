import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class JDBCQuestionProvider {
    private Connection connection;

    JDBCQuestionProvider(String dbUrl, String dbUser, String dbPass) {
        try {
            connection = DriverManager.getConnection(dbUrl, dbUser, dbPass);

            Statement init = connection.createStatement();
            init.addBatch("CREATE DATABASE IF NOT EXISTS quiz;");
            init.addBatch("USE quiz;");
            init.addBatch("CREATE TABLE IF NOT EXISTS question(id int(3) primary key, text varchar(200), correct varchar(4));");
            init.addBatch("CREATE TABLE IF NOT EXISTS question_answer(answer_id int(3) primary key, question_id int(3), text varchar(200), foreign key(question_id) references question(id));");
            init.executeBatch();
            init.close();

            ResultSet checkIfExists = connection.createStatement().executeQuery("SELECT 1 FROM question limit 1");

            if(checkIfExists.next()) {
                System.out.println("Znaleziono pytania w bazie danych!");
                return;
            }

            checkIfExists.getStatement().close();

            System.out.println("Wypełnianie bazy danych pytaniami z pliku");
            List<Question> questions = FileQuestionLoader.loadQuestions("bazaPytan.txt");

            int questionId = 1;
            int answerId = 1;
            PreparedStatement questionInsert = connection.prepareStatement("INSERT INTO question VALUES (?, ?, ?);");
            PreparedStatement answerInsert = connection.prepareStatement("INSERT INTO question_answer VALUES (?, ?, ?);");
            for(Question q : questions) {
                questionInsert.setInt(1, questionId);
                questionInsert.setString(2, q.text);
                questionInsert.setString(3, q.correct);

                questionInsert.addBatch();

                for(String ans : q.options){
                    answerInsert.setInt(1, answerId);
                    answerInsert.setInt(2, questionId);
                    answerInsert.setString(3, ans);

                    answerInsert.addBatch();
                    answerId++;
                }

                questionId++;
            }
            questionInsert.executeBatch();
            answerInsert.executeBatch();

            questionInsert.close();
            answerInsert.close();

            System.out.printf("Do bazy danych zapisano %d pytań%n", questionId);
        }
        catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    List<Question> getQuestions(){
        try {
            List<Question> result = new ArrayList<>();
            PreparedStatement answersStatement = connection.prepareStatement("SELECT * FROM question_answer WHERE question_id = ?");
            ResultSet questions = connection.createStatement().executeQuery("SELECT * FROM question");
            ResultSet answers;

            while(questions.next()) {
                int questionId = questions.getInt("id");
                String text = questions.getString("text");
                String correct = questions.getString("correct");

                answersStatement.setInt(1, questionId - 1);
                answers = answersStatement.executeQuery();

                String[] qa = new String[4];
                for(int i = 0; i < 4; i++) {
                    answers.next();
                    qa[i] = answers.getString("text");
                }

                result.add(new Question(text, qa, correct));
            }

            answersStatement.close();
            questions.getStatement().close();
            return result;
        }
        catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return Collections.emptyList();
    }

}
