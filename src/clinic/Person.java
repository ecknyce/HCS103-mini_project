package clinic;

/** Base class for every person in the clinic. */
public abstract class Person {
    private final String name;
    private final int id;
    private static int constructorCount = 0;

    protected Person(String name, int id) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Name cannot be empty.");
        if (id <= 0) throw new IllegalArgumentException("ID must be positive.");
        this.name = name.trim();
        this.id = id;
        constructorCount++;
    }

    public final String getName() { return name; }
    public final int getId() { return id; }
    public abstract String role();

    public static int getConstructorCount() { return constructorCount; }
}
