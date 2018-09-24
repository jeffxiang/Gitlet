package gitlet;

import ucb.junit.textui;
import org.junit.Test;

/** The suite of all JUnit tests for the gitlet package.
 *  @author Jeff Xiang
 */
public class UnitTest {

    /** Run the JUnit tests in the loa package. Add xxxTest.class entries to
     *  the arguments of runClasses to run other JUnit tests. */
    public static void main(String[] ignored) {
        textui.runClasses(UnitTest.class);
    }

    @Test
    public void testCalendar() {
        Commit c = new Commit();
        String s = c.getDate();
    }
}



