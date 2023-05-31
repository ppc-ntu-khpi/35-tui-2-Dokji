package com.mybank.tui;

import com.mybank.domain.Account;
import com.mybank.domain.CheckingAccount;
import com.mybank.domain.Customer;
import com.mybank.domain.SavingsAccount;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import org.jline.reader.*;
import org.jline.reader.impl.completer.*;
import org.jline.utils.*;
import org.fusesource.jansi.*;

/**
 * Console client for 'Banking' example
 *
 * author Alexander 'Taurus' Babich
 */
public class CLIdemo {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    private String[] commandsList;
    private List<Customer> customers;

    public void init() {
        commandsList = new String[]{"help", "customers", "customer", "report", "exit"};
        customers = new LinkedList<>();
    }

    public void run() {
        AnsiConsole.systemInstall(); // needed to support ansi on Windows cmd
        printWelcomeMessage();
        LineReaderBuilder readerBuilder = LineReaderBuilder.builder();
        List<Completer> completors = new LinkedList<Completer>();

        completors.add(new StringsCompleter(commandsList));
        readerBuilder.completer(new ArgumentCompleter(completors));

        LineReader reader = readerBuilder.build();

        String line;
        PrintWriter out = new PrintWriter(System.out);

        while ((line = readLine(reader, "")) != null) {
            if ("help".equals(line)) {
                printHelp();
            } else if ("customers".equals(line)) {
                printCustomerList();
            } else if (line.startsWith("customer")) {
                printCustomerDetails(line);
            } else if ("report".equals(line)) {
                generateReport();
            } else if ("exit".equals(line)) {
                System.out.println("Exiting application");
                return;
            } else {
                System.out.println(ANSI_RED + "Invalid command, For assistance press TAB or type \"help\" then hit ENTER." + ANSI_RESET);
            }
        }

        AnsiConsole.systemUninstall();
    }

    private void printWelcomeMessage() {
        System.out.println("\nWelcome to " + ANSI_GREEN + " MyBank Console Client App" + ANSI_RESET + "! \nFor assistance press TAB or type \"help\" then hit ENTER.");
    }

    private void printHelp() {
        System.out.println("help\t\t\t- Show help");
        System.out.println("customers\t\t- Show list of customers");
        System.out.println("customer 'index'\t- Show customer details");
        System.out.println("report\t\t\t- Generate report");
        System.out.println("exit\t\t\t- Exit the app");
    }

    private String readLine(LineReader reader, String promptMessage) {
        try {
            String line = reader.readLine(promptMessage + ANSI_YELLOW + "\nbank> " + ANSI_RESET);
            return line.trim();
        } catch (UserInterruptException e) {
            // e.g. ^C
            return null;
        } catch (EndOfFileException e) {
            // e.g. ^D
            return null;
        }
    }

    private void readCustomerData(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            Customer customer = null;

            while ((line = br.readLine()) != null) {
                String[] data = line.split("\t");

                if (data.length == 1 && customer != null) {
                    customers.add(customer);
                    customer = null;
                } else if (data.length == 3) {
                    String firstName = data[0];
                    String lastName = data[1];
                    int numAccounts = Integer.parseInt(data[2]);
                    customer = new Customer(firstName, lastName);

                    for (int i = 0; i < numAccounts; i++) {
                        line = br.readLine();
                        String[] accountData = line.split("\t");
                        char accountTypeCode = accountData[0].charAt(0);
                        double balance = Double.parseDouble(accountData[1]);
                        double interestRate = Double.parseDouble(accountData[2]);

                        if (accountTypeCode == 'S') {
                            SavingsAccount savingsAccount = new SavingsAccount(balance, interestRate);
                            customer.addAccount(savingsAccount);
                        } else if (accountTypeCode == 'C') {
                            CheckingAccount checkingAccount = new CheckingAccount(balance);
                            customer.addAccount(checkingAccount);
                        }
                    }
                }
            }

            if (customer != null) {
                customers.add(customer);
            }
        } catch (IOException e) {
            System.out.println("Error reading customer data file: " + e.getMessage());
        }
    }

    private void printCustomerList() {
        AnsiConsole.out.println();
        AnsiConsole.out.println(new AttributedStringBuilder()
                .append("This is all of your ")
                .append("customers", AttributedStyle.BOLD.foreground(AttributedStyle.RED))
                .append(":").toAnsi());
        if (!customers.isEmpty()) {
            System.out.println("\nLast name\tFirst Name\tBalance");
            System.out.println("---------------------------------------");
            for (Customer customer : customers) {
                System.out.println(customer.getLastName() + "\t\t" + customer.getFirstName() + "\t\t$" + customer.getAccount(0).getBalance());
            }
        } else {
            System.out.println(ANSI_RED + "Your bank has no customers!" + ANSI_RESET);
        }
    }

    private void printCustomerDetails(String line) {
        try {
            int custNo = 0;
            if (line.length() > 8) {
                String strNum = line.split(" ")[1];
                if (strNum != null) {
                    custNo = Integer.parseInt(strNum);
                }
            }
            if (custNo >= 0 && custNo < customers.size()) {
                Customer cust = customers.get(custNo);
                String accType = cust.getAccount(0) instanceof CheckingAccount ? "Checking" : "Savings";

                AnsiConsole.out.println();
                AnsiConsole.out.println(new AttributedStringBuilder()
                        .append("This is detailed information about customer #")
                        .append(Integer.toString(custNo), AttributedStyle.BOLD.foreground(AttributedStyle.RED))
                        .append("!").toAnsi());

                System.out.println("\nLast name\tFirst Name\tAccount Type\tBalance");
                System.out.println("-------------------------------------------------------");
                System.out.println(cust.getLastName() + "\t\t" + cust.getFirstName() + "\t\t" + accType + "\t\t$" + cust.getAccount(0).getBalance());
            } else {
                System.out.println(ANSI_RED + "ERROR! Wrong customer number!" + ANSI_RESET);
            }
        } catch (Exception e) {
            System.out.println(ANSI_RED + "ERROR! Wrong customer number!" + ANSI_RESET);
        }
    }

    private void generateReport() {
        System.out.println("\nReport:");
        System.out.println("---------------------------------------");
        for (Customer customer : customers) {
            System.out.println("\nCustomer: " + customer.getLastName() + " " + customer.getFirstName());
            System.out.println("Last name\tFirst Name\tAccount Type\tBalance");
            System.out.println("-------------------------------------------------------");

            for (int i = 0; i < customer.getNumberOfAccounts(); i++) {
                Account account = customer.getAccount(i);
                if (account != null) {
                    String accountType = (account instanceof CheckingAccount) ? "Checking" : "Savings";
                    System.out.println(customer.getLastName() + "\t\t" + customer.getFirstName() + "\t\t" + accountType + "\t\t$" + account.getBalance());
                }
            }

            System.out.println("---------------------------------------");
        }
    }

    public static void main(String[] args) {
        CLIdemo tuiDemo = new CLIdemo();
        tuiDemo.init();
        tuiDemo.readCustomerData("C:\\Users\\LENOVO\\Desktop\\TUIdemo\\src\\com\\mybank\\tui\\test.dat");
        tuiDemo.run();
    }
}
