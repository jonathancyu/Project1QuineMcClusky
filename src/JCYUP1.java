// classes are in one file due to project specs
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;


public class JCYUP1 {
    public static void main(String[] args) throws FileNotFoundException {
        processFile("pagein1.txt");
        processFile("pagein2.txt");
        processFile("pagein3.txt");
    }

    static void processFile(String fileName) throws FileNotFoundException {
        process(new Scanner(new File(fileName)));
    }
    static void process(Scanner in) {
        String input = in.nextLine();

        QuineMcClusky solver = new QuineMcClusky(input);
        solver.solve();
    }
}

class QuineMcClusky {
    int numTerms;
    LinkedList<Integer> minTerms;
    LinkedList<Integer> dontCares;

    ImplicantTable implicantTable;
    CoverageTable coverageTable;

    QuineMcClusky(String in) {
        String[] input = in.split(" ");
        numTerms = Integer.parseInt(input[0]);

        minTerms = new LinkedList<Integer>();
        dontCares = new LinkedList<Integer>();
        implicantTable = new ImplicantTable(numTerms);
        parseInput(input);
    }

    private void parseInput(String[] input) { // sets minTerms and dontCares, and adds minTerms to implicant table
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
        coverageTable = new CoverageTable(this);
        coverageTable.solve();
        System.out.println(coverageTable.getSolutionString());
    }
}

class ImplicantTable {
    private class Column { // wrapper class that allows for simpler code in the implicant table
        private boolean empty; // flag for exit condition of ImplicantTable.solve()
        private final ArrayList<LinkedList<Implicant>> entries;

        Column() {
            empty = true;
            entries = new ArrayList<LinkedList<Implicant>>();
            for (int i = 0; i <= numTerms; i++) {
                entries.add(new LinkedList<Implicant>());
            }
        }

        void add(int row, Implicant term) { entries.get(row).add(term); empty = false; }

        LinkedList<Implicant> get(int row) { return entries.get(row); }

        void set(int row, LinkedList<Implicant> list) {
            entries.set(row, list);
            if (empty && !list.isEmpty()) {
                empty = false;
            }
        }

        boolean isEmpty() { return empty; }

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

    private final int numTerms;
    private final ArrayList<Column> table;
    private final LinkedList<Implicant> primeImplicants;

    ImplicantTable(int numTerms) {
        this.numTerms = numTerms;
        table = new ArrayList<Column>();
        table.add(0, new Column());
        primeImplicants = new LinkedList<Implicant>();
    }

    void addTerm(int value) {
        Implicant minTerm = new Implicant(numTerms, value);
        table.get(0).add(minTerm.bitCount, minTerm); // add term to group corresponding with # of 1's
    }

    //this could be done in-place but it's hard to debug and there's no memory constraint
    void solve() {
        int i = 0;
        while (!table.get(i).isEmpty()) { // run until there are no adjacencies found
            table.add(computeAdjacencies(table.get(i)));
            i++;
        }
    }

    private Column computeAdjacencies(Column column) {
        Column newColumn = new Column();
        for (int i = 0; i < numTerms; i++) { // for each group, compare to the next group
            LinkedList<Implicant> newEntry = compareGroups(column.get(i), column.get(i + 1));
            newColumn.set(i, newEntry);
        }
        return newColumn;
    }

    private LinkedList<Implicant> compareGroups(LinkedList<Implicant> currentGroup, LinkedList<Implicant> nextGroup) {
        LinkedList<Implicant> newGroup = new LinkedList<Implicant>();

        //compare all from current to next. if they have a one bit difference, combine and add to new group
        for (Implicant a : currentGroup) {
            if (a.isPrimeImplicant) { continue; }
            for (Implicant b : nextGroup) {
                Implicant newImplicant = combineImplicants(a, b);
                if (newImplicant != null && !newGroup.contains(newImplicant)) { //if combination is possible & hasn't been found yet
                    newGroup.add(newImplicant);
                }
            }
            if (!a.hasBeenCombined) { // if a term hasn't been combined, add it to prime implicants
                a.isPrimeImplicant = true;
                primeImplicants.add(a);
            }
        }
        return newGroup;
    }

    private Implicant combineImplicants(Implicant a, Implicant b) {
        int position = findDifferencePosition(a, b);
        if (position == -1) { return null; } // no combination possible
        return Implicant.combine(a, b, position);
    }

    private int findDifferencePosition(Implicant a, Implicant b) { // returns -1 if exact same or more than 1 difference. otherwise, return position of dif
        if (a.numTerms != b.numTerms) { return -1; }

        boolean foundDifference = false;
        int differingPosition = -1;
        for (int i = 0; i < a.numTerms; i++) {
            if (a.literals[i] != b.literals[i]) {
                if (foundDifference) { // more than 1 bit is different so can't be combined
                    return -1;
                }
                foundDifference = true;
                differingPosition = i;
            }
        }
        return differingPosition;
    }

    LinkedList<Implicant> getPrimeImplicants() {
        return primeImplicants;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        int i = 0;
        for (Column current : table) {
            result.append("Column ").append(i).append(":\n");
            result.append(current);
            i++;
        }
        return result.toString();
    }
}

class Implicant implements Comparable<Implicant> {
    enum Literal {
        ELIMINATED("-"), FALSE("0"), TRUE("1");
        private final String str;
        Literal(String str) { this.str = str; }
        public String toString() { return str; }
    }

    Literal[] literals;

    int value;
    int numTerms;
    int bitCount;
    int eliminatedLiterals;
    boolean hasBeenCombined;
    boolean isPrimeImplicant;
    ArrayList<Integer> coverage;

    Implicant(int numTerms, int value) {
        this.value = value;
        this.numTerms = numTerms;
        this.literals = new Literal[numTerms];
        this.bitCount = 0;
        this.eliminatedLiterals = 0;
        this.isPrimeImplicant = false;
        this.hasBeenCombined = false;

        this.coverage = initializeCoverage();
        this.coverage.add(value);

        computeLiterals(value);
    }

    private void computeLiterals(int value) {
        int mask = 1;
        for (int i = 0; i < numTerms; i++) {
            if ((value & mask) == mask) { // if bit_i == 1
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
        this.value = other.value;
        this.eliminatedLiterals = other.eliminatedLiterals;
        this.bitCount = other.bitCount;
        this.isPrimeImplicant = false;
        this.hasBeenCombined = false;

        this.coverage = initializeCoverage();
        this.coverage.addAll(other.coverage);
    }

    private ArrayList<Integer> initializeCoverage() {
        return new ArrayList<Integer>() {
            public boolean add(Integer value) {
                int index = Collections.binarySearch(this, value);
                if (index < 0) { index = ~index; }
                super.add(index, value);
                return true;
            }
        };
    }


    static Implicant combine(Implicant a, Implicant b, int position) {
        Implicant newImplicant = new Implicant(a);

        for (Integer value : b.coverage) { // merge a and b coverage
            if (!a.coverage.contains(value)) {
                newImplicant.coverage.add(value);
            }
        }

        newImplicant.literals[position] = Literal.ELIMINATED;
        newImplicant.eliminatedLiterals++;
        a.hasBeenCombined = true;
        b.hasBeenCombined = true;

        return newImplicant;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        for (int i = numTerms-1; i >= 0; i--) {
            char asChar = (char) ('A' + (numTerms - i - 1));
            if (literals[i] == Literal.FALSE) {
                result.append(asChar).append("'");
                //result.append('\u0304');
            } else if (literals[i] == Literal.TRUE) {
                result.append(asChar);
            }
        }
        return result.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this.getClass() != obj.getClass()) { return false; }
        Implicant other = (Implicant) obj;
        for (int i = 0; i < numTerms; i++) {
            if (this.literals[i] != other.literals[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int compareTo(Implicant other) { // used so we can sort the coverageTable
        int thisSize = this.coverage.size();
        int otherSize = other.coverage.size();
        if (thisSize > otherSize) {
            return 1;
        } else if (thisSize < otherSize) {
            return -1;
        } else {
            if (this.eliminatedLiterals > other.eliminatedLiterals) {
                return 1;
            } else if (this.eliminatedLiterals < other.eliminatedLiterals) {
                return -1;
            } else {
                return 0;
            }
        }
    }


}

class CoverageTable {
    private final int[] coverage;
    private final ArrayList<Implicant> table;
    private final ArrayList<Implicant> solution;

    CoverageTable(QuineMcClusky template) {
        this.coverage = new int[1 << template.numTerms];
        this.solution = new ArrayList<Implicant>();
        this.table = new ArrayList<Implicant>();
        //add all prime implicants to the table while removing DC's from PI's coverage table
        for (Implicant current : template.implicantTable.getPrimeImplicants()) {
            Implicant newImplicant = new Implicant(current);
            for (Integer term : template.dontCares) {
                newImplicant.coverage.remove(term);
            }
            table.add(newImplicant);
        }
        Collections.sort(table, Collections.reverseOrder()); // sort from greatest to least so we can select most fit implicants
    }


    void solve() {
        boolean circular = false;
        while (!table.isEmpty()) {
            ArrayList<Implicant> lessThans = findAllLessThans();
            table.removeAll(lessThans);
            updateCoverage();
            circular = !addSolution(circular); // if no solution was added then there is a circular pattern
        }
    }

    private boolean addSolution(boolean circular) {
        Implicant selectedImplicant = null;
        boolean solutionAdded = false;
        //iterate until we find an implicant to add to the solution set
        for (Iterator<Implicant> it = table.iterator(); it.hasNext() && !solutionAdded;) {
            Implicant current = it.next();
            for (Integer i : current.coverage) {
                if (coverage[i] == 1 || circular) { // only term covering or just select the first one b/c circular structure
                    solution.add(current);
                    it.remove();
                    selectedImplicant = current;
                    solutionAdded = true;
                    break;
                }
            }
        }
        if (solutionAdded) {
            for (Integer i : selectedImplicant.coverage) {
                removeColumn(i);
            }
        }
        return solutionAdded; // if we weren't able to add a solution, there must be a circular structure
    }



    private ArrayList<Implicant> findAllLessThans() {
        ArrayList<Implicant> lessThans = new ArrayList<Implicant>();

        // compare each implicant to every other, j=i+1 so we don't do it twice
        for (int i = 0; i < table.size(); i++) {
            Implicant current = table.get(i);

            if (current.coverage.isEmpty()) { // occurs if we added a solution that covers this implicant
                lessThans.add(current);
                continue;
            }

            for (int j = i+1; j < table.size(); j++) {
                Implicant other = table.get(j);
                if (lessThans.contains(current) || lessThans.contains(other)) { continue; }
                switch (ALessThanB(current, other)) {
                    case 1:
                        lessThans.add(current); break;
                    case -1:
                        lessThans.add(other); break;
                }
            }
        }
        return lessThans;
    }

    int ALessThanB(Implicant A, Implicant B) { // 1 if A < B, -1 if B > A, else 0
        int Asize = A.coverage.size();
        int Bsize = B.coverage.size();
        boolean AsubsetB = Asize < Bsize;
        boolean BsubsetA = Bsize < Asize;

        for (int i = 0; i < Math.min(Asize, Bsize); i++) {
            if (!AsubsetB && !BsubsetA) { return 0; }
            if (AsubsetB) {
                AsubsetB = B.coverage.contains(A.coverage.get(i));
            }
            if (BsubsetA) {
                BsubsetA = A.coverage.contains(B.coverage.get(i));
            }

        }
        if (AsubsetB) {
            return 1;
        } else if (BsubsetA) {
            return -1;
        } else {
            return 0;
        }
    }

    boolean aIsSubsetOfB(ArrayList<Integer> A, ArrayList<Integer> B) {
        if (A.size() > B.size()) return false;
        for (Integer a : A) {
            if (!B.contains(a)) {
                return false;
            }
        }
        return true;
    }


    private void updateCoverage() {
        Arrays.fill(coverage, 0);
        for (Implicant current : table) {
            for (Integer i : current.coverage) {
                coverage[i]++;
            }
        }
    }


    private void removeColumn(int column) {
        for (Implicant row : table) {
            row.coverage.remove(Integer.valueOf(column));
        }
    }


    String getSolutionString() {
        StringBuilder result = new StringBuilder("F");
        for (Implicant current : solution) {
            result.append(" + ").append(current);
        }
        result.setCharAt(2, '=');
        return result.toString();
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        for (Implicant imp : table) {
            result.append(imp).append(" ");
            result.append("\t");
            result.append(imp.coverage).append('\n');
        }
        return result.toString();
    }
}