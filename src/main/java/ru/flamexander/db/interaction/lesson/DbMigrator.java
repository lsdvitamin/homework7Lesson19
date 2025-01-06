package ru.flamexander.db.interaction.lesson;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;

public class DbMigrator {

    private static DataSource dataSource;

    public DbMigrator(DataSource dataSource) {
        this.dataSource = dataSource;
        runScript();
    }

    public static void runScript() {
        String[] listCommands = writeScriptFile();
        for (String command : listCommands) {
            try {
                dataSource.getStatement().executeUpdate(command);
            } catch (SQLException e) {
                throw new ORMException("Ошибка выполнения команды " + command);
            }
        }
    }

    public static String[] writeScriptFile() {
        String file = "dbinit.sql";
        StringBuilder script = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while((line = reader.readLine()) != null) {
                script.append(line);
            }
            reader.close();
            return script.toString().split(";");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


}

