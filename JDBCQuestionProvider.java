import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JDBCQuestionProvider {
    private Connection connection;

    JDBCQuestionProvider(String dbUrl, String dbUser, String dbPass) {
        try {
            connection = DriverManager.getConnection(dbUrl, dbUser, dbPass);

            Statement stmt = connection.createStatement();
            stmt.addBatch("CREATE DATABASE IF NOT EXISTS quiz;");
            stmt.addBatch("USE quiz;");
            stmt.addBatch("CREATE TABLE IF NOT EXISTS question(id int(3) primary key, text varchar(200), correct varchar(4));");
            stmt.addBatch("CREATE TABLE IF NOT EXISTS question_answer(answer_id int(3) primary key, question_id int(3), text varchar(200), foreign key(question_id) references question(id));");
            stmt.executeBatch();

            ResultSet set = connection.createStatement().executeQuery("SELECT COUNT(*) FROM question");

            if(set.next() && set.getInt(1) != 0) {
                return;
            }

            List<Question> questions = FileQuestionLoader.loadQuestions("bazaPytan.txt");

            int questionId = 1;
            int answerId = 1;
            PreparedStatement questionInsert = connection.prepareStatement("INSERT INTO question VALUES (?, ?, ?);");
            PreparedStatement answerInsert = connection.prepareStatement("INSERT INTO question_answer VALUES (?, ?, ?);");
            for(Question q : questions) {
                questionInsert.setInt(1, questionId);
                questionInsert.setString(2, q.text);
                questionInsert.setString(3, q.correct);

                questionInsert.executeUpdate();

                for(String ans : q.options){
                    answerInsert.setInt(1, answerId);
                    answerInsert.setInt(2, questionId);
                    answerInsert.setString(3, ans);

                    answerInsert.executeUpdate();
                    answerId++;
                }

                questionId++;
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }


    public List<Question> getQuestions(){
        try {
            List<Question> result = new ArrayList<>();
            ResultSet questions = connection.createStatement().executeQuery("SELECT * FROM question");
            ResultSet answers = null;

            while(questions.next()) {
                int questionId = questions.getInt("id");
                String text = questions.getString("text");
                String correct = questions.getString("correct");

                answers = connection.createStatement().executeQuery("SELECT * FROM question_answer WHERE question_id = " + questionId);

                String[] qa = new String[4];
                for(int i = 0; i < 4; i++) {
                    answers.next();
                    qa[i] = answers.getString("text");
                }

                result.add(new Question(text, qa, correct));
            }

            answers.close();
            questions.close();
            return result;
        }
        catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

}
