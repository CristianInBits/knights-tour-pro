package knights.solver;

import knights.model.Position;
import java.util.List;

public interface TourSolver {

    /**
     * Solves the Knight's Tour from a given starting position.
     *
     * @return List of positions representing the tour in order,
     *         or empty list if no solution was found.
     */
    List<Position> solve();
}
