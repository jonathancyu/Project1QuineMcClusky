import java.io.File;
import java.io.FileNotFoundException;
import java.lang.Math;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;

class Implicant {
    private int numTerms;
    boolean[] bits;
    int bitCount;

    Implicant(int numTerms, int key) {
        this.numTerms = numTerms;
        this.bits = new boolean[numTerms];
        this.bitCount = 0;

        int mask = 1;
        for (int i = 0; i < numTerms; i++) {
            int bit = key & mask;
            bits[i] = (bit == mask);
            bitCount += bits[i] ? 1 : 0;
            mask *= 2;
        }
    }

    void print() {
        for (int i = numTerms-1; i >= 0; i--) {
            System.out.print(bits[i] ? 1 : 0);
        }
    }
}

class ImplicantTable {
    private int numTerms;
    private ArrayList<LinkedList<Implicant>> table;

    ImplicantTable(int n) {
        numTerms = n;
        initializeTable();
    }

    private void initializeTable() {
        table = new ArrayList<LinkedList<Implicant>>();
        for (int i = 0; i < numTerms+1; i++) {
            table.add(new LinkedList<Implicant>());
        }
    }

    void insert(int value) {
        Implicant minTerm = new Implicant(numTerms, value);
        table.get(minTerm.bitCount).add(minTerm);
    }


}


class QuineMcClusky {
    private int numTerms;
    int[] minTerms;
    int[] dontCares;

    private ImplicantTable implicantTable;

    QuineMcClusky(Scanner in) {
        String[] input = in.nextLine().split(" ");
        numTerms = Integer.parseInt(input[0]);

        boolean dontCare = false;
        for (int i = 1; i < input.length; i++) {
            if (!dontCare) { // if we do care
                if (input[i].equals("d")) {
                    dontCare = true;
                    continue;
                }
                //log as minterms

            } else {
                //log as dont cares
            }

        }

    }


    boolean oneBitDifference(int a, int b) {
        int x = Math.abs(a - b); // get difference
        return (x & (x-1)) == 0; // theoretically if the x is 0...1...0 (power of 2) then there is 1 bit dif
    }


}

public class JCYUP1 {

    public static void main(String[] args) throws FileNotFoundException {
        String inputLine = new Scanner(new File("hw3data.txt")).nextLine();
        String input[] = inputLine.split(" ");
        int N  = Integer.parseInt(input[0]);

        Implicant a = new Implicant(4, 11);
        a.print();
    }
}
