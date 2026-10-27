package io.freemytv;

/** App indices on the shelf; the fixed All Apps tile follows them at index `count`. */
final class ShelfLayout {
    private ShelfLayout() {}

    static int appSlots(int count, int limit) {
        return Math.min(count, Math.max(1, limit - 1));
    }

    /** Keep the focused app visible; focusing All Apps leaves the app row where it was. */
    static int first(int count, int slots, int first, int focused) {
        first = Math.max(0, Math.min(first, count - slots));
        if (focused >= count) return first;
        return Math.max(0, Math.min(Math.max(first, focused - slots + 1), Math.min(focused, count - slots)));
    }

    static int next(int count, int slots, int first, int focused, int direction) {
        // Back from the fixed tile goes to its visible neighbor, even if there are more apps off screen.
        if (focused == count && direction < 0) return Math.max(0, first + slots - 1);
        return Math.max(0, Math.min(count, focused + direction));
    }
}
