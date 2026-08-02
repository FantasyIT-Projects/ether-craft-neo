package studio.fantasyit.ether_craft.perf;

public class TickStat {
    public static final int WINDOW = 20;

    private final long[] buffer = new long[WINDOW];
    private int idx = 0;
    private int count = 0;
    private long sum = 0;
    private long last = 0;
    private long max = 0;

    public void record(long nanos) {
        last = nanos;
        if (count < WINDOW) {
            count++;
        } else {
            sum -= buffer[idx];
        }
        sum += nanos;
        buffer[idx] = nanos;
        idx = (idx + 1) % WINDOW;
        max = 0;
        for (int i = 0; i < count; i++) {
            if (buffer[i] > max) {
                max = buffer[i];
            }
        }
    }

    public long lastNanos() {
        return last;
    }

    public long avgNanos() {
        return count == 0 ? 0 : sum / count;
    }

    public long maxNanos() {
        return max;
    }
}
