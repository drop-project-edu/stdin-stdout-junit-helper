package pt.ulusofona.deisi;

public final class SystemUtil {

    private static final String SYSTEM_OS_NAME = "os.name";

    private static final String WINDOWS = "WINDOWS";

    public static String getOperatingSystemName() {
        return System.getProperty(SYSTEM_OS_NAME);
    }

    public static boolean isWindows() {
        return getOperatingSystemName().toUpperCase().startsWith(WINDOWS);
    }

}
