package ax25;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;

public class FrameQueue {

	public static final void main(String[] args) {
		Deque<String> myQueue = new LinkedList<String>();
        myQueue.add("iFrame1");
        myQueue.add("iFrame2");
        myQueue.add("iFrame3");
        myQueue.offer("iFrame4");
         
        for(String element : myQueue){
            System.out.println("Element : " + element);
        }
         
        System.out.println("Queue : " + myQueue);
        System.out.println(myQueue.peek());
        System.out.println("After peek : " + myQueue);
        System.out.println(myQueue.poll());
        System.out.println("After poll : " + myQueue);
        String s = myQueue.pop();
        System.out.println(s);
        System.out.println("After pop : " + myQueue);
        myQueue.push(s);
        System.out.println("After push : " + myQueue);
        
	}
}
