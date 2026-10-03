package app.aemu.setup;

public final class RotationPolicyTest {
    public static class Gingerbread {
        public int value = 3;
        public int getRotation() { return value; }
        public void setRotation(int rotation, boolean config, int flags) {
            if (!config || flags != 0) throw new AssertionError("bad legacy args");
            value = rotation;
        }
    }
    public static class Ics {
        public int value;
        public int getRotation() { return value; }
        public void freezeRotation(int rotation) { value = rotation; }
    }
    public static class Nougat {
        public int value = 2;
        public int getDefaultDisplayRotation() { return value; }
        public void freezeRotation(int rotation) { value = rotation; }
    }
    public static class Denied extends Ics {
        public void freezeRotation(int rotation) { throw new SecurityException("permission denied"); }
    }
    public static void main(String[] args) throws Exception {
        Gingerbread gb = new Gingerbread(); Ics ics = new Ics(); Nougat n = new Nougat();
        if (RotationPolicy.rotate(gb, Gingerbread.class) != 0 || gb.value != 0) throw new AssertionError();
        if (RotationPolicy.rotate(ics, Ics.class) != 1 || ics.value != 1) throw new AssertionError();
        if (RotationPolicy.rotate(n, Nougat.class) != 3 || n.value != 3) throw new AssertionError();
        try { RotationPolicy.rotate(new Denied(), Denied.class); throw new AssertionError("swallowed failure"); }
        catch (java.lang.reflect.InvocationTargetException expected) {
            if (!(expected.getCause() instanceof SecurityException)) throw expected;
        }
        System.out.println("Rotation policy passed: Gingerbread, ICS, Nougat, failure propagation");
    }
}
