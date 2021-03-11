// classes are in one file due to project specs
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;


public class JCYUP1 {
    public static void main(String[] args) throws FileNotFoundException {
        //String fileName = "hw3data.txt";
        //String input = new Scanner(new File(fileName)).nextLine();
        String input = new Scanner(System.in).nextLine();

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
        coverageTable = new CoverageTable(this);
        coverageTable.solve();
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

    void solve() { // TODO: might take more than 2 iterations
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
            if (a.isPrimeImplicant) { continue; }
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
        if (position == -1) { return null; }
        return Implicant.combine(a, b, position);
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
    int elims;
    boolean hasBeenCombined;
    boolean isPrimeImplicant;
    ArrayList<Integer> coverage;

    Implicant(int numTerms, int value) {
        this.value = value;
        this.numTerms = numTerms;
        this.literals = new Literal[numTerms];
        this.bitCount = 0;
        this.elims = 0;
        this.isPrimeImplicant = false;
        this.hasBeenCombined = false;

        this.coverage = initializeCoverage();
        this.coverage.add(value);

        computeLiterals(value);
    }

    private void computeLiterals(int value) {
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
        this.value = other.value;
        this.elims = other.elims;
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
        newImplicant.literals[position] = Literal.ELIMINATED;
        newImplicant.elims++;
        a.hasBeenCombined = true;
        b.hasBeenCombined = true;
        for (Integer value : b.coverage) { // merge a and b coverage
            if (!a.coverage.contains(value)) {
                newImplicant.coverage.add(value);
            }
        }

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

    public String toBitString() {
        StringBuilder result = new StringBuilder();
        for (int i = numTerms-1; i >= 0; i--) {
            result.append(literals[i]);
        }
        return result.append(this.isPrimeImplicant ? "*" : "").toString();
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
    public int compareTo(Implicant other) {
        int thisSize = this.coverage.size();
        int otherSize = other.coverage.size();
        if (thisSize > otherSize) {
            return 1;
        } else if (thisSize < otherSize) {
            return -1;
        } else {
            if (this.elims > other.elims) {
                return 1;
            } else if (this.elims < other.elims) {
                return -1;
            } else {
                String thisStr = this.toString();
                String otherStr = other.toString();
                return 0;//thisStr.compareTo(otherStr);

            }
        }
    }


}

class CoverageTable {
    int numTerms;

    int[] coverage;
    boolean[] covered;

    private ArrayList<Implicant> table;
    private LinkedList<Integer> minTerms;
    private LinkedList<Integer> dontCares;
    private ArrayList<Implicant> solution;

    CoverageTable(QuineMcClusky template) {
        this.numTerms = template.numTerms;

        this.coverage = new int[1 << numTerms];
        this.covered = new boolean[1 << numTerms];
        Arrays.fill(this.covered, false);
        this.minTerms = new LinkedList<Integer>();
        this.minTerms.addAll(template.minTerms);
        this.dontCares = new LinkedList<Integer>();
        this.dontCares.addAll(template.dontCares);

        this.solution = new ArrayList<Implicant>();

        table = new ArrayList<Implicant>();
        for (Implicant current : template.implicantTable.getPrimeImplicants()) {
            Implicant newImplicant = new Implicant(current);
            for (Integer column : dontCares) {
                newImplicant.coverage.remove(column);
            }
            table.add(newImplicant);
        }
        Collections.sort(table, Collections.reverseOrder());
    }


    void solve() {
        boolean circular = false;
        while (!table.isEmpty()) {
            ArrayList<Implicant> lessThans = findAllLessThans();
            table.removeAll(lessThans);
            updateCoverage();
            circular = !addSolutions(circular); // circular is true if nothing was removed
        }
        StringBuilder result = new StringBuilder("F");
        for (Implicant current : solution) {
            result.append(" + ").append(current);
        }
        result.setCharAt(2, '=');
        System.out.println(result);
    }

    private boolean addSolutions(boolean circular) {
        boolean removed = false;
        Implicant removee = null;
        for (Iterator it = table.iterator(); it.hasNext() && !removed;) {
            Implicant current = (Implicant) it.next();
            for (Integer i : current.coverage) {
                if (coverage[i] == 1 || circular) { //only term covering or just select the first one b/c circular
                    System.out.println(current + " is the only implicant covering " + i);
                    solution.add(current);
                    it.remove();
                    removee = current;
                    removed = true;
                    break;
                }
            }
        }
        if (removed) {
            for (Integer i : removee.coverage) {
                removeColumn(i);
            }
        }
        return removed;
    }


    private ArrayList<Implicant> findAllLessThans() {
        ArrayList<Implicant> trash = new ArrayList<Implicant>();
        for (int i = 0; i < table.size(); i++) {
            Implicant current = table.get(i);
            if (current.coverage.isEmpty()) {
                trash.add(current);
                continue;
            }
            for (int j = i+1; j < table.size(); j++) {
                Implicant other = table.get(j);
                if (trash.contains(current) || trash.contains(other)) { continue; }
                if (aIsSubsetofB(current.coverage, other.coverage)) {
                    trash.add(current);
                    System.out.println("Removing " + current);
                } else if (aIsSubsetofB(other.coverage, current.coverage)) {
                    trash.add(other);
                    System.out.println("Removing " + other);
                }
            }
        }
        return trash;
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

    void addTo(ArrayList<Implicant> list, Implicant a) {
        if (!list.contains(a)) {
            list.add(a);
        }
    }


    boolean aIsSubsetofB(ArrayList<Integer> A, ArrayList<Integer> B) { // implies |a| < |b|
        if (A.size() > B.size()) return false;
        for (Integer a : A) {
            if (!B.contains(a)) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < table.size(); i++) {
            Implicant imp = table.get(i);
            String filledRow = new String(new char[1 << imp.numTerms]).replace("\0", " |");
            StringBuilder cur = new StringBuilder(filledRow);
            result.append(imp).append(" ");
            result.append("\t");
            result.append(imp.coverage).append('\n');
        }
        return result.toString();
    }
}