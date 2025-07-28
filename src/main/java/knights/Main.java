package knights;

public class Main {
    public static void main(String[] args) {
        if (args.length < 5) {
            System.out.println("Usage: java -jar knights-tour.jar <rows> <cols> <startRow> <startCol> <mode>");
            System.out.println("Example: java -jar knights-tour.jar 5 5 0 0 single");
            return;
        }

        int rows = Integer.parseInt(args[0]);
        int cols = Integer.parseInt(args[1]);
        int startRow = Integer.parseInt(args[2]);
        int startCol = Integer.parseInt(args[3]);
        String mode = args[4];

        System.out.printf("Board: %dx%d | Start: (%d,%d) | Mode: %s\n",
                rows, cols, startRow, startCol, mode);
    }
}
