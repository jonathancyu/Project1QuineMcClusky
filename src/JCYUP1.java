import java.io.File;
import java.io.FileNotFoundException;
import java.lang.Math;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;



class Implicant {
    enum Literal {
        ELIMINATED('-'), FALSE('0'), TRUE('1');
        private final char character;
        Literal(char character) {
            this.character = character;
        }
        char toChar() {
            return character;
        }
    }
    Literal[] literals;

    int numTerms;
    int bitCount;
    private boolean primeImplicant;

    Implicant(int numTerms, int key) {
        this.numTerms = numTerms;
        this.literals = new Literal[numTerms];
        this.bitCount = 0;
        this.primeImplicant = false;

        int mask = 1;
        for (int i = 0; i < numTerms; i++) {
            if ((key & mask) == mask) {
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
        this.primeImplicant = false;
    }

    void eliminateLiteral(int pos) {
        literals[pos] = Literal.ELIMINATED;
    }

    void setPrimeImplicant() {
        primeImplicant = true;
    }

    boolean isPrimeImplicant() {
        return primeImplicant;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        for (int i = numTerms-1; i >= 0; i--) {
            result.append(literals[i].toChar());
        }
        if (isPrimeImplicant()) {
            result.append("*");
        }
        return result.toString();
    }

    public boolean equals(Implicant other) {
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
            if (entries.get(row).indexOf(term) == -1) {
                entries.get(row).add(term);
            }
        }
        LinkedList<Implicant> get(int row) {
            return entries.get(row);
        }
        void set(int row, LinkedList<Implicant> list) {
            entries.set(row, list);
        }
    }
    private int numTerms;
    private Column[] table;

    ImplicantTable(int numTerms) {
        this.numTerms = numTerms;
        table = new Column[]{new Column(), new Column(), new Column()};
    }

    void addTerm(int value) {
        Implicant minTerm = new Implicant(numTerms, value);
        table[0].add(minTerm.bitCount, minTerm);
    }

    /*for each row n except the last,
    *   consider n and n+1
    *       compare each in n to each in n+1
    *       then write over n?
    * */

    void compute() {
        computeAdjacencies(0);

        computeAdjacencies(1);
    }

    private void computeAdjacencies(int column) {
        for (int i = 0; i < numTerms; i++) {
            LinkedList<Implicant> newEntry = compareGroups(table[column].get(i), table[column].get(i + 1));
            table[column+1].set(i, newEntry);
        }
    }

    private LinkedList<Implicant> compareGroups(LinkedList<Implicant> currentGroup, LinkedList<Implicant> nextGroup) {
        LinkedList<Implicant> newGroup = new LinkedList<Implicant>();
        for (Implicant j : currentGroup) {
            if (j.isPrimeImplicant()) {
                continue;
            }
            boolean combined = false;
            for (Implicant k : nextGroup) {
                int position = compareImplicants(j, k);
                if (position == -1) { // if more than one bit differs, skip
                    continue;
                }
                Implicant newImplicant = new Implicant(j);
                newImplicant.eliminateLiteral(position);
                newGroup.add(newImplicant);
                combined = true;
            }
            if (!combined) {
                j.setPrimeImplicant();
            }
        }
        return newGroup;
    }

    private int compareImplicants(Implicant a, Implicant b) { // refactor this it's ugly
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

    public String toString() {
        StringBuilder result = new StringBuilder();
        for (int column = 0; column < 3; column++) {
            result.append("Column ").append(column).append(":\n");
            for (int i = 0; i <= numTerms; i++) {
                for (Implicant current : table[column].get(i)) {
                    result.append(current.toString());
                    result.append("\n");
                }
                result.append("____\n");
            }
        }
        return result.toString();
    }
}

class CoverageTable {

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
        implicantTable.compute();
        System.out.println(implicantTable);
    }


    boolean oneBitDifference(int a, int b) {
        int x = Math.abs(a - b); // get difference
        return (x & (x-1)) == 0; // theoretically if the x is 0...1...0 (power of 2) then there is 1 bit dif
    }


}

public class JCYUP1 {
    public static void main(String[] args) throws FileNotFoundException {
        String input = new Scanner(new File("hw3data.txt")).nextLine();
        QuineMcClusky solver = new QuineMcClusky(input);
        solver.solve();

    }
}
