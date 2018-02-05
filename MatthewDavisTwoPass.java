import java.util.Scanner;
import java.io.File;
import java.util.ArrayList;
import java.io.FileNotFoundException;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.HashMap;

public class MatthewDavisTwoPass {
    public static Scanner input;
    public static HashMap<String, Integer> symbolTable;
    public static ArrayList<Module> modules = new ArrayList<Module>();
    public static HashMap<String, Boolean> usedSymbols = new HashMap<String, Boolean>();
    public static int machineSize = 200 * 4;

    public static void main(String[] args) throws FileNotFoundException {
        if (args.length != 1) {
            System.err.println("Improper arguments. Try again!");
            System.exit(-1);
        }
        try {
            File inputFile = new File(args[0]);
            input = new Scanner(inputFile);

            symbolTable = new HashMap<String, Integer>();
            modules = new ArrayList<Module>();
            int baseAddress = 0; //initialize to zero
            int moduleCount = nextInt(); //the number of modules in this file
            //FIRST PASS
            for (int i = 0; i < moduleCount; i++) {
                modules.add(new Module());
                modules.get(i).baseAddress = baseAddress;
                modules.get(i).number = i;
                int symbolCountLocal = nextInt();
                for (int j = 0; j < symbolCountLocal; j++) { //handle line 1 of the module
                    String currentSymbol = input.next();
                    int currentValue = nextInt();
                    if (symbolTable.get(currentSymbol) != null) {
                        System.err.println("Error: This symbol " + currentSymbol
                                + " was defined multiple times. Initial value used.");
                    } else {
                        symbolTable.put(currentSymbol, baseAddress + currentValue);
                        modules.get(i).useList.add(currentSymbol);

                    }
                }
                int symbolsUsed = nextInt();
                for (int j = 0; j < symbolsUsed; j++) {
                    modules.get(i).symbols.add(input.next());

                }
                int instructionCount = input.nextInt();
                for (int j = 0; j < instructionCount; j++) {
                    input.next();
                    nextInt();
                }
                modules.get(i).size = instructionCount;
                defTooLarge(modules.get(i), baseAddress);
                baseAddress = baseAddress + modules.get(i).size;
            }
            //END FIRST PASS
            input.close();

            //START SECOND PASS
            input = new Scanner(inputFile);
            moduleCount = nextInt();
            for (int i = 0; i < moduleCount; i++) { //runs for each module
                int symbolCountLocal = nextInt();
                ArrayList<String> symbolList = new ArrayList<String>();
                for (int j = 0; j < symbolCountLocal; j++) { //line 1 of each module 
                    input.next();
                    nextInt();
                }
                int symbolsUsed = nextInt();
                for (int j = 0; j < symbolsUsed; j++) {
                    symbolList.add(input.next());
                }
                int instructionCount = input.nextInt();
                for (int j = 0; j < instructionCount; j++) {
                    modules.get(i).memoryMap
                            .add(addressTransformer(symbolList, modules.get(i).baseAddress, modules.get(i)));
                }
            }

            //END SECOND PASS
            finalPrint();
        } catch (FileNotFoundException e) {
            System.out.println("File not found. Please try again with a real file name");
        }
    }

    static class Module {
        public int number;
        public int size;
        public int baseAddress;
        public ArrayList<String> useList = new ArrayList<String>();
        public ArrayList<String> symbols = new ArrayList<String>();
        public ArrayList<Integer> memoryMap = new ArrayList<Integer>();
        public HashMap<String, Boolean> locallyUsed = new HashMap<String, Boolean>();
        public ArrayList<String> errorList = new ArrayList<String>();

    }

    public static int nextInt() {
        return Integer.parseInt(input.next());
    }

    public static int addressTransformer(ArrayList<String> symbolList, int baseAddress, Module module) {
        String type = input.next();
        int instruction = input.nextInt();
        int base = (instruction / 1000) * 1000;
        int index = instruction % 10;

        if (type.equals("E")) {
            if (index > module.useList.size()) {
                String error = ("Error: External address " + instruction
                        + " exceeds length of use list. Treated as immediate");
                module.errorList.add(error);
                //module.locallyUsed.put(module.symbols.get(index), true);
                return instruction;
            } else if (index > module.size) {
                String error = ("Error: Absolute address" + instruction + " exceeds module size. Zero used");
                module.errorList.add(error);
                module.locallyUsed.put(module.symbols.get(index), true);
                return base;
            } else if (!symbolTable.containsKey(symbolList.get(index))) {
                String error = ("ERROR: " + symbolList.get(index) + " is not defined. Zero used");
                module.errorList.add(error);
                module.locallyUsed.put(symbolList.get(index), true); //used but not defined!
                return base;
            } else {
                usedSymbols.put(symbolList.get(index), true);
                module.errorList.add(" ");
                module.locallyUsed.put(symbolList.get(index), true);
                return symbolTable.get(symbolList.get(index)) + base;
            }
        } else if (type.equals("R")) {
            if (index > module.size) {
                String error = ("Error: Relative address " + instruction + " exceeds module size. Zero used");
                module.errorList.add(error);
                return base;
            }
            module.errorList.add(" ");
            return baseAddress + instruction;
        } else {
            module.errorList.add(" ");
            return instruction;
        }
    }

    private static void printTransformed() {
        int j = 0;
        System.out.println("MEMORY MAP");
        for (Module module : modules) {
            for (int i = 0; i < module.memoryMap.size(); i++) {
                if ((module.memoryMap.get(i)) % 1000 > machineSize) {
                    System.out.print(j + ": " + (module.memoryMap.get(i) / 1000) * 1000);
                    System.out.println(" ERROR: Absolute address exceeds machine size. Zero used");
                    j++;
                    continue;
                }
                if (i >= module.errorList.size()) {
                    System.out.println(j + ": " + module.memoryMap.get(i));

                } else {
                    System.out.println(j + ": " + module.memoryMap.get(i) + " " + module.errorList.get(i));
                }
                //System.out.println(i + " " + "SIZE: " + errorList.size());
                j++;
            }
        }
    }

    private static void printSymbolTable() {
        System.out.println("SYMBOL TABLE: ");
        for (String s : symbolTable.keySet()) {
            System.out.println(s + "=" + symbolTable.get(s));
        }
    }

    public static void finalPrint() {
        printSymbolTable();
        printTransformed();
        printErrors();
    }

    //ERROR METHODS

    private static void definedNotUsed() {
        for (Module module : modules)
            for (String symbol : module.useList) {
                if (!(usedSymbols.containsKey(symbol))) {
                    System.err.println(
                            "WARNING: " + symbol + " was defined in module " + module.number + " but never used.");
                }
            }
    }

    private static void useListNotUsed() {
        for (Module module : modules) {
            for (String symbol : module.symbols) {
                if (!(module.locallyUsed.containsKey(symbol))) {
                    System.err.println("WARNING: " + symbol + " appears in the use list for module " + module.number
                            + " but was never used.");

                }
            }
        }
    }

    private static void defTooLarge(Module module, int offset) {
        for (String symbol : module.useList) {
            if (symbolTable.get(symbol) > module.size + offset) {
                System.err.println("ERROR: In module " + module.number + " the def of " + symbol
                        + " exceeds the size of the module. Zero (relative) used instead");
                symbolTable.put(symbol, offset);
            }
        }
    }

    private static void printErrors() {
        definedNotUsed();
        useListNotUsed();
    }
}
