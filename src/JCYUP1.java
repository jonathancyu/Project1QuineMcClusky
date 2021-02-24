// classes are in one file due to project specs
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;


public class JCYUP1 {
    public static void main(String[] args) throws FileNotFoundException {
        String input = new Scanner(new File("notesdata.txt")).nextLine();
        QuineMcClusky solver = new QuineMcClusky(input);
        solver.solve();
    }
}

class QuineMcClusky {
    private int numTerms;
    private LinkedList<Integer> minTerms;
    private LinkedList<Integer> dontCares;

    private ImplicantTable implicantTable;

    QuineMcClusky(String in) {
        String[] input = in.split(" ");
        numTerms = Integer.parseInt(input[0]);

        minTerms = new LinkedList<Integer>();
        dontCares = new LinkedList<Integer>();
        implicantTable = new ImplicantTable(numTerms);
        parseTerms(input);
    }

    private void parseTerms(String[] input) {
        LinkedList<Integer> target = minTerms;
        for (int i = 1; i < input.length; i++) {
            if (input[i].equals("d")) {
                target = dontCares;
                continue;
            }
            int value = Integer.parseInt(input[i]);
            target.add(value);
            implicantTable.addTerm(value);
        }
    }

    void solve() {
        implicantTable.solve();
        System.out.println(implicantTable.getPrimeImplicants());
    }
}

class ImplicantTable {
    private class Column {
        private ArrayList<LinkedList<Implicant>> entries;
        Column() {
            entries = new ArrayList<LinkedList<Implicant>>();
            for (int i = 0; i <= numTerms; i++) {
                entries.add(new LinkedList<Implicant>());
            }
        }
        void add(int row, Implicant term) {
            entries.get(row).add(term);
        }
        LinkedList<Implicant> get(int row) {
            return entries.get(row);
        }
        void set(int row, LinkedList<Implicant> list) {
            entries.set(row, list);
        }

        public String toString() {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i <= numTerms; i++) {
                if (entries.get(i).isEmpty()) {
                    continue;
                }
                for (Implicant current : entries.get(i)) {
                    result.append(current.toString());
                    result.append("\n");
                }
                result.append("____\n");
            }
            return result.toString();
        }
    }

    private int numTerms;
    private Column[] table;
    private LinkedList<Implicant> primeImplicants;

    ImplicantTable(int numTerms) {
        this.numTerms = numTerms;
        table = new Column[3];
        table[0] = new Column();
        primeImplicants = new LinkedList<Implicant>();
    }

    void addTerm(int value) {
        Implicant minTerm = new Implicant(numTerms, value);
        table[0].add(minTerm.bitCount, minTerm);
    }

    void solve() {
        table[1] = computeAdjacencies(table[0]);
        table[2] = makeAllPrimeImplicants(computeAdjacencies(table[1]));
    }

    private Column computeAdjacencies(Column column) {
        Column newColumn = new Column();
        for (int i = 0; i < numTerms; i++) {
            LinkedList<Implicant> newEntry = compareGroups(column.get(i), column.get(i + 1));
            newColumn.set(i, newEntry);
        }
        return newColumn;
    }

    private LinkedList<Implicant> compareGroups(LinkedList<Implicant> currentGroup, LinkedList<Implicant> nextGroup) {
        LinkedList<Implicant> newGroup = new LinkedList<Implicant>();
        for (Implicant a : currentGroup) {
            if (a.isPrimeImplicant) {
                continue;
            }
            for (Implicant b : nextGroup) {
                Implicant newImplicant = combineImplicants(a, b);
                if (newImplicant != null && newGroup.indexOf(newImplicant) == -1) {
                    newGroup.add(newImplicant);
                }
            }
            if (!a.hasBeenCombined) {
                a.isPrimeImplicant = true;
                primeImplicants.add(a);
            }
        }
        return newGroup;
    }

    private Implicant combineImplicants(Implicant a, Implicant b) {
        int position = compareImplicants(a, b);
        if (position == -1) { // if there's no need to combine return null
            return null;
        }
        Implicant newImplicant = new Implicant(a);
        newImplicant.eliminateLiteral(position);
        a.hasBeenCombined = true;
        b.hasBeenCombined = true;
        return newImplicant;
    }

    private int compareImplicants(Implicant a, Implicant b) { // returns -1 if exact same or more than 1 difference
        if (a.numTerms != b.numTerms) { return -1; }

        boolean foundDifference = false;
        int differingPosition = -1;
        for (int i = 0; i < a.numTerms; i++) {
            if (a.literals[i] != b.literals[i]) {
                if (foundDifference) { // more than 1 bit is different
                    return -1;
                }
                foundDifference = true;
                differingPosition = i;
            }
        }
        return differingPosition;
    }

    private Column makeAllPrimeImplicants(Column column) {
        for (int i = 0; i <= numTerms; i++) {
            if (column.get(i).isEmpty()) {
                continue;
            }
            for (Implicant current : column.get(i)) {
                current.isPrimeImplicant = true;
                primeImplicants.add(current);
            }
        }
        return column;
    }


    LinkedList<Implicant> getPrimeImplicants() {
        return primeImplicants;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i <= 2; i++) {
            result.append("Column ").append(i).append(":\n");
            result.append(table[i]);
        }
        return result.toString();
    }
}

class Implicant {
    enum Literal {
        ELIMINATED("-"), FALSE("0"), TRUE("1");
        private final String str;
        Literal(String str) {
            this.str = str;
        }
        public String toString() {
            return str;
        }
    }
    Literal[] literals;

    int numTerms;
    int bitCount;
    boolean hasBeenCombined;
    boolean isPrimeImplicant;

    Implicant(int numTerms, int value) {
        this.numTerms = numTerms;
        this.literals = new Literal[numTerms];
        this.bitCount = 0;
        this.isPrimeImplicant = false;
        this.hasBeenCombined = false;

        int mask = 1;
        for (int i = 0; i < numTerms; i++) {
            if ((value & mask) == mask) {
                literals[i] = Literal.TRUE;
                bitCount++;
            } else {
                literals[i] = Literal.FALSE;
            }
            mask *= 2;
        }
    }

    Implicant(Implicant other) {
        this.numTerms = other.numTerms;
        this.literals = other.literals.clone();
        this.bitCount = other.bitCount;
        this.isPrimeImplicant = false;
        this.hasBeenCombined = false;
    }

    void eliminateLiteral(int pos) {
        literals[pos] = Literal.ELIMINATED;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        for (int i = numTerms-1; i >= 0; i--) {
            result.append(literals[i]);
        }
        if (this.isPrimeImplicant) {
            result.append("*");
        }
        return result.toString();
    }

    public boolean equals(Object obj) {
        if (this.getClass() != obj.getClass()) { return false; }
        Implicant other = (Implicant) obj;
        boolean success = true;
        for (int i = 0; i < numTerms; i++) {
            if (this.literals[i] != other.literals[i]) {
                success = false;
                break;
            }
        }
        return success;
    }
}

class CoverageTable {
    LinkedList<Implicant> primeImplicants;
    CoverageTable(LinkedList<Implicant> primeImplicants) {
        this.primeImplicants = primeImplicants;
    }
}