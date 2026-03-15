package com.fca;

import com.fca.cli.ConsoleMenu;
import com.fca.implication.ImplicationGenerator;
import com.fca.io.JsonDataLoader;
import com.fca.io.JsonResultWriter;
import com.fca.service.ComparisonService;
import com.fca.service.SystemTimingService;

import java.io.PrintStream;
import java.util.Scanner;

/**
 * Точка входа приложения CbO Formal Concept Analysis.
 */
public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        PrintStream out = System.out;

        JsonDataLoader dataLoader = new JsonDataLoader();
        JsonResultWriter resultWriter = new JsonResultWriter();
        SystemTimingService timingService = new SystemTimingService();
        ImplicationGenerator implGenerator = new ImplicationGenerator();
        ComparisonService comparisonService = new ComparisonService(timingService, implGenerator);

        ConsoleMenu menu = new ConsoleMenu(scanner, out, dataLoader, resultWriter, comparisonService);
        menu.run();
    }
}
