package u4a1;

import java.util.EmptyStackException;
import java.util.Arrays;

/**
 * Dynamically growing stack.
 */
public class Stack {
	private int[] buffer;
	private int size;
	private int pointer; // points to an empty cell of the stack, that follows
							// the top element

	/**
	 * Creates a new stack
	 * 
	 * @param capacity
	 *            the initial capacity of the stack
	 */
	public Stack(int capacity) {
		size = capacity;
		buffer = new int[size];
	}

	/**
	 * Converts stack to a string representation.
	 * 
	 * @return A ", "-separated list of the numbers, enclosed in square
	 *         brackets. Bottom numbers come first.
	 */
	public String toString() {
		StringBuffer s = new StringBuffer(Arrays.toString(buffer));
		if (pointer == 0)
			return "[]";
		s.setLength(3 * pointer - 1);
		s.append("]");
		return s.toString();
	}

	/**
	 * Doubles the capacity of the stack.
	 * 
	 * Copies all objects to a new buffer of doubled size.
	 */
	private void grow() {
		int[] newBuffer = new int[2 * buffer.length];
		size = 2 * size;
		{
			for (int i = 0; i < buffer.length; i++)
				newBuffer[i] = buffer[i];
		}
		buffer = newBuffer;
	}

	/**
	 * Pushes a number onto the top of this stack.
	 * 
	 * Grows the stack if necessary.
	 * 
	 * @param number
	 *            the number to be pushed onto this stack.
	 */
	public void push(int number) {
		if (pointer == buffer.length)
			grow();
		buffer[pointer] = number;
		pointer++;
	}

	/**
	 * Removes the number at the top of this stack and returns it as the value
	 * of this function.
	 * 
	 * @return The number at the top of this stack
	 * @throws EmptyStackException
	 *             if this stack is empty
	 */
	public int pop() throws EmptyStackException {
		if (pointer == 0)
			throw new EmptyStackException();
		pointer--; // If we move the pointer to the top element, it will be
					// considered as an empty
					// cell of the stack (because pointer always points to an
					// empty cell of a stack),
					// though it will be formally still existing. Thus none of
					// the methods will actually
					// consider the element to be in a stack, so we can say the
					// element is removed.

		return buffer[pointer];
	}

	/**
	 * Looks at the number at the top of this stack without removing it from the
	 * stack.
	 * 
	 * @return the number at the top of this stack
	 * @throws EmptyStackException
	 *             if this stack is empty
	 */
	public int peek() throws EmptyStackException {
		if (pointer == 0)
			throw new EmptyStackException();
		return buffer[pointer - 1];
	}

	/**
	 * Tests if this stack is empty.
	 * 
	 * @return true if and only if this stack contains no items; false
	 *         otherwise.
	 */
	public boolean empty() {
		return pointer == 0;
	}

	/**
	 * Get the size of this stack.
	 * 
	 * @return the current number of items on this stack
	 */
	public int size() {
		return pointer;
	}

	/**
	 * Get the capacity of this stack.
	 * 
	 * @return the maximum number of items this stack can hold without having to
	 *         grow
	 */
	public int capacity() {
		return size;
	}
}
