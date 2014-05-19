package u10a3;

import java.util.ArrayList;

import reversi.Coordinates;
import reversi.GameBoard;
import reversi.OutOfBoundsException;
import reversi.ReversiPlayer;

public class Test1 implements ReversiPlayer{
	
	private static int player, opponent = 0;
	private double alpha = Integer.MIN_VALUE;
	private double beta = Integer.MAX_VALUE;
	private long timeLimit = 5000;
	
	public Test1 () {
		System.out.println("AlphaBetaPlayer erstellt.");
	}
	
	
	@Override
	public void initialize(int color, long timeout) {
		if (color == GameBoard.RED)
		{
			player = GameBoard.RED;
			opponent = GameBoard.GREEN;
			System.out.println("AlphaBetaPlayer ist Spieler RED.");
		}
		else if (color == GameBoard.GREEN)
		{
			player = GameBoard.GREEN;
			opponent = GameBoard.RED;
			System.out.println("AlphaBetaPlayer ist Spieler GREEN.");
		}
		
	}

	@Override
	public Coordinates nextMove(GameBoard gb) {
		long timeout = System.currentTimeMillis() + timeLimit;
		BestMove bestMove = null;
		
		ArrayList<Coordinates> possibleMoves = possibleMoves(gb, player);
		if (possibleMoves.size() == 1) return possibleMoves.get(0);
		
		try {
			try {
				bestMove = max(1, timeout, gb, 0, alpha, beta);
			} catch (OutOfBoundsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (Timeout e) {
			throw new AssertionError("Not enough time for depth 1");
		}
		try {
			for (int i = 2; bestMove.cut; i++ )
				try {
					bestMove = max(i, timeout, gb, 0, alpha, beta);
				} catch (OutOfBoundsException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		} catch (Timeout e) {
			
		}
		
		return bestMove.coord;
	}
	
	
	protected static BestMove max(int maxDepth, long timeout, GameBoard gb, int depth, 
			double alpha, double beta) throws Timeout, OutOfBoundsException {
		if (System.currentTimeMillis() > timeout) throw new Timeout();
		if (depth == maxDepth) return new BestMove (null, evaluate(gb), true);
		ArrayList<Coordinates> movesList = possibleMoves(gb, player);
		if (movesList.isEmpty()) {
			if (gb.isMoveAvailable(opponent)) {
				BestMove result = min(maxDepth, timeout, gb, depth+1, alpha, beta);
				return new BestMove(result.coord, result.value, false);				
			}
			else return new BestMove(null, evaluate(gb), false);
		}
		
		BestMove bestMove = new BestMove(null, Integer.MIN_VALUE, false);
		for (Coordinates coord : movesList) {
			GameBoard hypothetical = gb.clone();
			hypothetical.checkMove(player, coord);
			hypothetical.makeMove(player, coord);
			BestMove result = min(maxDepth, timeout, hypothetical, depth+1, alpha, beta);
			bestMove.cut = bestMove.cut || result.cut;
			if (result.value > bestMove.value) {
				bestMove.coord = coord;				
				bestMove.value = result.value;				
			}	
			if (bestMove.value > alpha) alpha = bestMove.value;
			if (alpha >= beta) break;			
		}
		
		return bestMove;
	}
	
	private static BestMove min(int maxDepth, long timeout, GameBoard gb, int depth,
			double alpha, double beta) throws Timeout, OutOfBoundsException {
		if (System.currentTimeMillis() > timeout) throw new Timeout();
		if (depth == maxDepth) return new BestMove (null, evaluate(gb), true);
		ArrayList<Coordinates> movesList = possibleMoves(gb, opponent);
		if (movesList.isEmpty()) {
			if (gb.isMoveAvailable(player)) {
				BestMove result = max(maxDepth, timeout, gb, depth+1, alpha, beta);
				return new BestMove(result.coord, result.value, false);				
			}
			else return new BestMove(null, evaluate(gb), false);
		}
		
		BestMove bestMove = new BestMove(null, Integer.MAX_VALUE, false);
		for (Coordinates coord : movesList) {
			GameBoard hypothetical = gb.clone();
			hypothetical.checkMove(opponent, coord);
			hypothetical.makeMove(opponent, coord);
			BestMove result = max(maxDepth, timeout, hypothetical, depth+1, alpha, beta);
			bestMove.cut = bestMove.cut || result.cut;
			if (result.value < bestMove.value) {
				bestMove.coord = coord;
				bestMove.value = result.value;
			}	
			if (bestMove.value < beta) beta = bestMove.value;
			if (alpha >= beta) break;
		}
		
		return bestMove;
	}
	
	
	
	/** 
	 * Ultimate evaluation function
    */
	private static double evaluate (GameBoard gb) throws OutOfBoundsException {	
		int V[][] = {{ 4, -3,  2,  2,  2,  2, -3,  4 },
			        { -3, -4, -1, -1, -1, -1, -4, -3 },
			        {  2, -1,  1,  0,  0,  1, -1,  2 },
			        {  2, -1,  0,  1,  1,  0, -1,  2 },
			        {  2, -1,  0,  1,  1,  0, -1,  2 },
			        {  2, -1,  1,  0,  0,  1, -1,  2 },
			        { -3, -4, -1, -1, -1, -1, -4, -3 },
			        {  4, -3,  2,  2,  2,  2, -3,  4 }};
		int movesLeft = 64 - gb.countStones(player) - gb.countStones(opponent);
		int my_tiles = 0, opp_tiles = 0, i, j, k, my_front_tiles = 0, opp_front_tiles = 0, x, y;
        double p = 0, c = 0, l = 0, m = 0, f = 0, d = 0;
        Coordinates grid [][] = new Coordinates [gb.getSize()][gb.getSize()];
		
		for (i = 1; i <= gb.getSize(); i++) {
			for (j = 1; j <= gb.getSize(); j++) {
				grid [i-1][j-1] = new Coordinates (i, j);
			}
		}
 
        int X1[] = {-1, -1, 0, 1, 1, 1, 0, -1};
        int Y1[] = {0, 1, 1, 1, 0, -1, -1, -1};
        
        for(i=0; i<8; i++)
            for(j=0; j<8; j++)  {
                    if(gb.getOccupation(grid[i][j]) == player)  {
                            d += V[i][j];
                            my_tiles++;
                    } else if(gb.getOccupation(grid[i][j]) == opponent)  {
                            d -= V[i][j];
                            opp_tiles++;
                    }
                    if(gb.getOccupation(grid[i][j]) != GameBoard.EMPTY)   {
                            for(k=0; k<8; k++)  {
                                    x = i + X1[k]; y = j + Y1[k];
                                    if(x >= 0 && x < 8 && y >= 0 && y < 8 && gb.getOccupation(grid[i][j]) == GameBoard.EMPTY) {
                                            if(gb.getOccupation(grid[i][j]) == player)  my_front_tiles++;
                                            else opp_front_tiles++;
                                            break;
                                    }
                            }
                    }
            }
    if(my_tiles > opp_tiles)
            p = (100.0 * my_tiles)/(my_tiles + opp_tiles);
    else if(my_tiles < opp_tiles)
            p = -(100.0 * opp_tiles)/(my_tiles + opp_tiles);
    else p = 0;

    if(my_front_tiles > opp_front_tiles)
            f = -(100.0 * my_front_tiles)/(my_front_tiles + opp_front_tiles);
    else if(my_front_tiles < opp_front_tiles)
            f = (100.0 * opp_front_tiles)/(my_front_tiles + opp_front_tiles);
    else f = 0;

//Corner occupancy
    my_tiles = opp_tiles = 0;
    if(gb.getOccupation(grid[0][0]) == player) my_tiles++;
    else if(gb.getOccupation(grid[0][0]) == opponent) opp_tiles++;
    if(gb.getOccupation(grid[0][7]) == player) my_tiles++;
    else if(gb.getOccupation(grid[0][7]) == opponent) opp_tiles++;
    if(gb.getOccupation(grid[7][0]) == player) my_tiles++;
    else if(gb.getOccupation(grid[7][0]) == opponent) opp_tiles++;
    if(gb.getOccupation(grid[7][7]) == player) my_tiles++;
    else if(gb.getOccupation(grid[7][7]) == opponent) opp_tiles++;
    c = 25 * (my_tiles - opp_tiles);

//Corner closeness
    my_tiles = opp_tiles = 0;
    if(gb.getOccupation(grid[0][0]) == GameBoard.EMPTY)   {
            if(gb.getOccupation(grid[0][1]) == player) my_tiles++;
            else if(gb.getOccupation(grid[0][1]) == opponent) opp_tiles++;
            if(gb.getOccupation(grid[1][1]) == player) my_tiles++;
            else if(gb.getOccupation(grid[1][1]) == opponent) opp_tiles++;
            if(gb.getOccupation(grid[1][0]) == player) my_tiles++;
            else if(gb.getOccupation(grid[1][0]) == opponent) opp_tiles++;
    }
    if(gb.getOccupation(grid[0][7]) == GameBoard.EMPTY)   {
            if(gb.getOccupation(grid[0][6]) == player) my_tiles++;
            else if(gb.getOccupation(grid[0][6]) == opponent) opp_tiles++;
            if(gb.getOccupation(grid[1][6]) == player) my_tiles++;
            else if(gb.getOccupation(grid[1][6]) == opponent) opp_tiles++;
            if(gb.getOccupation(grid[1][7]) == player) my_tiles++;
            else if(gb.getOccupation(grid[1][7]) == opponent) opp_tiles++;
    }
    if(gb.getOccupation(grid[7][0]) == GameBoard.EMPTY)   {
            if(gb.getOccupation(grid[7][1]) == player) my_tiles++;
            else if(gb.getOccupation(grid[7][1]) == opponent) opp_tiles++;
            if(gb.getOccupation(grid[6][1]) == player) my_tiles++;
            else if(gb.getOccupation(grid[6][1]) == opponent) opp_tiles++;
            if(gb.getOccupation(grid[6][0]) == player) my_tiles++;
            else if(gb.getOccupation(grid[6][0]) == opponent) opp_tiles++;
    }
    if(gb.getOccupation(grid[7][7]) == GameBoard.EMPTY)   {
            if(gb.getOccupation(grid[6][7]) == player) my_tiles++;
            else if(gb.getOccupation(grid[6][7]) == opponent) opp_tiles++;
            if(gb.getOccupation(grid[6][6]) == player) my_tiles++;
            else if(gb.getOccupation(grid[6][6]) == opponent) opp_tiles++;
            if(gb.getOccupation(grid[7][6]) == player) my_tiles++;
            else if(gb.getOccupation(grid[7][6]) == opponent) opp_tiles++;
    }
    l = -12.5 * (my_tiles - opp_tiles);

//Mobility
    my_tiles = possibleMoves(gb, player).size();
    opp_tiles = possibleMoves(gb, opponent).size();
    if(my_tiles > opp_tiles)
            m = (100.0 * my_tiles)/(my_tiles + opp_tiles);
    else if(my_tiles < opp_tiles)
            m = -(100.0 * opp_tiles)/(my_tiles + opp_tiles);
    else m = 0;

//final weighted score
    double score = (10 * p) + (801.724 * c) + (382.026 * l) + (78.922 * m) + (74.396 * f) + (10 * d);
    return score;
}
        
			
	

	/**
	 * Generates all possible moves for the player at the current game situation
	 * @param gb current state of GameBoard
	 * @param player player, whose possible moves are to be determined
	 * @return all possible moves saved in an ArrayList
	 */
	public static ArrayList<Coordinates> possibleMoves(GameBoard gb, int player) {
		ArrayList<Coordinates> possible = new ArrayList <Coordinates> (gb.getSize() * gb.getSize());
		Coordinates coord = null;
		for (int i = 1; i <= gb.getSize(); i++) {
			for (int j = 1; j <= gb.getSize(); j++) {
				coord = new Coordinates (i, j);
				if (gb.checkMove(player, coord)) possible.add(coord);
			}
		}
		return possible;
	}
}
	
