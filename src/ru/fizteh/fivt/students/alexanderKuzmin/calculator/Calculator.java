package ru.fizteh.fivt.students.alexanderKuzmin.calculator

/**
 * @author Kuzmin A.
 *      group 196
 *      Class Calculator for calculate expression.
 * 
 */

import java.math.BigInteger;
import java.util.Stack;

public class Calculator {

    // The method converts the string in RPN
    public String toPolishNotation(final String formula) {
        if (formula.length() <= 2) {
            System.out.println("Nothing input.");
            System.exit(1);
        }

        String rezultNotation = "";
        final Stack<Character> stack = new Stack<Character>();
        final Stack<Character> outString = new Stack<Character>();
        int sum = 0;
        boolean number = false;
        for (int i = 0; i < formula.length(); ++i) {
            if (formula.charAt(i) == ')') {
                --sum;
                number = false;
                try {
                    while (String.valueOf(stack.peek()).charAt(0) != '(') {
                        outString.push(stack.pop());
                    }
                    stack.pop(); // delete '('
                } catch (Exception e1) {
                    System.out
                            .println("Error with stack(error with parentheses).");
                    System.exit(1);
                }
            }
            if (sum < 0) {
                System.out.println("Error with parentheses");
                System.exit(1);
            }

            if (formula.charAt(i) == '(') {
                ++sum;
                number = false;
                stack.push('(');
            }
            if ((formula.charAt(i) >= '0') && (formula.charAt(i) <= '9')) {
                if (number) {
                    System.out.println("Error with order of numbers.");
                    System.exit(1);
                }
                outString.push(formula.charAt(i));
                try {
                    if (!((formula.charAt(i + 1) >= '0') && (formula
                            .charAt(i + 1) <= '9'))) {
                        outString.push(' ');
                        number = true;
                    }
                } catch (Exception e2) {
                    System.out.println("Error with next number.");
                    System.exit(1);
                }
            }
            if ((formula.charAt(i) == '+') || (formula.charAt(i) == '-')
                    || (formula.charAt(i) == '*') || (formula.charAt(i) == '/')) {
                number = false;
                if (stack.size() == 0) {
                    stack.push(formula.charAt(i));
                } else if (priority(formula.charAt(i)) > priority(String
                        .valueOf(stack.peek()).charAt(0))) {
                    stack.push(formula.charAt(i));
                } else {
                    while ((stack.size() != 0)
                            && (priority(String.valueOf(stack.peek()).charAt(0)) >= priority(formula
                                    .charAt(i)))) {
                        outString.push(stack.pop());
                    }
                    stack.push(formula.charAt(i));
                }
            }
            if ((formula.charAt(i) != '+') && (formula.charAt(i) != '-')
                    && (formula.charAt(i) != '*') && (formula.charAt(i) != '/')
                    && (formula.charAt(i) != '(') && (formula.charAt(i) != ')')
                    && ((formula.charAt(i) < 0) || (formula.charAt(i) > '9'))
                    && (formula.charAt(i) != ' ')) {
                System.out.println("Error with order of symbols.");
                System.exit(1);
            }
        }
        for (int i1 = 0; i1 < outString.size(); i1++) {
            rezultNotation = rezultNotation + String.valueOf(outString.get(i1));
        }
        if (sum != 0) {
            System.out.println("Error with parentheses");
            System.exit(1);
        }
        return rezultNotation;
    }

    // Method for priority operations
    private int priority(final char a) {
        switch (a) {
        case '/':
            return 3;
        case '*':
            return 3;
        case '-':
            return 2;
        case '+':
            return 2;
        case '(':
            return 1;
        }
        return 0;
    }

    // The method that calculate the expression by using Reverse Polish Notation
    private BigInteger solve(final String formula) {
        Stack<BigInteger> stack = new Stack<BigInteger>();
        for (int i = 0; i < formula.length(); ++i) {
            if ((formula.charAt(i) <= '9') && (formula.charAt(i) >= '0')) {
                BigInteger tmp = new BigInteger(new Integer(
                        formula.charAt(i) - '0').toString());
                for (int j = i + 1; j < formula.length(); ++j)
                    if ((formula.charAt(j) <= '9')
                            && (formula.charAt(j) >= '0')) {
                        tmp = tmp.multiply(
                                new BigInteger(new Integer(10).toString()))
                                .add(new BigInteger(new Integer(formula
                                        .charAt(j) - '0').toString()));
                        ++i;
                    } else
                        break;
                stack.push(tmp);
                continue;
            } else if (formula.charAt(i) == ' ')
                continue;
            try {
                switch (formula.charAt(i)) {
                case '/':
                    BigInteger a = stack.pop();
                    try {
                        stack.push(stack.pop().divide(a));
                    } catch (Exception e3) {
                        System.out
                                .print("Division by zero or another error with division!\n");
                        System.exit(1);
                    }
                    break;
                case '*':
                    stack.push(stack.pop().multiply(stack.pop()));
                    break;
                case '-':
                    BigInteger b = stack.pop();
                    stack.push(stack.pop().subtract(b));
                    break;
                case '+':
                    stack.push(stack.pop().add(stack.pop()));
                    break;
                }
            } catch (Exception e3) {
                System.out.print("Error with operations.");
                System.exit(1);
            }
        }
        return stack.pop();
    }

    /**
     * @param args
     *            input expression, which the program should calculate (supported
     *            Operations: multiplication - "*", division - "/", subtraction - "-"
     *            Addition - "+").
     */
    public static void main(String[] args) throws Exception {
        String s = "";
        if (args.length > 0)
            for (int i = 0; i < args.length; ++i)
                s += args[i];
        Calculator calc = new Calculator();
        System.out.print(calc.solve(calc.toPolishNotation("(" + s + ")")));
    }
}
