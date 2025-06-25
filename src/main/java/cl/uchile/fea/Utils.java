package cl.uchile.fea;

/**
 * The utilities.
 */
public final class Utils {

    /**
     * Gets the value of the environment as a string.
     * @param name The environment name
     * @param defVal The default value
     * @return The environment value
     */
    public static String getEnv(String name, String defVal) {
        String value = System.getenv(name);
        if (value != null && !value.isEmpty()) {
            return value;
        }

        return defVal;
    }

    /**
     * Gets the value of the environment as an integer.
     * @param name The environment name
     * @param defVal The default value
     * @return The environment value
     * @throws NumberFormatException if the string does not contain a parsable integer
     */
    public static int getEnv(String name, int defVal) throws NumberFormatException {
        String value = System.getenv(name);
        if (value != null && !value.isEmpty()) {
            return Integer.parseInt(value);
        }

        return defVal;
    }

    /**
     * Gets the value of the environment as an integer.
     * @param name The environment name
     * @param min The minimum value
     * @param max The maximum value
     * @param defVal The environment value
     * @return The environment value
     * @throws NumberFormatException if the string does not contain a parsable integer
     */
    public static int getEnv(String name, int min, int max, int defVal) throws NumberFormatException {
        int value = getEnv(name, defVal);
        if (value < min || value > max) {
            throw new NumberFormatException(String.format("value must be between %d and %d", min, max));
        }

        return value;
    }
}
