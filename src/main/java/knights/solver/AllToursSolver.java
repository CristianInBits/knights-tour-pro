package knights.solver;

import knights.model.Position;
import java.util.List;

/**
 * Strategy capable of enumerating all Knight's Tour solutions.
 * WARNING: The number of solutions can be extremely large (time/memory).
 */
public interface AllToursSolver extends TourSolver {
    /**
     * Enumerate all tours from the configured start position.
     * Implementations must return defensive copies of the solutions.
     */
    List<List<Position>> solveAll();
}
