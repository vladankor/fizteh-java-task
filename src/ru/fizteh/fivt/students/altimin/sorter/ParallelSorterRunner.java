package ru.fizteh.fivt.students.altimin.sorter;

import ru.fizteh.fivt.students.altimin.ArgumentsParser;

import java.io.*;
import java.security.KeyException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

/**
 * User: altimin
 * Date: 11/21/12
 * Time: 5:46 AM
 */

public class ParallelSorterRunner {

    public static void main(String[] args) throws KeyException, IOException {
        ArgumentsParser argumentsParser = new ArgumentsParser();
        argumentsParser.addKey("i");
        argumentsParser.addKey("u");
        argumentsParser.addKey("t", true);
        argumentsParser.addKey("o", true);
        ArgumentsParser.ParseResult parsedArgs = argumentsParser.parse(args);
        PrintWriter printer;
        if (!parsedArgs.hasProperty("o")) {
            printer = new PrintWriter(System.out);
        } else {
            printer = new PrintWriter(new FileWriter(parsedArgs.getProperty("o")));
        }
        List<String> array = new ArrayList<String>();
        if (parsedArgs.other.length == 0) {
            Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(System.in)));
            while (scanner.hasNext()) {
                array.add(scanner.next());
            }
        } else {
            for (String fileName: parsedArgs.other) {
                Scanner scanner = new Scanner(new BufferedReader(new FileReader(new File(fileName))));
                while (scanner.hasNext()) {
                    array.add(scanner.next());
                }
            }
        }
        Comparator<String> comparator;
        if (!parsedArgs.hasProperty("i")) {
            comparator = new Comparator<String>() {
                @Override
                public int compare(String lhs, String rhs) {
                    return lhs.compareTo(rhs);
                }
            };
        } else {
            comparator = String.CASE_INSENSITIVE_ORDER;
        }

        ParallelSorter<String> parallelSorter = new ParallelSorter<String>(comparator);
        String[] _array = array.toArray(new String[1]);
        int value = parsedArgs.hasProperty("t") ? Integer.parseInt(parsedArgs.getProperty("t")) : 2;
        Object[] result = parallelSorter.sort(_array, value);
        boolean printUnique = parsedArgs.hasProperty("u");
        for (int i = 0; i < result.length; i ++) {
            if (i == 0 || (!result[i].equals(result[i - 1]) || !printUnique)) {
                printer.println(result[i]);
            }
        }
        printer.close();
    }
}
