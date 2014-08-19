package nieskaz;

/*
 * Property of Jean-Francois Nies and Dmitry Kazakov
 * 
 * The formula for counting the scores and basic ideas for the heuristics were inspired by the work of 
 * Vaishnavi Sannidhanam and Muthukaruppan Annamalai "An Analysis of Heuristics in Othello".
 * It can be found here:
 * http://courses.cs.washington.edu/courses/cse573/04au/Project/mini1/RUSSIA/Final_Paper.pdf
 */
import java.util.ArrayList;

import reversi.Coordinates;
import reversi.GameBoard;
import reversi.OutOfBoundsException;
import reversi.ReversiPlayer;

public class Matilda implements ReversiPlayer{
	private static int player = 0, opponent = 0;
	private double alpha = Integer.MIN_VALUE;
	private double beta = Integer.MAX_VALUE;
	private long timeLimit = 5000;
	
	public Matilda () {
		System.out.println("Matilda erstellt.");
	}
	
	public void initialize(int color, long timeout) {
		if (color == GameBoard.RED)
		{
			player = GameBoard.RED;
			opponent = GameBoard.GREEN;
			System.out.println("Matilda ist Spieler RED.");
		}
		else if (color == GameBoard.GREEN)
		{
			player = GameBoard.GREEN;
			opponent = GameBoard.RED;
			System.out.println("Matilda ist Spieler GREEN.");
		}
		
	}

	public Coordinates nextMove(GameBoard gb) {
		long timeout = System.currentTimeMillis() + timeLimit - 10;
		BestMove bestMove = null;
		ArrayList<Coordinates> possibleMoves = possibleMoves(gb, player);
		if (possibleMoves.size() == 1) return possibleMoves.get(0);
		try {
			try {
				bestMove = max(1, timeout, gb, 0, alpha, beta);
			} catch (OutOfBoundsException e) {
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
					e.printStackTrace();
				}
		}	catch (Timeout e) {
		  	}
		
		return bestMove.coord;
	}
		
	private static BestMove max(int maxDepth, long timeout, GameBoard gb, int depth, 
			double alpha, double beta) throws Timeout, OutOfBoundsException {
		if (System.currentTimeMillis() > timeout) throw new Timeout();
		if (depth == maxDepth) return new BestMove (null, evaluate(gb), true);
		ArrayList<Coordinates> movesList = possibleMoves(gb, player);
		if (movesList.isEmpty()) {
			if (gb.isMoveAvailable(opponent)) {
				BestMove result = min(maxDepth, timeout, gb, depth+1, alpha, beta);
				return new BestMove(result.coord, result.value, false);				
			}
			else return new BestMove(null, finalEvaluation(gb), false);
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
			else return new BestMove(null, finalEvaluation(gb), false);
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
	
	private static Coordinates nearEdges [] = new Coordinates [20];
	static {
		for (int i = 0; i < 6; i++) {
			nearEdges[i] = new Coordinates (i+2, 2);
		}
		for (int i = 6; i < 11; i++) {
			nearEdges[i] = new Coordinates (7, i-3);
		}
		for (int i = 11; i < 16; i++) {
			nearEdges[i] = new Coordinates (17-i, 7);
		}
		for (int i = 16; i < 20; i++) {
			nearEdges[i] = new Coordinates (2, 22-i);
		}
	}
	
	private static double evaluate (GameBoard gb) throws OutOfBoundsException {	
		double kcc = 0, kcp = 0, kccl = 0, kam = 0, ks = 0, kne = 0;
		int movesLeft = 64 - gb.countStones(player) - gb.countStones(opponent);
		/*
		 * Dynamically changing weights
		 */
		if (movesLeft >= 40) {
			kcc = 15; // corners captured
			kcp = 0; // coin parity
			kccl = 30; // corner closeness
			kam = 40; // actual mobility
			ks = 20; // stability
			kne = 30; // near edges
		}
		
		else if (movesLeft > 8)  {
			kcc = 60; // corners captured
			kcp = 3; // coin parity
			kccl = 30; // corner closeness
			kam = 42; // actual mobility
			ks = 60; // stability
			kne = 0; // near edges
		}
		
		/*
		 * Endspiel evaluation.
		 * For last moves the whole game tree is very likely to be evaluated fully. 
		 * The best characteristics possible is the difference between player's and opponent's stones.
		 */
		else {			
			kcc = 0; // corners captured
			kcp = 50; // coin parity
			kccl = 0; // corner closeness
			kam = 0; // actual mobility
			ks = 0; // stability
			kne = 0; // near edges
		}
		
		/*
		 * Stability
		 */
		int s = 0;
		int playerScore = 0, oppScore = 0;
		playerScore = stability(player, gb);
		oppScore = stability(opponent, gb);
		if (playerScore + oppScore != 0) s = 100*(playerScore - oppScore)/(playerScore + oppScore);
		
		/*
		 * Coin parity
		 */
		int cp = 0; 		
		cp = 100*(gb.countStones(player) - gb.countStones(opponent))/
				(gb.countStones(player) + gb.countStones(opponent));
		
		/*
		 * Corners captured value
		 */
		playerScore = 0; oppScore = 0;
		int cc = 0; 
		
		if (gb.getOccupation(new Coordinates(1,1)) == player) playerScore++;
		else if (gb.getOccupation(new Coordinates(1,1)) == opponent) oppScore++;
		if (gb.getOccupation(new Coordinates(1,8)) == player) playerScore++;
		else if (gb.getOccupation(new Coordinates(1,8)) == opponent) oppScore++;
		if (gb.getOccupation(new Coordinates(8,1)) == player) playerScore++;
		else if (gb.getOccupation(new Coordinates(8,1)) == opponent) oppScore++;
		if (gb.getOccupation(new Coordinates(8,8)) == player) playerScore++;
		else if (gb.getOccupation(new Coordinates(8,8)) == opponent) oppScore++;
		if (playerScore + oppScore != 0) cc = 100*(playerScore - oppScore)/(playerScore + oppScore);
		
		/*
		 * Corners closeness
		 */
		int ccl = 0; 
		playerScore = oppScore = 0;
		if (gb.getOccupation(new Coordinates (1, 1)) == GameBoard.EMPTY) {
			if (gb.getOccupation(new Coordinates (1, 2)) == player) playerScore++;
			else if (gb.getOccupation(new Coordinates (1, 2)) == opponent) oppScore++;
			if (gb.getOccupation(new Coordinates (2, 1)) == player) playerScore++;
			else if (gb.getOccupation(new Coordinates (2, 1)) == opponent) oppScore++;
			if (gb.getOccupation(new Coordinates (2, 2)) == player) playerScore++;
			else if (gb.getOccupation(new Coordinates (2, 2)) == opponent) oppScore++;
		}
		if (gb.getOccupation(new Coordinates (1, 8)) == GameBoard.EMPTY) {
			if (gb.getOccupation(new Coordinates (1, 7)) == player) playerScore++;
			else if (gb.getOccupation(new Coordinates (1, 7)) == opponent) oppScore++;
			if (gb.getOccupation(new Coordinates (2, 8)) == player) playerScore++;
			else if (gb.getOccupation(new Coordinates (2, 8)) == opponent) oppScore++;
			if (gb.getOccupation(new Coordinates (2, 7)) == player) playerScore++;
			else if (gb.getOccupation(new Coordinates (2, 7)) == opponent) oppScore++;
		}
		if (gb.getOccupation(new Coordinates (8, 1)) == GameBoard.EMPTY) {
			if (gb.getOccupation(new Coordinates (7, 1)) == player) playerScore++;
			else if (gb.getOccupation(new Coordinates (7, 1)) == opponent) oppScore++;
			if (gb.getOccupation(new Coordinates (7, 2)) == player) playerScore++;
			else if (gb.getOccupation(new Coordinates (7, 2)) == opponent) oppScore++;
			if (gb.getOccupation(new Coordinates (8, 2)) == player) playerScore++;
			else if (gb.getOccupation(new Coordinates (8, 2)) == opponent) oppScore++;
		}
		if (gb.getOccupation(new Coordinates (8, 8)) == GameBoard.EMPTY) {
			if (gb.getOccupation(new Coordinates (8, 7)) == player) playerScore++;
			else if (gb.getOccupation(new Coordinates (8, 7)) == opponent) oppScore++;
			if (gb.getOccupation(new Coordinates (7, 7)) == player) playerScore++;
			else if (gb.getOccupation(new Coordinates (7, 7)) == opponent) oppScore++;
			if (gb.getOccupation(new Coordinates (7, 8)) == player) playerScore++;
			else if (gb.getOccupation(new Coordinates (7, 8)) == opponent) oppScore++;
		}
		if (playerScore + oppScore != 0) ccl = -100*(playerScore - oppScore)/(playerScore + oppScore); 
		
		/*
		 * Actual mobility
		 */
		double am = 0;
		playerScore = 0; oppScore = 0;
		playerScore = possibleMoves(gb, player).size();
		oppScore = possibleMoves(gb, opponent).size();
		if (playerScore + oppScore != 0) {
			if (playerScore > oppScore)
				am = 100.00 * playerScore/(playerScore + oppScore);
			else if (playerScore < oppScore)
				am = -100.00 * oppScore/(playerScore + oppScore);
			else am = 0;
		}
		
		/*
		 * Near edges
		 */
		playerScore = 0; oppScore = 0;
		int ne = 0;
		for (int i = 0; i < 20; i++) {
			if (gb.getOccupation(nearEdges[i]) == player) playerScore++;
			else if (gb.getOccupation(nearEdges[i]) == player) oppScore++;
		}
		if (playerScore + oppScore != 0) ne = -100*(playerScore - oppScore)/(playerScore + oppScore);
		
		return kcc * cc + kcp * cp + kccl * ccl + kam * am + ks * s + kne * ne;			
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
	
	private static int stability (int player, GameBoard gb) throws OutOfBoundsException {
		int playerScore = 0;
		if (gb.getOccupation(new Coordinates(1,3)) == player) {
			playerScore++;
			for (int i = 4; i <= 6; i++) {
				if (gb.getOccupation(new Coordinates(1,i)) == gb.getOccupation(new Coordinates(1,i-1))) 
					playerScore++;
				else break;
			}
		}
		if (gb.getOccupation(new Coordinates(1,6)) == player) {
			playerScore++;
			for (int i = 5; i <= 3; i--) {
				if (gb.getOccupation(new Coordinates(1,i)) == gb.getOccupation(new Coordinates(1,i-1))) 
					playerScore++;
				else break;
			}
		}
		if (gb.getOccupation(new Coordinates(8,3)) == player) {
			playerScore++;
			for (int i = 4; i <= 6; i++) {
				if (gb.getOccupation(new Coordinates(8,i)) == gb.getOccupation(new Coordinates(8,i-1))) 
					playerScore++;
				else break;
			}
		}
		if (gb.getOccupation(new Coordinates(8,6)) == player) {
			playerScore++;
			for (int i = 5; i <= 3; i--) {
				if (gb.getOccupation(new Coordinates(8,i)) == gb.getOccupation(new Coordinates(8,i-1))) 
					playerScore++;
				else break;
			}
		}
		if (gb.getOccupation(new Coordinates(3,1)) == player) {
			playerScore++;
			for (int i = 4; i <= 6; i++) {
				if (gb.getOccupation(new Coordinates(i,1)) == gb.getOccupation(new Coordinates(i-1,1))) 
					playerScore++;
				else break;
			}
		}
		if (gb.getOccupation(new Coordinates(3,8)) == player) {
			playerScore++;
			for (int i = 4; i <= 6; i++) {
				if (gb.getOccupation(new Coordinates(i,8)) == gb.getOccupation(new Coordinates(i-1,8))) 
					playerScore++;
				else break;
			}
		}
		if (gb.getOccupation(new Coordinates(6,1)) == player) {
			playerScore++;
			for (int i = 5; i <= 3; i--) {
				if (gb.getOccupation(new Coordinates(i,1)) == gb.getOccupation(new Coordinates(i-1,1))) 
					playerScore++;
				else break;
			}
		}
		if (gb.getOccupation(new Coordinates(6,8)) == player) {
			playerScore++;
			for (int i = 5; i <= 3; i--) {
				if (gb.getOccupation(new Coordinates(i,8)) == gb.getOccupation(new Coordinates(i-1,8))) 
					playerScore++;
				else break;
			}
		}
		return playerScore;
	}
	
	private static int finalEvaluation (GameBoard gb) {
		int f = gb.countStones(player) > gb.countStones(opponent) ? 1 : -1;
		return f * 1543 + gb.countStones(player) - gb.countStones(opponent);
	}
}
