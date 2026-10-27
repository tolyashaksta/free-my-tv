package io.freemytv;

/** Standalone JVM checks: compile with ShelfLayout.java, then run io.freemytv.ShelfLayoutTest. */
public final class ShelfLayoutTest {
    private static void equal(int expected, int actual) {
        if (expected != actual) throw new AssertionError("Expected " + expected + ", got " + actual);
    }

    public static void main(String[] args) {
        equal(0, ShelfLayout.appSlots(0, 8));
        equal(1, ShelfLayout.appSlots(1, 8));
        equal(7, ShelfLayout.appSlots(8, 8));
        equal(7, ShelfLayout.appSlots(20, 8));
        equal(0, ShelfLayout.first(0, 0, 0, 0));
        equal(0, ShelfLayout.next(0, 0, 0, 0, -1));
        equal(0, ShelfLayout.next(0, 0, 0, 0, 1));
        equal(0, ShelfLayout.first(12, 7, 0, 12));   // fixed tile never scrolls the apps
        equal(6, ShelfLayout.next(12, 7, 0, 12, -1));
        equal(5, ShelfLayout.first(12, 7, 0, 11));
        equal(2, ShelfLayout.first(12, 7, 5, 2));
        equal(0, ShelfLayout.first(2, 2, 5, 2));    // apps removed while the shelf was scrolled

        for (int count = 0; count <= 30; count++) {
            int slots = ShelfLayout.appSlots(count, 8), first = 0, focus = 0;
            for (int step = 0; step < count; step++) {
                focus = ShelfLayout.next(count, slots, first, focus, 1);
                first = ShelfLayout.first(count, slots, first, focus);
                if (focus < count && (focus < first || focus >= first + slots)) {
                    throw new AssertionError("Focused app scrolled out of view");
                }
            }
            equal(count, focus);   // All Apps is reachable after every app
            int before = first;
            equal(before, ShelfLayout.first(count, slots, first, focus));
            for (int step = 0; step <= count; step++) {
                focus = ShelfLayout.next(count, slots, first, focus, -1);
                first = ShelfLayout.first(count, slots, first, focus);
            }
            equal(0, focus);
            equal(0, first);
        }
        System.out.println("Shelf layout: navigation, scrolling, empty shelf and pinned tile passed.");
    }
}
