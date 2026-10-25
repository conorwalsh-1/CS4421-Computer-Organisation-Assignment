/**
 * Shared helpers for drawing charts in the terminal: colours, horizontal bars and
 * a graph of a percentage over time. Used by memReport and cpuReport.
 */
public class textChart {
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String CYAN = "\u001B[36m";

    private static final int GRAPH_ROWS = 10;

    /** How many of 'width' characters should be filled for 'percent', from 0 to width. */
    public static int filledWidth(double percent, int width) {
        int filled = (int) Math.round(percent / 100 * width);
        if (filled > width)
            filled = width;
        if (filled < 0)
            filled = 0;
        return filled;
    }

    /**
     * A bar in brackets followed by the percentage. Only the filled part is coloured;
     * the empty part is left in the normal text colour so it doesn't look filled.
     */
    public static String percentBar(double percent, int width) {
        int filled = filledWidth(percent, width);
        return "[" + colourFor(percent) + repeat('█', filled) + RESET + repeat('░', width - filled) + "] "
                + String.format("%.1f%%", percent);
    }

    /** Green under 60%, yellow under 85%, red otherwise. */
    public static String colourFor(double percent) {
        if (percent < 60)
            return GREEN;
        if (percent < 85)
            return YELLOW;
        return RED;
    }

    /**
     * A graph of a percentage over time, one column per reading, oldest on the left.
     * 'count' is how many readings in 'history' are filled in so far, and 'width' is
     * the total number of readings the graph has room for.
     *
     * Each row's label is the value at the middle of that row, and each row is split
     * into a lower and upper half, so a bar's top lines up with its label to within 5%.
     * A reading below 0 (not available) is left blank.
     */
    public static void graph(String title, double[] history, int count, int width) {
        System.out.println("  " + title);
        System.out.println();

        int percentPerRow = 100 / GRAPH_ROWS;
        for (int row = GRAPH_ROWS; row >= 0; row--) {
            String line = String.format("  %3d%% │", row * percentPerRow);
            for (int i = 0; i < count; i++) {
                // the bar's height in halves of a row, measured from the 0% line
                int halves = (int) Math.round(history[i] / (percentPerRow / 2.0));
                boolean lower = row > 0 && halves >= row * 2;
                boolean upper = halves >= row * 2 + 1;

                if (lower && upper) {
                    line = line + colourFor(history[i]) + "██" + RESET;
                } else if (lower) {
                    line = line + colourFor(history[i]) + "▄▄" + RESET;
                } else if (upper) {
                    line = line + colourFor(history[i]) + "▀▀" + RESET;
                } else {
                    line = line + "  ";
                }
            }
            System.out.println(line);
        }
        System.out.println("       └" + repeat('─', width * 2));
        System.out.println("        oldest" + repeat(' ', width * 2 - 12) + "newest");
    }

    /** The character c repeated 'count' times. */
    public static String repeat(char c, int count) {
        String s = "";
        for (int i = 0; i < count; i++)
            s = s + c;
        return s;
    }

    /** Clears the console. */
    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    /** Waits for 'ms' milliseconds. */
    public static void pause(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
