package framework;

/**
 *
 * @author Sander
 */
public class XorShiftRandom {

    private static final long DEFAULT_SEED = 88172645463325252L;
    private static long x;

    public XorShiftRandom() {
        this(DEFAULT_SEED);
    }

    public XorShiftRandom(long seed) {
        if (seed != 0) {
            x = seed;
        } else {
            x = DEFAULT_SEED;
        }
    }

    /**
     * Generate the next pseudo-random number in this generator's sequence.
     * The code is from http://www.javamex.com/tutorials/random_numbers/xorshift.shtml
     * @return
     */
    public long randomLong() {
        x ^= (x << 21);
        x ^= (x >>> 35);
        x ^= (x << 4);
        return x;
    }

    /**
     * Generates a random byte in the range [0, 15].
     * Returns the 4 lowest order bits from a random long.
     * @return
     */
    public byte next16() {
        // Take the first set of 4 bits
        return (byte) (randomLong() & 0xF);
    }

    /**
     * Generates a random boolean.
     * Returns the lowest order bit from the next random long.
     * @return
     */
    public boolean nextBool() {
        // Take the first bit. This is about twice as slow as next16()
        return (randomLong() & 0x1) != 0;
    }

    /*
    void test16() {
    Random r = new Random(x);
    long n = 200000000L;

    long[] count = new long[16];

    long start = System.currentTimeMillis();
    for (long i = 0; i < n; i++) {
    count[next16()]++;
    //count[r.nextInt(16)]++;
    }
    long end = System.currentTimeMillis();

    System.out.println("Distribution: ");
    for (int i = 0; i < 16; i++) {
    System.out.println(i + ": " + (count[i] / (double) n));
    }

    System.out.println(n + " calls took " + (end - start) + " ms. Average " + n / (double) (end - start) + " per ms.");
    }

    void testLong() {
    Random r = new Random(x);

    long n = 200000000L;

    long start = System.currentTimeMillis();
    for (long i = 0; i < n; i++) {
    //long l = randomLong();
    //long l = randomLong2();
    int l = r.nextInt(16);
    }
    long end = System.currentTimeMillis();

    System.out.println(n + " calls took " + (end - start) + " ms. Average " + n / (double) (end - start) + " per ms.");
    }

    void testBool() {
    Random r = new Random(x);

    long n = 200000000L;
    long t = 0, f = 0;

    long start = System.currentTimeMillis();
    for (long i = 0; i < n; i++) {
    boolean b = nextBool();
    //boolean b = r.nextBoolean();

    if (b) {
    t++;
    } else {
    f++;
    }
    }
    long end = System.currentTimeMillis();

    System.out.println("true: " + (t / (double) n));
    System.out.println("false: " + (f / (double) n));
    System.out.println(n + " calls took " + (end - start) + " ms. Average " + n / (double) (end - start) + " per ms.");
    }*/
}
