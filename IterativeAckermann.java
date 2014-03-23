package u4a2;

import u4a1.Stack;

public class IterativeAckermann {
	/**
	 * Stack-based iterative implementation of the Ackermann function.
	 * 
	 * @param n
	 *            parameter n
	 * @param m
	 *            parameter m
	 * @return Ackermann(n,m)
	 */
	private int result;

	public int A(int n, int m) {
		Stack myStack = new Stack(1000);

		myStack.push(n);
		myStack.push(m);
		while (myStack.size() > 1) {

			if (n != 0 && m != 0) {
				myStack.push(n - 1);
				myStack.push(n);
				myStack.push(m - 1);

				m = myStack.pop();
				n = myStack.pop();

			} else if (n != 0 && m == 0) {
				myStack.push(n - 1);
				myStack.push(1);

				m = myStack.pop();
				n = myStack.pop();

			} else { // if n == 0
				result = m + 1;
				myStack.push(result);
				m = myStack.pop();
				if (myStack.size() == 0) {
					result = m + 1;
					return result;
				} else
					n = myStack.pop();

			}

		}

		return result;
	}
}